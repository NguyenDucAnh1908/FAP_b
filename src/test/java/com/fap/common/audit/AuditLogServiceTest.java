package com.fap.common.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditLogServiceTest {

	private final AuditLogRepository repository = mock(AuditLogRepository.class);
	private final AuditLogMapper mapper = mock(AuditLogMapper.class);

	@AfterEach
	void clearRequest() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	@DisplayName("ignores a client-supplied X-Forwarded-For unless the proxy is trusted")
	void ignoresForwardedForByDefault() {
		bindRequest("10.0.0.5", "1.2.3.4");

		new AuditLogService(repository, mapper, false).record("UPDATE_USER", "user", 1L);

		assertThat(savedLog().getIpAddress()).isEqualTo("10.0.0.5");
	}

	@Test
	@DisplayName("uses the left-most X-Forwarded-For entry behind a trusted proxy")
	void usesForwardedForBehindTrustedProxy() {
		bindRequest("10.0.0.5", "1.2.3.4, 10.0.0.1");

		new AuditLogService(repository, mapper, true).record("UPDATE_USER", "user", 1L);

		assertThat(savedLog().getIpAddress()).isEqualTo("1.2.3.4");
	}

	private void bindRequest(String remoteAddr, String forwardedFor) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRemoteAddr(remoteAddr);
		request.addHeader("X-Forwarded-For", forwardedFor);
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
	}

	private AuditLog savedLog() {
		ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
		verify(repository).save(captor.capture());
		return captor.getValue();
	}
}
