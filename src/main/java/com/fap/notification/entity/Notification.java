package com.fap.notification.entity;

import com.fap.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "notifications")
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notifications_seq")
	@SequenceGenerator(name = "notifications_seq", sequenceName = "notifications_seq", allocationSize = 50)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private String title;

	@Lob
	@Column(nullable = false)
	private String message;

	/**
	 * Message keys and their arguments, rendered in the reader's language when the notification is
	 * read. Null on rows written before V34; those only have the English title/message above.
	 */
	@Column(name = "title_key", length = 100)
	private String titleKey;

	@Column(name = "message_key", length = 100)
	private String messageKey;

	/** JSON array of strings, one per {@code {n}} placeholder of the message key. */
	@Lob
	@Column(name = "message_args")
	private String messageArgs;

	@Column(name = "is_read", nullable = false)
	private boolean read;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;
}
