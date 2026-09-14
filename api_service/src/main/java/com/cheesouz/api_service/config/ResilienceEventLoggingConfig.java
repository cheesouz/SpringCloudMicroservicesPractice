package com.cheesouz.api_service.config;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;


@Configuration 
public class ResilienceEventLoggingConfig {

    private static final Logger log = LoggerFactory.getLogger(ResilienceEventLoggingConfig.class);

    public ResilienceEventLoggingConfig(RetryRegistry retryRegistry,
                                         CircuitBreakerRegistry circuitBreakerRegistry) {

        Retry retry = retryRegistry.retry("dbService");
        retry.getEventPublisher()
            .onRetry(event -> log.warn("[RETRY] attempt {} for {} after: {}",
                event.getNumberOfRetryAttempts(),
                event.getName(),
                event.getLastThrowable().toString()))
            .onSuccess(event -> log.info("[RETRY] {} succeeded after {} attempts",
                event.getName(), event.getNumberOfRetryAttempts()))
            .onError(event -> log.error("[RETRY] {} exhausted retries, giving up: {}",
                event.getName(), event.getLastThrowable().toString()));

        CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker("dbService");
        breaker.getEventPublisher()
            .onStateTransition(event -> log.warn("[CB] {} transitioned {} -> {}",
                event.getCircuitBreakerName(),
                event.getStateTransition().getFromState(),
                event.getStateTransition().getToState()))
            .onCallNotPermitted(event -> log.warn("[CB] {} rejected call — breaker is OPEN",
                event.getCircuitBreakerName()))
            .onError(event -> log.error("[CB] {} recorded a failure: {}",
                event.getCircuitBreakerName(), event.getThrowable().toString()));
    }
}
