package com.fap.auth.service;

import com.fap.common.i18n.MessageService;
import com.fap.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class PasswordResetMailService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetMailService.class);

	private final ObjectProvider<JavaMailSender> mailSenderProvider;
	private final MessageService messageService;
	private final boolean mailEnabled;
	private final String fromAddress;
	private final boolean logOtpWhenMailDisabled;

	public PasswordResetMailService(
			ObjectProvider<JavaMailSender> mailSenderProvider,
			MessageService messageService,
			@Value("${app.mail.enabled:${MAIL_ENABLED:false}}") boolean mailEnabled,
			@Value("${app.mail.from:${MAIL_FROM:no-reply@fap.local}}") String fromAddress,
			Environment environment) {
		this.mailSenderProvider = mailSenderProvider;
		this.messageService = messageService;
		this.mailEnabled = mailEnabled;
		this.fromAddress = fromAddress;
		// Writing the OTP to the log is a local-development convenience. Anywhere else the log is
		// shipped to an aggregator, where the OTP would let a log reader reset any account.
		this.logOtpWhenMailDisabled = environment.acceptsProfiles(Profiles.of("local"));
	}

	/**
	 * {@code locale} is the requester's, handed over explicitly because this runs on the async
	 * executor, where the request's locale context does not exist.
	 */
	@Async
	public void sendPasswordResetOtp(User user, String otp, long ttlMinutes, Locale locale) {
		JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
		if (!mailEnabled || mailSender == null) {
			if (logOtpWhenMailDisabled) {
				LOGGER.info("Password reset OTP generated for email={} otp={}", user.getEmail(), otp);
			} else {
				LOGGER.warn("Password reset OTP not delivered for userId={}: mail is disabled", user.getId());
			}
			return;
		}

		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(fromAddress);
		message.setTo(user.getEmail());
		message.setSubject(messageService.get(locale, "mail.password_reset.subject"));
		// The minutes go in as text: MessageFormat would apply the locale's number grouping to a Long.
		message.setText(messageService.get(locale, "mail.password_reset.body", otp, Long.toString(ttlMinutes)));
		mailSender.send(message);
	}
}
