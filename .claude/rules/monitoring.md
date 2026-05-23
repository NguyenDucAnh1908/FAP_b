# Monitoring And Observability

## Tools

- Health and runtime endpoints: Spring Boot Actuator
- Metrics: Micrometer + Prometheus registry when configured
- Logs: Logback, structured JSON when configured
- Tracing: OpenTelemetry when infrastructure is ready

## Required Signals

- HTTP request rate, error count, and duration.
- Database connection pool usage.
- Flyway migration status at startup.
- Authentication failure count.
- Quiz attempt submission latency.
- Training registration conflict and waitlist count.
- File upload success and failure count.

## Logging Rules

- Use appropriate levels: `error`, `warn`, `info`, `debug`.
- Do not use `System.out.println` for application logging.
- Do not log passwords, tokens, authorization headers, or sensitive PII.
- Include request ID once request correlation is implemented.
- Log unexpected errors server-side, but return generic `INTERNAL_ERROR` to clients.

## Actuator

Expose only safe endpoints by default:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

Do not expose environment, beans, heap dumps, or thread dumps publicly.
