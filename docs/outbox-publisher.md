# Outbox: базовый класс паблишера

`com.rentalert.kafka.outbox.AbstractOutboxEventPublisher` внедряет `OutboxPublisher` сам, наследнику не нужен конструктор с ним.

- `publish(topic, key, payload)` - записывает событие в outbox текущей транзакции.
- `publishEvent(topic, userId, event)` - для `AbstractEvent`: ставит `initiatorId = userId`, статус `SUCCESS`, если не задан, и ключ `String.valueOf(userId)`.
- Вызывать только внутри бизнес-транзакции. Базовый класс её не открывает; без активной транзакции `OutboxPublisher` бросает `IllegalStateException`.

```java
@Component
public class UserEventPublisher extends AbstractOutboxEventPublisher {

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(UserEvent userEvent, Long userId) {
        publishEvent(KafkaTopics.USER_EVENTS, userId,
            UserActionEvent.builder().event(userEvent).build());
    }
}
```
