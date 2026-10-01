# Взаимодействие сервисов (текущая реализация)

Состояние на 2026-09-30

## Сервисы

| Сервис          | Порт | База (Postgres :5440) | Отвечает за                                                                                                   | REST (через gateway)    |
|-----------------|------|-----------------------|---------------------------------------------------------------------------------------------------------------|-------------------------|
| gateway         | 8222 | —                     | единая точка входа, маршрутизация, `X-Trace-Id`                                                               | —                       |
| config-server   | 8888 | —                     | конфиги всех сервисов (`native`), подстановка значений из `docker/.env`                                       | —                       |
| discovery       | 8761 | —                     | Eureka, реестр сервисов                                                                                       | —                       |
| identity-ms     | 8081 | `identity`            | вход через Telegram, JWT, профили, фильтры, обратная связь, подбор фильтров к новым объявлениям, статус бота  | `/api/v1/identity/**`   |
| notification-ms | 8082 | `notification`        | очередь и доставка уведомлений (Telegram-бот, Telegram администратору, email, WebSocket — заготовка), шаблоны, каналы пользователя, напоминания о просмотрах | `/api/v1/notification/**` |
| ops-ms          | 8083 | `ops`                 | журнал аудита, история задач, статистика пользователей                                                        | нет контроллеров        |
| listing-ms      | 8084 | `listing`             | каталог объявлений (копия), избранное, скрытые, календарь просмотров, пересчёт цен по курсам из processing-ms | `/api/v1/listing/**`    |
| processing-ms   | 8085 | `processing`          | парсеры Kufar, Realt, Onliner, курсы валют (Frankfurter) и внутренний API курсов, города и настройки парсеров | `/api/v1/processing/**` |
| admin-ms        | 8086 | —                     | админ-API поверх сервисов-владельцев: вход, профили, города, настройки парсеров, отзывы, журнал аудита, задачи, статистика, рассылки; действия пишет в `ops.audit-events.v1` | `/api/v1/admin/**`      |

Инфраструктура (`docker/docker-compose.yml`):

| Компонент                                                | Порт на хосте |
|----------------------------------------------------------|---------------|
| Postgres, отдельная база и пользователь на каждый сервис | 5440          |
| Redis                                                    | 6400          |
| Kafka (внутри сети `kafka:29092`)                        | 9092          |
| Redpanda Console                                         | 9999          |

Общие библиотеки (`libs/`):

| Библиотека             | Что в ней                                                                                |
|------------------------|------------------------------------------------------------------------------------------|
| `dto`                  | контракты событий и запросов, `KafkaTopics`, `GlobalConstants`                           |
| `common-util`          | JWT, исключения, шифрование, логирование запросов и `traceId`                            |
| `kafka-util`           | outbox, конфигурация producer и consumer, DLT, `traceId` в заголовках                    |
| `redis-util`           | кэш, блокировки, сессии                                                                  |
| `internal-integration` | Feign-клиенты к identity-ms, listing-ms и processing-ms с кэшем в Redis, повторами и circuit breaker |

## Общая схема

```mermaid
flowchart LR
    client["Фронтенд / Telegram Mini App"]
    tg["Telegram Bot API"]
    ext["Kufar / Realt / Onliner / Frankfurter"]

    subgraph infra["Инфраструктура"]
        cfg["config-server :8888"]
        disc["discovery (Eureka) :8761"]
        redis[("Redis")]
        kafka{{"Kafka"}}
    end

    gw["gateway :8222"]

    subgraph svc["Сервисы"]
        idn["identity-ms"]
        lst["listing-ms"]
        prc["processing-ms"]
        ntf["notification-ms"]
        ops["ops-ms"]
    end

    subgraph db["Postgres :5440"]
        dbi[("identity")]
        dbl[("listing")]
        dbp[("processing")]
        dbn[("notification")]
        dbo[("ops")]
    end

    client -->|"HTTPS /api/v1/{service}/**"| gw
    gw -->|"lb:// через Eureka"| idn
    gw --> lst
    gw --> prc
    gw --> adm

    lst -->|"Feign /identity/internal/**<br/>X-Internal-Token"| idn
    lst -->|"Feign /processing/internal/currency-rates/**<br/>X-Internal-Token"| prc
    ntf -->|"Feign /listing/internal/favorites/**<br/>X-Internal-Token"| lst
    adm["admin-ms"] -->|"Feign /*/internal/admin/**<br/>X-Internal-Token"| idn
    adm --> prc
    adm --> ops
    adm --> ntf
    adm -->|"AdminActionEvent, JobExecutionEvent"| kafka
    ntf -->|"Feign /identity/internal/profiles/{id}/receiver<br/>X-Internal-Token"| idn

    prc -->|HTTP| ext
    ntf -->|HTTP| tg

    idn --- dbi
    lst --- dbl
    prc --- dbp
    ntf --- dbn
    ops --- dbo

    prc -->|outbox| kafka
    idn -->|outbox| kafka
    lst -->|outbox| kafka
    ntf -->|outbox| kafka
    kafka --> idn
    kafka --> lst
    kafka --> ntf
    kafka --> ops

    idn -.-> redis
    lst -.-> redis
    prc -.-> redis

    gw -.->|адреса сервисов| disc
    gw -.->|конфиг| cfg
```

## Kafka: топики, отправители и получатели

```mermaid
flowchart LR
    prc["processing-ms"]
    idn["identity-ms"]
    lst["listing-ms"]
    ntf["notification-ms"]
    ops["ops-ms"]

    t_list{{"processing.listings.v1"}}
    t_req{{"notification.requests.v1"}}
    t_prio{{"notification.requests.priority.v1"}}
    t_user{{"ops.user-events.v1"}}
    t_job{{"ops.job-events.v1"}}
    t_audit{{"ops.audit-events.v1"}}

    prc -->|ListingCreatedEvent| t_list
    prc -->|NEW_LISTING_ADMIN_ALERT| t_req
    prc -->|JobExecutionEvent| t_job

    idn -->|NEW_LISTING_MATCHED| t_req
    idn -->|LISTING_RECEIVED| t_user
    idn -->|USER_DELETED| t_user
    lst -->|"FAVORITE_*, HIDDEN_LISTING"| t_user
    ntf -->|BOT_BLOCKED| t_user

    t_list -->|listing.listings| lst
    t_list -->|identity.listings| idn
    t_req -->|notification.requests| ntf
    t_prio -->|notification.requests.priority| ntf
    t_user -->|ops.user-events| ops
    t_user -->|identity.user-events| idn
    t_user -->|"listing.user-events (USER_DELETED)"| lst
    t_user -->|"notification.user-events (USER_DELETED)"| ntf
    t_job -->|ops.job-events| ops
    t_audit -->|ops.audit| ops
```

## Сценарий: новое объявление → уведомления

```mermaid
sequenceDiagram
    autonumber
    participant P as processing-ms (парсер)
    participant EXT as Kufar / Realt / Onliner
    participant PDB as processing DB
    participant K as Kafka
    participant L as listing-ms
    participant I as identity-ms
    participant N as notification-ms
    participant TG as Telegram
    participant O as ops-ms

    Note over P: CommonScheduleParser.tick (45 с) → задача на каждое объявление,<br/>у задачи свой traceId
    P->>EXT: запрос списка объявлений города
    EXT-->>P: объявления
    P->>PDB: TX: listings + outbox(ListingCreatedEvent с priceUsd/priceByn/message)<br/>+ outbox(NEW_LISTING_ADMIN_ALERT, если notifications_enabled)
    P->>K: OutboxRelay → processing.listings.v1, notification.requests.v1 (+ X-Trace-Id)

    par копия каталога
        K->>L: ListingCreatedEvent
        L->>L: сохранить listings (id из события, повтор игнорируется)
    and подбор фильтров
        K->>I: ListingCreatedEvent
        I->>I: findActiveFiltersByRoomsAndArea + цена в валюте фильтра<br/>+ роль не BLOCKED, BotStatus ACTIVE, есть telegramId
        I->>I: QuietHoursPolicy → sendAt, если сейчас тихие часы
        I->>K: TX outbox: LISTING_RECEIVED на каждый фильтр → ops.user-events.v1<br/>NEW_LISTING_MATCHED один раз на пользователя → notification.requests.v1
    end

    K->>O: LISTING_RECEIVED → user_statistics (счётчик за день)
    K->>N: NEW_LISTING_MATCHED / NEW_LISTING_ADMIN_ALERT
    N->>N: enqueue: строка QUEUED, nextRetryAt = sendAt или сейчас
    loop NotificationDispatchScheduler, каждые 2 с
        N->>N: шаблон {{message}} из notification_templates
        N->>TG: sendMessage (бот пользователю / канал администратора с [MICROSERVICE])
    end
```

## Сценарий: пользователь заблокировал бота

```mermaid
sequenceDiagram
    autonumber
    participant N as notification-ms
    participant TG as Telegram
    participant K as Kafka
    participant I as identity-ms
    participant O as ops-ms

    N->>TG: sendMessage
    TG-->>N: 403 Forbidden
    N->>N: уведомление → EXHAUSTED (403 не повторяется)
    N->>N: BotBlockedService, одна TX:<br/>outbox BOT_BLOCKED → ops.user-events.v1<br/>+ в очередь BOT_BLOCKED администратору (ключ bot-blocked:{userId}:{дата})
    N->>K: OutboxRelay → ops.user-events.v1
    par
        K->>I: BOT_BLOCKED (identity.user-events)
        I->>I: BotStatus = BLOCKED, сброс кэша профиля
    and
        K->>O: BOT_BLOCKED → audit_log + user_statistics
    end
    N->>TG: канал администратора: «пользователь заблокировал бота»
    Note over I: следующий подбор фильтров пропускает пользователя
```

## Сценарий: пересчёт цены в listing-ms

```mermaid
sequenceDiagram
    autonumber
    participant L as listing-ms
    participant R as Redis
    participant P as processing-ms
    participant F as Frankfurter

    Note over P: CurrencyRateScheduler раз в сутки
    P->>F: курс на сегодня
    P->>P: currency_rates
    P->>R: DAILY_CURRENCY_RATE:{pair}
    L->>R: DAILY_CURRENCY_RATE:{pair}?
    alt нет в кэше
        L->>P: Feign GET /processing/internal/currency-rates/{pair}<br/>X-Internal-Token
        P-->>L: CurrencyRateDto или пустой ответ
        L->>R: сохранить на 24 ч
    end
```

Ключ `DAILY_CURRENCY_RATE:{pair}` в Redis общий для обоих сервисов: processing-ms обновляет его после загрузки курсов, поэтому listing-ms видит новый курс без вызова.

## Сценарий: запрос пользователя через gateway

```mermaid
sequenceDiagram
    autonumber
    participant C as Клиент
    participant G as gateway
    participant E as Eureka
    participant L as listing-ms
    participant I as identity-ms
    participant R as Redis

    C->>G: GET /api/v1/listing/listings (Authorization: Bearer JWT)
    G->>G: TraceIdGlobalFilter: X-Trace-Id из запроса или новый
    G->>E: адрес listing-ms
    G->>L: GET /listing/listings (StripPrefix=2, X-Trace-Id)
    L->>L: RequestLoggingFilter: traceId → MDC<br/>JwtTokenSecurityFilter: проверка JWT
    L->>R: активный фильтр в кэше?
    alt нет в кэше
        L->>I: Feign GET /identity/internal/filters/active/{userId}<br/>X-Internal-Token, Authorization, X-Trace-Id
        I-->>L: фильтр
    end
    L-->>G: 200 + X-Trace-Id
    G-->>C: 200 + X-Trace-Id
```

## Конфигурация и запуск

```mermaid
flowchart TB
    env["docker/.env"]
    compose["docker compose:<br/>postgres, redis, kafka, redpanda-console"]
    cfg["config-server<br/>(native: classpath:/config/*.yml)"]
    disc["discovery"]
    svc["gateway и *-ms"]

    env --> compose
    env -->|"DotEnvPlaceholderResolver:<br/>подставляет ${VAR} в отдаваемый конфиг"| cfg
    compose --> cfg --> disc --> svc
    svc -->|"spring.config.import:<br/>optional:configserver:http://127.0.0.1:8888"| cfg
    svc -->|"Flyway: classpath:db/migration<br/>при старте"| svc
```

Порядок запуска:

1. `docker compose up -d` в папке `docker/`.
2. config-server.
3. discovery.
4. Сервисы и gateway в любом порядке.

## Сквозной traceId

```mermaid
flowchart LR
    a["gateway<br/>TraceIdGlobalFilter"] -->|X-Trace-Id| b["сервис<br/>RequestLoggingFilter → MDC"]
    b -->|"Feign<br/>HeaderPropagationInterceptor"| c["другой сервис"]
    b -->|"OutboxPublisher:<br/>outbox_event.trace_id"| d["OutboxRelay:<br/>заголовок X-Trace-Id"]
    p["processing-ms:<br/>новый traceId на задачу парсера"] --> d
    d --> e["получатель:<br/>TraceIdRecordInterceptor → MDC"]
    e -->|"outbox дальше"| d
```

- **Где виден `traceId`.** В логах он стоит в квадратных скобках после имени потока (`%X{traceId}`). По нему находится вся цепочка от парсера до отправки уведомления.
- **Что пишется в лог:**

  | Запрос | Кто пишет | Что | Уровень |
    |---|---|---|---|
  | Входящий | `LoggingInterceptor` | метод, путь, статус, время | INFO (без `/actuator`) |
  | Входящий | `RequestResponseLoggingFilter` | тела запроса и ответа | только при `LOG_LEVEL_APP=DEBUG` |
  | Исходящий к внешним API | `OutgoingRequestLoggingInterceptor` | хост и путь без query, токен бота замаскирован | INFO |
  | Feign | уровень `BASIC` | метод, URL, статус, время | DEBUG для `com.rentalert.internal.client` |

- **Где `traceId` теряется:** уведомления, которые диспетчер отправляет из очереди (тихие часы, повторы), не связаны с исходным запросом.
