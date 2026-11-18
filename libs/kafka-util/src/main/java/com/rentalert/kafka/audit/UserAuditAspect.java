package com.rentalert.kafka.audit;

import com.rentalert.dto.event.EventStatus;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.kafka.outbox.OutboxPublisher;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Aspect
public class UserAuditAspect {

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer PARAM_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    private final CurrentUserIdProvider currentUserIdProvider;
    private final OutboxPublisher outboxPublisher;
    private final TransactionTemplate transactionTemplate;

    public UserAuditAspect(CurrentUserIdProvider currentUserIdProvider, OutboxPublisher outboxPublisher,
        PlatformTransactionManager transactionManager) {
        this.currentUserIdProvider = currentUserIdProvider;
        this.outboxPublisher = outboxPublisher;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Around("@annotation(userAudit)")
    public Object audit(ProceedingJoinPoint joinPoint, UserAudit userAudit) throws Throwable {
        StandardEvaluationContext context = buildContext(joinPoint);
        Long userId = resolveUserId(context, userAudit.userId());
        List<String> params = resolveParams(context, userAudit.params());

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            return proceedAndPublish(joinPoint, userAudit, context, userId, params);
        }
        try {
            return transactionTemplate.execute(status -> {
                try {
                    return proceedAndPublish(joinPoint, userAudit, context, userId, params);
                } catch (Throwable throwable) {
                    throw new ProceedFailure(throwable);
                }
            });
        } catch (ProceedFailure failure) {
            throw failure.getCause();
        }
    }

    private Object proceedAndPublish(ProceedingJoinPoint joinPoint, UserAudit userAudit,
        StandardEvaluationContext context, Long userId, List<String> params) throws Throwable {
        Object result = joinPoint.proceed();

        Long filterId = null;
        if (!userAudit.filterId().isBlank()) {
            context.setVariable("result", result);
            filterId = evaluate(context, userAudit.filterId(), Long.class);
        }
        publish(userAudit.value(), userId, filterId, params);
        return result;
    }

    private Long resolveUserId(StandardEvaluationContext context, String expression) {
        Long userId;
        if (!expression.isBlank()) {
            userId = evaluate(context, expression, Long.class);
        } else {
            if (currentUserIdProvider == null) {
                throw new IllegalStateException("@UserAudit has no userId expression and no CurrentUserIdProvider bean");
            }
            userId = currentUserIdProvider.currentUserId();
        }
        if (userId == null) {
            throw new IllegalStateException("@UserAudit could not resolve the user id");
        }
        return userId;
    }

    private List<String> resolveParams(StandardEvaluationContext context, String[] expressions) {
        return Arrays.stream(expressions)
            .map(expression -> String.valueOf(PARSER.parseExpression(expression).getValue(context)))
            .toList();
    }

    private void publish(UserEvent userEvent, Long userId, Long filterId, List<String> params) {
        UserActionEvent event = UserActionEvent.builder()
            .event(userEvent)
            .filterId(filterId)
            .params(params)
            .build();
        event.setInitiatorId(userId);
        event.setEventStatus(EventStatus.SUCCESS);
        outboxPublisher.publish(KafkaTopics.USER_EVENTS, String.valueOf(userId), event);
        log.debug("user audit published event={} userId={}", userEvent, userId);
    }

    private <T> T evaluate(StandardEvaluationContext context, String expression, Class<T> type) {
        return PARSER.parseExpression(expression).getValue(context, type);
    }

    private StandardEvaluationContext buildContext(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String[] paramNames = PARAM_NAME_DISCOVERER.getParameterNames(((MethodSignature) joinPoint.getSignature()).getMethod());
        StandardEvaluationContext context = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        return context;
    }

    private static final class ProceedFailure extends RuntimeException {

        private ProceedFailure(Throwable cause) {
            super(cause);
        }
    }
}
