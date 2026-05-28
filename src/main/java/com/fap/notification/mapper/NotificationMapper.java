package com.fap.notification.mapper;

import com.fap.notification.dto.NotificationResponse;
import com.fap.notification.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

	public NotificationResponse toResponse(Notification notification) {
		return new NotificationResponse(
				notification.getId(),
				notification.getTitle(),
				notification.getMessage(),
				notification.isRead(),
				notification.getCreatedAt());
	}
}
