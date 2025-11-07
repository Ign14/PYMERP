package com.datakomerz.pymes.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Tests para verificar que el logging MDC funciona correctamente.
 * Valida que los campos de contexto (traceId, userId, companyId) se incluyen en los logs.
 */
class LoggingTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger("test.logger");
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        listAppender.stop();
    }

    @Test
    void shouldIncludeMDCFieldsInLog() {
        // Given: MDC con campos de contexto
        MDC.put("traceId", "test-trace-123");
        MDC.put("userId", "test-user@example.com");
        MDC.put("companyId", "42");

        // When: Se emite un log
        logger.info("Test message with MDC context");

        // Then: El log debe contener los campos MDC
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent event = listAppender.list.get(0);
        assertThat(event.getMDCPropertyMap())
                .containsEntry("traceId", "test-trace-123")
                .containsEntry("userId", "test-user@example.com")
                .containsEntry("companyId", "42");
        assertThat(event.getMessage()).isEqualTo("Test message with MDC context");
    }

    @Test
    void shouldHandleMissingMDCFields() {
        // Given: MDC vacío
        MDC.clear();

        // When: Se emite un log sin contexto
        logger.info("Test message without MDC");

        // Then: El log debe existir sin errores
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent event = listAppender.list.get(0);
        assertThat(event.getMDCPropertyMap()).isEmpty();
    }

    @Test
    void shouldClearMDCAfterRequest() {
        // Given: MDC con datos
        MDC.put("traceId", "trace-to-clear");
        MDC.put("userId", "user-to-clear");

        // When: Se limpia el MDC
        MDC.clear();

        // Then: MDC debe estar vacío
        logger.info("Message after clear");
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent event = listAppender.list.get(0);
        assertThat(event.getMDCPropertyMap()).isEmpty();
    }
}
