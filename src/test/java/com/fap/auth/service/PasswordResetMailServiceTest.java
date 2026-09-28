package com.fap.auth.service;

import com.fap.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(OutputCaptureExtension.class)
class PasswordResetMailServiceTest {

	private static final String OTP = "482915";

	@SuppressWarnings("unchecked")
	private final ObjectProvider<JavaMailSender> noMailSender = mock(ObjectProvider.class);

	@Test
	@DisplayName("local profile logs the OTP when mail is disabled")
	void localProfileLogsOtp(CapturedOutput output) {
		service("local").sendPasswordResetOtp(user(), OTP, 15);

		assertThat(output.getOut()).contains(OTP);
	}

	@Test
	@DisplayName("other profiles never write the OTP to the log")
	void otherProfilesDoNotLogOtp(CapturedOutput output) {
		service("prod").sendPasswordResetOtp(user(), OTP, 15);

		assertThat(output.getOut()).doesNotContain(OTP).contains("not delivered");
	}

	private PasswordResetMailService service(String profile) {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles(profile);
		return new PasswordResetMailService(noMailSender, false, "no-reply@fap.local", environment);
	}

	private static User user() {
		User user = new User();
		user.setId(42L);
		user.setEmail("trainee@fap.local");
		return user;
	}
}
