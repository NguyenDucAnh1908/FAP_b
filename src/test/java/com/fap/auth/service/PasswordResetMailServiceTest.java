package com.fap.auth.service;

import com.fap.common.i18n.MessageService;
import com.fap.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.env.MockEnvironment;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class PasswordResetMailServiceTest {

	private static final String OTP = "482915";
	private static final Locale VIETNAMESE = Locale.forLanguageTag("vi");

	@SuppressWarnings("unchecked")
	private final ObjectProvider<JavaMailSender> noMailSender = mock(ObjectProvider.class);
	// The real bundles: the test pins the exact text a user receives, not a stand-in.
	private final MessageService messageService = new MessageService(bundles());

	@Test
	@DisplayName("local profile logs the OTP when mail is disabled")
	void localProfileLogsOtp(CapturedOutput output) {
		service(noMailSender, "local").sendPasswordResetOtp(user(), OTP, 15, Locale.ENGLISH);

		assertThat(output.getOut()).contains(OTP);
	}

	@Test
	@DisplayName("other profiles never write the OTP to the log")
	void otherProfilesDoNotLogOtp(CapturedOutput output) {
		service(noMailSender, "prod").sendPasswordResetOtp(user(), OTP, 15, Locale.ENGLISH);

		assertThat(output.getOut()).doesNotContain(OTP).contains("not delivered");
	}

	@Test
	@DisplayName("the mail is rendered in the locale handed over by the request")
	@SuppressWarnings("unchecked")
	void mailsSubjectAndBodyInTheRequestedLocale() {
		JavaMailSender mailSender = mock(JavaMailSender.class);
		ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(mailSender);
		PasswordResetMailService service = service(provider, "prod");

		service.sendPasswordResetOtp(user(), OTP, 15, Locale.ENGLISH);
		service.sendPasswordResetOtp(user(), OTP, 15, VIETNAMESE);

		ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender, times(2)).send(sent.capture());
		SimpleMailMessage english = sent.getAllValues().get(0);
		assertThat(english.getFrom()).isEqualTo("no-reply@fap.local");
		assertThat(english.getTo()).containsExactly("trainee@fap.local");
		assertThat(english.getSubject()).isEqualTo("FAP password reset OTP");
		assertThat(english.getText()).isEqualTo("""
				Your FAP password reset OTP is: 482915

				This OTP expires in 15 minutes.
				If you did not request a password reset, please ignore this email.
				""");
		SimpleMailMessage vietnamese = sent.getAllValues().get(1);
		assertThat(vietnamese.getSubject()).isEqualTo("Mã OTP đặt lại mật khẩu FAP");
		assertThat(vietnamese.getText()).contains(OTP, "15 phút");
	}

	private PasswordResetMailService service(ObjectProvider<JavaMailSender> mailSenderProvider, String profile) {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles(profile);
		return new PasswordResetMailService(mailSenderProvider, messageService, true, "no-reply@fap.local", environment);
	}

	private static ResourceBundleMessageSource bundles() {
		ResourceBundleMessageSource source = new ResourceBundleMessageSource();
		source.setBasename("messages");
		source.setDefaultEncoding("UTF-8");
		source.setFallbackToSystemLocale(false);
		return source;
	}

	private static User user() {
		User user = new User();
		user.setId(42L);
		user.setEmail("trainee@fap.local");
		return user;
	}
}
