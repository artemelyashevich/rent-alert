package com.rentalert.notification_ms.domain;

public enum NotificationStatus {
    PENDING,
    SUCCESS,
    FAILURE,
    /** Все попытки исчерпаны (attempt >= 3); повторов больше не будет. */
    EXHAUSTED,
    /** Отправка сознательно отложена (например, у пользователя тихие часы); будет отправлена планировщиком retryFailed(). */
    QUEUED
}