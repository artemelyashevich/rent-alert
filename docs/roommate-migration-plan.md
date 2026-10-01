# План переноса roommate в микросервисы

Состояние на 2026-10-01. Монолит: `../RentAlert`, пакет `com.bsu.renalert.roommate`.

## Решение: отдельный сервис `roommate-ms`

В монолите roommate занимает около 4,9 тыс. строк: 5 контроллеров, 9 сервисов, 18 сущностей, 2 периодические задачи, машина состояний пары и модерация. Ни identity-ms, ни listing-ms ему по смыслу не подходят. Поэтому нужен отдельный сервис со своей базой Postgres, как у остальных.

Перенос упрощает одна вещь: другие домены не ссылаются на roommate (кроме события `RoommateMatchTransitionEvent`), а запросы roommate берут у пользователя и объявления только `id`.

## 1. Развязка данных

| Сейчас в монолите | В roommate-ms |
|---|---|
| `@ManyToOne UserProfile` в `RoommateProfile`, `RoommateMatch` (`userLo`/`userHi`), `RoommateMatchParticipant`, `RoommateListing.author` | Числовое поле `userId`. Во всех запросах используется только `.id`, переписать их несложно |
| Имя, фото и контакт собеседника | Пакетный запрос в identity-ms. Сейчас `findBriefs` доступен только через админский путь, нужен обычный внутренний эндпоинт |
| `@ManyToOne Listing` в `RoommateListing`, `RoommateViewing`, `RoommateSharedFavorite` | Поле `listingId` плюс пакетный запрос карточек в listing-ms (`ListingDto` по списку id) |
| `ListingStatus` в общем избранном пары | Хранить название статуса (`ListingStatusName`), а не ссылку на таблицу |
| SQL-представление `v_roommate_calendar_events`, которое соединяет таблицу `listings` | Между базами так нельзя. При назначении просмотра сохранять снимок полей объявления (название, адрес, метро, цена, валюта, обложка) в `roommate_viewings`, чтобы календарь не зависел от доступности listing-ms |

## 2. Связи с другими сервисами

### identity-ms
- **Общий фильтр пары:** `upsertForMatch` и `setActivationForMatch` в `UserFilterServiceImpl` уже есть, но во внутреннем контроллере не выставлены. Их нужно открыть как внутренние эндпоинты.
- **Рассылка по общему фильтру:** проверить, что `NewListingNotificationServiceImpl` корректно рассылает объявления по фильтрам с `ownerMatchId` обоим участникам пары.
- **Жалобы:** `FeedbackSubjectType.ROOMMATE_PROFILE/ROOMMATE_LISTING` уже есть. Сейчас `RoommateComplaintHandler` вызывается напрямую, вместо этого identity будет публиковать событие, а roommate-ms по нему увеличивать `reportsCount`.
- **Тихие часы:** `QuietHoursPolicy` лежит в identity-ms. Ежедневной подборке кандидатов нужно время отправки, его стоит отдавать вместе с краткими данными пользователя.

### notification-ms
- **Уведомления:** шаблоны и типы `ROOMMATE_*` уже есть (V005, V007), канал WEBSOCKET тоже. Уведомления пойдут через `NotificationRequest` и outbox.
- **Напоминание о просмотре:** у `Reminder.roommateViewingId` поле есть, но читать по нему нечего. Нужен внутренний эндпоинт roommate-ms по аналогии с `ListingMsClient.getFavoriteViewing`.

### admin-ms
- `AdminRoommateModerationController` проксирует запросы во внутренние эндпоинты roommate-ms, аудит через `AdminAuditPublisher`.

### ops-ms
- **Статистика:** `ROOMMATE_CONTACT_REVEALED` и прочие события отправляются в `USER_EVENTS` через outbox.
- **Удаление пользователя:** roommate-ms становится участником саги удаления (`AbstractCleanupParticipant`), в `CleanupSagaOrchestrator` добавляется шаг.

### listing-ms
- **Карточки объявлений:** нужен внутренний эндпоинт для пакетного получения `ListingDto` по списку id.
- **Удаление объявлений:** в микросервисах сейчас никто не сообщает об удалении объявлений: `NotificationService.deleteByListingIds` нигде не вызывается. roommate-ms это тоже понадобится, чтобы чистить общее избранное и просмотры. Нужно событие `LISTINGS_DELETED` от listing-ms. Это отдельная небольшая задача, она полезна и без roommate.

### Инфраструктура
- маршрут gateway `/api/v1/roommate/**`;
- `roommate-ms.yml` в config-server;
- база в docker-compose;
- `RoommateMsClient` в `internal-integration`;
- задачи `RoommateMatchExpiryJob` (раз в час) и `RoommateCandidateDigestJob` (каждые 6 часов) под `LockService`, как `CurrencyRateScheduler`, чтобы при нескольких экземплярах их выполнял один.

## 3. Этапы

Каждый этап выкатывается отдельно и проверяется своими тестами.

| № | Этап | Что входит | Проверка |
|---|---|---|---|
| 0 | Каркас | Модуль, Flyway-схема без внешних ключей на пользователей и объявления, gateway, config, `RoommateMsClient` | Сервис стартует; `*ApplicationTests` с тегом `integration` |
| 1 | Анкеты и объявления о комнате | `RoommateProfile`, `RoommateListing`, `TextModerationService`, `RoommateModerationGate`, админская модерация в admin-ms, уведомления `ROOMMATE_PROFILE_APPROVED/REJECTED` | Unit-тесты модерации, тест прокси в admin-ms |
| 2 | Лента | `RoommateFeedService`, `RoommateScoringService`, скрытие (`RoommateHidden`), пакетная подгрузка имён и фото из identity | Тесты оценки и курсора ленты |
| 3 | Пары | `RoommateMatchStateMachine`, события пары, `RoommateNotificationListener` на outbox, раскрытие контакта со статистикой | Тесты всех переходов машины состояний |
| 4 | Совместный поиск | Общий фильтр (identity), общее избранное и просмотры (listing-ms), календарь со снимком данных объявления, напоминания в notification-ms | Тесты клиентов и снимка календаря |
| 5 | Фоновые процессы | Задачи истечения пар и ежедневной подборки, жалобы через событие, участие в саге удаления, событие `LISTINGS_DELETED` | Тесты задач и участника саги |
| 6 | Перенос данных и переключение | Скрипт выгрузки `roommate_*` из монолита (`user_profile_id` → `user_id`, данные объявления копируются в снимок просмотра), переключение фронта | Сверка количества строк, проверка на стенде |

## 4. Что нужно решить до начала

1. **Сохранились ли id пользователей и объявлений** при переносе в identity-ms и listing-ms? Если нет, для переноса данных нужна таблица соответствия.
2. **Где хранить общее избранное пары:** в roommate-ms или в listing-ms как избранное с владельцем `matchId`, по аналогии с `ownerMatchId` у фильтров? Второй вариант держит всё избранное в одном месте, но усложняет listing-ms. Предложение — roommate-ms.
3. **WebSocket и несколько экземпляров:** уведомления пары — основной пользователь WebSocket. Встроенный брокер работает только при одном экземпляре notification-ms. Если планируется масштабирование, нужен внешний брокер-ретранслятор (например, RabbitMQ STOMP) или рассылка между экземплярами через Redis.
