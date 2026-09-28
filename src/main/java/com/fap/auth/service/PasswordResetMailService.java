package com.fap.auth.service;

import com.fap.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetMailService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetMailService.class);

	private final ObjectProvider<JavaMailSender> mailSenderProvider;
	private final boolean mailEnabled;
	private final String fromAddress;
	private final boolean logOtpWhenMailDisabled;

	public PasswordResetMailService(
			ObjectProvider<JavaMailSender> mailSenderProvider,
			@Value("${app.mail.enabled:${MAIL_ENABLED:false}}") boolean mailEnabled,
			@Value("${app.mail.from:${MAIL_FROM:no-reply@fap.local}}") String fromAddress,
			Environment environment) {
		this.mailSenderProvider = mailSenderProvider;
		this.mailEnabled = mailEnabled;
		this.fromAddress = fromAddress;
		// Writing the OTP to the log is a local-development convenience. Anywhere else the log is
		// shipped to an aggregator, where the OTP would let a log reader reset any account.
		this.logOtpWhenMailDisabled = environment.acceptsProfiles(Profiles.of("local"));
	}

	public void sendPasswordResetOtp(User user, String otp, long ttlMinutes) {
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
		message.setSubject("FAP password reset OTP");
		message.setText("""
				Your FAP password reset OTP is: %s

				This OTP expires in %d minutes.
				If you did not request a password reset, please ignore this email.
				""".formatted(otp, ttlMinutes));
		mailSender.send(message);
	}
}
