package com.fap.training.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.training.dto.CreateTrainingFeedbackRequest;
import com.fap.training.dto.TrainingFeedbackResponse;
import com.fap.training.dto.TrainingFeedbackSummaryResponse;
import com.fap.training.entity.TrainingFeedback;
import com.fap.training.entity.TrainingRegistration;
import com.fap.training.entity.TrainingSession;
import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.mapper.TrainingFeedbackMapper;
import com.fap.training.repository.TrainingFeedbackRepository;
import com.fap.training.repository.TrainingRegistrationRepository;
import com.fap.training.repository.TrainingSessionRepository;
import com.fap.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Feedback is one-shot and only for people who actually took part: it feeds trainer and session
 * ratings, so a second submission, a submission for a session that has not happened, or one from a
 * waitlisted or cancelled trainee would skew those numbers with no way to take it back. The checks
 * run in a fixed order (session, completion, duplicate, registration, eligibility), and the tests pin
 * that order because it decides which error a caller sees.
 */
class TrainingFeedbackServiceTest {

	private static final long SESSION_ID = 61L;
	private static final long USER_ID = 500L;
	private static final long OTHER_USER_ID = 501L;
	private static final long SAVED_FEEDBACK_ID = 9001L;

	private final TrainingFeedbackRepository trainingFeedbackRepository = mock(TrainingFeedbackRepository.class);
	private final TrainingSessionRepository trainingSessionRepository = mock(TrainingSessionRepository.class);
	private final TrainingRegistrationRepository trainingRegistrationRepository =
			mock(TrainingRegistrationRepository.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);

	// The mapper has no collaborators, so the real one lets the tests assert the returned response.
	private final TrainingFeedbackService service = new TrainingFeedbackService(
			trainingFeedbackRepository,
			trainingSessionRepository,
			trainingRegistrationRepository,
			new TrainingFeedbackMapper(),
			auditLogService);

	@BeforeEach
	void returnSavedEntityWithId() {
		when(trainingFeedbackRepository.save(any())).thenAnswer(invocation -> {
			TrainingFeedback feedback = invocation.getArgument(0);
			feedback.setId(SAVED_FEEDBACK_ID);
			return feedback;
		});
	}

	@BeforeEach
	void delegateLookupsToFinders() {
		lenient().doCallRealMethod().when(trainingSessionRepository).getWithClassAndTrainerOrThrow(any());
		// Not called by the service today; wiring it keeps a refactor onto it from failing with a
		// null registration instead of the NotFoundException it would really throw.
		lenient().doCallRealMethod().when(trainingRegistrationRepository)
				.getByTrainingSessionIdAndUserIdOrThrow(any(), any());
	}

	// --- submit: allowed ------------------------------------------------------------------------

	@ParameterizedTest(name = "{0} participant can submit feedback")
	@EnumSource(value = TrainingRegistrationStatus.class, names = {"Registered", "Completed"})
	void submitSavesFeedbackForEligibleParticipant(TrainingRegistrationStatus status) {
		TrainingSession session = givenSession(TrainingSessionStatus.Completed);
		givenNoFeedbackYet();
		TrainingRegistration registration = givenRegistration(status);
		LocalDateTime before = LocalDateTime.now();

		TrainingFeedbackResponse response = service.submit(SESSION_ID, USER_ID, request("Clear and practical"));

		LocalDateTime after = LocalDateTime.now();
		TrainingFeedback saved = captureSaved();
		assertThat(saved.getTrainingSession()).isSameAs(session);
		assertThat(saved.getUser()).isSameAs(registration.getUser());
		assertThat(saved.getRatingContent()).isEqualTo(5);
		assertThat(saved.getRatingTrainer()).isEqualTo(4);
		assertThat(saved.getRatingOrganization()).isEqualTo(3);
		assertThat(saved.getComment()).isEqualTo("Clear and practical");
		assertThat(saved.getCreatedAt()).isBetween(before, after);
		assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
		verify(auditLogService).record("SUBMIT_TRAINING_FEEDBACK", "training_session", SESSION_ID);

		assertThat(response.id()).isEqualTo(SAVED_FEEDBACK_ID);
		assertThat(response.trainingSessionId()).isEqualTo(SESSION_ID);
		assertThat(response.userId()).isEqualTo(USER_ID);
		assertThat(response.ratingContent()).isEqualTo(5);
		assertThat(response.ratingTrainer()).isEqualTo(4);
		assertThat(response.ratingOrganization()).isEqualTo(3);
	}

	@Test
	void submitTrimsComment() {
		givenSession(TrainingSessionStatus.Completed);
		givenNoFeedbackYet();
		givenRegistration(TrainingRegistrationStatus.Completed);

		service.submit(SESSION_ID, USER_ID, request("  great session  "));

		assertThat(captureSaved().getComment()).isEqualTo("great session");
	}

	@ParameterizedTest(name = "comment [{0}] is stored as null")
	@NullAndEmptySource
	@ValueSource(strings = {"   ", "\t\n"})
	void submitStoresBlankCommentAsNull(String comment) {
		givenSession(TrainingSessionStatus.Completed);
		givenNoFeedbackYet();
		givenRegistration(TrainingRegistrationStatus.Completed);

		service.submit(SESSION_ID, USER_ID, request(comment));

		assertThat(captureSaved().getComment()).isNull();
	}

	/**
	 * Every lookup is keyed by the caller, so a trainee cannot submit on the strength of somebody
	 * else's registration or be blocked by somebody else's earlier feedback.
	 */
	@Test
	void submitChecksTheCallersOwnFeedbackAndRegistration() {
		givenSession(TrainingSessionStatus.Completed);
		when(trainingFeedbackRepository.existsByTrainingSessionIdAndUserId(SESSION_ID, OTHER_USER_ID))
				.thenReturn(true);
		when(trainingFeedbackRepository.existsByTrainingSessionIdAndUserId(SESSION_ID, USER_ID))
				.thenReturn(false);
		givenRegistration(TrainingRegistrationStatus.Registered);

		service.submit(SESSION_ID, USER_ID, request(null));

		verify(trainingFeedbackRepository).existsByTrainingSessionIdAndUserId(SESSION_ID, USER_ID);
		verify(trainingRegistrationRepository).findByTrainingSessionIdAndUserId(SESSION_ID, USER_ID);
		assertThat(captureSaved().getUser().getId()).isEqualTo(USER_ID);
	}

	// --- submit: rejected -----------------------------------------------------------------------

	@Test
	void submitRejectsUnknownSession() {
		when(trainingSessionRepository.findWithClassAndTrainerById(SESSION_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.submit(SESSION_ID, USER_ID, request("late")))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Training session not found")
				.extracting("code")
				.isEqualTo("RESOURCE_NOT_FOUND");

		verify(trainingFeedbackRepository, never()).existsByTrainingSessionIdAndUserId(anyLong(), anyLong());
		verify(trainingFeedbackRepository, never()).save(any());
		verifyNoInteractions(trainingRegistrationRepository, auditLogService);
	}

	/** Rating a session that has not taken place, or never will, would publish made-up scores. */
	@ParameterizedTest(name = "{0} session does not accept feedback")
	@EnumSource(value = TrainingSessionStatus.class, names = {"Upcoming", "Canceled"})
	void submitRejectsSessionThatIsNotCompleted(TrainingSessionStatus status) {
		givenSession(status);

		assertThatThrownBy(() -> service.submit(SESSION_ID, USER_ID, request("too early")))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("FEEDBACK_SESSION_NOT_COMPLETED");

		verify(trainingFeedbackRepository, never()).existsByTrainingSessionIdAndUserId(anyLong(), anyLong());
		verify(trainingFeedbackRepository, never()).save(any());
		verifyNoInteractions(trainingRegistrationRepository, auditLogService);
	}

	@Test
	void submitRejectsSecondFeedbackFromSameParticipant() {
		givenSession(TrainingSessionStatus.Completed);
		when(trainingFeedbackRepository.existsByTrainingSessionIdAndUserId(SESSION_ID, USER_ID)).thenReturn(true);
		givenRegistration(TrainingRegistrationStatus.Completed);

		assertThatThrownBy(() -> service.submit(SESSION_ID, USER_ID, request("again")))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("FEEDBACK_ALREADY_SUBMITTED");

		verify(trainingFeedbackRepository, never()).save(any());
		verifyNoInteractions(auditLogService);
	}

	/**
	 * The duplicate check runs before the registration lookup, so a caller who already gave feedback
	 * is told so even when their registration has since disappeared.
	 */
	@Test
	void submitReportsDuplicateBeforeLookingUpRegistration() {
		givenSession(TrainingSessionStatus.Completed);
		when(trainingFeedbackRepository.existsByTrainingSessionIdAndUserId(SESSION_ID, USER_ID)).thenReturn(true);
		when(trainingRegistrationRepository.findByTrainingSessionIdAndUserId(SESSION_ID, USER_ID))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.submit(SESSION_ID, USER_ID, request("again")))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("FEEDBACK_ALREADY_SUBMITTED");

		verifyNoInteractions(trainingRegistrationRepository);
	}

	/** A missing registration is a business conflict (409), not a missing resource (404). */
	@Test
	void submitRequiresRegistration() {
		givenSession(TrainingSessionStatus.Completed);
		givenNoFeedbackYet();
		when(trainingRegistrationRepository.findByTrainingSessionIdAndUserId(SESSION_ID, USER_ID))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.submit(SESSION_ID, USER_ID, request("drive-by")))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Only registered participants can submit feedback")
				.extracting("code")
				.isEqualTo("FEEDBACK_REGISTRATION_REQUIRED");

		verify(trainingFeedbackRepository, never()).save(any());
		verifyNoInteractions(auditLogService);
	}

	@Test
	void submitDoesNotAcceptAnotherParticipantsRegistration() {
		givenSession(TrainingSessionStatus.Completed);
		givenNoFeedbackYet();
		TrainingRegistration othersRegistration = registration(OTHER_USER_ID, TrainingRegistrationStatus.Registered);
		when(trainingRegistrationRepository.findByTrainingSessionIdAndUserId(SESSION_ID, OTHER_USER_ID))
				.thenReturn(Optional.of(othersRegistration));
		when(trainingRegistrationRepository.findByTrainingSessionIdAndUserId(SESSION_ID, USER_ID))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.submit(SESSION_ID, USER_ID, request("not mine")))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("FEEDBACK_REGISTRATION_REQUIRED");

		verify(trainingFeedbackRepository, never()).save(any());
	}

	/** Waitlisted trainees never got a seat and cancelled ones left, so neither attended the session. */
	@ParameterizedTest(name = "{0} registration cannot submit feedback")
	@EnumSource(value = TrainingRegistrationStatus.class, names = {"Waitlist", "Cancelled"})
	void submitRejectsIneligibleRegistration(TrainingRegistrationStatus status) {
		givenSession(TrainingSessionStatus.Completed);
		givenNoFeedbackYet();
		givenRegistration(status);

		assertThatThrownBy(() -> service.submit(SESSION_ID, USER_ID, request("was not there")))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("FEEDBACK_REGISTRATION_NOT_ELIGIBLE");

		verify(trainingFeedbackRepository, never()).save(any());
		verifyNoInteractions(auditLogService);
	}

	// --- summary --------------------------------------------------------------------------------

	@Test
	void summaryRejectsUnknownSession() {
		when(trainingSessionRepository.existsById(SESSION_ID)).thenReturn(false);

		assertThatThrownBy(() -> service.summary(SESSION_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Training session not found")
				.extracting("code")
				.isEqualTo("RESOURCE_NOT_FOUND");

		verify(trainingFeedbackRepository, never()).summarizeByTrainingSessionId(anyLong());
	}

	@Test
	void summaryAveragesTheThreeRatingAverages() {
		givenSummary(4L, 4.0, 3.0, 5.0);

		TrainingFeedbackSummaryResponse summary = service.summary(SESSION_ID);

		assertThat(summary.trainingSessionId()).isEqualTo(SESSION_ID);
		assertThat(summary.feedbackCount()).isEqualTo(4L);
		assertThat(summary.averageContentRating()).isEqualTo(4.0);
		assertThat(summary.averageTrainerRating()).isEqualTo(3.0);
		assertThat(summary.averageOrganizationRating()).isEqualTo(5.0);
		assertThat(summary.overallAverageRating()).isEqualTo(4.0);
	}

	/** SQL {@code avg} over no rows is null, and some drivers also return a null count. */
	@ParameterizedTest(name = "feedback count [{0}] yields an all-zero summary")
	@NullSource
	@ValueSource(longs = 0L)
	void summaryReturnsZerosWithoutFeedback(Long feedbackCount) {
		givenSummary(feedbackCount, null, null, null);

		TrainingFeedbackSummaryResponse summary = service.summary(SESSION_ID);

		assertThat(summary.feedbackCount()).isZero();
		assertThat(summary.averageContentRating()).isEqualTo(0.0);
		assertThat(summary.averageTrainerRating()).isEqualTo(0.0);
		assertThat(summary.averageOrganizationRating()).isEqualTo(0.0);
		assertThat(summary.overallAverageRating()).isEqualTo(0.0);
	}

	/**
	 * Pins current behaviour: nothing is rounded here, whereas the admin dashboard rounds to one
	 * decimal and the quiz summary to two. Unifying that is a deliberate change, not a refactor.
	 */
	@Test
	void summaryDoesNotRoundAverages() {
		double contentAverage = 13.0 / 3;
		givenSummary(3L, contentAverage, 4.0, 5.0);

		TrainingFeedbackSummaryResponse summary = service.summary(SESSION_ID);

		assertThat(summary.averageContentRating()).isEqualTo(contentAverage);
		assertThat(summary.overallAverageRating()).isEqualTo((contentAverage + 4.0 + 5.0) / 3);
	}

	// --- listMine -------------------------------------------------------------------------------

	@Test
	void listMineDefaultsToNewestFirstAndOnlyReadsCallersFeedback() {
		TrainingFeedback feedback = feedback();
		when(trainingFeedbackRepository.findByUserId(eq(USER_ID), any()))
				.thenReturn(new PageImpl<>(List.of(feedback)));

		Page<TrainingFeedbackResponse> page = service.listMine(USER_ID, 2, 10);

		Pageable pageable = captureListPageable();
		assertThat(pageable.getPageNumber()).isEqualTo(2);
		assertThat(pageable.getPageSize()).isEqualTo(10);
		assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
		assertThat(page.getContent())
				.singleElement()
				.satisfies(response -> {
					assertThat(response.id()).isEqualTo(feedback.getId());
					assertThat(response.userId()).isEqualTo(USER_ID);
					assertThat(response.trainingSessionId()).isEqualTo(SESSION_ID);
				});
	}

	@Test
	void listMineSortsByAllowedField() {
		when(trainingFeedbackRepository.findByUserId(eq(USER_ID), any())).thenReturn(Page.empty());

		service.listMine(USER_ID, 0, 20, " ratingTrainer ", "asc");

		assertThat(captureListPageable().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "ratingTrainer"));
	}

	@Test
	void listMineRejectsUnknownSortField() {
		assertThatThrownBy(() -> service.listMine(USER_ID, 0, 20, "user.email", "asc"))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("INVALID_SORT_FIELD");

		verify(trainingFeedbackRepository, never()).findByUserId(anyLong(), any());
	}

	@Test
	void listMineRejectsUnknownSortOrder() {
		assertThatThrownBy(() -> service.listMine(USER_ID, 0, 20, "createdAt", "sideways"))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("INVALID_SORT_ORDER");

		verify(trainingFeedbackRepository, never()).findByUserId(anyLong(), any());
	}

	// --- fixtures -------------------------------------------------------------------------------

	private TrainingSession givenSession(TrainingSessionStatus status) {
		TrainingSession session = session();
		session.setStatus(status);
		when(trainingSessionRepository.findWithClassAndTrainerById(SESSION_ID)).thenReturn(Optional.of(session));
		return session;
	}

	private void givenNoFeedbackYet() {
		when(trainingFeedbackRepository.existsByTrainingSessionIdAndUserId(SESSION_ID, USER_ID)).thenReturn(false);
	}

	private TrainingRegistration givenRegistration(TrainingRegistrationStatus status) {
		TrainingRegistration registration = registration(USER_ID, status);
		when(trainingRegistrationRepository.findByTrainingSessionIdAndUserId(SESSION_ID, USER_ID))
				.thenReturn(Optional.of(registration));
		return registration;
	}

	private void givenSummary(Long count, Double content, Double trainer, Double organization) {
		when(trainingSessionRepository.existsById(SESSION_ID)).thenReturn(true);
		TrainingFeedbackRepository.FeedbackSummary summary = mock(TrainingFeedbackRepository.FeedbackSummary.class);
		when(summary.getFeedbackCount()).thenReturn(count);
		when(summary.getAverageContentRating()).thenReturn(content);
		when(summary.getAverageTrainerRating()).thenReturn(trainer);
		when(summary.getAverageOrganizationRating()).thenReturn(organization);
		when(trainingFeedbackRepository.summarizeByTrainingSessionId(SESSION_ID)).thenReturn(summary);
	}

	private TrainingSession session() {
		TrainingSession session = new TrainingSession();
		session.setId(SESSION_ID);
		session.setTitle("Spring transactions deep dive");
		return session;
	}

	private TrainingRegistration registration(long userId, TrainingRegistrationStatus status) {
		TrainingRegistration registration = new TrainingRegistration();
		registration.setId(300L + userId);
		registration.setUser(user(userId));
		registration.setStatus(status);
		return registration;
	}

	private User user(long id) {
		User user = new User();
		user.setId(id);
		user.setFullName("Trainee " + id);
		user.setEmail("user" + id + "@fap.local");
		return user;
	}

	private TrainingFeedback feedback() {
		TrainingFeedback feedback = new TrainingFeedback();
		feedback.setId(77L);
		feedback.setTrainingSession(session());
		feedback.setUser(user(USER_ID));
		feedback.setRatingContent(4);
		feedback.setRatingTrainer(4);
		feedback.setRatingOrganization(5);
		return feedback;
	}

	private CreateTrainingFeedbackRequest request(String comment) {
		return new CreateTrainingFeedbackRequest(5, 4, 3, comment);
	}

	private TrainingFeedback captureSaved() {
		ArgumentCaptor<TrainingFeedback> captor = ArgumentCaptor.forClass(TrainingFeedback.class);
		verify(trainingFeedbackRepository).save(captor.capture());
		return captor.getValue();
	}

	private Pageable captureListPageable() {
		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		verify(trainingFeedbackRepository).findByUserId(eq(USER_ID), captor.capture());
		return captor.getValue();
	}
}
