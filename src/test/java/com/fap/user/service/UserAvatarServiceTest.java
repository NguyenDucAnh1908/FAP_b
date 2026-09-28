package com.fap.user.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.NotFoundException;
import com.fap.common.metrics.DomainMetrics;
import com.fap.user.dto.AvatarDownload;
import com.fap.user.entity.UserAvatarContent;
import com.fap.user.repository.UserAvatarContentRepository;
import com.fap.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAvatarServiceTest {

	private static final Long USER_ID = 1000L;

	private final UserRepository userRepository = mock(UserRepository.class);
	private final UserAvatarContentRepository avatarContentRepository = mock(UserAvatarContentRepository.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	private final DomainMetrics domainMetrics = mock(DomainMetrics.class);
	private final UserAvatarService userAvatarService = new UserAvatarService(
			userRepository,
			avatarContentRepository,
			auditLogService,
			domainMetrics);

	@BeforeEach
	void useRealRepositoryLookups() {
		// The default lookups must run so the findById stubs below keep driving behaviour.
		lenient().doCallRealMethod().when(userRepository).getUserOrThrow(any());
		lenient().doCallRealMethod().when(avatarContentRepository).getAvatarContentOrThrow(any());
	}

	@Test
	void downloadReturnsStoredContent() {
		byte[] data = {1, 2, 3};
		UserAvatarContent content = new UserAvatarContent();
		content.setContentType("image/png");
		content.setFileData(data);
		when(avatarContentRepository.findById(USER_ID)).thenReturn(Optional.of(content));

		AvatarDownload download = userAvatarService.download(USER_ID);

		assertThat(download.contentType()).isEqualTo("image/png");
		assertThat(download.data()).isEqualTo(data);
	}

	@Test
	void downloadRejectsMissingAvatar() {
		when(avatarContentRepository.findById(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userAvatarService.download(USER_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Avatar not found");
	}

	@Test
	void uploadRejectsUnknownUserAndCountsFailure() {
		MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[] {1, 2, 3});
		when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userAvatarService.upload(USER_ID, file))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("User not found");
		verify(domainMetrics).recordUpload(false);
		verify(avatarContentRepository, never()).save(any());
	}
}
