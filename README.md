# 🌟 OnterCore (NeoForge 1.21.1)

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.233+-orange.svg)](https://neoforged.net/)
[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**OnterCore** — это модульный, высокопроизводительный серверный кор-мод (**Server-Side Only**) для Minecraft 1.21.1 на базе загрузчика **NeoForge**.

Мод спроектирован для независимых серверов и крупных сетей, предоставляя синхронизацию с единой базой данных **MySQL** (общей с веб-сайтом и лаунчером), детальное досье игроков, аудит команд, асинхронные уведомления в **Telegram** и внутриигровой авто-бродкастер.

---

## ⚡️ Главные преимущества и возможности

* 🛡 **Server-Side Only**: Не требует установки мода на стороне клиентов (игроки заходят с обычным клиентом).
* ⚡️ **Zero-Tick-Lag (Асинхронность)**: Все операции с базой данных MySQL и Telegram API выполняются в неблокирующих потоках (Java 21 `HttpClient` и `CompletableFuture`), что исключает любые просадки TPS.
* 🗄 **Общая база данных MySQL**: Пул соединений **HikariCP** с авто-созданием таблиц. Идеально для интеграции с личным кабинетом сайта и панелью администратора.
* 📋 **Цифровое Досье и Аналитика Игроков**:
  * Фиксация первого/последнего входа, IP-адресов и расчет суммарного времени онлайн (`playtime`).
  * Сохранение координат выхода `(X, Y, Z, Dimension)` и здоровья.
  * История сессий и логов смертей.
  * Полный аудит выполненных команд с защитой от логирования паролей (`/login`, `/register`).
* 🤖 **Telegram Бот и Алерты**:
  * Оповещения о входе/выходе игроков с текущим онлайном сети.
  * Алерты безопасности при входе персонала и использовании опасных команд (`/op`, `/stop`, `/ban`).
  * Уведомления о старте/остановке сервера.
  * Предупреждения при падении TPS ниже заданного порога.
* 📢 **Внутриигровой Авто-Бродкастер**:
  * Периодические анонсы в чат (каждые 5 минут или настраиваемый интервал).
  * Поддержка цветовых кодов (`&a`, `&e`, `&6`), префиксов, последовательного или случайного порядка.
* 🚧 **Режим Техработ (Maintenance Mode)**:
  * Включение/выключение без перезагрузки сервера (`/onter maintenance on/off`).
  * Доступ только для администрации по permission level.
* ⚙️ **Полная модульность**: Любой модуль включается или отключается одним параметром в конфиге.

---

## 💻 Команды и права (LuckPerms / NeoForge Permissions)

Все команды и возможности мода интегрированы в стандартную систему прав NeoForge и полностью совместимы с плагином/модом **[LuckPerms](https://luckperms.net/)**. Вы можете выдавать права группам (`lp group admin permission set ...`) или отдельным игрокам.

| Команда / Действие | Право (Permission Node) | Описание | По умолчанию |
| :--- | :--- | :--- | :--- |
| `/onter` / `/oc` | `ontercore.command.base` | Базовый доступ к корневой команде и справке | OP (Уровень 2) |
| `/onter info <ник\|UUID>` | `ontercore.command.info` | Просмотр детального досье игрока (IP, координаты, телепорт, команды) | OP (Уровень 2) |
| `/onter status` | `ontercore.command.status` | Просмотр TPS, MSPT, аптайма, ОЗУ и статуса MySQL | OP (Уровень 2) |
| `/onter maintenance ...` | `ontercore.command.maintenance` | Управление режимом техработ сервера | OP (Уровень 2) |
| `/onter broadcast <текст>` | `ontercore.command.broadcast` | Отправка мгновенных системных анонсов в чат | OP (Уровень 2) |
| `/onter reload` | `ontercore.command.reload` | Горячая перезагрузка конфигурации, модулей и БД | OP (Уровень 2) |
| *Обход техработ* | `ontercore.bypass.maintenance` | Разрешает подключаться к серверу во время техработ | OP (Уровень 2) |

> 💡 **Пример выдачи прав в LuckPerms:**
> ```bash
> /lp group admin permission set ontercore.command.info true
> /lp group admin permission set ontercore.bypass.maintenance true
> ```
> 
> 💡 **Алиас:** Вместо `/onter` можно использовать короткую команду `/oc` (например, `/oc info Steve`).

---

## ⚙️ Конфигурация (`config/ontercore-server.toml`)

Файл конфигурации автоматически генерируется при первом запуске сервера:

```toml
[database]
    # Включить подключение к MySQL
    enabled = true
    host = "127.0.0.1"
    port = 3306
    database = "minecraft_network"
    username = "onter"
    password = "your_strong_password"
    pool_size = 10
    # Идентификатор сервера в сети (например: 'rpg-1', 'lobby', 'tech')
    server_name = "rpg-1"

[analytics]
    # Мастер-тумблер аналитики игроков
    enabled = true
    log_logins = true
    log_commands = true
    log_chat = false
    log_deaths = true
    # Команды, которые НЕ будут сохраняться в базу (для защиты паролей)
    command_exclusions = ["/login", "/register", "/auth", "/l", "/reg", "/changepassword", "/cp"]

[telegram]
    # Включить отправку в Telegram
    enabled = true
    bot_token = "123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ"
    chat_id = "-1001234567890"
    notify_join = true
    notify_leave = true
    notify_server_lifecycle = true
    notify_security = true
    notify_low_tps = true
    low_tps_threshold = 14.0

[broadcaster]
    # Включить периодические авто-анонсы
    enabled = true
    # Интервал в секундах (300 сек = 5 минут)
    interval_seconds = 300
    random_order = false
    prefix = "&8[&6Инфо&8]&r "
    messages = [
        "&aДобро пожаловать на наш сервер! Приятной игры!",
        "&eНаш официальный Discord: &bhttps://discord.gg/example",
        "&6Заметили баг или нарушение? Сообщите администрации: &e/report",
        "&dГолосуйте за сервер каждый день и получайте ценные награды!"
    ]

[maintenance]
    # Режим техработ
    enabled = false
    kick_message = "&cСервер находится на техническом обслуживании.\n&eПожалуйста, попробуйте подключиться позже!"
    bypass_permission_level = 2
```

---

## 🗄 Структура таблиц базы данных

Мод автоматически создает следующие таблицы в указанной базе данных MySQL:

1. **`onter_players`** — Основной реестр игроков:
   * `uuid`, `username`, `first_join`, `last_join`, `last_leave`, `last_ip`, `total_playtime`, `last_server`, `last_dimension`, `last_x`, `last_y`, `last_z`, `last_health`.
2. **`onter_sessions`** — История игровых сессий:
   * `id`, `player_uuid`, `username`, `server_name`, `ip_address`, `join_time`, `leave_time`, `duration_seconds`.
3. **`onter_command_logs`** — Аудит выполненных команд:
   * `id`, `player_uuid`, `username`, `server_name`, `command`, `dimension`, `x`, `y`, `z`, `created_at`.
4. **`onter_death_logs`** — Лог смертей:
   * `id`, `player_uuid`, `username`, `server_name`, `death_message`, `killer`, `dimension`, `x`, `y`, `z`, `created_at`.

---

## 🔨 Сборка из исходников

Для сборки JAR-файла мода требуется **JDK 21**:

```bash
# Клонировать репозиторий
git clone https://github.com/Fro-lov/OnterCore.git
cd OnterCore

# Сборка мода
./gradlew build
```

Готовый JAR-файл будет находиться в папке `build/libs/`.

---

## 📜 Лицензия

Проект распространяется под лицензией [MIT](LICENSE).
