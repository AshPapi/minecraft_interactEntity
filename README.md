# InteractEntity

A mod for Minecraft Forge 1.20.1. Lets you build full-featured dialogues with mobs through JSON files.

**Language / Язык:** [🇬🇧 English](#english) · [🇷🇺 Русский](#русский)

---

<a id="english"></a>

## English

> [🇷🇺 Перейти к русской версии](#русский)

A mod for Minecraft Forge 1.20.1. Lets you create dialogues with any mob using JSON files.

---

### Getting started

A dialogue is a JSON file describing: which exact mob to talk with, what it says, and what happens in response to player actions. When the player right-clicks a matching mob — the mod finds the JSON and opens the dialogue GUI.

#### Where files live

After the first launch the mod creates a folder next to `.minecraft`:

```
.minecraft/
  interactentity/
    dialogues/
      zombie.json        ← dialogue ID: "zombie"
      story/
        intro.json       ← dialogue ID: "story/intro"
```

Subfolders are supported. The file name (without `.json`) plus its path inside `dialogues/` becomes the **dialogue ID** — used everywhere else: in conditions, triggers, quests, commands.

After adding or editing files run `/dialogue reload` — without it the mod won't see your changes.

---

### 1. Nodes — the building block

Before looking at the file structure you need to understand a **node**. A node is one piece of dialogue. At any moment the player is in a specific node: sees the NPC text and either right-clicks to continue, or picks one of the answer options.

#### Three node types

The type is detected automatically — by which fields are present.

**Linear node** — NPC says something, player right-clicks to advance to the next node. Used for monologues, intros, narration.

```json
"start": {
  "text": "&fI've been waiting for you, traveler...",
  "next": "continue"
}
```

`"next": "continue"` is the next node's ID. Right-click jumps to the node named `"continue"`. Has `next`, no `options` — that's a linear node.

**Choice node** — NPC asks a question, player picks an option with mouse or keys 1–5. Used for branching dialogue.

```json
"question": {
  "text": "&fWill you help me?",
  "options": [
    { "text": "&aOf course!", "next": "accept" },
    { "text": "&cSorry, no", "next": "refuse" }
  ]
}
```

`options` is the list of answers. Each has `text` (button label) and `next` (where it leads). Has `options` → choice node.

**End node** — dialogue closes. NPC says a final line, player right-clicks, GUI closes. Used as a final point of a branch.

```json
"end": {
  "text": "&7*The zombie shuffles into the darkness*"
}
```

Has neither `next` nor `options` → end node.

#### How transitions form a dialogue

Nodes are linked through `next`. Imagine a graph: each node is a vertex, each `next` or option is an edge. The dialogue starts at the node specified in `entry` and walks the graph until it hits an end node.

```
start → intro → question → accept → reward (end)
                         ↘ refuse → bye (end)
```

#### All node fields

| Field | Type | Description |
|-------|------|-------------|
| `text` | string | NPC's line |
| `random_text` | string array | Random text — picks one from the list each time the node is entered |
| `next` | string | Next node ID (makes the node linear) |
| `auto_next_ticks` | number | Auto-advance after N ticks without right-click. 20 ticks = 1 second |
| `options` | array | Answer options (makes the node a choice node) |
| `actions` | array | Actions performed **on enter** of this node |

#### All option fields

| Field | Type | Description |
|-------|------|-------------|
| `text` | string | Button label |
| `next` | string | Next node ID. If absent — dialogue ends |
| `condition` | object | Visibility condition. If false — option isn't shown |
| `actions` | array | Actions performed **when this option is picked** |

**Difference between node `actions` and option `actions`:**
- Node `actions` — fire whenever the player enters this node, regardless of how
- Option `actions` — fire only when the player picks this specific option

---

### 2. File skeleton

```json
{
  "target": {
    "name": "Old Zombie",
    "tag": "old_zombie"
  },
  "display_name": "&6[&eOld Zombie&6]",
  "entry": "start",
  "nodes": {
    "start": {
      "text": "&fHello, traveler...",
      "next": "end"
    },
    "end": {
      "text": "&7*The zombie falls silent*"
    }
  }
}
```

#### How the mod finds the right mob

The `target` field is the mob's "address". On right-click the mod checks **two conditions at once**:
1. The mob's `CustomName` (nameplate) matches `target.name`
2. The mob has a scoreboard tag matching `target.tag`

Both match → the dialogue opens. Otherwise nothing happens.

Why two? Because mob names aren't unique, but tags are Minecraft's flexible mechanism for marking specific entities.

#### All root fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `target.name` | string | yes | Mob name (CustomName plate) |
| `target.tag` | string | yes | Scoreboard tag |
| `display_name` | string | no | Name in GUI. Falls back to `target.name`. Supports formatting |
| `entry` | string | yes | Starting node — every fresh dialogue begins here |
| `nodes` | object | yes | All dialogue nodes |
| `repeatable` | bool | no | `true` — dialogue can be replayed. Default `false` — after completion the NPC stops responding |
| `invulnerable` | bool | no | `true` (default) — NPC is invulnerable during dialogue |
| `avatar` | string | no | Avatar texture in GUI, format `namespace:path` |
| `background` | string | no | Custom background texture for the dialogue panel |
| `options_background` | string | no | Custom background texture for option panels |
| `on_revisit` | object | no | What happens when the player approaches the NPC after completing the dialogue |
| `summon` | object | no | Automatic NPC spawning |

---

### 3. Text formatting

Supported in any text field: `text`, `display_name`, `random_text`, options, quests.

#### Color codes

Use `&` followed by a letter or digit:

| Code | Color | Code | Color |
|------|-------|------|-------|
| `&0` | Black | `&8` | Dark Gray |
| `&1` | Dark Blue | `&9` | Blue |
| `&2` | Dark Green | `&a` | Green |
| `&3` | Dark Aqua | `&b` | Aqua |
| `&4` | Dark Red | `&c` | Red |
| `&5` | Dark Purple | `&d` | Pink |
| `&6` | Gold | `&e` | Yellow |
| `&7` | Gray | `&f` | White |

#### Style codes

| Code | Effect |
|------|--------|
| `&l` | Bold |
| `&o` | Italic |
| `&n` | Underline |
| `&m` | Strikethrough |
| `&k` | Obfuscated (flickering chars) |
| `&r` | Reset all styles and colors |

#### HEX color

Arbitrary color via `&#RRGGBB`:

```
&#FF6600  →  orange
&#00AAFF  →  light blue
```

#### Example

```json
"display_name": "&6[&eOld Zombie&6]"
"text": "&fI was... &chuman... &7once, long ago..."
"text": "&#FF6600*Flash of fire*"
"text": "&lWARNING! &rThis is important."
```

Color persists until the next code or end of line. `&r` resets everything back to white.

---

### 4. Actions

Actions are what **happens** in dialogue: give an item, run a command, start a quest, play a sound. They fire either on node entry or when picking an option.

```json
"reward_node": {
  "text": "&aTake this as a token of gratitude!",
  "actions": [
    { "type": "give_item", "item": "minecraft:diamond", "count": 3 },
    { "type": "play_sound", "sound": "minecraft:entity.player.levelup", "volume": 1.0 }
  ]
}
```

On entering `reward_node` the player gets 3 diamonds and hears a sound.

#### give_item — give an item

```json
{ "type": "give_item", "item": "minecraft:golden_apple", "count": 1 }
```

- `item` — item ID in `namespace:name` format
- `count` — quantity (default 1)

#### remove_item — take away an item

```json
{ "type": "remove_item", "item": "minecraft:spider_eye", "count": 3 }
```

Removes items from the inventory. Usually paired with the `has_item` condition — first verify, then take.

```json
{
  "text": "&aHere",
  "next": "ritual",
  "condition": { "type": "has_item", "item": "minecraft:spider_eye", "count": 3 },
  "actions": [{ "type": "remove_item", "item": "minecraft:spider_eye", "count": 3 }]
}
```

#### run_command — run a server command

```json
{ "type": "run_command", "command": "effect give @s minecraft:speed 60 2" }
```

- `command` — command without leading slash
- `@s` — the player having the dialogue
- Runs as the server with permission level 2

#### teleport — teleport the player

```json
{ "type": "teleport", "x": 100, "y": 64, "z": -200 }
{ "type": "teleport", "x": 0, "y": 5, "z": 0, "mode": "relative" }
```

- `x`, `y`, `z` — coordinates
- `yaw`, `pitch` — rotation after teleport (optional)
- `mode` — `"absolute"` (specific coords, default) or `"relative"` (offset from current position)

#### play_sound — play a sound

```json
{
  "type": "play_sound",
  "sound": "minecraft:entity.zombie.ambient",
  "volume": 0.8,
  "pitch": 1.2,
  "target": "player"
}
```

- `sound` — sound ID
- `volume` — 0.0 to 2.0 (default 1.0)
- `pitch` — above 1.0 = higher pitch, below = lower (default 1.0)
- `target` — `"player"` (sound on player) or `"entity"` (sound on NPC)

#### give_effect — apply a potion effect

```json
{
  "type": "give_effect",
  "effect": "minecraft:regeneration",
  "duration": 200,
  "amplifier": 1,
  "target": "player"
}
```

- `effect` — effect ID
- `duration` — duration in ticks (200 ticks = 10 seconds)
- `amplifier` — level: 0 = level I, 1 = level II, etc.
- `ambient` — `true` makes particles transparent like a beacon (default `false`)
- `particles` — `false` hides particles (default `true`)
- `target` — `"player"` or `"entity"`

#### remove_effect — remove an effect

```json
{ "type": "remove_effect", "effect": "minecraft:speed" }
{ "type": "remove_effect" }
```

Without `effect` removes all effects. `target`: `"player"` or `"entity"`.

#### spawn_particles — particles

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

- `particle` — particle ID (basic only: `minecraft:heart`, `minecraft:smoke`, `minecraft:portal`, etc.)
- `count` — quantity (default 20)
- `spread` — spread radius in blocks (default 0.5)
- `speed` — initial speed (default 0.0)
- `target` — spawn center: `"player"` or `"entity"`

#### camera_shake — shake the camera

```json
{ "type": "camera_shake", "intensity": 1.0, "duration": 20 }
```

- `intensity` — shake strength (1.0 = standard)
- `duration` — duration in ticks

#### set_time — change time of day

```json
{ "type": "set_time", "time": "night" }
{ "type": "set_time", "time": 13000 }
```

Named values: `"day"` (1000), `"noon"` (6000), `"night"` (13000), `"midnight"` (18000).

#### set_weather — change weather

```json
{ "type": "set_weather", "weather": "thunder", "duration": 6000 }
```

- `weather` — `"clear"`, `"rain"`, `"thunder"`
- `duration` — in ticks (optional)

#### force_dialogue — start another dialogue immediately

Closes the current dialogue and immediately opens another. Used to hand off control from one character to another mid-scene.

```json
{
  "type": "force_dialogue",
  "dialogue_id": "guard_captain",
  "start_node": "confrontation",
  "target_tag": "guard_captain",
  "radius": 16.0
}
```

- `dialogue_id` — dialogue ID to start
- `start_node` — node to start at. Default — the target dialogue's `entry`
- `target_tag` — scoreboard tag of the new NPC. Default — same NPC
- `radius` — search radius for the tagged NPC (default 32 blocks)

#### summon_npc — create an NPC during dialogue

Spawns a new NPC behind the player while talking. Can immediately start a dialogue with them.

```json
{
  "type": "summon_npc",
  "entity": "minecraft:zombie",
  "name": "Mysterious Stranger",
  "tags": ["mystery_npc"],
  "despawn": true,
  "walk_away": true,
  "start_dialogue": "mystery_npc"
}
```

- `entity` — mob type
- `name` — name (must match `target.name` of the desired dialogue)
- `tags` — tags (must match `target.tag`)
- `despawn` — `true` = mob disappears after dialogue
- `walk_away` — `true` = mob walks away first, then despawns
- `start_dialogue` — dialogue ID to start instantly (optional). If absent — NPC just appears and the player initiates the talk themselves

#### notify_npc — light up `!` over another NPC

Marks another NPC as "has something new". A yellow `!` appears above its head — even if the player has talked to them before. Removed when the player starts a conversation.

```json
{ "type": "notify_npc", "dialogue_id": "cursed_historian" }
```

- `dialogue_id` — ID of the dialogue whose NPC should be highlighted

Typical usage: player completes part of a quest at NPC #2 → NPC #2 fires `notify_npc` on NPC #1 → player sees `!` and knows to return.

#### Quest actions

Detailed in [Quest System](#7-quest-system).

| Type | Short description |
|------|-------------------|
| `start_quest` | Add quest to journal and HUD |
| `update_quest` | Update objectives list |
| `complete_quest` | Mark quest complete |
| `fail_quest` | Mark quest failed |

---

### 5. Conditions

Conditions control button visibility. If an option's `condition` is false — the button simply isn't shown. Also used inside `on_revisit`.

```json
"offer": {
  "text": "&fCan I trust you?",
  "options": [
    {
      "text": "&a[Hand over the golden apple]",
      "next": "trust_gained",
      "condition": { "type": "has_item", "item": "minecraft:golden_apple", "count": 1 },
      "actions": [{ "type": "remove_item", "item": "minecraft:golden_apple", "count": 1 }]
    },
    { "text": "&7No, not now", "next": "distrust" }
  ]
}
```

The "Hand over apple" button shows only if it's in the inventory. The second is always visible.

#### has_item — has the item

```json
{ "type": "has_item", "item": "minecraft:golden_apple", "count": 1 }
```

- `item` — item ID
- `count` — minimum quantity (default 1)

#### has_effect — has the effect

```json
{ "type": "has_effect", "effect": "minecraft:regeneration" }
```

#### health_below — health below threshold

```json
{ "type": "health_below", "value": 10 }
{ "type": "health_below", "value": 50, "percent": true }
```

- `value` — threshold
- `percent` — `true` = value as a % of max HP

#### hunger_below — hunger below threshold

```json
{ "type": "hunger_below", "value": 6 }
```

Hunger scale 0–20.

#### time_of_day — time of day

```json
{ "type": "time_of_day", "period": "night" }
```

| Value | Ticks | Description |
|-------|-------|-------------|
| `"day"` | 0–12000 | Daylight |
| `"dusk"` | 12000–13000 | Sunset |
| `"night"` | 13000–23000 | Night |
| `"dawn"` | 23000–1000 | Dawn |

#### weather — weather

```json
{ "type": "weather", "weather": "rain" }
```

Values: `"clear"`, `"rain"`, `"thunder"`.

#### dimension — dimension

```json
{ "type": "dimension", "dimension": "minecraft:the_nether" }
```

Standard IDs: `minecraft:overworld`, `minecraft:the_nether`, `minecraft:the_end`.

#### biome — biome

```json
{ "type": "biome", "biome": "minecraft:desert" }
```

#### visited_node — has the player visited a specific node

```json
{ "type": "visited_node", "dialogue": "chapter1_intro", "node": "accepted_quest" }
```

- `dialogue` — dialogue ID (JSON file name without extension)
- `node` — node name in that dialogue

The main tool for **linking NPCs** — see [NPC linking](#8-linking-npcs).

Progress is shared: if any player visited the node — the condition is true for everyone on the server.

#### killed_mob — kills of a mob type

```json
{ "type": "killed_mob", "entity": "minecraft:zombie", "count": 5 }
```

- `entity` — mob type
- `count` — minimum kills

Counter is shared server-wide.

#### quest_status — quest status

```json
{ "type": "quest_status", "quest_id": "cure_zombie", "status": "active" }
```

- `quest_id` — quest ID
- `status` — `"active"`, `"completed"`, `"failed"`, `"none"` (quest doesn't exist)

#### if_var — variable value

```json
{ "type": "if_var", "name": "trust_level", "op": "eq", "value": "high" }
{ "type": "if_var", "name": "kills", "op": "gte", "value": "10" }
{ "type": "if_var", "name": "met_trader", "op": "exists" }
```

- `name` — variable name
- `op` — comparison operator
- `value` — what to compare with

| Operator | Description |
|----------|-------------|
| `"eq"` | Equal (string) |
| `"neq"` | Not equal |
| `"gt"` | Greater than (number) |
| `"lt"` | Less than (number) |
| `"gte"` | Greater than or equal (number) |
| `"lte"` | Less than or equal (number) |
| `"exists"` | Variable is set and not empty |

More on variables in the next section.

---

### 6. Variables

Variables are named strings stored on the server, shared across all players. Used to remember story progress and branch between different dialogues.

#### How to set — set_var action

```json
{ "type": "set_var", "name": "chapter", "value": "2" }
```

- `name` — variable name, any string without spaces
- `value` — value, always a string (default `""`)
- `op` — operation (default `"set"`)

Numeric ops:

```json
{ "type": "set_var", "name": "kills", "op": "inc", "value": "1" }
{ "type": "set_var", "name": "score", "op": "dec", "value": "5" }
{ "type": "set_var", "name": "counter", "op": "inc" }
```

- `"set"` — assign value
- `"inc"` — add a number. If `value` is missing — adds 1
- `"dec"` — subtract a number. If `value` is missing — subtracts 1

#### How to check — if_var condition

```json
{ "type": "if_var", "name": "chapter", "op": "eq", "value": "2" }
{ "type": "if_var", "name": "met_bob", "op": "exists" }
{ "type": "if_var", "name": "trust", "op": "gte", "value": "3" }
```

#### Example: NPC remembers the player has met them

```json
"nodes": {
  "start": {
    "text": "&fHi! First time we meet?",
    "actions": [{ "type": "set_var", "name": "met_bob", "value": "true" }],
    "next": "chat"
  }
}
```

On the first conversation `met_bob` is set. On revisit (`on_revisit`):

```json
"on_revisit": {
  "default": "&7*Bob stays silent*",
  "conditions": [
    {
      "condition": { "type": "if_var", "name": "met_bob", "op": "exists" },
      "text": "&fOh, you again! How are you?"
    }
  ]
}
```

#### Example: trust counter

The player picks a kind option several times → `trust_bob` accumulates → unlocks a secret branch:

```json
"kind_answer": {
  "text": "&aSure, I'll help",
  "actions": [{ "type": "set_var", "name": "trust_bob", "op": "inc" }],
  "next": "continue"
}
```

```json
{
  "text": "&d[Friends only] Let me tell you a secret...",
  "next": "secret",
  "condition": { "type": "if_var", "name": "trust_bob", "op": "gte", "value": "3" }
}
```

#### Example: tracking a multi-step task

```json
"actions": [{ "type": "set_var", "name": "dungeon_phase", "value": "1" }]
```

```json
"actions": [{ "type": "set_var", "name": "dungeon_phase", "value": "2" }]
```

```json
"condition": { "type": "if_var", "name": "dungeon_phase", "op": "eq", "value": "2" }
```

---

### 7. Quest system

Quests are tasks with text descriptions, shown in the HUD and journal. The mod doesn't track anything automatically — the author drives everything via actions.

**J** — open journal (Dialogues / Quests tabs)  
**K** — toggle quests HUD

Journal: Active / Completed / Failed. HUD shows up to 3 active quests.

#### start_quest — give a quest

```json
{
  "type": "start_quest",
  "quest": {
    "id": "find_herb",
    "title": "&2Find the healing herb",
    "description": "&fThe old healer needs a &ahealing herb &ffrom the swamps.",
    "objectives": [
      "&7[ ] Find the healing herb in the swamps",
      "&8[ ] Return to the healer"
    ]
  }
}
```

- `id` — unique identifier. Used in `update_quest`, `complete_quest`, `fail_quest`, `quest_status`
- `title` — short name in HUD and journal header
- `description` — detailed description, visible only in the journal
- `objectives` — list of strings. Convention: `&8` = locked (gray), `&7[ ]` = current, `&a[✓]` = done

Optional `required_item` — if the player already has the item, the first objective is auto-checked when the quest starts:

```json
"required_item": { "id": "minecraft:golden_apple", "count": 1 }
```

#### update_quest — update objectives

```json
{
  "type": "update_quest",
  "quest_id": "find_herb",
  "objectives": [
    "&a[✓] Find the healing herb in the swamps",
    "&7[ ] Return to the healer"
  ]
}
```

Replaces the objectives list entirely. Called when the player completes a step.

#### complete_quest and fail_quest

```json
{ "type": "complete_quest", "quest_id": "find_herb" }
{ "type": "fail_quest", "quest_id": "find_herb" }
```

Quest leaves the HUD and moves into the matching journal section.

---

### 8. Linking NPCs

One of the most important mechanics — when different NPCs know about each other's actions. Three tools enable this: `visited_node`, `quest_status`, and `set_var` / `if_var`.

#### Principle

Progress is stored on the server, shared by all. When a player walks through a node in dialogue A — that's recorded. Dialogue B can ask: "has the player been at node X of dialogue Y?". Yes → show different lines or options.

#### Tool 1: visited_node

NPC B knows the player talked to NPC A — opens the right branch:

```json
"options": [
  {
    "text": "&fThe hermit sent me. I need your help.",
    "next": "knows_hermit",
    "condition": {
      "type": "visited_node",
      "dialogue": "cursed_historian",
      "node": "ask"
    }
  },
  { "text": "&7I found you on my own.", "next": "suspicious" }
]
```

The "hermit sent me" option appears only after the player has visited node `"ask"` in dialogue `cursed_historian`. Otherwise only the second option shows. This creates a natural quest order.

#### Tool 2: quest_status

NPC reacts differently based on quest state:

```json
"on_revisit": {
  "default": "&7*The historian stays quiet*",
  "conditions": [
    {
      "condition": { "type": "quest_status", "quest_id": "cursed_stone", "status": "active" },
      "text": "&fGo to the hermit. He knows where to look."
    },
    {
      "condition": { "type": "if_var", "name": "stone_cleansed", "op": "exists" },
      "start_node": "return_with_stone"
    },
    {
      "condition": { "type": "quest_status", "quest_id": "cursed_stone", "status": "completed" },
      "text": "&aIt is over. Thank you."
    }
  ]
}
```

#### Tool 3: set_var / if_var

Variables pass arbitrary state between dialogues. The witch sets a variable — the historian reads it:

```json
"done": {
  "actions": [
    { "type": "set_var", "name": "stone_cleansed", "value": "true" },
    { "type": "notify_npc", "dialogue_id": "cursed_historian" }
  ]
}
```

Historian's `on_revisit.conditions`:
```json
{
  "condition": { "type": "if_var", "name": "stone_cleansed", "op": "exists" },
  "start_node": "return_with_stone"
}
```

#### Tool 4: notify_npc — visual cue

So the player knows where to return — use `notify_npc`. After the witch's ritual a `!` lights up over the historian:

```json
{ "type": "notify_npc", "dialogue_id": "cursed_historian" }
```

#### Chains via after_dialogue summon

Make the next NPC appear only after the previous one was spoken to — use the `after_dialogue` trigger:

```json
"summon": {
  "entity": "minecraft:zombie",
  "custom_name": "Hermit",
  "tags": ["forest_hermit"],
  "trigger": {
    "type": "after_dialogue",
    "dialogue_id": "cursed_historian",
    "delay": 120
  }
}
```

The hermit appears 6 seconds after the player finishes the dialogue with the historian.

---

### 9. Revisit (on_revisit)

By default, after the player finishes a dialogue (reaches an end node) the mod marks it complete. On the next right-click — instead of the full dialogue, a short message is shown or a dialogue starts at a specific node.

If you want the dialogue to be replayable in full — use `"repeatable": true` at the root. Then `on_revisit` isn't needed.

#### Structure

```json
"on_revisit": {
  "default": "&7*The zombie stares at you silently*",
  "default_start_node": "small_talk",
  "conditions": [
    {
      "condition": { "type": "quest_status", "quest_id": "find_herb", "status": "active" },
      "text": "&fFound the herb yet?"
    },
    {
      "condition": { "type": "has_item", "item": "minecraft:fern", "count": 3 },
      "text": "&eI see you have the herb!",
      "start_node": "return_with_herb"
    },
    {
      "condition": { "type": "quest_status", "quest_id": "find_herb", "status": "completed" },
      "text": "&aThanks, friend."
    }
  ]
}
```

#### Logic

1. Conditions are checked **top-down**
2. The first matching condition applies:
   - With `start_node` — opens the full dialogue at that node
   - Without — shows a short message for a few seconds
3. If **none** match:
   - With `default_start_node` — opens the dialogue there
   - With `default` — shows a short message

| Field | Description |
|-------|-------------|
| `default` | Default text |
| `default_start_node` | Default start node |
| `conditions[].condition` | Any condition from section 5 |
| `conditions[].text` | Short message if condition matched |
| `conditions[].start_node` | Node for full dialogue instead of message |

---

### 10. Auto-spawn (summon)

The `summon` block lets NPCs appear in the world automatically. The mod creates the mob, assigns name and tags — then the player walks up and triggers the dialogue with right-click.

```json
"summon": {
  "entity": "minecraft:zombie",
  "custom_name": "Historian",
  "tags": ["cursed_historian"],
  "spawn_position": "behind_player",
  "despawn_after_dialogue": false,
  "walk_away_before_despawn": false,
  "trigger": {
    "type": "on_join",
    "delay": 60
  }
}
```

#### summon fields

| Field | Description |
|-------|-------------|
| `entity` | Mob type: `minecraft:zombie`, `minecraft:villager`, `minecraft:witch`, etc. |
| `custom_name` | Mob name. Must match `target.name` in the same file |
| `tags` | Tags. Must match `target.tag` |
| `spawn_position` | `"behind_player"` — 3 blocks behind the player |
| `despawn_after_dialogue` | `true` — mob removed after dialogue |
| `walk_away_before_despawn` | `true` — mob walks ~10 blocks away first |
| `trigger` | Trigger object — when to spawn |

#### Triggers

##### on_join — when entering the world

```json
"trigger": { "type": "on_join", "delay": 60 }
```

Spawns `delay` ticks after the player joins. Fires once — only if the dialogue isn't completed.

##### after_dialogue — after another dialogue completes

```json
"trigger": {
  "type": "after_dialogue",
  "dialogue_id": "cursed_historian",
  "delay": 120
}
```

Spawns `delay` ticks after `cursed_historian` completes. Backbone of story chains.

##### player_near — player approaches a point

```json
"trigger": {
  "type": "player_near",
  "x": 100, "y": 64, "z": -200,
  "radius": 8.0
}
```

##### player_entered_area — first entry into a zone

```json
"trigger": {
  "type": "player_entered_area",
  "x": 0, "y": 64, "z": 0,
  "radius": 15.0
}
```

Fires only on entry, doesn't repeat while the player stays inside.

##### player_looking_for_seconds — player looks at a point for N seconds

```json
"trigger": {
  "type": "player_looking_for_seconds",
  "x": 50, "y": 70, "z": 50,
  "radius": 64,
  "seconds": 3
}
```

##### on_player_death — after player death

```json
"trigger": { "type": "on_player_death", "delay": 20 }
```

---

### 11. NPC icon

The mod automatically draws a yellow `!` over an NPC in two cases:

1. **Dialogue not started** — player has never spoken to this NPC
2. **Triggered by `notify_npc`** — author explicitly signaled there's something new

Visible within 16 blocks. Disappears once the player starts the conversation.

---

### 12. GUI customization

#### NPC avatar

Defaults to a zombie head. Custom avatar:

```json
"avatar": "minecraft:textures/entity/villager/villager.png"
```

Format: `namespace:path_to_texture`. The mod takes an 8×8 area starting at (8, 8) — the first head layer of the skin.

Via NBT without a resource pack:
```
/data merge entity @e[name=NPC_NAME,limit=1] {DialogueAvatar:"minecraft:textures/entity/zombie/zombie.png"}
```

#### Dialogue panel background

Default — dark blue gradient. Replaceable:

```json
"background": "mypack:textures/gui/dialogue_bg.png"
```

The texture is stretched across the full width and height of the panel. Must be in a resource pack at `assets/mypack/textures/gui/dialogue_bg.png`.

#### Option panel background

```json
"options_background": "mypack:textures/gui/option_bg.png"
```

Applied to each button separately, also stretched to its size.

#### Resource pack layout

```
resourcepacks/
  my_dialogue_pack/
    pack.mcmeta
    assets/
      mydialogue/
        textures/
          gui/
            dialogue_bg.png
            option_bg.png
```

JSON:
```json
"background": "mydialogue:textures/gui/dialogue_bg.png",
"options_background": "mydialogue:textures/gui/option_bg.png"
```

The pack goes into `.minecraft/resourcepacks/` and is enabled in the resource packs settings as usual.

---

### 13. Dialogue controls

| Key | Action |
|-----|--------|
| **RMB** | Next node (linear) / Open options (choice) / Close (end) |
| **Left click** | Pick an option |
| **1–5** | Quick-pick by number |
| **↑ ↓** | Navigate options |
| **Enter** | Pick the highlighted option |
| **ESC** | Close dialogue |
| **J** | Journal (Dialogues / Quests tabs) |
| **K** | Toggle quests HUD |

---

### 14. Commands

#### /dialogue reload

Reloads all dialogues from the folder. Use after any JSON change.

#### /dialogue reload `<id>`

Reloads one dialogue and resets all progress for it. For testing.

```
/dialogue reload cursed_historian
```

#### /dialogue test `<id>` [node]

Starts the dialogue with the nearest matching mob without progress checks. If `node` is specified — starts there.

```
/dialogue test old_zombie check_apple
```

#### /dialogue goto `<node>`

In an active dialogue — jumps to the specified node immediately.

#### /npc spawn `<id>`

Spawns the NPC at your feet. Mob type is taken from `summon.entity` in the JSON. Name and tags — from `target`.

```
/npc spawn cursed_historian
```

#### /npc tag `<id>`

Assigns the name and tag from the specified dialogue to the nearest mob.

#### /npc remove

Removes the nearest NPC.

#### /npc list [radius]

Lists all NPCs within radius (default 32 blocks).

---

### 15. How to put NPCs in the world

#### Way 1 — /npc spawn (simplest)

```
/npc spawn my_dialogue
```

The mod creates the right mob type with the right name and tag at your feet.

#### Way 2 — /npc tag (for an existing mob)

Walk up to the mob and run `/npc tag my_dialogue` — the mod assigns the right name and tag.

#### Way 3 — manually

```
/tag @e[type=minecraft:zombie,distance=..3,limit=1,sort=nearest] add my_tag
/data merge entity @e[type=minecraft:zombie,distance=..3,limit=1,sort=nearest] {CustomName:'"My Name"',CustomNameVisible:1b}
```

#### Way 4 — auto-spawn (summon)

The mod creates the mob itself when a condition triggers — see section 10.

---

### 16. Progress and multiplayer

All progress is **shared by all players** on the server:

- `visited_node` is true if any player visited the node
- `killed_mob` — total kills by all players
- Variables (`set_var`) — same for everyone
- Quests visible to all in the journal
- `notify_npc` lights the icon for everyone

Progress is preserved across dimensions — storage is bound to the Overworld.

Reset progress for a specific dialogue: `/dialogue reload <id>`

---

### Full example — three linked NPCs

**Historian** appears on world join. Gives the quest.  
**Hermit** appears after talking to the Historian, explains what the witch needs.  
**Witch** appears after the Hermit, takes payment, fires `notify_npc` on the Historian.  
The player returns to the Historian and gets the reward.

#### cursed_historian.json

```json
{
  "target": { "name": "Historian", "tag": "cursed_historian" },
  "display_name": "&e[&6Historian&e]",
  "entry": "start",

  "summon": {
    "entity": "minecraft:zombie",
    "custom_name": "Historian",
    "tags": ["cursed_historian"],
    "spawn_position": "behind_player",
    "despawn_after_dialogue": false,
    "trigger": { "type": "on_join", "delay": 60 }
  },

  "on_revisit": {
    "default": "&e[&6Historian&e] &7Go to the hermit in the forest.",
    "conditions": [
      {
        "condition": { "type": "quest_status", "quest_id": "cursed_stone", "status": "none" },
        "start_node": "start"
      },
      {
        "condition": { "type": "if_var", "name": "stone_cleansed", "op": "exists" },
        "start_node": "return_with_stone"
      },
      {
        "condition": { "type": "quest_status", "quest_id": "cursed_stone", "status": "completed" },
        "text": "&e[&6Historian&e] &aThank you. The village is safe again."
      }
    ]
  },

  "nodes": {
    "start": {
      "text": "&fAh, a stranger. I need help.",
      "next": "explain"
    },
    "explain": {
      "text": "&fThe &cCursed Stone&f has gone missing from my cellar.",
      "next": "danger"
    },
    "danger": {
      "text": "&cIf it falls into the wrong hands — &fall life around will rot.",
      "next": "ask"
    },
    "ask": {
      "text": "&fA hermit lives in the forest. He knows about curses. &eGo speak with him.",
      "options": [
        {
          "text": "&aI'll help",
          "next": "accept",
          "actions": [{
            "type": "start_quest",
            "quest": {
              "id": "cursed_stone",
              "title": "&cThe Cursed Stone",
              "description": "&fFind a way to neutralize the cursed stone.",
              "objectives": [
                "&7[ ] Speak with the hermit in the forest",
                "&8[ ] Find the witch",
                "&8[ ] Return the stone to the historian"
              ]
            }
          }]
        },
        { "text": "&7Not my problem", "next": "refuse" }
      ]
    },
    "accept": { "text": "&aThank you. The hermit lives north of the village." },
    "refuse": { "text": "&7I understand. If you change your mind — I'm here." },
    "return_with_stone": {
      "text": "&f*The historian looks up hopeful* &eIs the stone cleansed?",
      "next": "reward"
    },
    "reward": {
      "text": "&eTake this. You earned it.",
      "actions": [
        { "type": "give_item", "item": "minecraft:diamond", "count": 3 },
        { "type": "give_item", "item": "minecraft:golden_apple", "count": 2 },
        { "type": "complete_quest", "quest_id": "cursed_stone" }
      ]
    }
  }
}
```

#### forest_hermit.json

```json
{
  "target": { "name": "Hermit", "tag": "forest_hermit" },
  "display_name": "&2[&aHermit&2]",
  "entry": "start",

  "summon": {
    "entity": "minecraft:zombie",
    "custom_name": "Hermit",
    "tags": ["forest_hermit"],
    "spawn_position": "behind_player",
    "despawn_after_dialogue": false,
    "trigger": {
      "type": "after_dialogue",
      "dialogue_id": "cursed_historian",
      "delay": 120
    }
  },

  "on_revisit": {
    "default": "&2[&aHermit&2] &7Go to the witch. Only she can help."
  },

  "nodes": {
    "start": {
      "text": "&7*The old man stares at you for a long time* &fDid the historian send you?",
      "options": [
        {
          "text": "&fYes, he said you know about curses",
          "next": "knows",
          "condition": { "type": "visited_node", "dialogue": "cursed_historian", "node": "ask" }
        },
        { "text": "&7No, I'm on my own", "next": "go_away" }
      ]
    },
    "knows": {
      "text": "&fThe Cursed Stone... yes, I've heard.",
      "next": "explain"
    },
    "explain": {
      "text": "&fIt cannot be destroyed by ordinary means. You need a &dswamp witch&f.",
      "next": "requirement"
    },
    "requirement": {
      "text": "&cBut she won't help for free. &fBring her &e3 spider eyes &fand &e1 night vision potion&f.",
      "next": "farewell",
      "actions": [{
        "type": "update_quest",
        "quest_id": "cursed_stone",
        "objectives": [
          "&a[✓] Speak with the hermit in the forest",
          "&7[ ] Find the witch in the swamps",
          "&7[ ] Bring: 3x spider eye + night vision potion",
          "&8[ ] Return the stone to the historian"
        ]
      }]
    },
    "farewell": { "text": "&7*The hermit returns to his fire*" },
    "go_away": { "text": "&7Leave. I don't need guests." }
  }
}
```

#### swamp_witch.json

```json
{
  "target": { "name": "Witch", "tag": "swamp_witch" },
  "display_name": "&5[&dWitch&5]",
  "entry": "start",

  "summon": {
    "entity": "minecraft:witch",
    "custom_name": "Witch",
    "tags": ["swamp_witch"],
    "spawn_position": "behind_player",
    "despawn_after_dialogue": false,
    "trigger": {
      "type": "after_dialogue",
      "dialogue_id": "forest_hermit",
      "delay": 160
    }
  },

  "on_revisit": {
    "default": "&5[&dWitch&5] &7Bring what I asked.",
    "conditions": [
      {
        "condition": { "type": "has_item", "item": "minecraft:spider_eye", "count": 3 },
        "start_node": "has_items_check"
      }
    ]
  },

  "nodes": {
    "start": {
      "text": "&d*The witch turns slowly* &fWho sent you?",
      "options": [
        {
          "text": "&fThe forest hermit. I need help with a stone.",
          "next": "knows_hermit",
          "condition": { "type": "visited_node", "dialogue": "forest_hermit", "node": "requirement" }
        },
        { "text": "&7I found you on my own", "next": "suspicious" }
      ]
    },
    "knows_hermit": {
      "text": "&dThe old hermit... &fI can cleanse the stone.",
      "next": "demand"
    },
    "demand": {
      "text": "&cBut payment first. &f3 spider eyes and a night vision potion.",
      "next": "wait"
    },
    "wait": { "text": "&7*The witch crosses her arms and waits*" },
    "has_items_check": {
      "text": "&dAh, you brought it at last.",
      "options": [
        {
          "text": "&aHere, take it",
          "next": "ritual",
          "condition": { "type": "has_item", "item": "minecraft:spider_eye", "count": 3 },
          "actions": [
            { "type": "remove_item", "item": "minecraft:spider_eye", "count": 3 },
            { "type": "remove_item", "item": "minecraft:potion", "count": 1 }
          ]
        },
        { "text": "&7Still gathering", "next": "not_ready" }
      ]
    },
    "ritual": {
      "text": "&d*The witch mutters an incantation...*",
      "next": "ritual2",
      "auto_next_ticks": 60
    },
    "ritual2": {
      "text": "&5*A flash of purple light. The air smells of sulfur.*",
      "next": "done",
      "auto_next_ticks": 40
    },
    "done": {
      "text": "&aDone. &fThe stone is cleansed. Return to the historian.",
      "actions": [
        { "type": "give_item", "item": "minecraft:enchanted_book", "count": 1 },
        { "type": "give_effect", "effect": "minecraft:night_vision", "duration": 600, "amplifier": 0 },
        { "type": "set_var", "name": "stone_cleansed", "value": "true" },
        { "type": "notify_npc", "dialogue_id": "cursed_historian" },
        {
          "type": "update_quest",
          "quest_id": "cursed_stone",
          "objectives": [
            "&a[✓] Speak with the hermit in the forest",
            "&a[✓] Find the witch in the swamps",
            "&a[✓] Bring: 3x spider eye + night vision potion",
            "&7[ ] Return the stone to the historian"
          ]
        }
      ]
    },
    "not_ready": { "text": "&7Then come back when you have everything." },
    "suspicious": { "text": "&cWithout a recommendation I don't help strangers. Leave." }
  }
}
```

After the witch's `done` node: the quest is updated, `stone_cleansed` is set, a `!` lights up over the Historian. The player returns — `on_revisit` sees `if_var(stone_cleansed)` → opens `return_with_stone` → gives the reward → `complete_quest`.

[⬆ Back to top](#interactentity)

---

<a id="русский"></a>

## Русский

> [🇬🇧 Switch to English](#english)

Мод для Minecraft Forge 1.20.1. Позволяет создавать диалоги с мобами через JSON-файлы.

---

## С чего начать

Диалог — это JSON-файл, в котором описано: с каким именно мобом разговаривать, что он говорит, и что происходит в ответ на действия игрока. Когда игрок нажимает ПКМ по нужному мобу — мод находит подходящий JSON и открывает GUI диалога.

### Где хранятся файлы

После первого запуска мода создаётся папка рядом с `.minecraft`:

```
.minecraft/
  interactentity/
    dialogues/
      zombie.json        ← ID диалога: "zombie"
      story/
        intro.json       ← ID диалога: "story/intro"
```

Можно создавать подпапки. Имя файла (без `.json`) и путь внутри папки `dialogues/` становятся **ID диалога** — этот ID используется во всём остальном: условиях, триггерах, квестах, командах.

После добавления или редактирования файлов обязательно выполните `/dialogue reload` — без этого мод не увидит изменения.

---

## 1. Узлы — основа диалога

Прежде чем разбирать структуру файла, нужно понять **что такое узел** — это единица диалога. Каждый диалог состоит из набора узлов. В каждый момент времени игрок находится в каком-то конкретном узле: видит текст NPC и либо жмёт ПКМ чтобы продолжить, либо выбирает один из вариантов ответа.

### Три типа узлов

Тип узла определяется автоматически — по тому, какие поля в нём есть.

**Линейный узел** — NPC говорит что-то, игрок жмёт ПКМ и переходит к следующему узлу. Используется для монологов, вступлений, рассказов.

```json
"start": {
  "text": "&fЯ давно тебя жду, путник...",
  "next": "continue"
}
```

Здесь `"next": "continue"` — это имя следующего узла. Когда игрок нажмёт ПКМ — диалог перейдёт к узлу с именем `"continue"`. Это и есть линейный узел — есть поле `next`, нет поля `options`.

**Узел с выбором** — NPC задаёт вопрос или предлагает варианты, игрок выбирает один из них кликом мыши или клавишами 1–5. Используется для ветвящихся диалогов.

```json
"question": {
  "text": "&fТы поможешь мне?",
  "options": [
    { "text": "&aКонечно!", "next": "accept" },
    { "text": "&cНет, извини", "next": "refuse" }
  ]
}
```

Здесь `options` — список вариантов ответа. У каждого варианта есть `text` (что написано на кнопке) и `next` (куда перейти после выбора). Это узел с выбором — есть поле `options`.

**Конечный узел** — диалог заканчивается. NPC говорит последнее слово, игрок жмёт ПКМ и GUI закрывается. Используется как финальная точка ветки.

```json
"end": {
  "text": "&7*Зомби уходит в темноту*"
}
```

Нет ни `next`, ни `options` — значит конечный узел.

### Как переходы образуют диалог

Узлы связаны между собой через поле `next`. Представь граф: каждый узел — вершина, каждый `next` или вариант ответа — ребро. Диалог начинается с узла указанного в `entry`, и двигается по графу пока не дойдёт до конечного узла.

```
start → intro → question → accept → reward (конец)
                         ↘ refuse → bye (конец)
```

### Все поля узла

| Поле | Тип | Описание |
|------|-----|----------|
| `text` | строка | Текст реплики NPC |
| `random_text` | массив строк | Случайный текст — при каждом входе в узел выбирается один из списка |
| `next` | строка | Имя следующего узла (делает узел линейным) |
| `auto_next_ticks` | число | Автоматический переход через N тиков без нажатия ПКМ. 20 тиков = 1 секунда |
| `options` | массив | Варианты ответа (делает узел узлом с выбором) |
| `actions` | массив | Действия, которые выполняются **при входе** в этот узел |

### Все поля варианта ответа

| Поле | Тип | Описание |
|------|-----|----------|
| `text` | строка | Текст кнопки |
| `next` | строка | Имя следующего узла. Если не указано — диалог завершается |
| `condition` | объект | Условие видимости кнопки. Если условие не выполнено — кнопка не показывается |
| `actions` | массив | Действия, которые выполняются **при выборе** этого варианта |

**Разница между `actions` на узле и на варианте:**
- `actions` на узле — выполняются всегда когда игрок попадает в этот узел, независимо от того как он туда попал
- `actions` на варианте — выполняются только когда игрок выбирает конкретно этот вариант

---

## 2. Базовая структура файла

Теперь, понимая что такое узлы, посмотрим на полный скелет JSON-файла:

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

### Как мод находит нужного моба

Поле `target` — это «адрес» моба в мире. Мод при ПКМ по мобу проверяет **два условия одновременно**:
1. У моба совпадает `CustomName` (бирка) с `target.name`
2. У моба есть scoreboard-тег совпадающий с `target.tag`

Если совпало — открывается этот диалог. Если нет — ничего не происходит.

Почему два условия? Потому что имена мобов не уникальны, а теги — гибкий механизм Minecraft для маркировки конкретных сущностей.

### Все корневые поля

| Поле | Тип | Обяз. | Описание |
|------|-----|-------|----------|
| `target.name` | строка | да | Имя моба (CustomName бирка) |
| `target.tag` | строка | да | Scoreboard-тег моба |
| `display_name` | строка | нет | Как имя NPC выглядит в GUI. Если не указано — берётся `target.name`. Поддерживает форматирование |
| `entry` | строка | да | Имя начального узла — с него начинается каждый новый диалог |
| `nodes` | объект | да | Все узлы диалога |
| `repeatable` | bool | нет | `true` — диалог можно проходить повторно. По умолчанию `false` — после прохождения NPC больше не реагирует |
| `invulnerable` | bool | нет | `true` (по умолчанию) — NPC неуязвим во время диалога |
| `avatar` | строка | нет | Текстура аватара в GUI, формат `namespace:path` |
| `background` | строка | нет | Кастомная текстура фона панели диалога |
| `options_background` | строка | нет | Кастомная текстура фона панелей вариантов |
| `on_revisit` | объект | нет | Что происходит когда игрок подходит к NPC повторно после завершения диалога |
| `summon` | объект | нет | Автоматический спавн NPC |

---

## 3. Форматирование текста

Поддерживается в любом текстовом поле: `text`, `display_name`, `random_text`, варианты ответа, квесты.

### Коды цветов

Используй `&` перед буквой или цифрой:

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

### Коды стилей

| Код | Эффект |
|-----|--------|
| `&l` | Жирный |
| `&o` | Курсив |
| `&n` | Подчёркнутый |
| `&m` | Зачёркнутый |
| `&k` | Обфускация (мигающие символы) |
| `&r` | Сброс всех стилей и цветов |

### HEX-цвет

Произвольный цвет через `&#RRGGBB`:

```
&#FF6600  →  оранжевый
&#00AAFF  →  голубой
```

### Пример

```json
"display_name": "&6[&eСтарый Зомби&6]"
"text": "&fЯ был... &cчеловеком... &7когда-то давно..."
"text": "&#FF6600*Вспышка огня*"
"text": "&lВНИМАНИЕ! &rЭто важно."
```

Цвет действует до следующего кода или конца строки. `&r` сбрасывает всё обратно к белому.

---

## 4. Действия (actions)

Действия — это то что **происходит** в диалоге: выдать предмет, запустить команду, начать квест, поставить звук. Они выполняются либо при входе в узел, либо при выборе варианта ответа.

```json
"reward_node": {
  "text": "&aВозьми это в знак благодарности!",
  "actions": [
    { "type": "give_item", "item": "minecraft:diamond", "count": 3 },
    { "type": "play_sound", "sound": "minecraft:entity.player.levelup", "volume": 1.0 }
  ]
}
```

Здесь при входе в узел `reward_node` игрок получает 3 алмаза и слышит звук.

### give_item — выдать предмет

```json
{ "type": "give_item", "item": "minecraft:golden_apple", "count": 1 }
```

- `item` — ID предмета в формате `namespace:name`
- `count` — количество (по умолчанию 1)

Пример: NPC выдаёт награду — 5 изумрудов и золотое яблоко:
```json
"actions": [
  { "type": "give_item", "item": "minecraft:emerald", "count": 5 },
  { "type": "give_item", "item": "minecraft:golden_apple", "count": 1 }
]
```

### remove_item — забрать предмет

```json
{ "type": "remove_item", "item": "minecraft:spider_eye", "count": 3 }
```

Забирает предметы из инвентаря. Обычно используется вместе с условием `has_item` — чтобы сначала проверить наличие, потом забрать.

Пример: ведьма берёт плату и только если предмет есть:
```json
{
  "text": "&aВозьми",
  "next": "ritual",
  "condition": { "type": "has_item", "item": "minecraft:spider_eye", "count": 3 },
  "actions": [{ "type": "remove_item", "item": "minecraft:spider_eye", "count": 3 }]
}
```

### run_command — выполнить команду сервера

```json
{ "type": "run_command", "command": "effect give @s minecraft:speed 60 2" }
```

- `command` — команда без слеша в начале
- `@s` — это игрок ведущий диалог
- Выполняется от сервера с правами уровня 2

### teleport — телепортировать игрока

```json
{ "type": "teleport", "x": 100, "y": 64, "z": -200 }
{ "type": "teleport", "x": 0, "y": 5, "z": 0, "mode": "relative" }
```

- `x`, `y`, `z` — координаты
- `yaw`, `pitch` — поворот после телепортации (необязательно)
- `mode` — `"absolute"` (конкретные координаты, по умолчанию) или `"relative"` (смещение от текущей позиции)

### play_sound — воспроизвести звук

```json
{
  "type": "play_sound",
  "sound": "minecraft:entity.zombie.ambient",
  "volume": 0.8,
  "pitch": 1.2,
  "target": "player"
}
```

- `sound` — ID звука
- `volume` — громкость от 0.0 до 2.0 (по умолчанию 1.0)
- `pitch` — тон: выше 1.0 = выше тон, ниже 1.0 = ниже тон (по умолчанию 1.0)
- `target` — `"player"` (звук на игроке) или `"entity"` (звук на NPC)

### give_effect — наложить эффект зелья

```json
{
  "type": "give_effect",
  "effect": "minecraft:regeneration",
  "duration": 200,
  "amplifier": 1,
  "target": "player"
}
```

- `effect` — ID эффекта
- `duration` — длительность в тиках (200 тиков = 10 секунд)
- `amplifier` — уровень: 0 = уровень I, 1 = уровень II и т.д.
- `ambient` — `true` делает частицы прозрачными как от маяка (по умолчанию `false`)
- `particles` — `false` скрывает частицы (по умолчанию `true`)
- `target` — `"player"` или `"entity"`

### remove_effect — убрать эффект

```json
{ "type": "remove_effect", "effect": "minecraft:speed" }
{ "type": "remove_effect" }
```

Без поля `effect` убирает все эффекты. `target`: `"player"` или `"entity"`.

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

- `particle` — ID частицы (только базовые: `minecraft:heart`, `minecraft:smoke`, `minecraft:portal` и т.д.)
- `count` — количество (по умолчанию 20)
- `spread` — радиус разброса в блоках (по умолчанию 0.5)
- `speed` — начальная скорость частиц (по умолчанию 0.0)
- `target` — центр спавна: `"player"` или `"entity"`

### camera_shake — тряска камеры

```json
{ "type": "camera_shake", "intensity": 1.0, "duration": 20 }
```

- `intensity` — сила тряски (1.0 = стандартная)
- `duration` — длительность в тиках

### set_time — изменить время суток

```json
{ "type": "set_time", "time": "night" }
{ "type": "set_time", "time": 13000 }
```

Именованные значения: `"day"` (1000), `"noon"` (6000), `"night"` (13000), `"midnight"` (18000).

### set_weather — изменить погоду

```json
{ "type": "set_weather", "weather": "thunder", "duration": 6000 }
```

- `weather` — `"clear"`, `"rain"`, `"thunder"`
- `duration` — длительность в тиках (необязательно)

### force_dialogue — сразу запустить другой диалог

Завершает текущий диалог и немедленно открывает другой. Используется для передачи управления от одного персонажа к другому прямо внутри сцены.

```json
{
  "type": "force_dialogue",
  "dialogue_id": "guard_captain",
  "start_node": "confrontation",
  "target_tag": "guard_captain",
  "radius": 16.0
}
```

- `dialogue_id` — ID диалога который нужно запустить
- `start_node` — с какого узла начать. Если не указано — с `entry` того диалога
- `target_tag` — scoreboard-тег NPC для нового диалога. Если не указано — используется тот же NPC
- `radius` — радиус поиска NPC с этим тегом (по умолчанию 32 блока)

Пример — торговец зовёт стражника, тот сразу начинает говорить:
```json
"call_guard": {
  "text": "&cСтража! Задержите его!",
  "actions": [{
    "type": "force_dialogue",
    "dialogue_id": "city_guard",
    "target_tag": "city_guard",
    "radius": 20.0
  }]
}
```

### summon_npc — создать NPC прямо во время диалога

Спавнит нового NPC за спиной игрока в процессе разговора. Можно сразу начать с ним диалог.

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

- `entity` — тип моба
- `name` — имя (должно совпадать с `target.name` нужного диалога)
- `tags` — теги (должны совпадать с `target.tag`)
- `despawn` — `true` = моб исчезнет после диалога
- `walk_away` — `true` = моб сначала уйдёт, потом исчезнет
- `start_dialogue` — ID диалога для немедленного запуска (необязательно). Если не указано — NPC просто появляется, игрок сам инициирует разговор

### notify_npc — зажечь `!` над другим NPC

Помечает другой NPC как «есть что-то новое». Над его головой появится жёлтый `!` — даже если игрок уже говорил с ним. Значок снимается когда игрок начинает разговор.

```json
{ "type": "notify_npc", "dialogue_id": "cursed_historian" }
```

- `dialogue_id` — ID диалога того NPC которого нужно подсветить

Типичный сценарий: игрок выполнил часть квеста у второго NPC → второй NPC ставит `notify_npc` на первого → игрок видит `!` и знает что нужно вернуться.

### Квестовые действия

Описаны подробно в разделе [Система квестов](#7-система-квестов).

| Тип | Краткое описание |
|-----|-----------------|
| `start_quest` | Добавить квест в журнал и HUD |
| `update_quest` | Обновить список целей |
| `complete_quest` | Отметить квест завершённым |
| `fail_quest` | Отметить квест проваленным |

---

## 5. Условия (conditions)

Условия управляют видимостью кнопок. Если `condition` на варианте ответа не выполнено — эта кнопка просто не показывается игроку. Также используются в `on_revisit`.

```json
"offer": {
  "text": "&fМогу ли я тебе доверять?",
  "options": [
    {
      "text": "&a[Отдать золотое яблоко]",
      "next": "trust_gained",
      "condition": { "type": "has_item", "item": "minecraft:golden_apple", "count": 1 },
      "actions": [{ "type": "remove_item", "item": "minecraft:golden_apple", "count": 1 }]
    },
    { "text": "&7Нет, не сейчас", "next": "distrust" }
  ]
}
```

Кнопка «Отдать яблоко» видна только если оно есть в инвентаре. Второй вариант виден всегда.

### has_item — есть ли предмет

```json
{ "type": "has_item", "item": "minecraft:golden_apple", "count": 1 }
```

- `item` — ID предмета
- `count` — минимальное количество (по умолчанию 1)

### has_effect — есть ли эффект

```json
{ "type": "has_effect", "effect": "minecraft:regeneration" }
```

### health_below — здоровье ниже порога

```json
{ "type": "health_below", "value": 10 }
{ "type": "health_below", "value": 50, "percent": true }
```

- `value` — пороговое значение
- `percent` — `true` = значение в процентах от максимального HP

### hunger_below — голод ниже порога

```json
{ "type": "hunger_below", "value": 6 }
```

Шкала голода от 0 до 20.

### time_of_day — время суток

```json
{ "type": "time_of_day", "period": "night" }
```

| Значение | Тики | Описание |
|----------|------|----------|
| `"day"` | 0–12000 | Светлое время |
| `"dusk"` | 12000–13000 | Закат |
| `"night"` | 13000–23000 | Ночь |
| `"dawn"` | 23000–1000 | Рассвет |

### weather — погода

```json
{ "type": "weather", "weather": "rain" }
```

Значения: `"clear"`, `"rain"`, `"thunder"`.

### dimension — измерение

```json
{ "type": "dimension", "dimension": "minecraft:the_nether" }
```

Стандартные ID: `minecraft:overworld`, `minecraft:the_nether`, `minecraft:the_end`.

### biome — биом

```json
{ "type": "biome", "biome": "minecraft:desert" }
```

### visited_node — посещал ли игрок конкретный узел

```json
{ "type": "visited_node", "dialogue": "chapter1_intro", "node": "accepted_quest" }
```

- `dialogue` — ID диалога (имя JSON-файла без расширения)
- `node` — имя узла в том диалоге

Это главный инструмент для **связывания NPC** — подробнее в разделе [Связывание NPC](#8-связывание-npc).

Прогресс общий: если один игрок посетил узел — условие выполнено для всех на сервере.

### killed_mob — убито мобов определённого типа

```json
{ "type": "killed_mob", "entity": "minecraft:zombie", "count": 5 }
```

- `entity` — тип моба
- `count` — минимальное количество убийств

Счётчик общий для всего сервера.

### quest_status — статус квеста

```json
{ "type": "quest_status", "quest_id": "cure_zombie", "status": "active" }
```

- `quest_id` — ID квеста
- `status` — `"active"`, `"completed"`, `"failed"`, `"none"` (квест не существует)

### if_var — значение переменной

```json
{ "type": "if_var", "name": "trust_level", "op": "eq", "value": "high" }
{ "type": "if_var", "name": "kills", "op": "gte", "value": "10" }
{ "type": "if_var", "name": "met_trader", "op": "exists" }
```

- `name` — имя переменной
- `op` — оператор сравнения
- `value` — с чем сравнивать

| Оператор | Описание |
|----------|----------|
| `"eq"` | Равно (строки) |
| `"neq"` | Не равно |
| `"gt"` | Больше (числа) |
| `"lt"` | Меньше (числа) |
| `"gte"` | Больше или равно (числа) |
| `"lte"` | Меньше или равно (числа) |
| `"exists"` | Переменная установлена и не пустая |

Подробнее о переменных — в следующем разделе.

---

## 6. Переменные

Переменные — именованные строки, хранящиеся на сервере, общие для всех игроков. Используются чтобы запомнить что произошло в сюжете и управлять ветвлением между разными диалогами.

### Как установить — действие set_var

```json
{ "type": "set_var", "name": "chapter", "value": "2" }
```

- `name` — имя переменной, любая строка без пробелов
- `value` — значение, всегда строка (по умолчанию `""`)
- `op` — операция (по умолчанию `"set"`)

Для числовых операций:

```json
{ "type": "set_var", "name": "kills", "op": "inc", "value": "1" }
{ "type": "set_var", "name": "score", "op": "dec", "value": "5" }
{ "type": "set_var", "name": "counter", "op": "inc" }
```

- `"set"` — присвоить значение
- `"inc"` — прибавить число. Если `value` не указан — прибавляет 1
- `"dec"` — вычесть число. Если `value` не указан — вычитает 1

### Как проверить — условие if_var

```json
{ "type": "if_var", "name": "chapter", "op": "eq", "value": "2" }
{ "type": "if_var", "name": "met_bob", "op": "exists" }
{ "type": "if_var", "name": "trust", "op": "gte", "value": "3" }
```

### Пример: NPC помнит что игрок уже встречался с ним

```json
"nodes": {
  "start": {
    "text": "&fПривет! Первый раз видимся?",
    "actions": [{ "type": "set_var", "name": "met_bob", "value": "true" }],
    "next": "chat"
  }
}
```

При первом разговоре устанавливается переменная `met_bob`. При повторном (`on_revisit`):

```json
"on_revisit": {
  "default": "&7*Боб молчит*",
  "conditions": [
    {
      "condition": { "type": "if_var", "name": "met_bob", "op": "exists" },
      "text": "&fА, снова ты! Как дела?"
    }
  ]
}
```

### Пример: счётчик доверия

Игрок несколько раз делает добрый выбор → накапливается `trust_bob` → разблокируется секретная ветка:

```json
"kind_answer": {
  "text": "&aХорошо, я помогу",
  "actions": [{ "type": "set_var", "name": "trust_bob", "op": "inc" }],
  "next": "continue"
}
```

```json
{
  "text": "&d[Только для друзей] Расскажу тебе секрет...",
  "next": "secret",
  "condition": { "type": "if_var", "name": "trust_bob", "op": "gte", "value": "3" }
}
```

### Пример: отслеживать многошаговую задачу

```json
"actions": [{ "type": "set_var", "name": "dungeon_phase", "value": "1" }]
```

```json
"actions": [{ "type": "set_var", "name": "dungeon_phase", "value": "2" }]
```

```json
"condition": { "type": "if_var", "name": "dungeon_phase", "op": "eq", "value": "2" }
```

---

## 7. Система квестов

Квесты — задачи с текстовым описанием, показываемые в HUD и журнале. Мод ничего не считает автоматически — автор управляет всем через actions.

**J** — открыть журнал (вкладки «Диалоги» и «Квесты»)  
**K** — скрыть/показать HUD квестов  

В журнале: Активные / Завершённые / Проваленные. В HUD — максимум 3 активных квеста.

### start_quest — выдать квест

```json
{
  "type": "start_quest",
  "quest": {
    "id": "find_herb",
    "title": "&2Найди целебную траву",
    "description": "&fСтарый лекарь просит принести ему &aцелебную траву &fс болот.",
    "objectives": [
      "&7[ ] Найти целебную траву на болотах",
      "&8[ ] Вернуться к лекарю"
    ]
  }
}
```

- `id` — уникальный идентификатор. Используется в `update_quest`, `complete_quest`, `fail_quest`, `quest_status`
- `title` — короткое название в HUD и шапке журнала
- `description` — подробное описание, видно только в журнале
- `objectives` — список строк. Соглашение: `&8` = серая (ещё недоступна), `&7[ ]` = текущая, `&a[✓]` = выполнена

Необязательное поле `required_item` — если игрок уже несёт нужный предмет, первая цель отмечается автоматически при выдаче:

```json
"required_item": { "id": "minecraft:golden_apple", "count": 1 }
```

### update_quest — обновить цели

```json
{
  "type": "update_quest",
  "quest_id": "find_herb",
  "objectives": [
    "&a[✓] Найти целебную траву на болотах",
    "&7[ ] Вернуться к лекарю"
  ]
}
```

Полностью заменяет список целей. Вызывается когда игрок выполнил какой-то шаг.

### complete_quest и fail_quest

```json
{ "type": "complete_quest", "quest_id": "find_herb" }
{ "type": "fail_quest", "quest_id": "find_herb" }
```

Квест уходит из HUD и перемещается в соответствующий раздел журнала.

---

## 8. Связывание NPC

Один из самых важных механизмов — когда разные NPC знают о действиях друг друга. Это реализуется через три инструмента: `visited_node`, `quest_status` и `set_var`/`if_var`.

### Принцип

Прогресс хранится на сервере и общий для всех. Когда игрок проходит узел в диалоге A — это записывается. Диалог B может проверить: «игрок уже был в таком-то узле такого-то диалога?». Если да — показать другие реплики или опции.

### Инструмент 1: visited_node

NPC B знает что игрок говорил с NPC A — и открывает нужную ветку:

```json
"options": [
  {
    "text": "&fОтшельник послал меня. Мне нужна твоя помощь.",
    "next": "knows_hermit",
    "condition": {
      "type": "visited_node",
      "dialogue": "cursed_historian",
      "node": "ask"
    }
  },
  { "text": "&7Я сам нашёл тебя.", "next": "suspicious" }
]
```

Опция «Отшельник послал меня» появится только если игрок уже прошёл узел `"ask"` в диалоге `cursed_historian`. Иначе только второй вариант. Это создаёт естественный порядок прохождения.

### Инструмент 2: quest_status

NPC реагирует по-разному в зависимости от состояния квеста:

```json
"on_revisit": {
  "default": "&7*Историк молчит*",
  "conditions": [
    {
      "condition": { "type": "quest_status", "quest_id": "cursed_stone", "status": "active" },
      "text": "&fИди к отшельнику. Он знает где искать."
    },
    {
      "condition": { "type": "if_var", "name": "stone_cleansed", "op": "exists" },
      "start_node": "return_with_stone"
    },
    {
      "condition": { "type": "quest_status", "quest_id": "cursed_stone", "status": "completed" },
      "text": "&aВсё закончилось. Благодарю тебя."
    }
  ]
}
```

### Инструмент 3: set_var / if_var

Переменные передают произвольное состояние между диалогами. Ведьма ставит переменную — Историк читает её:

```json
"done": {
  "actions": [
    { "type": "set_var", "name": "stone_cleansed", "value": "true" },
    { "type": "notify_npc", "dialogue_id": "cursed_historian" }
  ]
}
```

Историк в `on_revisit.conditions`:
```json
{
  "condition": { "type": "if_var", "name": "stone_cleansed", "op": "exists" },
  "start_node": "return_with_stone"
}
```

### Инструмент 4: notify_npc — визуальный сигнал

Чтобы игрок знал что нужно вернуться — используй `notify_npc`. После ритуала у ведьмы над Историком загорится `!`:

```json
{ "type": "notify_npc", "dialogue_id": "cursed_historian" }
```

### Цепочка через after_dialogue summon

Чтобы следующий NPC появлялся только после разговора с предыдущим — используй триггер `after_dialogue`:

```json
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

Отшельник появится через 6 секунд после того как игрок завершил диалог с Историком.

---

## 9. Повторный разговор (on_revisit)

По умолчанию после прохождения диалога (игрок дошёл до конечного узла) мод считает его завершённым. При следующем ПКМ — вместо полного диалога показывается короткое сообщение или запускается диалог с нужного узла.

Если нужно чтобы диалог вообще можно было пройти заново — используй `"repeatable": true` в корне файла. Тогда `on_revisit` не нужен.

### Структура

```json
"on_revisit": {
  "default": "&7*Зомби молча смотрит на тебя*",
  "default_start_node": "small_talk",
  "conditions": [
    {
      "condition": { "type": "quest_status", "quest_id": "find_herb", "status": "active" },
      "text": "&fТы уже нашёл траву?"
    },
    {
      "condition": { "type": "has_item", "item": "minecraft:fern", "count": 3 },
      "text": "&eВижу у тебя есть трава!",
      "start_node": "return_with_herb"
    },
    {
      "condition": { "type": "quest_status", "quest_id": "find_herb", "status": "completed" },
      "text": "&aСпасибо тебе, друг."
    }
  ]
}
```

### Как работает логика

1. Условия проверяются **сверху вниз**
2. Первое выполненное условие применяется:
   - Если есть `start_node` — открывается полный диалог с этого узла
   - Если нет — показывается короткое сообщение на несколько секунд
3. Если **ни одно** не выполнено:
   - Если есть `default_start_node` — открывается диалог с него
   - Если есть `default` — показывается короткое сообщение

| Поле | Описание |
|------|----------|
| `default` | Текст по умолчанию |
| `default_start_node` | Узел для диалога по умолчанию |
| `conditions[].condition` | Любое условие из раздела 5 |
| `conditions[].text` | Короткое сообщение если условие сработало |
| `conditions[].start_node` | Узел для полного диалога вместо сообщения |

---

## 10. Автоспавн (summon)

Блок `summon` позволяет NPC появляться в мире автоматически. Мод создаёт моба, присваивает имя и теги — после чего игрок подходит и сам инициирует разговор ПКМ.

```json
"summon": {
  "entity": "minecraft:zombie",
  "custom_name": "Историк",
  "tags": ["cursed_historian"],
  "spawn_position": "behind_player",
  "despawn_after_dialogue": false,
  "walk_away_before_despawn": false,
  "trigger": {
    "type": "on_join",
    "delay": 60
  }
}
```

### Поля summon

| Поле | Описание |
|------|----------|
| `entity` | Тип моба: `minecraft:zombie`, `minecraft:villager`, `minecraft:witch` и т.д. |
| `custom_name` | Имя моба. Должно совпадать с `target.name` в этом же файле |
| `tags` | Теги. Должны совпадать с `target.tag` |
| `spawn_position` | `"behind_player"` — 3 блока за спиной игрока |
| `despawn_after_dialogue` | `true` — моб удалится после диалога |
| `walk_away_before_despawn` | `true` — перед удалением моб уйдёт на ~10 блоков |
| `trigger` | Объект-триггер — когда спавнить |

### Триггеры

#### on_join — при входе в мир

```json
"trigger": { "type": "on_join", "delay": 60 }
```

Спавнит через `delay` тиков после входа игрока. Срабатывает один раз — только если диалог ещё не пройден.

#### after_dialogue — после завершения другого диалога

```json
"trigger": {
  "type": "after_dialogue",
  "dialogue_id": "cursed_historian",
  "delay": 120
}
```

Спавнит через `delay` тиков после завершения диалога `cursed_historian`. Основа сюжетных цепочек.

#### player_near — игрок приближается к точке

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

Срабатывает только в момент входа, не повторяется пока игрок внутри.

#### player_looking_for_seconds — игрок смотрит на точку N секунд

```json
"trigger": {
  "type": "player_looking_for_seconds",
  "x": 50, "y": 70, "z": 50,
  "radius": 64,
  "seconds": 3
}
```

#### on_player_death — после смерти игрока

```json
"trigger": { "type": "on_player_death", "delay": 20 }
```

---

## 11. Иконка над NPC

Мод автоматически рисует жёлтый `!` над NPC в двух случаях:

1. **Диалог ещё не начат** — игрок никогда не разговаривал с этим NPC
2. **Вызвано `notify_npc`** — автор явно сигнализировал что у NPC появилось новое

Видна в радиусе 16 блоков. Исчезает как только игрок начинает разговор.

---

## 12. Настройка внешнего вида GUI

### Аватар NPC

По умолчанию показывается голова зомби. Чтобы поставить свою:

```json
"avatar": "minecraft:textures/entity/villager/villager.png"
```

Формат: `namespace:path_to_texture`. Из текстуры берётся область 8×8 начиная с позиции (8, 8) — это первый слой головы скина.

Через NBT без ресурспака:
```
/data merge entity @e[name=ИмяНПС,limit=1] {DialogueAvatar:"minecraft:textures/entity/zombie/zombie.png"}
```

### Фон панели диалога

По умолчанию — тёмно-синий градиент. Заменяется своей текстурой:

```json
"background": "mypack:textures/gui/dialogue_bg.png"
```

Текстура растягивается на всю ширину и высоту панели. Должна быть в ресурспаке по пути `assets/mypack/textures/gui/dialogue_bg.png`.

### Фон панелей вариантов ответа

```json
"options_background": "mypack:textures/gui/option_bg.png"
```

Применяется к каждой кнопке отдельно, тоже растягивается под её размер.

### Структура ресурспака

```
resourcepacks/
  my_dialogue_pack/
    pack.mcmeta
    assets/
      mydialogue/
        textures/
          gui/
            dialogue_bg.png
            option_bg.png
```

JSON:
```json
"background": "mydialogue:textures/gui/dialogue_bg.png",
"options_background": "mydialogue:textures/gui/option_bg.png"
```

Ресурспак кладётся в `.minecraft/resourcepacks/` и включается в настройках ресурспаков как обычно.

---

## 13. Управление диалогом

| Клавиша | Действие |
|---------|----------|
| **ПКМ** | Следующий узел (линейный) / Открыть варианты (выбор) / Закрыть (конечный) |
| **Клик мышью** | Выбрать вариант ответа |
| **1–5** | Быстрый выбор варианта по номеру |
| **↑ ↓** | Навигация по вариантам |
| **Enter** | Выбрать выделенный вариант |
| **ESC** | Закрыть диалог |
| **J** | Журнал (вкладки «Диалоги» и «Квесты») |
| **K** | Скрыть/показать HUD квестов |

---

## 14. Команды

### /dialogue reload

Перезагружает все диалоги из папки. Использовать после любых изменений в JSON.

### /dialogue reload `<id>`

Перезагружает один диалог и сбрасывает весь прогресс по нему. Для тестирования.

```
/dialogue reload cursed_historian
```

### /dialogue test `<id>` [node]

Запускает диалог с ближайшим подходящим мобом без проверки прогресса. Если указан `node` — начинает с него.

```
/dialogue test old_zombie check_apple
```

### /dialogue goto `<node>`

В активном диалоге — немедленно переходит к указанному узлу.

### /npc spawn `<id>`

Спавнит NPC у ног. Тип моба берётся из `summon.entity` в JSON. Имя и теги — из `target`.

```
/npc spawn cursed_historian
```

### /npc tag `<id>`

Присваивает ближайшему мобу имя и тег из указанного диалога.

### /npc remove

Удаляет ближайшего NPC.

### /npc list [radius]

Показывает всех NPC в радиусе (по умолчанию 32 блока).

---

## 15. Как добавить NPC в мир

### Способ 1 — /npc spawn (проще всего)

```
/npc spawn my_dialogue
```

Мод создаёт моба нужного типа с нужным именем и тегом прямо у ног.

### Способ 2 — /npc tag (для существующего моба)

Подойди к мобу и выполни `/npc tag my_dialogue` — мод присвоит ему нужные имя и тег.

### Способ 3 — вручную

```
/tag @e[type=minecraft:zombie,distance=..3,limit=1,sort=nearest] add my_tag
/data merge entity @e[type=minecraft:zombie,distance=..3,limit=1,sort=nearest] {CustomName:'"Моё Имя"',CustomNameVisible:1b}
```

### Способ 4 — автоспавн (summon)

Мод сам создаёт моба при нужном условии — см. раздел 10.

---

## 16. Прогресс и мультиплеер

Весь прогресс **общий для всех игроков** на сервере:

- `visited_node` выполнено если хоть один игрок посетил этот узел
- `killed_mob` — суммарные убийства всех игроков
- Переменные (`set_var`) — одинаковы для всех
- Квесты видны всем в журнале
- `notify_npc` зажигает иконку для всех

Прогресс сохраняется между измерениями — хранилище привязано к Overworld.

Сбросить прогресс конкретного диалога: `/dialogue reload <id>`

---

## Полный пример — три связанных NPC

**Историк** появляется при входе в мир. Выдаёт квест.  
**Отшельник** появляется после разговора с Историком, объясняет что нужно ведьме.  
**Ведьма** появляется после Отшельника, берёт плату, ставит `notify_npc` на Историка.  
Игрок возвращается к Историку и получает награду.

### cursed_historian.json

```json
{
  "target": { "name": "Историк", "tag": "cursed_historian" },
  "display_name": "&e[&6Историк&e]",
  "entry": "start",

  "summon": {
    "entity": "minecraft:zombie",
    "custom_name": "Историк",
    "tags": ["cursed_historian"],
    "spawn_position": "behind_player",
    "despawn_after_dialogue": false,
    "trigger": { "type": "on_join", "delay": 60 }
  },

  "on_revisit": {
    "default": "&e[&6Историк&e] &7Иди к отшельнику в лесу.",
    "conditions": [
      {
        "condition": { "type": "quest_status", "quest_id": "cursed_stone", "status": "none" },
        "start_node": "start"
      },
      {
        "condition": { "type": "if_var", "name": "stone_cleansed", "op": "exists" },
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
      "text": "&fИз деревни пропал &cПроклятый камень&f. Он хранился у меня в подвале.",
      "next": "danger"
    },
    "danger": {
      "text": "&cЕсли камень попадёт не в те руки — &fвсё живое в округе начнёт гнить.",
      "next": "ask"
    },
    "ask": {
      "text": "&fВ лесу живёт отшельник. Он знает о проклятиях. &eПоговори с ним.",
      "options": [
        {
          "text": "&aЯ помогу",
          "next": "accept",
          "actions": [{
            "type": "start_quest",
            "quest": {
              "id": "cursed_stone",
              "title": "&cПроклятый камень",
              "description": "&fНайди способ обезвредить проклятый камень.",
              "objectives": [
                "&7[ ] Поговорить с отшельником в лесу",
                "&8[ ] Найти ведьму",
                "&8[ ] Вернуть камень историку"
              ]
            }
          }]
        },
        { "text": "&7Не моё дело", "next": "refuse" }
      ]
    },
    "accept": { "text": "&aСпасибо. Отшельник живёт к северу от деревни." },
    "refuse": { "text": "&7Понимаю. Если передумаешь — я здесь." },
    "return_with_stone": {
      "text": "&f*Историк смотрит с надеждой* &eКамень очищён?",
      "next": "reward"
    },
    "reward": {
      "text": "&eВозьми это. Ты заслужил.",
      "actions": [
        { "type": "give_item", "item": "minecraft:diamond", "count": 3 },
        { "type": "give_item", "item": "minecraft:golden_apple", "count": 2 },
        { "type": "complete_quest", "quest_id": "cursed_stone" }
      ]
    }
  }
}
```

### forest_hermit.json

```json
{
  "target": { "name": "Отшельник", "tag": "forest_hermit" },
  "display_name": "&2[&aОтшельник&2]",
  "entry": "start",

  "summon": {
    "entity": "minecraft:zombie",
    "custom_name": "Отшельник",
    "tags": ["forest_hermit"],
    "spawn_position": "behind_player",
    "despawn_after_dialogue": false,
    "trigger": {
      "type": "after_dialogue",
      "dialogue_id": "cursed_historian",
      "delay": 120
    }
  },

  "on_revisit": {
    "default": "&2[&aОтшельник&2] &7Ступай к ведьме. Только она поможет."
  },

  "nodes": {
    "start": {
      "text": "&7*Старик долго смотрит на тебя* &fТебя послал историк?",
      "options": [
        {
          "text": "&fДа, он сказал что ты знаешь о проклятиях",
          "next": "knows",
          "condition": { "type": "visited_node", "dialogue": "cursed_historian", "node": "ask" }
        },
        { "text": "&7Нет, я сам по себе", "next": "go_away" }
      ]
    },
    "knows": {
      "text": "&fПроклятый камень... Да, слышал.",
      "next": "explain"
    },
    "explain": {
      "text": "&fЕго нельзя уничтожить обычными способами. Нужна &dведьма с болот&f.",
      "next": "requirement"
    },
    "requirement": {
      "text": "&cНо она не поможет просто так. &fПринеси ей &e3 паучьих глаза &fи &e1 зелье ночного зрения&f.",
      "next": "farewell",
      "actions": [{
        "type": "update_quest",
        "quest_id": "cursed_stone",
        "objectives": [
          "&a[✓] Поговорить с отшельником в лесу",
          "&7[ ] Найти ведьму на болотах",
          "&7[ ] Принести: 3x паучий глаз + зелье ночного зрения",
          "&8[ ] Вернуть камень историку"
        ]
      }]
    },
    "farewell": { "text": "&7*Отшельник возвращается к костру*" },
    "go_away": { "text": "&7Уходи. Мне не нужны гости." }
  }
}
```

### swamp_witch.json

```json
{
  "target": { "name": "Ведьма", "tag": "swamp_witch" },
  "display_name": "&5[&dВедьма&5]",
  "entry": "start",

  "summon": {
    "entity": "minecraft:witch",
    "custom_name": "Ведьма",
    "tags": ["swamp_witch"],
    "spawn_position": "behind_player",
    "despawn_after_dialogue": false,
    "trigger": {
      "type": "after_dialogue",
      "dialogue_id": "forest_hermit",
      "delay": 160
    }
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
      "text": "&d*Ведьма медленно оборачивается* &fКто тебя послал?",
      "options": [
        {
          "text": "&fОтшельник из леса. Мне нужна помощь с камнем.",
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
    "wait": { "text": "&7*Ведьма скрещивает руки и ждёт*" },
    "has_items_check": {
      "text": "&dА, принёс наконец.",
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
      "text": "&d*Ведьма бормочет заклинание...*",
      "next": "ritual2",
      "auto_next_ticks": 60
    },
    "ritual2": {
      "text": "&5*Вспышка фиолетового света. Воздух пахнет серой.*",
      "next": "done",
      "auto_next_ticks": 40
    },
    "done": {
      "text": "&aГотово. &fКамень очищен. Возвращайся к историку.",
      "actions": [
        { "type": "give_item", "item": "minecraft:enchanted_book", "count": 1 },
        { "type": "give_effect", "effect": "minecraft:night_vision", "duration": 600, "amplifier": 0 },
        { "type": "set_var", "name": "stone_cleansed", "value": "true" },
        { "type": "notify_npc", "dialogue_id": "cursed_historian" },
        {
          "type": "update_quest",
          "quest_id": "cursed_stone",
          "objectives": [
            "&a[✓] Поговорить с отшельником в лесу",
            "&a[✓] Найти ведьму на болотах",
            "&a[✓] Принести: 3x паучий глаз + зелье ночного зрения",
            "&7[ ] Вернуть камень историку"
          ]
        }
      ]
    },
    "not_ready": { "text": "&7Тогда возвращайся когда будет всё." },
    "suspicious": { "text": "&cБез рекомендации я не помогаю чужакам. Уходи." }
  }
}
```

После узла `done` у ведьмы: квест обновился, переменная `stone_cleansed` установлена, над Историком загорится `!`. Игрок возвращается — `on_revisit` видит `if_var(stone_cleansed)` → открывает `return_with_stone` → выдаёт награду → `complete_quest`.

[⬆ Наверх](#interactentity) · [🇬🇧 Switch to English](#english)
