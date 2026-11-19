# Запуск RentAlert в Kubernetes (minikube)

Namespace `rentalert`. Всё разворачивается через kustomize: `k8s/base` (общие манифесты), `k8s/overlays/minikube` (один экземпляр каждого сервиса), `k8s/overlays/minikube-ha` (по два экземпляра и автомасштабирование).

## Что поднимается

| Компонент | Тип | Порт |
|---|---|---|
| postgres | StatefulSet, PVC 2Gi | 5432 |
| redis | Deployment, PVC 1Gi | 6379 |
| kafka (KRaft, одна нода) | StatefulSet | 9092 |
| redpanda-console | Deployment | 9999 |
| config-server | Deployment | 8888 |
| discovery (Eureka) | Deployment | 8761 |
| gateway | Deployment, NodePort 30080 | 8222 |
| identity-ms | Deployment | 8081 |
| notification-ms | Deployment | 8082 |
| ops-ms | Deployment | 8083 |
| listing-ms | Deployment | 8084 |
| processing-ms | Deployment | 8085 |
| admin-ms | Deployment | 8086 |
| roommate-ms | Deployment | 8087 |
| realtime-ms | Deployment | 8088 |

Порядок старта обеспечивают `initContainers`: инфраструктура, затем config-server и discovery, затем gateway и сервисы.

## Требования

- Docker, minikube, kubectl.
- Для обычного варианта около 6 CPU и 10 ГБ памяти (значения меняются переменными `MINIKUBE_CPUS`, `MINIKUBE_MEMORY`). Для `minikube-ha` нужно заметно больше.

## Быстрый старт

```
k8s/scripts/minikube-up.sh minikube
```

Первый запуск создаст `k8s/overlays/minikube/secrets.env` из `secrets.example.env` и остановится. Заполните значения и запустите команду снова. Файл `secrets.env` в `.gitignore`, в репозиторий он не попадает.

Обязательные ключи: `POSTGRES_PASSWORD`, все `*_DB_PASSWORD`, `APP_AUTH_ACCESS_SECRET`, `APP_AUTH_REFRESH_SECRET`, `APP_INTERNAL_TOKEN`, `APP_CRYPTO_AES_KEY`, `APP_CRYPTO_HMAC_KEY`. Случайные значения: `openssl rand -base64 32`.

Если нужно перенести данные из монолита, ключи `APP_CRYPTO_AES_KEY` и `APP_CRYPTO_HMAC_KEY` должны совпадать с ключами монолита, иначе перенесённые зашифрованные поля не расшифруются.

Скрипт запускает minikube, включает `metrics-server`, собирает образы обычным Docker хоста и загружает их в кластер, применяет overlay, ждёт готовности по порядку и печатает адрес gateway. Повторный запуск безопасен: уже созданный кластер он не пересоздаёт.

Вариант с двумя экземплярами каждого сервиса:

```
k8s/scripts/minikube-up.sh minikube-ha
```

## Доступ

```
minikube service gateway -n rentalert --url
k8s/scripts/port-forward.sh
```

`port-forward.sh` пробрасывает gateway на `http://localhost:8222`, консоль Kafka на `http://localhost:9999`, Postgres на `localhost:15432` и Redis на `localhost:6379` (порты меняются `GATEWAY_PORT`, `CONSOLE_PORT`, `POSTGRES_PORT` и `REDIS_PORT`).

Eureka и config-server:

```
kubectl -n rentalert port-forward svc/discovery 8761:8761
kubectl -n rentalert port-forward svc/config-server 8888:8888
```

Админка (`RentAlertAdmin`) ходит в gateway: задайте `BACKEND_BASE_URL` на адрес gateway.

## Состояние

```
kubectl -n rentalert get pods
kubectl -n rentalert get pods -w
kubectl -n rentalert get deploy,sts,svc
kubectl -n rentalert describe pod <pod>
kubectl -n rentalert get events --sort-by=.lastTimestamp
kubectl -n rentalert top pods
```

Готовность сервиса определяют пробы `/actuator/health/readiness` и `/actuator/health/liveness`. Spring стартует медленно, поэтому у `startupProbe` большой запас (до пяти минут).

## Логи

```
kubectl -n rentalert logs -f deploy/identity-ms
kubectl -n rentalert logs -f deploy/gateway --tail=200
kubectl -n rentalert logs <pod> --previous
kubectl -n rentalert logs -l app.kubernetes.io/name=roommate-ms --all-containers --prefix -f
kubectl -n rentalert logs -l app.kubernetes.io/part-of=rentalert --prefix --since=10m
```

`--previous` показывает логи предыдущего контейнера после перезапуска. Для подов в `Init:` смотрите логи контейнера ожидания: `kubectl -n rentalert logs <pod> -c <имя-init-контейнера>`.

События Kafka логируются в `com.rentalert.kafka`. Подробный уровень включается переменной окружения сервиса:

```
kubectl -n rentalert set env deploy/identity-ms LOGGING_LEVEL_COM_RENTALERT_KAFKA=DEBUG
```

## Перезапуск

```
kubectl -n rentalert rollout restart deploy/identity-ms
kubectl -n rentalert rollout status deploy/identity-ms
kubectl -n rentalert rollout restart deploy
kubectl -n rentalert rollout undo deploy/identity-ms
```

Автоматический перезапуск всех сервисов по волнам (config-server и discovery, затем сервисы, затем gateway; следующая волна стартует после готовности предыдущей):

```
k8s/scripts/restart-all.sh
k8s/scripts/restart-all.sh --build
k8s/scripts/restart-all.sh --build --apply
k8s/scripts/restart-all.sh --build identity-ms gateway
```

`--build` пересобирает образы и загружает их в minikube, `--apply` применяет overlay (`--overlay minikube-ha` для HA) перед перезапуском, в конце печатается список подов.

Секрет создан без хеш-суффикса, поэтому после изменения `secrets.env` поды сами не перезапустятся. Обновите секрет и перезапустите сервисы:

```
kubectl kustomize --load-restrictor=LoadRestrictionsNone k8s/overlays/minikube | kubectl apply -f -
kubectl -n rentalert rollout restart deploy
```

Обычный `kubectl apply -k` не подойдёт: init-скрипт Postgres берётся из `docker/postgres/init`, вне каталога kustomize, поэтому нужен флаг `--load-restrictor=LoadRestrictionsNone`. Скрипты его уже используют.

## Масштабирование

```
kubectl -n rentalert scale deploy/listing-ms --replicas=3
kubectl -n rentalert get hpa
```

В `minikube-ha` у gateway и восьми сервисов по два экземпляра, а автомасштабирование идёт по CPU (нужен `metrics-server`, `minikube-up.sh` его включает). Планировщики работают под распределённой блокировкой, outbox читается с `SKIP LOCKED` и advisory-блокировкой, Telegram-бот запускается только на одном экземпляре (выбор лидера через Redis), WebSocket раздаётся из `realtime-ms`: каждый экземпляр получает все события и отдаёт их своим клиентам.

## Образы

Образы называются `rentalert/<сервис>:local`. Они собираются обычным Docker хоста, а в кластер попадают через `minikube image load`, поэтому схема не зависит от контейнерного рантайма minikube (по умолчанию в нём `containerd`, и `minikube docker-env` с ним не работает).

```
k8s/scripts/build-images.sh --load
k8s/scripts/build-images.sh --load identity-ms gateway
kubectl -n rentalert rollout restart deploy/identity-ms
```

Без `--load` образ только собирается на хосте и в кластер не попадает. После загрузки перезапустите соответствующий Deployment: тег не меняется, поэтому Kubernetes сам новый образ не подхватит.

## Конфигурация

Несекретное лежит в ConfigMap `rentalert-config` (генерируется в `k8s/base/kustomization.yaml`), секретное в Secret `rentalert-secrets` (из `secrets.env`). Адрес config-server для сервисов задаёт `SPRING_CONFIG_IMPORT`, регистрацию в Eureka по IP пода задают `EUREKA_*`.

Проверить применённую конфигурацию без печати секретов:

```
kubectl -n rentalert get cm rentalert-config -o yaml
kubectl -n rentalert get secret rentalert-secrets -o jsonpath='{.data}' | jq 'keys'
```

Значения из секрета в терминал не выводите.

## База данных, Redis, Kafka

```
kubectl -n rentalert exec -it sts/postgres -- psql -U rentalert -l
kubectl -n rentalert exec -it sts/postgres -- psql -U rentalert -d identity
kubectl -n rentalert exec -it deploy/redis -- redis-cli
kubectl -n rentalert exec -it sts/kafka -- /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

Базы и роли создаёт скрипт `docker/postgres/init/01-create-databases.sh`, но только при первом старте с пустым томом. Если том уже был инициализирован без части баз, пересоздайте его (`minikube-down.sh`) или создайте недостающие вручную. Топики создают сами сервисы при старте.

## Перенос данных из монолита

Перенос запускается из админки (`POST /admin/migrations`) как сага. Каждый сервис читает свою часть прямо из БД монолита.

1. Укажите в `secrets.env` `MIGRATION_SOURCE_URL`, `MIGRATION_SOURCE_USERNAME`, `MIGRATION_SOURCE_PASSWORD`. Из пода монолит на хосте обычно доступен как `host.minikube.internal`, например `jdbc:postgresql://host.minikube.internal:5432/rentalert`. Нужна учётная запись только на чтение.
2. Перенос включён по умолчанию (`MIGRATION_ENABLED=true` в ConfigMap `rentalert-config`). После изменения секретов или ConfigMap примените их и перезапустите сервисы: `k8s/scripts/restart-all.sh --apply`.

3. Остановите монолит и его планировщики на время переноса: чтение идёт пачками по ключу, а не одним снимком, и уведомления могут уйти дважды.
4. Запустите с `dryRun` и проверьте счётчики на странице «Миграция данных», затем без `dryRun`.
5. После переноса выключите:

```
for s in identity-ms listing-ms processing-ms notification-ms roommate-ms ops-ms; do
  kubectl -n rentalert set env deploy/$s MIGRATION_ENABLED=false
done
```

## Остановка и очистка

```
k8s/scripts/minikube-down.sh minikube
k8s/scripts/minikube-down.sh minikube --purge
minikube stop
```

Первая команда удаляет namespace вместе с данными томов. С флагом `--purge` дополнительно удаляется весь кластер minikube. `minikube stop` останавливает кластер, не удаляя данные.

## Типичные проблемы

| Симптом | Что проверить |
|---|---|
| Под в `Init:` долго | `kubectl logs <pod> -c <init-контейнер>`: ждёт зависимость (postgres, redis, kafka, config-server, discovery) |
| `CrashLoopBackOff` у сервиса | `kubectl logs <pod> --previous`: обычно пустой обязательный секрет или недоступен config-server |
| `ImagePullBackOff` или `ErrImagePull` | образ не загружен в кластер: `k8s/scripts/build-images.sh --load <сервис>`; проверить список образов: `minikube image ls \| grep rentalert` |
| Сервис не виден gateway | Eureka: `port-forward svc/discovery 8761` и проверить регистрацию; должен быть `EUREKA_INSTANCE_PREFER_IP_ADDRESS=true` |
| `OOMKilled` | лимит 768Mi при 70% кучи; увеличьте `resources.limits.memory` в манифесте сервиса |
| Пустые `KUFAR_URL`, `REALT_URL`, `ONLINER_URL`, `FRONT_ADDRESS` | заполните в `configMapGenerator` в `k8s/base/kustomization.yaml`, если сервис требует непустые значения |
| Не хватает ресурсов | уменьшите `minikube-ha` до `minikube` или увеличьте `MINIKUBE_CPUS` и `MINIKUBE_MEMORY` |
