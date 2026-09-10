# 🌟 OnterCore (NeoForge 1.21.1)

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.233+-orange.svg)](https://neoforged.net/)
[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**OnterCore** — модульный, высокопроизводительный серверный кор-мод (**Server-Side Only**) для Minecraft 1.21.1 (NeoForge).

Спроектирован для одиночных серверов и крупных мультисерверных сетей. Предоставляет единую базу данных **MySQL/MariaDB**, двустороннюю веб-очередь действий (**Сайт ⇄ Сервер**), выгрузку снапшотов инвентарей в JSON для веб-визуализаторов, цифровое досье игроков, аудит команд, асинхронные уведомления в **Telegram** и внутриигровой авто-бродкастер с полным контролем.

---

## ⚡️ Главные возможности

* 🛡 **Server-Side Only**: Не требует установки мода на стороне клиентов (игроки заходят с обычным клиентом).
* ⚡️ **Zero-Tick-Lag (Асинхронность)**: Все операции с базой данных MySQL и Telegram API выполняются в неблокирующих потоках (`CompletableFuture` / Virtual Threads), что исключает любые просадки TPS/MSPT.
* 🌐 **Двусторонняя веб-интеграция (Сайт ⇄ Сервер)**:
  * **Сайт ➔ Сервер (`onter_web_queue`):** Выполнение любых команд, телепортов, выдачи доната, киков и сообщений с веб-панели без открытия внешних портов (работает за любым NAT/Firewall).
  * **Сервер ➔ Сайт (`onter_player_inventories`):** Сериализация инвентаря, брони, хотбара, эндер-сундука, здоровья, опыта и координат игрока в чистый JSON для веб-инвентарей.
* 📋 **Цифровое Досье и Глобальная Аналитика**:
  * Фиксация сессий, IP-адресов, расчет суммарного времени онлайн (`playtime`).
  * Фиксация точных координат выхода `(X, Y, Z, Dimension)` и здоровья.
  * Глобальный аудит команд с привязкой к конкретным серверам сети (`/onter history`).
* 🤖 **Telegram Бот и Алерты**:
  * Оповещения о входе/выходе игроков с текущим онлайном и именем сервера (`rpg-1`).
  * Алерты безопасности при входе администраторов и выполнении критических команд (`/op`, `/stop`, `/ban`).
  * Уведомления о старте/остановке сервера и критическом падении TPS (< 14.0).
* 📢 **Умный Внутриигровой Авто-Бродкастер**:
  * Периодические анонсы (каждые 5 минут или настраиваемый интервал).
  * Команды просмотра списка, времени до следующего сообщения, принудительного триггера и переключения на лету.
* 🚧 **Режим Техработ (Maintenance Mode)**:
  * Мгновенный перевод сервера в режим обслуживания (`/onter maintenance on/off`).
  * Пропуск персонала по правам или permission level.
* 🔑 **Полная интеграция с [LuckPerms](https://luckperms.net/)**:
  * Поддержка гранулярных прав на каждую команду и функцию.

---

## 💻 Команды и права (LuckPerms / NeoForge Permissions)

| Команда | Право (Permission Node) | Описание | По умолчанию |
| :--- | :--- | :--- | :--- |
| `/onter` / `/oc` | `ontercore.command.base` | Главное меню и справка по командам | OP (Уровень 2) |
| `/onter info <ник\|UUID>` | `ontercore.command.info` | Карточка досье игрока (IP, координаты, телепорт, сессии, команды) | OP (Уровень 2) |
| `/onter history <ник> [лимит]` | `ontercore.command.history` | Глобальная история выполненных команд игрока по всей сети серверов | OP (Уровень 2) |
| `/onter status` | `ontercore.command.status` | Состояние сервера (TPS, MSPT, ОЗУ, Аптайм, статус MySQL и веб-очереди) | OP (Уровень 2) |
| `/onter maintenance <on\|off\|toggle\|status>` | `ontercore.command.maintenance` | Управление режимом техработ | OP (Уровень 2) |
| `/onter broadcast list` | `ontercore.command.broadcast` | Пронумерованный список всех анонсов и время до следующего | OP (Уровень 2) |
| `/onter broadcast time` | `ontercore.command.broadcast` | Показывает точное время в секундах/минутах до следующего авто-анонса | OP (Уровень 2) |
| `/onter broadcast next` | `ontercore.command.broadcast` | Принудительно отправляет следующий анонс и сбрасывает таймер | OP (Уровень 2) |
| `/onter broadcast toggle` | `ontercore.command.broadcast` | Включение / выключение авто-анонсов на лету | OP (Уровень 2) |
| `/onter broadcast send <текст>` | `ontercore.command.broadcast` | Мгновенная отправка произвольного анонса всем игрокам | OP (Уровень 2) |
| `/onter sync [ник]` | `ontercore.command.sync` | Принудительная выгрузка инвентаря игрока/всех игроков в MySQL | OP (Уровень 2) |
| `/onter reload` | `ontercore.command.reload` | Горячая перезагрузка конфигурации, базы данных и таймеров | OP (Уровень 2) |
| *Обход техработ* | `ontercore.bypass.maintenance` | Право на подключение к серверу во время техработ | OP (Уровень 2) |

---

## 🌐 Двусторонняя работа с веб-сайтом

### 1. Сайт ➔ Сервер: Очередь действий (`onter_web_queue`)
Любое действие с сайта (кнопка в личном кабинете, админке или магазине) отправляется через обычный `INSERT` в общую БД MySQL:

```sql
INSERT INTO onter_web_queue (server_name, player_uuid, action_type, payload, status, created_at)
VALUES ('rpg-1', '069a79f4-44e9-4726-a5be-fca90e38aaf5', 'COMMAND', '{"command":"spawn %player%"}', 'PENDING', NOW());
```

Поддерживаемые типы `action_type`:
* `COMMAND`: Выполнение консольной команды (`{"command": "give %player% diamond 5"}`).
* `TELEPORT`: Телепортация игрока (`{"x": 100.5, "y": 70.0, "z": -200.5, "dimension": "minecraft:overworld"}`).
* `MESSAGE`: Отправка сообщения игроку в чат (`{"message": "&aВам начислен ежедневный бонус!"}`).
* `KICK`: Отключение игрока от сервера (`{"reason": "&cВаш аккаунт был временно заблокирован"}`).

Сервер проверяет очередь каждую секунду, выполняет действие в главном потоке и возвращает `status = 'SUCCESS'` / `status = 'FAILED'` с текстом ответа в колонке `response`.

---

### 2. Сервер ➔ Сайт: Снапшот инвентаря и состояния (`onter_player_inventories`)
Сервер автоматически обновляет состояние игрока при выходе, смерти или периодически (раз в 60 сек):

```json
{
  "inventory": [
    {
      "slot": 0,
      "id": "minecraft:diamond_sword",
      "count": 1,
      "display_name": "Меч Победителя",
      "damage": 12,
      "max_damage": 1561,
      "enchantments": {
        "minecraft:sharpness": 5,
        "minecraft:unbreaking": 3
      }
    },
    {
      "slot": 39,
      "id": "minecraft:netherite_helmet",
      "count": 1,
      "display_name": "Шлем Вечности",
      "enchantments": { "minecraft:protection": 4 }
    }
  ],
  "ender_chest": [ ... ]
}
```

---

## ⚙️ Конфигурация (`config/ontercore-server.toml`)

```toml
[database]
    enabled = true
    host = "127.0.0.1"
    port = 3306
    database = "minecraft_network"
    username = "onter"
    password = "your_strong_password"
    pool_size = 10
    server_name = "rpg-1"

[analytics]
    enabled = true
    log_logins = true
    log_commands = true
    log_chat = false
    log_deaths = true
    command_exclusions = ["/login", "/register", "/auth", "/l", "/reg", "/changepassword", "/cp"]

[telegram]
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
    enabled = true
    interval_seconds = 300
    random_order = false
    prefix = "&8[&6Инфо&8]&r "
    messages = [
        "&aДобро пожаловать на наш сервер! Приятной игры!",
        "&eНаш официальный Discord: &bhttps://discord.gg/example",
        "&6Заметили баг или нарушение? Сообщите администрации через &e/report",
        "&dГолосуйте за сервер каждый день и получайте ценные награды!"
    ]

[maintenance]
    enabled = false
    kick_message = "&cСервер находится на техническом обслуживании.\n&eПожалуйста, попробуйте подключиться позже!"
    bypass_permission_level = 2

[web_queue]
    enabled = true
    poll_interval_ticks = 20

[inventory_sync]
    enabled = true
    sync_on_logout = true
    sync_interval_seconds = 60
```

---

## 🗄 Структура базы данных

1. **`onter_players`**: основной профиль игрока (UUID, ник, первый/последний вход, IP, суммарный онлайн, координаты выхода, здоровье, последний сервер).
2. **`onter_sessions`**: история игровых сессий с длительностью и IP по каждому серверу сети.
3. **`onter_command_logs`**: журнал всех выполненных команд с сервером, координатами и временем.
4. **`onter_death_logs`**: лог смертей с координатами, убийцами и причинами.
5. **`onter_web_queue`**: очередь команд и действий с сайта.
6. **`onter_player_inventories`**: JSON-снапшоты инвентаря, брони, эндер-сундука, здоровья и опыта.

---

## 🔨 Сборка из исходников

```bash
git clone https://github.com/Fro-lov/OnterCore.git
cd OnterCore
./gradlew build
```

Готовый JAR-файл находится в: `build/libs/ontercore-1.0.1.jar`.

---

## 📜 Лицензия

Распространяется под лицензией [MIT](LICENSE).
