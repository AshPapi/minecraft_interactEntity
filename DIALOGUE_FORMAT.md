# Формат диалогов — справочник

Полная спецификация JSON-диалогов мода **InteractEntity**, сверенная с парсерами в `src/main/java/net/ashpapi/interactentity/`.

Если в этом документе и `README.md` есть расхождения — этот документ авторитетный (`README.md` устарел по части новых фич).

---

## 1. Расположение и ID

Файлы лежат в `<minecraft>/interactentity/dialogues/`. Подпапки разрешены.

```
interactentity/dialogues/
  zombie.json          → ID: "zombie"
  showcase/mayor.json  → ID: "showcase/mayor"
```

После любой правки — `/dialogue reload` (без аргументов: сбрасывает прогресс **всех** диалогов и in-memory флаги спавна; с аргументом `<id>` — только один).

---

## 2. Корневые поля (DialogueTree)

| Поле | Тип | Обязат. | Описание |
|------|-----|---------|----------|
| `target` | object | да | См. §3 |
| `entry` | string | да | ID стартового узла |
| `nodes` | object | да | `{id: NodeJson}` |
| `display_name` | string | нет | Имя в GUI. По умолчанию = `target.name` |
| `scope` | string | нет | `"global"` (default) или `"per_player"` — где хранить прогресс |
| `repeatable` | bool | нет | `false` (default). Если `true` — диалог можно проходить заново |
| `invulnerable` | bool | нет | `true` (default) — NPC неуязвим во время диалога |
| `avatar` | string | нет | `namespace:path` — текстура аватара в GUI (берётся область 8×8 от (8,8)) |
| `background` | string | нет | Текстура фона панели диалога |
| `options_background` | string | нет | Текстура фона кнопок-вариантов |
| `faction` | string | нет | Название фракции (для UI) |
| `reputation_id` | string | нет | ID фракции для репутации. По умолчанию = `faction` |
| `character_info` | string | нет | Краткое описание (показывается где-нибудь в UI) |
| `visual` | object | нет | См. §11 |
| `summon` | object | нет | См. §8 — авто-спавн NPC |
| `triggers` | array | нет | См. §9 — триггеры авто-старта диалога с уже спавненным NPC |
| `routines` | array | нет | См. §10 — расписание поведения NPC |
| `on_revisit` | object | нет | См. §7 — поведение при повторном разговоре |

### Поле `start_trigger`
Старый legacy-формат (один триггер). Если есть `triggers[]` — игнорируется.

---

## 3. target — кого ищем

| Поле | Тип | Обязат. | Описание |
|------|-----|---------|----------|
| `name` | string | да | Должен совпадать с `CustomName` моба |
| `tag` | string | да | Должен быть в scoreboard-тегах моба |
| `entity_type` | string | нет | Если задан — проверяется тип сущности (`minecraft:zombie`, `interactentity:custom_npc`, …) |
| `faction` | string | нет | Метаданные |

Все указанные поля должны совпасть, иначе ПКМ по мобу ничего не сделает.

---

## 4. Узлы (Node)

Тип узла определяется автоматически:

| Тип | Признак | Поведение |
|-----|---------|-----------|
| **Линейный** | есть `next`, нет `options` | ПКМ → следующий узел |
| **Выбор** | есть `options` | Игрок выбирает кнопку |
| **Конец** | нет `next` и нет `options` | Диалог закрывается. `"next": null` тоже считается концом |

### Поля узла

| Поле | Тип | Описание |
|------|-----|----------|
| `text` | string | Реплика. По умолчанию `""` |
| `random_text` | array | Массив строк — при входе берётся случайная. Перекрывает `text` |
| `next` | string \| null | ID следующего узла |
| `auto_next_ticks` | int | Автоматический переход через N тиков (без ПКМ). 20 тиков = 1 сек |
| `options` | array | См. §5 |
| `actions` | array | Действия при входе в узел (см. §6) |
| `camera` | string | Режим камеры. По умолчанию `"npc"` |
| `camera_yaw_offset` | float | Сдвиг ракурса по горизонтали |
| `camera_pitch_offset` | float | Сдвиг ракурса по вертикали |

---

## 5. Опции (Option)

| Поле | Тип | Описание |
|------|-----|----------|
| `text` | string | Текст на кнопке (обязат.) |
| `next` | string \| null | Куда переходить. `null` или отсутствие → конец |
| `condition` | object | Если задано и false — кнопка **скрывается** (см. §6.2) |
| `actions` | array | Действия при клике на эту опцию |
| `locked` | bool | Если `true` — кнопка отображается, но недоступна |
| `lock_reason` | string | Текст-причина блокировки |

**Важно:** опция поддерживает только **одно** `condition`. Compound (and/or) не реализованы. Если нужна составная логика — делай промежуточный узел.

---

## 6. Действия (actions)

Все 28 типов с точными именами полей (сверено с `ActionRegistry`).

### 6.1 Базовые

| `type` | Поля |
|--------|------|
| `give_item` | `item`, `count?` (default 1) |
| `remove_item` | `item`, `count?` (default 1) |
| `run_command` | `command` (без `/`, исполняется от сервера perm-level 2, `@s` = игрок) |
| `teleport` | `x?`, `y?`, `z?`, `yaw?`, `pitch?`, `mode?` (`"absolute"` default или `"relative"`) |
| `play_sound` | `sound`, `volume?` (1.0), `pitch?` (1.0), `target?` (`"player"` default, `"entity"`) |
| `give_effect` | `effect`, `duration?` (200), `amplifier?` (0), `ambient?` (false), `particles?` (true), `target?` |
| `remove_effect` | `effect?` (без него — все эффекты), `target?` |
| `spawn_particles` | `particle`, `count?` (20), `spread?` (0.5), `speed?` (0.0), `target?` (`"entity"` default, `"player"`) |
| `camera_shake` | `intensity?` (1.0), `duration?` (20 тиков) |
| `set_time` | `time`: `"day"` / `"noon"` / `"night"` / `"midnight"` или число тиков |
| `set_weather` | `weather`: `"clear"` / `"rain"` / `"thunder"`; `duration?` (6000) |

### 6.2 Сценарные

| `type` | Поля |
|--------|------|
| `set_var` | `name`, `value?` (default `""`), `op?`: `"set"` (default), `"inc"`, `"dec"` |
| `fire_event` | `tag?` (default `"default"`) — постит `DialogueChoiceEvent` для Forge/KubeJS |
| `schedule_event` | `id?` (auto), `delay` (тики), `actions: [...]` — отложенное исполнение |
| `force_dialogue` | `dialogue_id`, `target_tag?`, `radius?` (32.0), `start_node?` |
| `notify_npc` | `dialogue_id` — зажигает `!` над NPC с этим диалогом |
| `summon_npc` | `entity?` (default `minecraft:zombie`), `name?`, `tags?`, `despawn?`, `walk_away?`, `start_dialogue?`, `spawn_position?` |

### 6.3 Квесты

| `type` | Поля |
|--------|------|
| `start_quest` | `quest`: см. §12 |
| `update_quest` | `quest_id`, `objectives: [...]` — заменяет весь список (использовать осторожно: ломает счётчик kills) |
| `complete_objective` | `quest_id` + **одно из**: `objective` (0-индекс) / `objective_number` (1-индекс) / `objective_text` (по тексту) |
| `complete_quest` | `quest_id` |
| `fail_quest` | `quest_id` |

### 6.4 Социальные (репутация, подарки, NPC)

| `type` | Поля |
|--------|------|
| `add_reputation` | `id`, `value`, `label?` |
| `give_gift` | `character_id`, `item`, `amount?` (1), `reputation?` (5), `label?`, `success_message?`, `cooldown_message?` — есть кулдаун 1 час на персонажа |
| `set_relationship` | `npc_a`, `npc_b`, `relationship` (произвольная строка типа `"ally"`, `"enemy"`) |
| `set_companion` | `enable?` (default true) — делает NPC спутником игрока |
| `set_home` | `x?`, `y?`, `z?` (default — позиция NPC), `radius?` (16) |
| `play_emote` | `emote`, `duration_ticks?` (зависит от эмоции) — работает только на `interactentity:custom_npc`. Список см. §13 |

---

## 7. Условия (conditions)

19 типов (сверено с `ConditionRegistry`):

| `type` | Поля | Семантика |
|--------|------|-----------|
| `has_item` | `item`, `count?` (1) | Игнорирует предметы с NBT (хвостом `hasTag`) |
| `visited_node` | `dialogue`, `node` | true если игрок проходил этот узел |
| `quest_status` | `quest_id`, `status` (`"active"`, `"completed"`, `"failed"`, `"none"`) | |
| `if_var` | `name`, `op?` (default `"eq"`: `eq`/`neq`/`gt`/`lt`/`gte`/`lte`/`exists`), `value?` | Сравнивает переменную |
| `reputation` | `id`, `op?` (default `"gte"`: `eq`/`neq`/`gt`/`lt`/`gte`/`lte`), `value` | |
| `killed_mob` | `entity`, `tag?`, `count?` (1) | Счётчик убийств общий на сервер |
| `has_effect` | `effect` | |
| `health_below` | `value`, `percent?` (false) | `percent: true` — value трактуется как % от max HP |
| `hunger_below` | `value` | Шкала 0–20 |
| `time_of_day` | `period?` (`"day"`/`"dusk"`/`"night"`/`"dawn"`) | |
| `weather` | `weather`: `"clear"`/`"rain"`/`"thunder"` | |
| `dimension` | `dimension`: `minecraft:overworld` и т.п. | |
| `biome` | `biome`: `minecraft:desert` и т.п. | |
| `can_give_gift` | `character_id` | true если кулдаун подарка истёк |
| `npc_relationship` | `npc_a`, `npc_b`, `relationship` | |
| `has_advancement` | `advancement` | Vanilla advancement id |
| `experience_level` | `level`, `op?` (default `"gte"`) | |
| `is_raining` | — | |
| `is_night` | — | true 13000–23000 тиков |

---

## 8. Авто-спавн NPC (`summon`)

Если хочешь, чтобы NPC появился автоматически — добавь блок `summon`. Иначе спавни через `/npc spawn <id>` или вручную.

| Поле | Тип | Обязат. | Описание |
|------|-----|---------|----------|
| `entity` | string | да | Тип сущности |
| `custom_name` | string | да | Должно совпадать с `target.name` |
| `tags` | array | нет | Должно содержать `target.tag` |
| `trigger` | object | **да** | См. ниже — без этого NPE при загрузке |
| `spawn_position` | string | нет | `"behind_player"` (default), `"front_of_player"` и т.п. |
| `despawn_after_dialogue` | bool | нет | false — после диалога моб исчезает |
| `walk_away_before_despawn` | bool | нет | false — сперва уходит ~10 блоков |

### Типы спавн-триггеров (`summon.trigger`)

| `type` | Поля | Когда срабатывает |
|--------|------|-------------------|
| `on_join` | `delay?` (тики) | Через delay после входа игрока в мир |
| `after_dialogue` | `dialogue_id`, `delay?` | Через delay после **завершения** указанного диалога (дойдёт до end-нода) |
| `player_near` | `x`, `y`, `z`, `radius?` (8.0) | Игрок в радиусе от точки |
| `player_entered_area` | `x`, `y`, `z`, `radius?` (8.0) | Первый вход в зону (не повторяется пока внутри) |
| `player_looking_for_seconds` | `x`, `y`, `z`, `radius?` (8.0), `seconds?` (2) | Игрок смотрит N секунд на точку |
| `on_player_death` | `delay?` | После смерти игрока |

**Важно:** для не-`repeatable` диалогов спавн блокируется через in-memory `TRIGGERED_DIALOGUES` (сбрасывается только полным `/dialogue reload`) и через `hasVisited(entry)`. Если NPC «не спавнится снова» — это потому что прошлый раз entry уже был посещён.

---

## 9. Триггеры взаимодействия (`triggers`)

Top-level массив. Запускает диалог с **уже существующим** NPC при событии.

| `type` | Поля | Когда срабатывает |
|--------|------|-------------------|
| `proximity` | `radius?` (4.0) | Игрок в радиусе (poll каждые 10 тиков, cooldown 200 тиков) |
| `on_hurt` | `radius?` (4.0) | Игрок ударил NPC |
| `on_death` | `radius?` (4.0) | Игрок убил NPC |
| `health_below` | `threshold?` (0.5) | HP NPC опустилось ниже доли от max (0..1) |

**Не путать с `summon.trigger`** — там другой набор типов, и он только для спавна.

---

## 10. Рутины (`routines`)

Расписание поведения NPC в течение игрового дня (0..24000 тиков).

| Поле | Тип | Описание |
|------|-----|----------|
| `type` | string | `"idle_at"` / `"wander"` / `"patrol"` |
| `start` | int | Начало периода (default 0) |
| `end` | int | Конец (default 24000). Если `start > end` — период перекрывает полночь |
| `x`, `y`, `z` | int | Опорная точка (для `idle_at`, `wander`) |
| `radius` | int | Радиус блуждания (default 8) |
| `waypoints` | array | Для `patrol`: `[{x,y,z}, ...]` |

---

## 11. on_revisit — повторный разговор

Срабатывает после того как игрок дошёл до end-нода (диалог помечен завершённым).

```json
"on_revisit": {
  "default": "&7*тишина*",
  "default_start_node": "hub",
  "conditions": [
    { "condition": {...}, "text": "короткое сообщение" },
    { "condition": {...}, "start_node": "имя_узла" }
  ]
}
```

Логика:
1. Условия проверяются сверху вниз
2. Первое match'нувшее: если есть `start_node` — открывается полный диалог с этого узла; иначе — короткое `text`
3. Если ни одно не match — fallback: `default_start_node` (полный диалог) или `default` (текст)

---

## 12. Визуал (`visual`)

Применяется к спавненному `interactentity:custom_npc`:

| Поле | Тип | Описание |
|------|-----|----------|
| `model` | string | `namespace:geo/file.geo.json` |
| `texture` | string | `namespace:textures/entity/file.png` |
| `scale` | float | 0.1..5.0 (валидируется при `/npc set_scale`) |

---

## 13. Квесты — детали

### `start_quest.quest`

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | string | Уникальный ID |
| `title` | string | Краткое название |
| `description` | string | Описание для журнала |
| `objectives` | array | Список строк. **Не пиши `[ ]`/`[✓]` руками** — мод сам ставит галочки |
| `required_item` | object | `{id, count?}` — если у игрока уже есть нужное кол-во, первая цель закрывается сразу |
| `required_kills` | object | `{entity, tag?, count, objective?}` — авто-счётчик убийств. `objective` — индекс цели для подписи `(N/M)` (default 0) |
| `deadline` | object | `{type, value?}`: `"ticks"`/`"game_days"` (`value` обязат.), `"sunset"`/`"sunrise"` |

### `complete_objective` нюанс

Используй **одно** из трёх:
- `"objective": 1` — индекс с нуля
- `"objective_number": 2` — индекс с единицы
- `"objective_text": "Принести меч"` — поиск по нормализованному тексту

Поле `"index"` **не поддерживается**, молча провалится в warn.

---

## 14. Репутация и подарки

1. Назови фракцию: `faction` + `reputation_id` в корне диалога
2. Начисляй: `add_reputation` action — `{id, value, label?}`
3. Читай в условиях: `reputation` condition — `{id, op, value}`
4. Показывай в тексте: плейсхолдер `{reputation:id}`
5. Подарки: `give_gift` берёт предмет, начисляет репутацию, ставит 1-часовой кулдаун; проверка кулдауна — `can_give_gift`

---

## 15. Плейсхолдеры

В любом текстовом поле (`text`, `display_name`, `random_text`, `options[].text` и т.п.):

| Плейсхолдер | Что подставляется |
|-------------|-------------------|
| `{player}` | Имя игрока |
| `{player_uuid}` | UUID игрока |
| `{npc_uuid}` | UUID NPC |
| `{var:NAME}` | Значение переменной (`set_var`) |
| `{reputation:ID}` | Текущая репутация фракции |

Реализация: `PlaceholderResolver.java`.

---

## 16. Эмоции (`play_emote`)

Работают только на `interactentity:custom_npc`. Доступные ключи (сверено с `CustomNpcEntity.java`):

`beckon`, `bow`, `celebrate`, `clap`, `confused`, `crossed_arms`, `dismiss`, `facepalm`, `handshake`, `happy`, `laugh`, `no` (= shake_head), `nod`, `please`, `point`, `scared`, `shake_head`, `shrug`, `surprised`, `think`, `wave`, `yawn`

Удалены: `angry`, `sad`, `salute`.

---

## 17. Форматирование текста

| Код | Эффект |
|-----|--------|
| `&0`..`&9`, `&a`..`&f` | Цвета (как в Minecraft) |
| `&l` | Жирный |
| `&o` | Курсив |
| `&n` | Подчёркивание |
| `&m` | Зачёркнутый |
| `&k` | Мерцающие символы |
| `&r` | Сброс |
| `&#RRGGBB` | Произвольный HEX-цвет |

---

## 18. Scope

| Значение | Где хранится прогресс |
|----------|------------------------|
| `"global"` (default) | На сервере, общий для всех игроков |
| `"per_player"` | Привязан к UUID игрока |

Влияет на: переменные, репутацию, прохождение узлов, квесты, отношения.

---

## 19. Команды

| Команда | Что делает |
|---------|------------|
| `/dialogue reload` | Перезагружает все диалоги, сбрасывает прогресс и in-memory флаги спавна |
| `/dialogue reload <id>` | Перезагружает один диалог + сбрасывает его прогресс (но **не** in-memory флаги спавна — баг) |
| `/dialogue test <id> [node]` | Открывает диалог с ближайшим подходящим мобом без проверок |
| `/dialogue goto <node>` | В активном диалоге — прыжок к узлу |
| `/npc spawn <id>` | Спавнит NPC на месте игрока (использует `summon.entity` + `target.name/tag`) |
| `/npc tag <id>` | Присваивает имя+тег ближайшему мобу |
| `/npc remove` | Удаляет ближайшего NPC |
| `/npc list [radius]` | Список NPC в радиусе (default 32) |

---

## 20. Подводные камни

1. **`summon` без `trigger`** → NPE при загрузке. Триггер внутри `summon` обязателен.
2. **`update_quest` ломает счётчик kills**, потому что заменяет весь `objectives[]`. Если нужна автоматическая подпись `(N/M)` — используй `complete_objective` или не трогай objectives вручную.
3. **`triggers[]` ≠ `summon.trigger`**. Первое — для авто-старта диалога с существующим NPC (`proximity`, `on_hurt`, `on_death`, `health_below`). Второе — для спавна (`on_join`, `after_dialogue`, …).
4. **Не-repeatable диалог не перевыдаст after_dialogue-спавн дочернего NPC**, если энтри-нода дочернего уже посещался. Сброс: `/dialogue reload` (без аргумента).
5. **ESC не помечает диалог завершённым**. Чтобы `on_revisit` сработал — нужен реальный end-нод (`text` без `next`/`options`, либо `"next": null`).
6. **`has_item` игнорирует предметы с NBT** (зачарованные, переименованные через анвил и т.п.).
7. **Опции поддерживают только одно `condition`**. Compound — через промежуточный узел.
8. **`avatar` — overlay-текстуры профессий жителей пустые в области (8,8)-(16,16)**. Используй базовые скины (`villager.png`) или текстуры мода.
9. **`fire_event` тег** ловится через `MinecraftForge.EVENT_BUS` подписку на `DialogueChoiceEvent` (поле `source == "action"`).
10. **`schedule_event`** не сохраняется через рестарт сервера если игрок офлайн — проверь `DelayedEventHandler`.
