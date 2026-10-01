# Что осталось перенести из монолита

Состояние на 2026-10-01. Монолит: `../RentAlert`, пакет `com.bsu.renalert`.

Отсортировано по сложности: сверху то, где большая часть кода уже есть в сервисах.

## 1. Админ-API (admin-ms)

Уже есть в admin-ms: вход администратора, города, настройки парсеров, отзывы, журнал аудита.

| Контроллер монолита | Что нужно | Владелец данных | Сложность |
|---|---|---|---|
| `AdminUserProfileController` (список, профиль, смена роли, удаление) | внутренние эндпоинты `/identity/internal/admin/profiles/**`; смена роли должна сбрасывать сессию и кэш профиля; удаление пользователя затрагивает данные listing-ms, notification-ms, ops-ms — нужна saga по образцу очистки | identity-ms | средняя (удаление — высокая) |
| `AdminJobSettingsController` | список `job_setting` (данные уже в ops-ms); запуск очистки → старт `CleanupSagaOrchestrator`; синхронизация курсов → команда в processing-ms; очистка и синхронизация кэшей — решить, какой сервис за что отвечает (`SettingsServiceImpl` монолита трогает кэши всех доменов) | ops-ms, processing-ms | средняя |
| `AdminStatisticsController` (статистика пользователя, сводка действий, журнал активности, обзор системы) | в ops-ms есть только запись `user_statistics`; запросы и DTO `UserStatisticsResponse`, `UserActionSummary`, `UserActivityLog`, `SystemActivityOverview` не перенесены | ops-ms | средняя |
| `AdminListingStatisticsController` | `ListingStatisticsService` и ~10 проекций (`CityListingCountProjection`, `ProviderArrivalProjection` …) не перенесены; данные объявлений есть и в listing-ms, и в processing-ms — выбрать источник | listing-ms или processing-ms | средняя |
| `AdminNotificationController` (рассылки) | `AdminBroadcastService` не перенесён; таблицы, сущности, репозитории и мапперы рассылок в notification-ms уже есть; список получателей — из identity-ms | notification-ms | средняя |
| `AdminRoommateModerationController` | заблокировано: нет домена roommate (см. п. 5) | — | — |

## 2. Пользовательские функции

| Что | Где в монолите | Что нужно |
|---|---|---|
| Подтверждение email (OTP) | `EmailController`, `OtpService`, `UserNotificationGateway.sendOtpEmail` / `sendEmailActivationSuccess` | эндпоинты в identity-ms, OTP в Redis, письма через `NotificationRequest` (типы уже есть в `NotificationType`) |
| Настройки приложения для фронта | `AppSettingsController` (`/api/v1/app-settings`) | только перечисления — можно отдавать из любого сервиса или gateway |
| Уведомление администратору о новом пользователе | `UserCreatedEventListener` | identity-ms при регистрации кладёт `NotificationRequest` типа `USER_CREATED` в outbox |
| Настройки уведомлений по умолчанию при регистрации | `UserNotificationGateway.createDefaultSettings` | сейчас заменено ответом по умолчанию (Telegram) в `NotificationSettingsServiceImpl.getSettings`, пока у пользователя нет строк; если нужна строка в БД — событие регистрации из identity-ms |

## 3. События статистики (`@UserAudit`)

В монолите эти действия пишутся в статистику, в микросервисах — нет:

| Событие | Где должно публиковаться |
|---|---|
| `LOGIN_SUCCESS` | identity-ms, `TelegramLoginService` и `AdminLoginService` |
| `FILTER_CREATED`, `FILTER_UPDATED`, `FILTER_DELETED` | identity-ms, `UserFilterServiceImpl` |
| `NOTIFICATION_SETTINGS_UPDATED` | notification-ms, `NotificationSettingsServiceImpl.updateSettings` (для outbox нужна транзакция, таблица `outbox_event` в notification-ms есть) |

`HIDDEN_LISTING`, `FAVORITE_*`, `BOT_BLOCKED`, `LISTING_RECEIVED` уже публикуются.

## 4. Telegram-бот (входящие команды)

Каркас в notification-ms есть (`TelegramBot`, `TelegramCommandExecutor`, `TelegramCallbackExecutor`), бот выключен (`app.telegram.bot-enabled=false`). Не перенесены:

- команды `/start`, `/help`, `/app` (`TelegramStartCommandExecutor`, `TelegramHelpCommandExecutor`, `TelegramAppCommandExecutor`);
- кнопки под объявлением: в избранное, убрать из избранного, скрыть (`FavoriteListingCallbackExecutor`, `UnfavoriteListingCallbackExecutor`, `HideListingCallbackExecutor`) — нужны внутренние эндпоинты listing-ms и поиск пользователя по Telegram id в identity-ms.

## 5. Домен roommate (поиск соседей)

Целиком в монолите, ~90 классов: профили, объявления, лента с курсором, скоринг, мэтчи с машиной состояний, общие избранные и фильтры, просмотры, жалобы, модерация текста, задачи `RoommateCandidateDigestJob` и `RoommateMatchExpiryJob`, админская модерация. Логично выделить в отдельный `roommate-ms` со своей БД; зависит от identity-ms (профиль, возраст, пол) и listing-ms (объявления).

## 6. Инфраструктура

| Что | Состояние |
|---|---|
| WebSocket-уведомления (`WebSocketConfiguration`, `WebSocketAuthChannelInterceptor`) | в notification-ms только заглушка `WebSocketNotificationStrategy` |
| Уведомление «приложение запущено» и синхронизация курсов при старте (`AppInitService`) | не перенесено |
| Периодическая синхронизация кэшей (`CacheSyncScheduler`, каждые 5 мин) | не перенесено; решить, нужно ли при кэшах с TTL |

## Известные проблемы, найденные при переносе

- **Feign использует `HttpURLConnection`**, который не умеет PATCH, поэтому внутренние эндпоинты делаются через PUT.
- **`ListingMsApplicationTests`, `ProcessingMsApplicationTests`** падают без config-server (нет datasource).
- **`redis-util`**: тесты не компилируются (`RedisUtilAutoConfigurationTests` обращается к непубличному `jsonRedisSerializer()`).
