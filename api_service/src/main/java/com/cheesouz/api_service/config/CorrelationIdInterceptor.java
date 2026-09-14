package com.cheesouz.api_service.config;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CorrelationIdInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdInterceptor.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String MDC_KEY = "correlationId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            log.info("[X-Correlation-ID] generated new correlation id: {}", correlationId);
        } else {
            log.info("[X-Correlation-ID] received correlation id: {}", correlationId);
        }

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        CorrelationIdHolder.set(correlationId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CorrelationIdHolder.clear();
        MDC.remove(MDC_KEY);
    }
}