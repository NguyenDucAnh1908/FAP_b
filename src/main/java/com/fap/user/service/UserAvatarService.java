package com.fap.user.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.NotFoundException;
import com.fap.common.metrics.DomainMetrics;
import com.fap.user.dto.AvatarDownload;
import com.fap.user.entity.User;
import com.fap.user.entity.UserAvatarContent;
import com.fap.user.repository.UserAvatarContentRepository;
import com.fap.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class UserAvatarService {

	private static final long MAX_AVATAR_SIZE_BYTES = 2L * 1024 * 1024;

	private static final Set<String> ALLOWED_AVATAR_CONTENT_TYPES = Set.of(
			"image/jpeg",
			"image/png",
			"image/webp");

	public static String avatarPath(Long userId) {
		return "/api/v1/users/" + userId + "/avatar";
	}

	private final UserRepository userRepository;
	private final UserAvatarContentRepository avatarContentRepository;
	private final AuditLogService auditLogService;
	private final DomainMetrics domainMetrics;

	public UserAvatarService(
			UserRepository userRepository,
			UserAvatarContentRepository avatarContentRepository,
			AuditLogService auditLogService,
			DomainMetrics domainMetrics) {
		this.userRepository = userRepository;
		this.avatarContentRepository = avatarContentRepository;
		this.auditLogService = auditLogService;
		this.domainMetrics = domainMetrics;
	}

	/**
	 * Replaces the caller's avatar. Only image/jpeg, image/png, and image/webp are accepted;
	 * maximum size is 2 MB. Upserts so a second upload simply overwrites the existing BLOB.
	 */
	@Transactional
	public void upload(Long currentUserId, MultipartFile file) {
		boolean counted = false;
		try {
			validateAvatarFile(file);
			User user = userRepository.findById(currentUserId)
					.orElseThrow(() -> new NotFoundException("User not found"));
			byte[] data;
			try {
				data = file.getBytes();
			} catch (IOException e) {
				domainMetrics.recordUpload(false);
				counted = true;
				throw new BadRequestException("FILE_READ_ERROR", "Failed to read uploaded file");
			}
			String contentType = file.getContentType().toLowerCase();
			// Upsert: reuse the existing row if present, create a new one otherwise
			UserAvatarContent content = avatarContentRepository.findById(currentUserId)
					.orElseGet(() -> {
						UserAvatarContent c = new UserAvatarContent();
						c.setUser(user);
						return c;
					});
			content.setFileData(data);
			content.setContentType(contentType);
			avatarContentRepository.save(content);
			user.setAvatarUrl(avatarPath(currentUserId));
			user.setUpdatedAt(LocalDateTime.now());
			auditLogService.record("UPLOAD_AVATAR", "user", currentUserId);
			domainMetrics.recordUpload(true);
			counted = true;
		} finally {
			// Validation failures, user-not-found, and persistence errors land here;
			// the IOException path above already counted before rethrowing.
			if (!counted) {
				domainMetrics.recordUpload(false);
			}
		}
	}

	@Transactional(readOnly = true)
	public AvatarDownload download(Long userId) {
		UserAvatarContent content = avatarContentRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("Avatar not found"));
		return new AvatarDownload(content.getContentType(), content.getFileData());
	}

	private void validateAvatarFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("FILE_REQUIRED", "A non-empty file is required");
		}
		if (file.getSize() > MAX_AVATAR_SIZE_BYTES) {
			throw new BadRequestException("FILE_TOO_LARGE", "Avatar exceeds the maximum allowed size of 2MB");
		}
		String contentType = file.getContentType();
		if (contentType == null || !ALLOWED_AVATAR_CONTENT_TYPES.contains(contentType.toLowerCase())) {
			throw new BadRequestException("FILE_TYPE_NOT_ALLOWED", "Avatar must be JPEG, PNG, or WebP");
		}
	}
}
