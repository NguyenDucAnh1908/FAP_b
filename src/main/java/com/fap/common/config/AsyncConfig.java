package com.fap.common.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync
@Configuration
public class AsyncConfig {

	/**
	 * Copies the MDC into the worker thread.
	 *
	 * <p>MDC is thread-local, so without this an {@code @Async} method loses the correlation id set
	 * by {@code RequestIdFilter}. That matters most for the mail send behind forgot-password: the
	 * failure lands on a pool thread, and an uncorrelated stack trace cannot be tied back to the
	 * request the user reports.
	 * </p>
	 */
	@Bean
	TaskDecorator mdcTaskDecorator() {
		return runnable -> {
			Map<String, String> callerContext = MDC.getCopyOfContextMap();
			return () -> {
				Map<String, String> previous = MDC.getCopyOfContextMap();
				if (callerContext != null) {
					MDC.setContextMap(callerContext);
				} else {
					MDC.clear();
				}
				try {
					runnable.run();
				} finally {
					// Pool threads are reused. Restore what was there rather than clearing, so a
					// nested submission does not strip context from its caller.
					if (previous != null) {
						MDC.setContextMap(previous);
					} else {
						MDC.clear();
					}
				}
			};
		};
	}

	/**
	 * Explicit executor for {@code @Async}. Without a {@code TaskExecutor} bean Spring falls back to
	 * {@code SimpleAsyncTaskExecutor}, which creates an unbounded number of threads: a burst of
	 * forgot-password requests would spawn one thread per call.
	 */
	@Bean
	ThreadPoolTaskExecutor taskExecutor(TaskDecorator mdcTaskDecorator) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(8);
		executor.setQueueCapacity(200);
		executor.setThreadNamePrefix("fap-async-");
		executor.setTaskDecorator(mdcTaskDecorator);
		// Run on the caller thread when the queue is full instead of dropping the task. A silently
		// discarded password-reset mail looks like a working request that never arrives.
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(20);
		return executor;
	}
}
