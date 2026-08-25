package com.fap.training.service;

import com.fap.clazz.dto.ClassResponse;
import com.fap.clazz.entity.FapClass;
import com.fap.clazz.enums.ClassEnrollmentStatus;
import com.fap.clazz.mapper.ClassMapper;
import com.fap.clazz.repository.ClassRepository;
import com.fap.common.exception.NotFoundException;
import com.fap.common.api.PageRequestFactory;
import com.fap.program.entity.TrainingProgramSyllabus;
import com.fap.program.repository.TrainingProgramSyllabusRepository;
import com.fap.quiz.dto.AssignedQuizResponse;
import com.fap.quiz.entity.Quiz;
import com.fap.quiz.entity.QuizAttempt;
import com.fap.quiz.enums.QuizStatus;
import com.fap.quiz.mapper.QuizAttemptMapper;
import com.fap.quiz.repository.QuizAttemptRepository;
import com.fap.quiz.repository.QuizQuestionRepository;
import com.fap.quiz.repository.QuizRepository;
import com.fap.syllabus.dto.AssignedMaterialFileResponse;
import com.fap.syllabus.entity.MaterialFile;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.mapper.MaterialFileMapper;
import com.fap.syllabus.repository.MaterialFileRepository;
import com.fap.training.dto.MyClassDetailResponse;
import com.fap.training.dto.MyClassLearningContentResponse;
import com.fap.training.dto.MyClassProgressResponse;
import com.fap.training.dto.MyClassSyllabusResponse;
import com.fap.training.dto.MyTrainingSessionResponse;
import com.fap.training.entity.TrainingRegistration;
import com.fap.training.enums.AttendanceStatus;
import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.mapper.MyTrainingMapper;
import com.fap.training.repository.AttendanceRecordRepository;
import com.fap.training.repository.TrainingRegistrationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Service
public class MyLearningService {

	private static final Collection<ClassEnrollmentStatus> ELIGIBLE_CLASS_ENROLLMENT_STATUSES = List.of(
			ClassEnrollmentStatus.Enrolled,
			ClassEnrollmentStatus.Completed);
	private static final Collection<TrainingRegistrationStatus> ELIGIBLE_REGISTRATION_STATUSES = List.of(
			TrainingRegistrationStatus.Registered,
			TrainingRegistrationStatus.Completed);

	private final ClassRepository classRepository;
	private final TrainingRegistrationRepository trainingRegistrationRepository;
	private final AttendanceRecordRepository attendanceRecordRepository;
	private final TrainingProgramSyllabusRepository trainingProgramSyllabusRepository;
	private final MaterialFileRepository materialFileRepository;
	private final QuizRepository quizRepository;
	private final QuizQuestionRepository quizQuestionRepository;
	private final QuizAttemptRepository quizAttemptRepository;
	private final ClassMapper classMapper;
	private final MyTrainingMapper myTrainingMapper;
	private final MaterialFileMapper materialFileMapper;
	private final QuizAttemptMapper quizAttemptMapper;

	public MyLearningService(
			ClassRepository classRepository,
			TrainingRegistrationRepository trainingRegistrationRepository,
			AttendanceRecordRepository attendanceRecordRepository,
			TrainingProgramSyllabusRepository trainingProgramSyllabusRepository,
			MaterialFileRepository materialFileRepository,
			QuizRepository quizRepository,
			QuizQuestionRepository quizQuestionRepository,
			QuizAttemptRepository quizAttemptRepository,
			ClassMapper classMapper,
			MyTrainingMapper myTrainingMapper,
			MaterialFileMapper materialFileMapper,
			QuizAttemptMapper quizAttemptMapper) {
		this.classRepository = classRepository;
		this.trainingRegistrationRepository = trainingRegistrationRepository;
		this.attendanceRecordRepository = attendanceRecordRepository;
		this.trainingProgramSyllabusRepository = trainingProgramSyllabusRepository;
		this.materialFileRepository = materialFileRepository;
		this.quizRepository = quizRepository;
		this.quizQuestionRepository = quizQuestionRepository;
		this.quizAttemptRepository = quizAttemptRepository;
		this.classMapper = classMapper;
		this.myTrainingMapper = myTrainingMapper;
		this.materialFileMapper = materialFileMapper;
		this.quizAttemptMapper = quizAttemptMapper;
	}

	@Transactional(readOnly = true)
	public Page<ClassResponse> classes(Long currentUserId, String keyword, int page, int limit) {
		return classes(currentUserId, keyword, page, limit, null, null);
	}

	@Transactional(readOnly = true)
	public Page<ClassResponse> classes(
			Long currentUserId,
			String keyword,
			int page,
			int limit,
			String sortBy,
			String order) {
		PageRequest pageRequest = PageRequestFactory.create(
				page,
				limit,
				sortBy,
				order,
				Sort.by(Sort.Direction.DESC, "createdAt"),
				"id", "createdAt", "name", "classCode", "startDate", "endDate", "status");
		return classRepository
				.searchMine(currentUserId, ELIGIBLE_CLASS_ENROLLMENT_STATUSES, normalize(keyword), pageRequest)
				.map(classMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public MyClassDetailResponse classDetail(Long classId, Long currentUserId) {
		FapClass fapClass = findMyClass(classId, currentUserId);
		return new MyClassDetailResponse(
				classMapper.toResponse(fapClass),
				syllabuses(fapClass));
	}

	@Transactional(readOnly = true)
	public MyClassLearningContentResponse learningContent(
			Long classId,
			Long currentUserId,
			String keyword) {
		FapClass fapClass = findMyClass(classId, currentUserId);
		String normalizedKeyword = normalize(keyword);
		List<MyTrainingSessionResponse> sessions = trainingRegistrationRepository
				.findMineByClassId(currentUserId, classId, ELIGIBLE_REGISTRATION_STATUSES)
				.stream()
				.map(myTrainingMapper::toSessionResponse)
				.toList();
		List<AssignedMaterialFileResponse> materials = materialFileRepository
				.findAssignedToUserByClass(currentUserId, classId, ELIGIBLE_CLASS_ENROLLMENT_STATUSES, normalizedKeyword)
				.stream()
				.sorted(Comparator
						.comparing(MaterialFile::getUploadedAt, Comparator.nullsLast(Comparator.reverseOrder()))
						.thenComparing(MaterialFile::getId, Comparator.reverseOrder()))
				.map(materialFileMapper::toAssignedResponse)
				.toList();
		List<AssignedQuizResponse> quizzes = assignedQuizzes(currentUserId, classId)
				.stream()
				.map(quiz -> toAssignedQuiz(quiz, currentUserId))
				.toList();
		return new MyClassLearningContentResponse(
				classMapper.toResponse(fapClass),
				syllabuses(fapClass),
				sessions,
				materials,
				quizzes);
	}

	@Transactional(readOnly = true)
	public MyClassProgressResponse progress(Long classId, Long currentUserId) {
		FapClass fapClass = findMyClass(classId, currentUserId);
		List<TrainingRegistration> registrations = trainingRegistrationRepository
				.findMineByClassId(currentUserId, classId, ELIGIBLE_REGISTRATION_STATUSES);
		List<MaterialFile> materials = materialFileRepository
				.findAssignedToUserByClass(currentUserId, classId, ELIGIBLE_CLASS_ENROLLMENT_STATUSES, null);
		List<Quiz> quizzes = assignedQuizzes(currentUserId, classId);
		QuizAttempt latestAttempt = latestAttempt(currentUserId, quizzes);

		return new MyClassProgressResponse(
				classMapper.toResponse(fapClass),
				sessionProgress(registrations),
				attendanceProgress(currentUserId, classId),
				new MyClassProgressResponse.MaterialProgress(materials.size()),
				quizProgress(currentUserId, quizzes, latestAttempt));
	}

	private FapClass findMyClass(Long classId, Long currentUserId) {
		return classRepository.findMineById(classId, currentUserId, ELIGIBLE_CLASS_ENROLLMENT_STATUSES)
				.orElseThrow(() -> new NotFoundException("Class not found"));
	}

	private List<MyClassSyllabusResponse> syllabuses(FapClass fapClass) {
		return trainingProgramSyllabusRepository
				.findByIdProgramIdOrderBySortOrderAsc(fapClass.getTrainingProgram().getId())
				.stream()
				.map(this::toSyllabusResponse)
				.toList();
	}

	private MyClassSyllabusResponse toSyllabusResponse(TrainingProgramSyllabus programSyllabus) {
		Syllabus syllabus = programSyllabus.getSyllabus();
		return new MyClassSyllabusResponse(
				syllabus.getId(),
				syllabus.getName(),
				syllabus.getCode(),
				syllabus.getVersion(),
				syllabus.getStatus(),
				syllabus.getLevelName(),
				syllabus.getDuration(),
				programSyllabus.getSortOrder());
	}

	private AssignedQuizResponse toAssignedQuiz(Quiz quiz, Long currentUserId) {
		long attemptCount = quizAttemptRepository.countByQuizIdAndUserId(quiz.getId(), currentUserId);
		QuizAttempt latestAttempt = quizAttemptRepository
				.findFirstByQuizIdAndUserIdOrderByIdDesc(quiz.getId(), currentUserId)
				.orElse(null);
		return quizAttemptMapper.toAssignedResponse(
				quiz,
				quizQuestionRepository.countByIdQuizId(quiz.getId()),
				attemptCount,
				latestAttempt);
	}

	private List<Quiz> assignedQuizzes(Long currentUserId, Long classId) {
		return quizRepository
				.findAssignedToUserByClass(
						currentUserId,
						classId,
						QuizStatus.Published,
						ELIGIBLE_REGISTRATION_STATUSES,
						LocalDate.now())
				.stream()
				.sorted(Comparator.comparing(Quiz::getCloseDate, Comparator.nullsLast(Comparator.naturalOrder()))
						.thenComparing(Quiz::getId, Comparator.reverseOrder()))
				.toList();
	}

	private MyClassProgressResponse.SessionProgress sessionProgress(List<TrainingRegistration> registrations) {
		long completed = countSessions(registrations, TrainingSessionStatus.Completed);
		long upcoming = countSessions(registrations, TrainingSessionStatus.Upcoming);
		long canceled = countSessions(registrations, TrainingSessionStatus.Canceled);
		return new MyClassProgressResponse.SessionProgress(registrations.size(), completed, upcoming, canceled);
	}

	private long countSessions(List<TrainingRegistration> registrations, TrainingSessionStatus status) {
		return registrations.stream()
				.filter(registration -> registration.getTrainingSession().getStatus() == status)
				.count();
	}

	private MyClassProgressResponse.AttendanceProgress attendanceProgress(Long currentUserId, Long classId) {
		return new MyClassProgressResponse.AttendanceProgress(
				attendanceRecordRepository.countMineByClassId(currentUserId, classId, AttendanceStatus.Present),
				attendanceRecordRepository.countMineByClassId(currentUserId, classId, AttendanceStatus.Late),
				attendanceRecordRepository.countMineByClassId(currentUserId, classId, AttendanceStatus.Absent));
	}

	private MyClassProgressResponse.QuizProgress quizProgress(
			Long currentUserId,
			List<Quiz> quizzes,
			QuizAttempt latestAttempt) {
		long attempted = quizzes.stream()
				.filter(quiz -> quizAttemptRepository.countByQuizIdAndUserId(quiz.getId(), currentUserId) > 0)
				.count();
		long passed = quizzes.stream()
				.filter(quiz -> quizAttemptRepository.countByQuizIdAndUserIdAndPassed(quiz.getId(), currentUserId, true) > 0)
				.count();
		return new MyClassProgressResponse.QuizProgress(
				quizzes.size(),
				attempted,
				passed,
				Math.max(quizzes.size() - attempted, 0),
				latestAttempt == null ? null : latestAttempt.getId(),
				latestAttempt == null ? null : latestAttempt.getScore(),
				latestAttempt == null ? null : latestAttempt.getPassed());
	}

	private QuizAttempt latestAttempt(Long currentUserId, List<Quiz> quizzes) {
		return quizzes.stream()
				.map(quiz -> quizAttemptRepository
						.findFirstByQuizIdAndUserIdOrderByIdDesc(quiz.getId(), currentUserId)
						.orElse(null))
				.filter(attempt -> attempt != null)
				.max(Comparator.comparing(QuizAttempt::getStartedAt)
						.thenComparing(QuizAttempt::getId))
				.orElse(null);
	}

	private String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
