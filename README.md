# InteractEntity — Документация

Мод для Minecraft Forge 1.20.1. Позволяет создавать диалоги с мобами через JSON-файлы без программирования.

---

## Содержание

1. [Установка и структура файлов](#1-установка-и-структура-файлов)
2. [Базовая структура JSON](#2-базовая-структура-json)
3. [Узлы (nodes)](#3-узлы-nodes)
4. [Форматирование текста](#4-форматирование-текста)
5. [Действия (actions)](#5-действия-actions)
6. [Условия (conditions)](#6-условия-conditions)
7. [Система квестов](#7-система-квестов)
8. [Переменные](#8-переменные)
9. [Повторный диалог (on_revisit)](#9-повторный-диалог-on_revisit)
10. [Автоспавн (summon)](#10-автоспавн-summon)
11. [Иконка над NPC](#11-иконка-над-npc)
12. [Внешний вид GUI](#12-внешний-вид-gui)
13. [Команды](#13-команды)
14. [Назначение NPC в мире](#14-назначение-npc-в-мире)
15. [Прогресс и сервер](#15-прогресс-и-сервер)
16. [Клавиши](#16-клавиши)

---

## 1. Установка и структура файлов

После первого запуска мода появится папка:

```
.minecraft/
  interactentity/
    dialogues/
      ← сюда кладите JSON-файлы
```

Поддерживаются подпапки. Путь к файлу становится ID диалога:

| Файл | ID диалога |
|------|-----------|
| `dialogues/zombie.json` | `zombie` |
| `dialogues/story/intro.json` | `story/intro` |

После добавления или изменения файлов используйте `/dialogue reload`.

---

## 2. Базовая структура JSON

```json
{
  "target": {
    "name": "Старый Зомби",
    "tag": "old_zombie"
  },
  "display_name": "&6[&eСтарый Зомби&6]",
  "entry": "start",
  "nodes": {
    "start": {
      "text": "&fПривет, путник...",
      "next": "end"
    },
    "end": {
      "text": "&7*Зомби замолкает*"
    }
  }
}
```

### Корневые поля

| Поле | Тип | Обяз. | Описание |
|------|-----|-------|----------|
| `target.name` | строка | да | Имя моба (бирка CustomName) |
| `target.tag` | строка | да | Scoreboard-тег моба |
| `display_name` | строка | нет | Имя в GUI (по умолч. = target.name) |
| `entry` | строка | да | ID начального узла |
| `nodes` | объект | да | Все узлы диалога |
| `repeatable` | bool | нет | Диалог можно проходить повторно (по умолч. `false`) |
| `invulnerable` | bool | нет | Защита NPC от урона (по умолч. `true`) |
| `avatar` | строка | нет | Текстура аватара (`namespace:path`) |
| `background` | строка | нет | Текстура фона панели диалога |
| `options_background` | строка | нет | Текстура фона кнопок ответов |
| `on_revisit` | объект | нет | Поведение при повторном разговоре |
| `summon` | объект | нет | Конфигурация автоспавна |

### Повторный диалог (repeatable)

По умолчанию диалог можно пройти один раз. Если нужно разрешить повторное прохождение:

```json
{
  "target": { "name": "Торговец", "tag": "trader" },
  "entry": "start",
  "repeatable": true,
  "nodes": {
    "start": {
      "text": "&fЧем могу помочь?",
      "options": [
        { "text": "&aКупить зелье", "next": "sell" },
        { "text": "&7Ничего", "next": "bye" }
      ]
    },
    "sell": {
      "text": "&aПожалуйста!",
      "actions": [{ "type": "give_item", "item": "minecraft:potion", "count": 1 }]
    },
    "bye": { "text": "&7До встречи." }
  }
}
```

---

## 3. Узлы (nodes)

Каждый диалог состоит из узлов. Тип определяется автоматически по полям.

### Линейный узел

Есть `next`, нет `options`. ПКМ — следующий узел, ЛКМ — предыдущий.

```json
"intro": {
  "text": "&fЯ давно тебя жду...",
  "next": "question"
}
```

### Узел с выбором

Есть `options`. Показывает кнопки.

```json
"question": {
  "text": "&fТы поможешь мне?",
  "options": [
    { "text": "&aДа!", "next": "accept" },
    { "text": "&cНет", "next": "refuse" }
  ]
}
```

### Конечный узел

Нет ни `next`, ни `options`. ПКМ закрывает диалог.

```json
"end": {
  "text": "&7*Зомби уходит в темноту*"
}
```

### Случайный текст

Вместо `text` используйте `random_text` — при каждом входе выбирается случайная строка.

```json
"idle": {
  "random_text": [
    "&7*Моб смотрит в сторону*",
    "&7*Моб зевает*",
    "&fХмм..."
  ],
  "next": "talk"
}
```

### Автопереход

Узел автоматически переходит к следующему через N тиков (20 тиков = 1 секунда). Работает только с `next`, без `options`.

```json
"cutscene": {
  "text": "&5*Вспышка света...*",
  "next": "aftermath",
  "auto_next_ticks": 60
}
```

### Управление камерой в узле

```json
"revelation": {
  "text": "&eСмотри туда!",
  "camera": "player",
  "camera_yaw_offset": 90.0,
  "camera_pitch_offset": -30.0,
  "next": "next"
}
```

| Поле | Описание |
|------|----------|
| `camera` | `"npc"` (смотреть на NPC) или `"player"` (смотреть от игрока) |
| `camera_yaw_offset` | Горизонтальное смещение в градусах |
| `camera_pitch_offset` | Вертикальное смещение в градусах |

### Все поля узла

| Поле | Тип | Описание |
|------|-----|----------|
| `text` | строка | Текст реплики |
| `random_text` | массив | Случайный текст |
| `next` | строка | ID следующего узла |
| `auto_next_ticks` | int | Автопереход через N тиков |
| `options` | массив | Варианты ответа |
| `actions` | массив | Действия при входе в узел |
| `camera` | строка | Режим камеры: `"npc"` или `"player"` |
| `camera_yaw_offset` | float | Смещение камеры по горизонтали |
| `camera_pitch_offset` | float | Смещение камеры по вертикали |

### Поля варианта ответа

| Поле | Тип | Описание |
|------|-----|----------|
| `text` | строка | Текст кнопки |
| `next` | строка | Следующий узел (нет = завершить) |
| `condition` | объект | Условие показа кнопки |
| `actions` | массив | Действия при выборе |

---

## 4. Форматирование текста

Работает везде: имя, текст узла, кнопки, квесты.

### Цвета

| Код | Цвет | Код | Цвет |
|-----|------|-----|------|
| `&0` | Чёрный | `&8` | Тёмно-серый |
| `&1` | Тёмно-синий | `&9` | Синий |
| `&2` | Тёмно-зелёный | `&a` | Зелёный |
| `&3` | Тёмно-голубой | `&b` | Голубой |
| `&4` | Тёмно-красный | `&c` | Красный |
| `&5` | Тёмно-фиолетовый | `&d` | Розовый |
| `&6` | Золотой | `&e` | Жёлтый |
| `&7` | Серый | `&f` | Белый |

### Стили

| Код | Эффект |
|-----|--------|
| `&l` | Жирный |
| `&o` | Курсив |
| `&n` | Подчёркнутый |
| `&m` | Зачёркнутый |
| `&k` | Обфускация |
| `&r` | Сброс |

### HEX-цвет

```
&#FF6600  →  оранжевый
&#00AAFF  →  голубой
```

### Примеры

```json
"text": "&6[&eЗомби&6] &fЯ был... &cчеловеком... &7когда-то..."
"display_name": "&4[&cВраг&4]"
"text": "&#FF6600Огненное приветствие!"
"text": "&lВажное сообщение &r— обычный текст"
```

---

## 5. Действия (actions)

Действия выполняются при входе в узел (`actions` на узле) или при выборе варианта (`actions` на варианте).

```json
"give_stuff": {
  "text": "&aВозьми!",
  "actions": [
    { "type": "give_item", "item": "minecraft:diamond", "count": 3 },
    { "type": "play_sound", "sound": "minecraft:entity.player.levelup" }
  ]
}
```

### give_item — выдать предмет

```json
{ "type": "give_item", "item": "minecraft:diamond", "count": 1 }
```

### remove_item — забрать предмет

```json
{ "type": "remove_item", "item": "minecraft:golden_apple", "count": 1 }
```

### run_command — выполнить команду

```json
{ "type": "run_command", "command": "say Привет!" }
{ "type": "run_command", "command": "effect give @s minecraft:speed 60 1" }
```

`@s` — игрок. Команда выполняется от сервера с правами 2.

### teleport — телепортировать игрока

```json
{ "type": "teleport", "x": 100, "y": 64, "z": -200 }
{ "type": "teleport", "x": 0, "y": 5, "z": 0, "mode": "relative" }
{ "type": "teleport", "x": 0, "y": 64, "z": 0, "yaw": 90, "pitch": 0 }
```

| Поле | Описание |
|------|----------|
| `x`, `y`, `z` | Координаты |
| `yaw`, `pitch` | Поворот (необязательно) |
| `mode` | `"absolute"` (по умолч.) или `"relative"` |

### play_sound — звук

```json
{
  "type": "play_sound",
  "sound": "minecraft:entity.zombie.ambient",
  "volume": 1.0,
  "pitch": 1.2,
  "target": "player"
}
```

| Поле | Описание |
|------|----------|
| `sound` | ID звука |
| `volume` | Громкость (по умолч. 1.0) |
| `pitch` | Тон (по умолч. 1.0) |
| `target` | `"player"` или `"entity"` |

### give_effect — эффект

```json
{
  "type": "give_effect",
  "effect": "minecraft:regeneration",
  "duration": 200,
  "amplifier": 1,
  "target": "player"
}
```

| Поле | Описание |
|------|----------|
| `effect` | ID эффекта |
| `duration` | Длительность в тиках (по умолч. 200) |
| `amplifier` | Уровень (по умолч. 0) |
| `ambient` | Фоновые частицы (по умолч. false) |
| `particles` | Показывать частицы (по умолч. true) |
| `target` | `"player"` или `"entity"` |

### remove_effect — убрать эффект

```json
{ "type": "remove_effect", "effect": "minecraft:speed" }
{ "type": "remove_effect" }
```

Без `effect` — убирает все эффекты.

### spawn_particles — частицы

```json
{
  "type": "spawn_particles",
  "particle": "minecraft:heart",
  "count": 20,
  "spread": 0.5,
  "speed": 0.0,
  "target": "entity"
}
```

| Поле | Описание |
|------|----------|
| `particle` | ID частицы (только SimpleParticleType) |
| `count` | Количество (по умолч. 20) |
| `spread` | Разброс (по умолч. 0.5) |
| `speed` | Скорость (по умолч. 0.0) |
| `target` | `"player"` или `"entity"` |

### camera_shake — тряска камеры

```json
{ "type": "camera_shake", "intensity": 1.0, "duration": 20 }
```

### set_time — время суток

```json
{ "type": "set_time", "time": "night" }
{ "type": "set_time", "time": 6000 }
```

Именованные значения: `"day"` (1000), `"noon"` (6000), `"night"` (13000), `"midnight"` (18000).

### set_weather — погода

```json
{ "type": "set_weather", "weather": "thunder", "duration": 6000 }
```

Значения: `"clear"`, `"rain"`, `"thunder"`.

### set_var — переменная

```json
{ "type": "set_var", "name": "chapter", "value": "2" }
{ "type": "set_var", "name": "kills", "op": "inc", "value": "1" }
```

Подробнее в разделе [Переменные](#8-переменные).

### force_dialogue — принудительно запустить другой диалог

Немедленно заменяет текущий диалог на другой. Полезно для переходов между сценами.

```json
{
  "type": "force_dialogue",
  "dialogue_id": "chapter2_intro",
  "start_node": "greeting",
  "target_tag": "guide_npc",
  "radius": 32.0
}
```

| Поле | Описание |
|------|----------|
| `dialogue_id` | ID диалога для запуска |
| `start_node` | Начальный узел (по умолч. = entry) |
| `target_tag` | Scoreboard-тег NPC (по умолч. = текущий NPC) |
| `radius` | Радиус поиска NPC (по умолч. 32.0) |

### summon_npc — заспавнить NPC прямо из диалога

Создаёт нового NPC во время разговора. Можно сразу начать диалог с ним.

```json
{
  "type": "summon_npc",
  "entity": "minecraft:zombie",
  "name": "Таинственный незнакомец",
  "tags": ["mystery_npc"],
  "despawn": true,
  "walk_away": true,
  "start_dialogue": "mystery_npc"
}
```

| Поле | Описание |
|------|----------|
| `entity` | Тип моба |
| `name` | Имя (для target.name) |
| `tags` | Теги (для target.tag) |
| `despawn` | Удалить после диалога (по умолч. false) |
| `walk_away` | Уйти перед исчезновением (по умолч. false) |
| `start_dialogue` | ID диалога для немедленного запуска (необязательно) |

Если `start_dialogue` не указан — NPC просто появляется, игрок сам инициирует разговор ПКМ.

### notify_npc — зажечь `!` над другим NPC

Помечает NPC как «есть новый контент». Значок `!` появится над головой того NPC, даже если игрок уже говорил с ним раньше. Снимается автоматически когда игрок начинает разговор.

```json
{ "type": "notify_npc", "dialogue_id": "cursed_historian" }
```

**Типичный сценарий:** игрок получил квест от NPC A → пошёл к NPC B → при входе в нужный узел NPC B добавляется `notify_npc` на NPC A → игрок видит `!` над NPC A и знает что нужно вернуться.

```json
"gave_item": {
  "text": "&aВот ингредиенты.",
  "actions": [
    { "type": "remove_item", "item": "minecraft:spider_eye", "count": 3 },
    { "type": "update_quest", "quest_id": "my_quest", "objectives": ["&a[✓] Готово"] },
    { "type": "notify_npc", "dialogue_id": "quest_giver" }
  ]
}
```

### Квестовые действия

Подробнее в разделе [Система квестов](#7-система-квестов).

| Тип | Описание |
|-----|----------|
| `start_quest` | Начать квест |
| `update_quest` | Обновить цели |
| `complete_quest` | Завершить квест |
| `fail_quest` | Провалить квест |

---

## 6. Условия (conditions)

Условие в `condition` варианта ответа — кнопка скрывается если условие не выполнено. Также используются в `on_revisit.conditions`.

```json
{
  "text": "&a[Отдать яблоко]",
  "next": "give_apple",
  "condition": { "type": "has_item", "item": "minecraft:golden_apple", "count": 1 }
}
```

### has_item — есть ли предмет

```json
{ "type": "has_item", "item": "minecraft:golden_apple", "count": 1 }
```

### has_effect — есть ли эффект

```json
{ "type": "has_effect", "effect": "minecraft:regeneration" }
```

### health_below — здоровье ниже N

```json
{ "type": "health_below", "value": 10 }
{ "type": "health_below", "value": 50, "percent": true }
```

`percent: true` — значение в процентах от максимального HP.

### hunger_below — голод ниже N

```json
{ "type": "hunger_below", "value": 6 }
```

Шкала голода: 0–20.

### time_of_day — время суток

```json
{ "type": "time_of_day", "period": "night" }
```

| Значение | Диапазон тиков |
|----------|---------------|
| `"day"` | 0–12000 |
| `"dusk"` | 12000–13000 |
| `"night"` | 13000–23000 |
| `"dawn"` | 23000–1000 |

### weather — погода

```json
{ "type": "weather", "weather": "rain" }
```

Значения: `"clear"`, `"rain"`, `"thunder"`.

### dimension — измерение

```json
{ "type": "dimension", "dimension": "minecraft:the_nether" }
```

### biome — биом

```json
{ "type": "biome", "biome": "minecraft:desert" }
```

### visited_node — посещал ли узел

```json
{ "type": "visited_node", "dialogue": "chapter1_intro", "node": "accepted_quest" }
```

Прогресс общий для всех игроков на сервере.

### killed_mob — убито мобов

```json
{ "type": "killed_mob", "entity": "minecraft:zombie", "count": 5 }
```

Счётчик общий для всего сервера.

### if_var — значение переменной

```json
{ "type": "if_var", "name": "trust", "op": "eq", "value": "1" }
{ "type": "if_var", "name": "score", "op": "gte", "value": "10" }
{ "type": "if_var", "name": "chapter", "op": "exists" }
```

| Оператор | Описание |
|----------|----------|
| `"eq"` | Равно (строка) |
| `"neq"` | Не равно (строка) |
| `"gt"` | Больше (число) |
| `"lt"` | Меньше (число) |
| `"gte"` | Больше или равно (число) |
| `"lte"` | Меньше или равно (число) |
| `"exists"` | Переменная не пустая |

### quest_status — статус квеста

```json
{ "type": "quest_status", "quest_id": "cure_zombie", "status": "active" }
```

Статусы: `"active"`, `"completed"`, `"failed"`, `"none"` (квеста нет).

---

## 7. Система квестов

### start_quest — начать квест

```json
{
  "type": "start_quest",
  "quest": {
    "id": "cure_zombie",
    "title": "&6Лечение зомби",
    "description": "&fПомоги старому зомби найти лекарство",
    "objectives": [
      "&7[ ] Найти золотое яблоко",
      "&7[ ] Вернуться к зомби"
    ],
    "required_item": {
      "id": "minecraft:golden_apple",
      "count": 1
    }
  }
}
```

| Поле | Описание |
|------|----------|
| `quest.id` | Уникальный ID |
| `quest.title` | Заголовок (поддерживает цвета) |
| `quest.description` | Описание |
| `quest.objectives` | Список целей |
| `quest.required_item` | Отслеживаемый предмет (необязательно) — если уже есть, первая цель отмечается автоматически |

### update_quest — обновить цели

```json
{
  "type": "update_quest",
  "quest_id": "cure_zombie",
  "objectives": [
    "&a[✔] Найти золотое яблоко",
    "&7[ ] Вернуться к зомби"
  ]
}
```

### complete_quest / fail_quest

```json
{ "type": "complete_quest", "quest_id": "cure_zombie" }
{ "type": "fail_quest", "quest_id": "cure_zombie" }
```

### HUD и журнал

Активные квесты показываются в правом верхнем углу экрана (максимум 3).

- **K** — скрыть/показать HUD квестов
- **J** — открыть журнал (вкладки «Диалоги» и «Квесты»)

В журнале квесты разделены на: Активные / Завершённые / Проваленные.

---

## 8. Переменные

Строковые переменные, общие для всего сервера. Используются для хранения состояния сюжета.

### Запись

```json
{ "type": "set_var", "name": "chapter", "value": "2" }
{ "type": "set_var", "name": "kills", "op": "inc" }
{ "type": "set_var", "name": "score", "op": "inc", "value": "5" }
{ "type": "set_var", "name": "score", "op": "dec", "value": "2" }
```

| Оператор | Описание |
|----------|----------|
| `"set"` | Присвоить значение (по умолч.) |
| `"inc"` | Прибавить (`value` по умолч. = 1) |
| `"dec"` | Вычесть (`value` по умолч. = 1) |

### Проверка

```json
{ "type": "if_var", "name": "chapter", "op": "eq", "value": "2" }
{ "type": "if_var", "name": "chapter", "op": "exists" }
```

### Пример: сюжетная цепочка с памятью

```json
"first_meeting": {
  "text": "&fПривет! Первый раз видимся?",
  "actions": [{ "type": "set_var", "name": "met_bob", "value": "true" }],
  "next": "offer"
}
```

```json
{
  "text": "&aРад снова тебя видеть!",
  "condition": { "type": "if_var", "name": "met_bob", "op": "eq", "value": "true" }
}
```

---

## 9. Повторный диалог (on_revisit)

Когда диалог уже пройден (достигнут конечный узел), следующий ПКМ запускает `on_revisit` вместо основного диалога.

```json
"on_revisit": {
  "default": "&7*Зомби молча смотрит на тебя*",
  "default_start_node": "idle_chat",
  "conditions": [
    {
      "condition": { "type": "quest_status", "quest_id": "cure_zombie", "status": "active" },
      "text": "&fТы уже принёс яблоко?",
      "start_node": "check_apple"
    },
    {
      "condition": { "type": "quest_status", "quest_id": "cure_zombie", "status": "completed" },
      "text": "&aСпасибо тебе, друг..."
    }
  ]
}
```

### Логика

1. Условия проверяются сверху вниз
2. Первое сработавшее:
   - Если есть `start_node` — открывается полный диалог с этого узла
   - Если нет `start_node` — показывается короткое сообщение на несколько секунд
3. Если ни одно не сработало:
   - Если есть `default_start_node` — открывается полный диалог
   - Если есть `default` — показывается короткое сообщение

| Поле | Описание |
|------|----------|
| `default` | Сообщение по умолчанию |
| `default_start_node` | Узел для диалога по умолчанию |
| `conditions[].condition` | Условие |
| `conditions[].text` | Сообщение если условие выполнено |
| `conditions[].start_node` | Узел для диалога (необязательно) |

---

## 10. Автоспавн (summon)

Блок `summon` позволяет мобу появляться автоматически без команды. Игрок сам инициирует разговор ПКМ.

```json
"summon": {
  "entity": "minecraft:zombie",
  "custom_name": "Старый Зомби",
  "tags": ["old_zombie"],
  "despawn_after_dialogue": true,
  "walk_away_before_despawn": true,
  "spawn_position": "behind_player",
  "trigger": {
    "type": "on_join",
    "delay": 60
  }
}
```

### Поля summon

| Поле | Описание |
|------|----------|
| `entity` | Тип моба |
| `custom_name` | Имя (должно совпадать с `target.name`) |
| `tags` | Теги (должны совпадать с `target.tag`) |
| `spawn_position` | Позиция: `"behind_player"` (за спиной, 3 блока) |
| `despawn_after_dialogue` | Удалить после диалога |
| `walk_away_before_despawn` | Моб уходит перед исчезновением |
| `trigger` | Условие спавна |

### Триггеры

#### on_join — при входе в мир

```json
"trigger": { "type": "on_join", "delay": 100 }
```

Спавнится однократно при первом входе (если диалог ещё не пройден). `delay` — задержка в тиках.

#### after_dialogue — после другого диалога

```json
"trigger": {
  "type": "after_dialogue",
  "dialogue_id": "chapter1_intro",
  "delay": 60
}
```

Спавнится через `delay` тиков после завершения указанного диалога. Используется для сюжетных цепочек.

#### player_near — игрок рядом с точкой

```json
"trigger": {
  "type": "player_near",
  "x": 100, "y": 64, "z": -200,
  "radius": 8.0
}
```

#### player_entered_area — первый вход в зону

```json
"trigger": {
  "type": "player_entered_area",
  "x": 0, "y": 64, "z": 0,
  "radius": 15.0
}
```

Срабатывает только при входе, не при нахождении внутри.

#### player_looking_for_seconds — смотрит на точку

```json
"trigger": {
  "type": "player_looking_for_seconds",
  "x": 50, "y": 70, "z": 50,
  "radius": 64,
  "seconds": 3
}
```

#### on_player_death — при смерти игрока

```json
"trigger": { "type": "on_player_death", "delay": 20 }
```

### Пример сюжетной цепочки через summon

Историк спавнится при входе, Отшельник — после диалога с Историком:

```json
// cursed_historian.json
"summon": {
  "entity": "minecraft:zombie",
  "custom_name": "Историк",
  "tags": ["cursed_historian"],
  "trigger": { "type": "on_join", "delay": 60 }
}

// forest_hermit.json
"summon": {
  "entity": "minecraft:zombie",
  "custom_name": "Отшельник",
  "tags": ["forest_hermit"],
  "trigger": {
    "type": "after_dialogue",
    "dialogue_id": "cursed_historian",
    "delay": 120
  }
}
```

---

## 11. Иконка над NPC

Над NPC автоматически появляется жёлтый `!` в двух случаях:

1. **Диалог ещё не начат** — игрок никогда не разговаривал с этим NPC
2. **Вызвана команда `notify_npc`** — автор диалога явно указал, что у NPC появился новый контент

Иконка видна в радиусе 16 блоков и исчезает когда игрок начинает разговор.

### Управление через JSON

Чтобы зажечь `!` над другим NPC когда игрок попадает в нужный узел:

```json
"gave_stone": {
  "text": "&aОтлично. &fКамень у меня. &7Возвращайся к историку.",
  "actions": [
    { "type": "remove_item", "item": "minecraft:stone", "count": 1 },
    { "type": "notify_npc", "dialogue_id": "cursed_historian" }
  ]
}
```

После этого над Историком загорится `!`, даже если игрок уже говорил с ним раньше.

---

## 12. Внешний вид GUI

### Аватар

Текстура в окне диалога (голова NPC):

```json
"avatar": "mypack:textures/entity/my_npc.png"
```

Текстура должна быть в ресурспаке: `assets/mypack/textures/entity/my_npc.png`.  
Формат: стандартная скин-текстура 64×64 (вырезается область головы 8×8).

Через NBT команду (без ресурспака):
```
/data merge entity @e[name=МойНПС,limit=1] {DialogueAvatar:"minecraft:textures/entity/zombie/zombie.png"}
```

### Фон панели диалога

```json
"background": "mypack:textures/gui/dialogue_bg.png"
```

Без этого поля — стандартный тёмно-синий фон.

### Фон кнопок ответов

```json
"options_background": "mypack:textures/gui/option_bg.png"
```

### Размещение текстур

Формат: `namespace:path`. Путь в ресурспаке:
```
assets/<namespace>/textures/<path>.png
```

Ресурспак: `.minecraft/resourcepacks/`.

---

## 13. Команды

### /dialogue reload

Перезагрузить все диалоги из папки.

```
/dialogue reload
```

### /dialogue reload `<id>`

Перезагрузить один диалог и сбросить весь прогресс по нему (для тестирования).

```
/dialogue reload cursed_historian
```

### /dialogue test `<id>` [node]

Начать диалог с ближайшим подходящим мобом.

```
/dialogue test old_zombie
/dialogue test old_zombie check_apple
```

### /dialogue goto `<node>`

Перейти к узлу в текущем активном диалоге.

```
/dialogue goto reward
```

### /npc spawn `<id>`

Заспавнить NPC у своих ног. Тип моба берётся из JSON (`summon.entity`).

```
/npc spawn old_zombie
```

### /npc tag `<id>`

Назначить ближайшему мобу роль NPC (присвоить имя и тег из JSON).

```
/npc tag old_zombie
```

### /npc remove

Удалить ближайшего NPC.

### /npc list [radius]

Список всех NPC в радиусе (по умолч. 32 блока).

```
/npc list
/npc list 64
```

---

## 14. Назначение NPC в мире

Мод ищет мобов у которых совпадают **оба** поля: `target.name` (CustomName) и `target.tag` (scoreboard-тег).

### Способ 1 — /npc spawn (рекомендуется)

```
/npc spawn old_zombie
```

Мод сам заспавнит моба нужного типа с нужным именем и тегом.

### Способ 2 — вручную

```
/tag @e[type=minecraft:zombie,distance=..3,limit=1,sort=nearest] add old_zombie
/data merge entity @e[type=minecraft:zombie,distance=..3,limit=1,sort=nearest] {CustomName:'"Старый Зомби"',CustomNameVisible:1b}
```

### Способ 3 — автоспавн (summon)

Мод создаёт моба сам при нужном условии. Имя и теги назначаются автоматически.

---

## 15. Прогресс и сервер

Весь прогресс **общий для всех игроков** на сервере:

- `visited_node` срабатывает если кто-то один посетил узел
- `killed_mob` считает убийства всех игроков суммарно
- Переменные (`set_var`/`if_var`) одинаковы для всех
- Квесты видны всем в журнале
- Уведомления `notify_npc` видят все игроки

Прогресс **сохраняется между измерениями** — используется хранилище Overworld независимо от того в каком измерении находится игрок.

Прогресс **не сбрасывается** при смерти или смене измерения.

Для сброса прогресса конкретного диалога: `/dialogue reload <id>`

---

## 16. Клавиши

| Клавиша | Действие |
|---------|----------|
| **J** | Открыть журнал (диалоги + квесты) |
| **K** | Скрыть/показать HUD квестов |
| **ПКМ** | Следующий узел / выбрать вариант |
| **ЛКМ** | Предыдущий узел (линейные диалоги) |
| **ESC** | Закрыть диалог |
| **1–5** | Быстрый выбор варианта ответа |

---

## Полный пример диалога

Квест с несколькими NPC, условиями, переменными и автоспавном.

### Историк (cursed_historian.json)

```json
{
  "target": { "name": "Историк", "tag": "cursed_historian" },
  "display_name": "&e[&6Историк&e]",
  "entry": "start",

  "summon": {
    "entity": "minecraft:zombie",
    "custom_name": "Историк",
    "tags": ["cursed_historian"],
    "trigger": { "type": "on_join", "delay": 60 },
    "spawn_position": "behind_player",
    "despawn_after_dialogue": false
  },

  "on_revisit": {
    "default": "&e[&6Историк&e] &7Иди к отшельнику. Он знает где искать.",
    "conditions": [
      {
        "condition": { "type": "if_var", "name": "stone_cleansed", "value": "true" },
        "start_node": "return_with_stone"
      },
      {
        "condition": { "type": "quest_status", "quest_id": "cursed_stone", "status": "completed" },
        "text": "&e[&6Историк&e] &aБлагодарю. Деревня снова в безопасности."
      }
    ]
  },

  "nodes": {
    "start": {
      "text": "&fА, чужестранец. Мне нужна помощь.",
      "next": "explain"
    },
    "explain": {
      "text": "&fИз деревни пропал &cПроклятый камень&f. Он хранился у меня.",
      "next": "ask"
    },
    "ask": {
      "text": "&fВ лесу живёт отшельник. &7Он знает о проклятиях. &eПоговори с ним.",
      "options": [
        {
          "text": "&aЯ помогу",
          "next": "accept",
          "actions": [
            {
              "type": "start_quest",
              "quest": {
                "id": "cursed_stone",
                "title": "&cПроклятый камень",
                "description": "&fНайди способ обезвредить камень.",
                "objectives": [
                  "&7[ ] Поговорить с отшельником",
                  "&8[ ] Найти ведьму",
                  "&8[ ] Вернуть камень историку"
                ]
              }
            }
          ]
        },
        { "text": "&7Не моё дело", "next": "refuse" }
      ]
    },
    "accept": {
      "text": "&aСпасибо. Отшельник живёт к северу от деревни."
    },
    "refuse": {
      "text": "&7Понимаю. Если передумаешь — я здесь."
    },
    "return_with_stone": {
      "text": "&fКамень очищён?",
      "next": "reward"
    },
    "reward": {
      "text": "&eВозьми это. Ты заслужил.",
      "actions": [
        { "type": "give_item", "item": "minecraft:diamond", "count": 3 },
        { "type": "complete_quest", "quest_id": "cursed_stone" }
      ]
    }
  }
}
```

### Отшельник (forest_hermit.json)

```json
{
  "target": { "name": "Отшельник", "tag": "forest_hermit" },
  "display_name": "&2[&aОтшельник&2]",
  "entry": "start",

  "summon": {
    "entity": "minecraft:zombie",
    "custom_name": "Отшельник",
    "tags": ["forest_hermit"],
    "trigger": {
      "type": "after_dialogue",
      "dialogue_id": "cursed_historian",
      "delay": 120
    },
    "spawn_position": "behind_player",
    "despawn_after_dialogue": false
  },

  "nodes": {
    "start": {
      "text": "&7*Старик смотрит на тебя долгим взглядом* &fИсторик послал?",
      "options": [
        {
          "text": "&fДа, по поводу камня",
          "next": "knows",
          "condition": { "type": "quest_status", "quest_id": "cursed_stone", "status": "active" }
        },
        { "text": "&7Нет", "next": "go_away" }
      ]
    },
    "knows": {
      "text": "&fЕго нельзя уничтожить обычными способами. Нужна &dведьма с болот&f.",
      "next": "requirement"
    },
    "requirement": {
      "text": "&cНо она не поможет просто так. &fПринеси &e3 паучьих глаза &fи &e1 зелье ночного зрения&f.",
      "actions": [
        {
          "type": "update_quest",
          "quest_id": "cursed_stone",
          "objectives": [
            "&a[✓] Поговорить с отшельником",
            "&7[ ] Найти ведьму на болотах",
            "&7[ ] Принести: 3x паучий глаз + зелье ночного зрения",
            "&8[ ] Вернуть камень историку"
          ]
        }
      ],
      "next": "farewell"
    },
    "farewell": {
      "text": "&7*Отшельник возвращается к костру*"
    },
    "go_away": {
      "text": "&7Уходи. Мне не нужны гости."
    }
  }
}
```

### Ведьма (swamp_witch.json)

```json
{
  "target": { "name": "Ведьма", "tag": "swamp_witch" },
  "display_name": "&5[&dВедьма&5]",
  "entry": "start",

  "summon": {
    "entity": "minecraft:witch",
    "custom_name": "Ведьма",
    "tags": ["swamp_witch"],
    "trigger": {
      "type": "after_dialogue",
      "dialogue_id": "forest_hermit",
      "delay": 160
    },
    "spawn_position": "behind_player",
    "despawn_after_dialogue": false
  },

  "on_revisit": {
    "default": "&5[&dВедьма&5] &7Принеси то что просила.",
    "conditions": [
      {
        "condition": { "type": "has_item", "item": "minecraft:spider_eye", "count": 3 },
        "start_node": "has_items_check"
      }
    ]
  },

  "nodes": {
    "start": {
      "text": "&d*Ведьма оборачивается* &fКто тебя послал?",
      "options": [
        {
          "text": "&fОтшельник. Мне нужна твоя помощь с камнем",
          "next": "knows_hermit",
          "condition": { "type": "visited_node", "dialogue": "forest_hermit", "node": "requirement" }
        },
        { "text": "&7Я сам нашёл тебя", "next": "suspicious" }
      ]
    },
    "knows_hermit": {
      "text": "&dСтарый отшельник... &fМогу очистить камень.",
      "next": "demand"
    },
    "demand": {
      "text": "&cСначала плата. &f3 паучьих глаза и зелье ночного зрения.",
      "next": "wait"
    },
    "wait": {
      "text": "&7*Ведьма ждёт*"
    },
    "has_items_check": {
      "text": "&dА, принёс.",
      "options": [
        {
          "text": "&aВот, возьми",
          "next": "ritual",
          "condition": { "type": "has_item", "item": "minecraft:spider_eye", "count": 3 },
          "actions": [
            { "type": "remove_item", "item": "minecraft:spider_eye", "count": 3 },
            { "type": "remove_item", "item": "minecraft:potion", "count": 1 }
          ]
        },
        { "text": "&7Ещё не всё собрал", "next": "not_ready" }
      ]
    },
    "ritual": {
      "text": "&d*Ведьма начинает бормотать заклинание...*",
      "next": "done",
      "auto_next_ticks": 60
    },
    "done": {
      "text": "&aГотово. &fКамень очищен. Возвращайся к историку.",
      "actions": [
        { "type": "give_item", "item": "minecraft:enchanted_book", "count": 1 },
        { "type": "set_var", "name": "stone_cleansed", "value": "true" },
        { "type": "notify_npc", "dialogue_id": "cursed_historian" },
        {
          "type": "update_quest",
          "quest_id": "cursed_stone",
          "objectives": [
            "&a[✓] Поговорить с отшельником",
            "&a[✓] Найти ведьму на болотах",
            "&a[✓] Принести: 3x паучий глаз + зелье ночного зрения",
            "&7[ ] Вернуть камень историку"
          ]
        }
      ]
    },
    "not_ready": { "text": "&7Тогда возвращайся когда будет всё." },
    "suspicious": { "text": "&cБез рекомендации не помогаю. Уходи." }
  }
}
```

После того как ведьма очищает камень — над Историком автоматически появится `!`, указывая что нужно вернуться.
