# InteractEntity — Руководство разработчика диалогов

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
11. [Внешний вид GUI](#11-внешний-вид-gui)
12. [Команды](#12-команды)
13. [Назначение NPC в мире](#13-назначение-npc-в-мире)
14. [Полный пример диалога](#14-полный-пример-диалога)

---

## 1. Установка и структура файлов

### Папка с диалогами

После первого запуска мода в папке `.minecraft` появится:

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

После добавления файлов используйте `/dialogue reload` в игре.

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

| Поле | Тип | Обязательно | Описание |
|------|-----|-------------|----------|
| `target` | объект | да | Привязка к мобу |
| `target.name` | строка | да | Имя моба (бирка, CustomName) |
| `target.tag` | строка | да | Scoreboard-тег моба |
| `display_name` | строка | нет | Имя в GUI (по умолчанию = target.name) |
| `entry` | строка | да | ID начального узла |
| `nodes` | объект | да | Все узлы диалога |
| `invulnerable` | bool | нет | Защита NPC от урона (по умолч. `true`) |
| `avatar` | строка | нет | Текстура аватара (`namespace:path`) |
| `background` | строка | нет | Текстура фона панели диалога |
| `options_background` | строка | нет | Текстура фона панелей ответов |
| `on_revisit` | объект | нет | Поведение при повторном разговоре |
| `summon` | объект | нет | Конфигурация автоспавна |

---

## 3. Узлы (nodes)

Каждый диалог состоит из узлов. Тип узла определяется автоматически.

### Линейный узел

Есть `next`, нет `options`. ПКМ — следующий узел, ЛКМ — предыдущий.

```json
"intro": {
  "text": "&fЯ давно тебя жду...",
  "next": "question"
}
```

### Узел с выбором

Есть `options`. Показывает кнопки выбора.

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

### Все поля узла

| Поле | Тип | Описание |
|------|-----|----------|
| `text` | строка | Текст реплики |
| `random_text` | массив строк | Случайный текст из списка |
| `next` | строка | ID следующего узла (линейный) |
| `auto_next_ticks` | int | Автопереход через N тиков (только с `next`, без `options`) |
| `options` | массив | Варианты ответа |
| `actions` | массив | Действия при входе в узел |
| `camera` | строка | Режим камеры: `"npc"` или `"player"` |
| `camera_yaw_offset` | float | Смещение поворота камеры по горизонтали |
| `camera_pitch_offset` | float | Смещение поворота камеры по вертикали |

### Поля варианта ответа (option)

| Поле | Тип | Описание |
|------|-----|----------|
| `text` | строка | Текст кнопки |
| `next` | строка | Следующий узел (null = завершить) |
| `condition` | объект | Условие показа кнопки |
| `actions` | массив | Действия при выборе |

---

## 4. Форматирование текста

Поддерживаются `&`-коды и HEX-цвета. Работают везде: имя, текст, варианты ответа, квесты.

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

### Форматирование

| Код | Эффект |
|-----|--------|
| `&l` | **Жирный** |
| `&o` | *Курсив* |
| `&n` | Подчёркнутый |
| `&m` | ~~Зачёркнутый~~ |
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
```

---

## 5. Действия (actions)

Действия выполняются при входе в узел (поле `actions` узла) или при выборе варианта (поле `actions` варианта).

### give_item — выдать предмет

```json
{ "type": "give_item", "item": "minecraft:diamond", "count": 1 }
```

| Поле | Описание |
|------|----------|
| `item` | ID предмета |
| `count` | Количество (по умолч. 1) |

### remove_item — забрать предмет

```json
{ "type": "remove_item", "item": "minecraft:golden_apple", "count": 1 }
```

### run_command — выполнить команду

```json
{ "type": "run_command", "command": "say Привет от зомби!" }
```

Команда выполняется от сервера с правами 2. `@s` = игрок.

### teleport — телепортировать игрока

```json
{ "type": "teleport", "x": 100, "y": 64, "z": -200 }
{ "type": "teleport", "x": 0, "y": 10, "z": 0, "mode": "relative" }
```

| Поле | Описание |
|------|----------|
| `x`, `y`, `z` | Координаты (по умолч. текущая позиция) |
| `yaw`, `pitch` | Поворот (по умолч. текущий) |
| `mode` | `"absolute"` или `"relative"` |

### play_sound — воспроизвести звук

```json
{
  "type": "play_sound",
  "sound": "minecraft:entity.zombie.ambient",
  "volume": 1.0,
  "pitch": 1.0,
  "target": "player"
}
```

| Поле | Описание |
|------|----------|
| `sound` | ID звука |
| `volume` | Громкость (по умолч. 1.0) |
| `pitch` | Тон (по умолч. 1.0) |
| `target` | `"player"` или `"entity"` |

### give_effect — выдать эффект

```json
{
  "type": "give_effect",
  "effect": "minecraft:regeneration",
  "duration": 200,
  "amplifier": 1,
  "ambient": false,
  "particles": true,
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

Без `effect` — убирает все эффекты. `target`: `"player"` или `"entity"`.

### spawn_particles — спавнить частицы

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
| `target` | `"player"` или `"entity"` — центр спавна |

### camera_shake — тряска камеры

```json
{ "type": "camera_shake", "intensity": 1.0, "duration": 20 }
```

### set_time — установить время

```json
{ "type": "set_time", "time": "night" }
{ "type": "set_time", "time": 6000 }
```

Именованные значения: `"day"` (1000), `"noon"` (6000), `"night"` (13000), `"midnight"` (18000).

### set_weather — установить погоду

```json
{ "type": "set_weather", "weather": "rain", "duration": 6000 }
```

Значения: `"clear"`, `"rain"`, `"thunder"`.

### set_var — установить переменную

```json
{ "type": "set_var", "name": "trust", "value": "1" }
{ "type": "set_var", "name": "kills", "op": "inc", "value": "1" }
```

| Поле | Описание |
|------|----------|
| `name` | Имя переменной |
| `value` | Значение (по умолч. `""`) |
| `op` | `"set"`, `"inc"`, `"dec"` (по умолч. `"set"`) |

### force_dialogue — принудительно запустить диалог

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
| `start_node` | Начальный узел (по умолч. entry) |
| `target_tag` | Scoreboard-тег NPC (по умолч. текущий) |
| `radius` | Радиус поиска (по умолч. 32.0) |

### Квестовые действия

Подробнее в разделе [Система квестов](#7-система-квестов).

| Тип | Описание |
|-----|----------|
| `start_quest` | Начать квест |
| `update_quest` | Обновить цели квеста |
| `complete_quest` | Завершить квест |
| `fail_quest` | Провалить квест |

---

## 6. Условия (conditions)

Условия используются в `condition` варианта ответа — кнопка скрывается если условие не выполнено. Также используются в `on_revisit.conditions`.

### has_item — есть ли предмет

```json
{ "type": "has_item", "item": "minecraft:golden_apple", "count": 1 }
```

### has_effect — есть ли эффект

```json
{ "type": "has_effect", "effect": "minecraft:regeneration" }
```

### health_below — здоровье ниже

```json
{ "type": "health_below", "value": 10 }
{ "type": "health_below", "value": 50, "percent": true }
```

`percent: true` — значение в процентах от максимального HP.

### hunger_below — голод ниже

```json
{ "type": "hunger_below", "value": 6 }
```

Голод измеряется от 0 до 20.

### time_of_day — время суток

```json
{ "type": "time_of_day", "period": "night" }
```

| Значение | Диапазон тиков |
|----------|---------------|
| `"day"` | 0 – 12000 |
| `"dusk"` | 12000 – 13000 |
| `"night"` | 13000 – 23000 |
| `"dawn"` | 23000 – 1000 |

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
{ "type": "visited_node", "dialogue": "chapter1_intro", "node": "helped" }
```

Проверяет прогресс (общий для всех игроков на сервере).

### killed_mob — убито мобов

```json
{ "type": "killed_mob", "entity": "minecraft:zombie", "count": 5 }
```

Счётчик убийств общий для всего сервера.

### if_var — значение переменной

```json
{ "type": "if_var", "name": "trust", "op": "eq", "value": "1" }
{ "type": "if_var", "name": "kills", "op": "gte", "value": "10" }
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

Статусы: `"active"`, `"completed"`, `"failed"`, `"none"` (квеста не существует).

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
| `quest.id` | Уникальный ID квеста |
| `quest.title` | Заголовок (поддерживает цвета) |
| `quest.description` | Описание |
| `quest.objectives` | Список целей |
| `quest.required_item` | Отслеживаемый предмет (необязательно) |

Если у игрока уже есть нужный предмет при выдаче квеста — первая цель отмечается автоматически.

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

### HUD-трекер

Активные квесты отображаются в правом верхнем углу (максимум 3).  
Клавиша **K** — скрыть/показать трекер.  
Клавиша **J** — открыть журнал (вкладки «Диалоги» и «Квесты»).

---

## 8. Переменные

Переменные — строки, общие для всего сервера. Используются для хранения состояния сюжета.

### Запись

```json
{ "type": "set_var", "name": "chapter", "value": "2" }
{ "type": "set_var", "name": "kills", "op": "inc" }
{ "type": "set_var", "name": "score", "op": "inc", "value": "5" }
```

- `"set"` — присвоить значение
- `"inc"` — прибавить (пустое значение = +1)
- `"dec"` — вычесть (пустое значение = -1)

### Проверка

```json
{ "type": "if_var", "name": "chapter", "op": "eq", "value": "2" }
{ "type": "if_var", "name": "chapter", "op": "exists" }
```

### Пример сюжетной цепочки

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

После прохождения диалога (игрок дошёл до конечного узла), при следующем ПКМ запускается `on_revisit`.

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

1. Условия проверяются по порядку
2. Первое сработавшее:
   - Если есть `start_node` — открывается полный диалог с этого узла
   - Если нет `start_node` — показывается короткое сообщение
3. Если ни одно не сработало:
   - Если есть `default_start_node` — открывается полный диалог
   - Если есть `default` — показывается короткое сообщение

| Поле | Описание |
|------|----------|
| `default` | Сообщение по умолчанию |
| `default_start_node` | Узел для полного диалога по умолчанию |
| `conditions[].condition` | Условие |
| `conditions[].text` | Сообщение если условие выполнено |
| `conditions[].start_node` | Узел для полного диалога (необязательно) |

---

## 10. Автоспавн (summon)

Блок `summon` позволяет мобу появляться автоматически без команды.

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
| `entity` | Тип моба (`minecraft:zombie`) |
| `custom_name` | Имя (для совпадения с `target.name`) |
| `tags` | Теги (для совпадения с `target.tag`) |
| `despawn_after_dialogue` | Удалить после диалога |
| `walk_away_before_despawn` | Моб уходит перед исчезновением |
| `spawn_position` | Позиция спавна (`"behind_player"`) |
| `trigger` | Триггер спавна |

### Типы триггеров

#### on_join — при входе в мир

```json
"trigger": { "type": "on_join", "delay": 100 }
```

Спавнится однократно при первом входе игрока (если диалог ещё не пройден).

#### after_dialogue — после другого диалога

```json
"trigger": {
  "type": "after_dialogue",
  "dialogue_id": "chapter1_intro",
  "delay": 60
}
```

Спавнится через `delay` тиков после завершения указанного диалога.

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

### Логика деспавна

При `despawn_after_dialogue: true` после завершения диалога:
1. Если `walk_away_before_despawn: true` — моб идёт прочь ~10 блоков
2. Через 4 секунды появляются частицы портала и моб исчезает

### Важно

Спавн происходит **за спиной игрока** (3 блока). Мод автоматически:
- Присваивает имя и теги
- Открывает диалог без ПКМ
- Предотвращает повторный спавн если диалог уже пройден

---

## 11. Внешний вид GUI

### Аватар

Текстура в окне NPC. Берётся из поля `avatar`:

```json
"avatar": "mypack:textures/entity/my_npc.png"
```

Текстура должна находиться в ресурспаке в папке `assets/mypack/textures/entity/`.

Формат: стандартная скин-текстура 64×64 (вырезается область головы 8×8 пикселей).

Для установки через команду без ресурспака:
```
/data merge entity @e[name=МойНПС,limit=1] {DialogueAvatar:"minecraft:textures/entity/zombie/zombie.png"}
```

### Фон панели диалога

```json
"background": "mypack:textures/gui/dialogue_bg.png"
```

Текстура растягивается на всю панель диалога. Если не указана — рисуется стандартный тёмно-синий фон.

### Фон панелей ответов

```json
"options_background": "mypack:textures/gui/option_bg.png"
```

Текстура для каждой кнопки ответа отдельно.

### Размещение текстур

Текстуры указываются как `namespace:path`. Путь в ресурспаке:
```
assets/<namespace>/textures/<path>.png
```

Ресурспак кладётся в `.minecraft/resourcepacks/`.

---

## 12. Команды

### /dialogue reload

Перезагрузить все диалоги из папки.

```
/dialogue reload
```

### /dialogue reload `<id>`

Перезагрузить один диалог и **сбросить весь прогресс** (для тестирования).

```
/dialogue reload chapter1_intro
```

### /dialogue test `<id>` [node]

Начать диалог с ближайшим мобом.

```
/dialogue test old_zombie
/dialogue test old_zombie check_apple
```

### /dialogue goto `<node>`

Перейти к узлу в активном диалоге.

```
/dialogue goto reward
```

### /npc spawn `<id>` [entity_type]

Заспавнить NPC для диалога у своих ног.

```
/npc spawn old_zombie
/npc spawn old_zombie minecraft:skeleton
```

### /npc tag `<id>`

Назначить ближайшему мобу роль NPC для диалога.

```
/npc tag old_zombie
```

### /npc remove

Удалить ближайшего NPC.

### /npc list [radius]

Список всех NPC в радиусе (по умолч. 32).

```
/npc list
/npc list 64
```

---

## 13. Назначение NPC в мире

Чтобы моб реагировал на ПКМ, у него должны совпасть `target.name` и `target.tag` из JSON.

### Способ 1 — /npc spawn (рекомендуется для тестирования)

```
/npc spawn old_zombie
```

Мод сам заспавнит моба с нужным именем и тегом.

### Способ 2 — вручную через команды

Подойдите к мобу и выполните:

```
/tag @e[type=minecraft:zombie,distance=..3,limit=1,sort=nearest] add old_zombie
/data merge entity @e[type=minecraft:zombie,distance=..3,limit=1,sort=nearest] {CustomName:'"Старый Зомби"',CustomNameVisible:1b}
```

### Способ 3 — через автоспавн (summon)

Мод сам создаст моба при нужном условии. Имя и теги назначаются автоматически.

---

## 14. Полный пример диалога

Пример сюжетного диалога с квестом, переменными, условиями и автоспавном следующего персонажа.

```json
{
  "target": {
    "name": "Старый Зомби",
    "tag": "old_zombie"
  },
  "display_name": "&6[&eСтарый Зомби&6]",
  "entry": "start",

  "on_revisit": {
    "default": "&7*Зомби молча смотрит на тебя*",
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
  },

  "summon": {
    "entity": "minecraft:zombie",
    "custom_name": "Старый Зомби",
    "tags": ["old_zombie"],
    "despawn_after_dialogue": false,
    "trigger": {
      "type": "on_join",
      "delay": 100
    }
  },

  "nodes": {
    "start": {
      "random_text": [
        "&7*Зомби медленно поворачивается...*",
        "&7*Зомби смотрит на тебя пустым взглядом...*"
      ],
      "next": "intro",
      "actions": [
        { "type": "play_sound", "sound": "minecraft:entity.zombie.ambient", "volume": 0.8 }
      ]
    },

    "intro": {
      "text": "&fЯ был... &cчеловеком... &7когда-то давно...",
      "next": "question"
    },

    "question": {
      "text": "&fМне очень плохо. Ты... можешь помочь?",
      "options": [
        {
          "text": "&aКонечно, чем могу помочь?",
          "next": "explain"
        },
        {
          "text": "&6[Я убил немало зомби]",
          "next": "killer_branch",
          "condition": { "type": "killed_mob", "entity": "minecraft:zombie", "count": 3 }
        },
        {
          "text": "&d[Ночью говорят по-другому]",
          "next": "night_branch",
          "condition": { "type": "time_of_day", "period": "night" }
        },
        {
          "text": "&cНет, мне не до тебя",
          "next": "refuse"
        }
      ]
    },

    "explain": {
      "text": "&fМне нужно &eзолотое яблоко&f. Говорят, оно может... &7вернуть меня...",
      "next": "offer"
    },

    "killer_branch": {
      "text": "&cТы убивал нас?! &7...Но... я не могу злиться. Я &fне такой&7, как они...",
      "next": "offer",
      "actions": [
        { "type": "set_var", "name": "trust_zombie", "value": "1" }
      ]
    },

    "night_branch": {
      "text": "&9Ночью... мы чувствуем себя &fсвободнее&9. Но я устал от темноты...",
      "next": "offer"
    },

    "offer": {
      "text": "&fПринесёшь мне золотое яблоко?",
      "options": [
        {
          "text": "&aДа, я найду его!",
          "next": "quest_start",
          "actions": [
            {
              "type": "start_quest",
              "quest": {
                "id": "cure_zombie",
                "title": "&6Лечение зомби",
                "description": "&fСтарый зомби просит принести ему золотое яблоко",
                "objectives": [
                  "&7[ ] Найти золотое яблоко",
                  "&7[ ] Вернуться к Старому Зомби"
                ],
                "required_item": {
                  "id": "minecraft:golden_apple",
                  "count": 1
                }
              }
            }
          ]
        },
        {
          "text": "&cИзвини, не могу",
          "next": "refuse"
        }
      ]
    },

    "quest_start": {
      "text": "&eСпасибо... &fЯ буду ждать тебя здесь...",
      "actions": [
        { "type": "play_sound", "sound": "minecraft:block.note_block.harp", "pitch": 1.2 }
      ]
    },

    "check_apple": {
      "text": "&fТы принёс яблоко?",
      "options": [
        {
          "text": "&aДа, вот оно!",
          "next": "give_apple",
          "condition": { "type": "has_item", "item": "minecraft:golden_apple", "count": 1 }
        },
        {
          "text": "&cЕщё нет...",
          "next": "wait"
        }
      ]
    },

    "give_apple": {
      "text": "&7*Зомби берёт яблоко дрожащими руками*",
      "actions": [
        { "type": "remove_item", "item": "minecraft:golden_apple", "count": 1 },
        { "type": "camera_shake", "intensity": 0.6, "duration": 30 },
        { "type": "spawn_particles", "particle": "minecraft:heart", "count": 30, "target": "entity" },
        { "type": "play_sound", "sound": "minecraft:entity.player.levelup", "volume": 0.7 },
        {
          "type": "update_quest",
          "quest_id": "cure_zombie",
          "objectives": [
            "&a[✔] Найти золотое яблоко",
            "&a[✔] Вернуться к Старому Зомби"
          ]
        }
      ],
      "next": "finish"
    },

    "finish": {
      "text": "&eЯ чувствую себя... &aлучше&e... Спасибо тебе...",
      "options": [
        {
          "text": "&aРад помочь!",
          "next": "reward"
        },
        {
          "text": "&6[Мы теперь союзники]",
          "next": "trust_reward",
          "condition": { "type": "if_var", "name": "trust_zombie", "op": "eq", "value": "1" }
        }
      ]
    },

    "reward": {
      "text": "&fВозьми это... &7в знак благодарности...",
      "actions": [
        { "type": "complete_quest", "quest_id": "cure_zombie" },
        { "type": "give_item", "item": "minecraft:emerald", "count": 5 }
      ]
    },

    "trust_reward": {
      "text": "&6Ты понял меня... &fВозьми кое-что особенное...",
      "actions": [
        { "type": "complete_quest", "quest_id": "cure_zombie" },
        { "type": "give_item", "item": "minecraft:diamond", "count": 2 },
        { "type": "give_item", "item": "minecraft:emerald", "count": 5 }
      ]
    },

    "wait": {
      "text": "&7...Я подожду..."
    },

    "refuse": {
      "text": "&7Понимаю... &fМожет, другой путник поможет мне..."
    }
  }
}
```

---

## Прогресс и общий сервер

Весь прогресс **общий для всех игроков** на сервере:
- Условие `visited_node` срабатывает если кто-то один посетил узел
- `killed_mob` считает убийства всех игроков суммарно
- Переменные (`set_var`/`if_var`) одинаковы для всех
- Квесты видны всем в журнале

Это позволяет строить совместные истории, где действия одного влияют на всех.

---

## Клавиши

| Клавиша | Действие |
|---------|----------|
| **J** | Открыть журнал (диалоги + квесты) |
| **K** | Скрыть/показать HUD квестов |
| **ПКМ** | Следующий узел / выбрать вариант |
| **ЛКМ** | Предыдущий узел (в линейных диалогах) |
| **ESC** | Закрыть диалог |
| **1–5** | Быстрый выбор варианта ответа |
