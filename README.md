# InteractEntity

A mod for Minecraft Forge 1.20.1. Lets you build full-featured dialogues with mobs through JSON files.

**Language / Язык:** [🇬🇧 English](#english) · [🇷🇺 Русский](#русский)

---

<a id="english"></a>

## English

> [🇷🇺 Перейти к русской версии](#русский)

Complete reference + tutorial for the **InteractEntity** mod: JSON dialogue format, quests, reputation, NPCs, skins, journal, emotes, KubeJS integration and more.

> [!TIP]
> ### 💡 Too lazy to read? Let AI write the JSON files for you!
> You don't have to study the entire guide! You can let an AI (like Gemini or ChatGPT) write fully working JSON dialogue files for you.
> 
> Simply **upload or copy this entire `README.md` file** to your AI assistant, and copy-paste the prompt below:
> 
> ```
> You are an expert Minecraft quest writer. Using the attached documentation for the "InteractEntity" mod, generate a fully working branching dialogue in the specified JSON format.
> 
> Story details: [Describe your story/quest here. E.g., "A blacksmith named Borgin wants 10 iron ingots. If player brings them, remove iron, reward with a diamond, give +20 guild reputation, and spawn a sage NPC in front of the player. Otherwise tell him to come back later."]
> 
> Output ONLY the clean, valid JSON matching the spec. Double-check for trailing commas and correct action/condition types.
> ```
> 
> Once generated, paste the JSON into your dialogue file inside the world, run `/dialogue reload` in-game, and test it!

---

## Table of contents

1. [About the mod](#1-about-the-mod)
2. [Quick start — minimal dialogue in 30 seconds](#2-quick-start)
3. [Where dialogue files live and how IDs work](#3-file-location-and-ids)
4. [Root dialogue fields](#4-root-dialogue-fields)
5. [target — who we're looking for](#5-target)
6. [Nodes and transitions](#6-nodes)
7. [Options](#7-options)
8. [Actions](#8-actions)
9. [Conditions](#9-conditions)
10. [Variables](#10-variables)
11. [Quests](#11-quests)
12. [Linking NPCs together](#12-linking-npcs)
13. [on_revisit — revisiting](#13-on_revisit)
14. [Auto-spawning NPCs (summon)](#14-summon)
15. [Dialogue triggers](#15-triggers)
16. [Routines — NPC schedules](#16-routines)
17. [NPC entities (32 types + custom_npc)](#17-npc-entities)
18. [Emotes and animations](#18-emotes)
19. [custom_npc visuals](#19-visuals)
20. [Skins — dynamic loading](#20-skins)
21. [Journal, quest HUD and the `!` icon](#21-journal-and-hud)
22. [NPC avatar in dialogue window](#22-avatar)
23. [Reputation and factions](#23-reputation)
24. [Gifts](#24-gifts)
25. [Relationships between NPCs](#25-relationships)
26. [Companions and NPC home](#26-companions)
27. [Scope — global vs per_player](#27-scope)
28. [Placeholders](#28-placeholders)
28a. [Multilingual Dialogues in JSON](#28a-multilingual-dialogues-in-json)
29. [Text formatting](#29-formatting)
30. [Commands](#30-commands)
31. [Keybinds](#31-keybinds)
32. [KubeJS integration](#32-kubejs)
33. [Forge API — events and hooks](#33-forge-api)
34. [Gotchas](#34-gotchas)
35. [Big example — story map](#35-example)

---

## 1. About the mod

**InteractEntity** is a Forge 1.20.1 mod that lets you make proper NPCs with dialogues in Minecraft. Not vanilla-villager-style (mumbling and trading) but actual scripted characters: branching conversations, quests, reputation, relationships with each other and the player.

How it works: you put any mob in the world (zombie, villager, skeleton — anything), give it a `CustomName` and a scoreboard tag. Then you write a JSON file describing the dialogue and put the same name and tag inside. When a player right-clicks that mob, your dialogue opens.

The core idea is that **all content lives in JSON files**. No compilation, no Java. Want a new character? Drop a JSON in the dialogues folder, drop a PNG in the skins folder, run `/dialogue reload` — done.

What the mod can do:
- Branching dialogues with conditions ("if the player has a diamond, show the buy-sword option")
- Quests (personal and global, with objectives, deadlines, auto-counting kills)
- Reputation and factions
- Gifts with a cooldown
- Variables (NPC "memory" — who remembers what about the player)
- Auto-spawning NPCs (triggers like "player entered a zone")
- Custom skins via folder — no resource packs to build
- Emotes and animations (for the `custom_npc` type)
- A journal with character cards and a quest HUD
- KubeJS hooks — listen to events and tweak progress from JS

### How the mod is structured internally

Quick mental model so you know where things go:

- **JSON dialogues** are your content. They live in the configuration folder under `config/interactentity/dialogues/`. One file = one dialogue.
- **Skins (PNG)** live separately from the mod, in the player's config folder (`config/interactentity/skins/`) or in the world folder (`<world>/interactentity/skins/`). Details in §20.
- **Player progress** is saved automatically by the mod inside the world. You don't have to touch it — just make sure the scope (see §27) is right.
- **Journal** is opened with `J` — players see every NPC they've talked to, their reply history, and quest list.
- **If something doesn't work** — check the server log. The mod writes clear warnings about invalid JSON, badly-named skins and so on.

---

## 2. Quick start

The fastest way to confirm everything is working is to make a one-NPC dialogue with two replies and test it.

Create `config/interactentity/dialogues/test.json`:

```json
{
  "target": { "name": "Test", "tag": "test_npc" },
  "entry": "hi",
  "nodes": {
    "hi": {
      "text": "&aHi, &e{player}&a!",
      "options": [
        { "text": "Give me bread", "next": "give" },
        { "text": "Bye",           "next": null }
      ]
    },
    "give": {
      "text": "Here you go.",
      "actions": [
        { "type": "give_item", "item": "minecraft:bread", "count": 3 }
      ]
    }
  }
}
```

In-game, summon a zombie with the right name and tag:
```
/summon zombie ~ ~ ~ {CustomName:'"Test"',CustomNameVisible:1b,Tags:["test_npc"]}
```

Right-click the zombie — the dialogue opens. If nothing happens, check that the name matches `target.name` and the tag matches `target.tag`. The classic mistake is an extra space in the name.

---

## 3. File location and IDs

All dialogue JSONs live in the configuration folder: `config/interactentity/dialogues/`. Subfolders are allowed — handy for organizing chapters or zones.

Example structure:
```
config/interactentity/dialogues/
  zombie.json                → dialogue ID: "zombie"
  showcase/mayor.json        → dialogue ID: "showcase/mayor"
  story/chapter_1/intro.json → dialogue ID: "story/chapter_1/intro"
```

The **dialogue ID** is just the path relative to `dialogues/` without `.json`. Subfolders become slashes. You'll use this ID in commands (`/npc spawn showcase/mayor`) and in action fields that reference other dialogues (`force_dialogue`, `notify_npc`).

After any JSON edit you have to tell the mod to reread the file. Run `/dialogue reload` in chat — it rereads everything, also resetting progress and in-memory flags (handy when you're testing and want a clean state). If you want to reload only one file, use `/dialogue reload <id>` — but note that this variant does **not** reset spawn flags (see §34, point 14).

---

## 4. Root dialogue fields

| Field | Type | Req. | Description |
|-------|------|------|-------------|
| `target` | object | yes | See §5 |
| `entry` | string | yes | ID of the starting node |
| `nodes` | object | yes | `{id: NodeJson}` — node dictionary |
| `display_name` | string | — | Name shown in the dialogue UI. Defaults to `target.name` |
| `scope` | string | — | `"global"` (default) or `"per_player"` — where progress is stored. See §27 |
| `repeatable` | bool | — | `false` (default). If `true` the dialogue can be replayed |
| `invulnerable` | bool | — | `true` (default) — NPC is invulnerable while talking |
| `disable_knockback` | bool | — | `false` (default). If `true` disables knockback/movement from hits for this NPC |
| `disable_attacks` | bool | — | `false` (default). If `true` completely disables attack registration (pain animation, pain sound, and hits) for this NPC |
| `avatar` | string | — | Avatar texture in the dialogue window and journal. **Full path required** (e.g. `"interactentity:textures/entity/skins/harold.png"`). A bare skin name is NOT expanded here — that shortcut works only for `visual.texture` |
| `faction` | string | — | Faction name (shown in the journal) |
| `reputation_id` | string | — | Faction ID for reputation accumulation. Defaults to `faction` |
| `character_info` | string | — | Character description for the journal |
| `visual` | object | — | See §19 — model/texture/scale for `interactentity:custom_npc` |
| `summon` | object | — | See §14 — auto-spawn config |
| `triggers` | array | — | See §15 — auto-start dialogue with an existing NPC |
| `routines` | array | — | See §16 — NPC schedule |
| `on_revisit` | object | — | See §13 — reaction on subsequent visits |

Legacy: `start_trigger` (a single trigger). If `triggers[]` is present, `start_trigger` is ignored.

---

## 5. target

| Field | Type | Req. | Description |
|-------|------|------|-------------|
| `name` | string | yes | Must match the mob's `CustomName` |
| `tag` | string | yes | Must be in the mob's scoreboard tags |
| `entity_type` | string | — | Entity type (`minecraft:zombie`, `interactentity:custom_npc`, …) |
| `faction` | string | — | Metadata (doesn't affect resolution) |

All specified fields must match. Otherwise right-click does nothing.

> [!WARNING]
> ### ⚠️ Critical Matching Rule: entity_type and tags
> - If you specify `entity_type` in the `"target"` section, it must **exactly** match the entity type spawned in the world. For example, if you spawned a `minecraft:villager` but `"target"` requires `"interactentity:custom_npc"`, you will not be able to talk to the NPC.
> - When using the `/summon` command manually to test, make sure you include the scoreboard tag matching `"target.tag"` (e.g. `/summon interactentity:custom_npc ~ ~ ~ {CustomName:'"Elsa"',Tags:["story_elsa"]}`), otherwise the dialogue will not resolve!

**Auto-mapping on `/npc spawn`:** if `entity_type: minecraft:<mob>` is one of the 32 "peaceful" mobs (see §17), the mod substitutes `interactentity:npc_<mob>` to make sure the NPC won't attack you. In JSON keep writing the vanilla name — the conversion is automatic.

---

## 6. Nodes

Every dialogue is a set of nodes (`nodes`). A node is one NPC reply. The player walks through nodes by right-clicking or choosing an option.

The mod auto-detects node type from its fields — no need to declare it. There are three types:

| Type | Marker | Behavior |
|------|--------|----------|
| **Linear** | has `next`, no `options` | Right-click → next node |
| **Choice** | has `options` | Player picks a button |
| **End** | no `next` and no `options` | Dialogue closes. `"next": null` also counts as end |

### Node fields

| Field | Type | Description |
|-------|------|-------------|
| `text` | string | NPC reply. Defaults to `""` |
| `random_text` | array | Array of strings — one picked at random on entry. Overrides `text` |
| `next` | string \| null | ID of the next node |
| `auto_next_ticks` | int | Auto-advance after N ticks (20 = 1 sec) |
| `options` | array | See §7 |
| `actions` | array | Actions on node entry (see §8) |
| `camera` | string | Camera mode. Default `"npc"` |
| `camera_yaw_offset` | float | Horizontal camera offset |
| `camera_pitch_offset` | float | Vertical camera offset |

### Example: linear → choice → end

```json
"nodes": {
  "intro": {
    "text": "Hello there.",
    "next": "main"
  },
  "main": {
    "text": "What do you want?",
    "options": [
      { "text": "A gift", "next": "gift" },
      { "text": "Nothing", "next": null }
    ]
  },
  "gift": {
    "text": "Take this apple.",
    "actions": [{ "type": "give_item", "item": "minecraft:apple" }]
  }
}
```

### Example: random_text

```json
"greeting": {
  "random_text": [
    "Hi!",
    "Heyo.",
    "Oh, you again.",
    "&7*nods*"
  ],
  "next": "hub"
}
```

---

## 7. Options

Options are the buttons shown in choice nodes. Each option is an object in the node's `options` array. It has button text and a destination. You can attach a condition (the button goes grey if it fails) and actions (run on click).

| Field | Type | Description |
|-------|------|-------------|
| `text` | string | Button text (required) |
| `next` | string \| null | Where to go |
| `condition` | object | If set and false → button is shown **grey** (locked) |
| `locked` | bool | Force locked state |
| `lock_reason` | string | Tooltip text for the locked reason |
| `actions` | array | Actions on click |

**Only one condition per option.** Compound (AND/OR) is not supported — for composite logic use an intermediate node.

### Example: conditional option

```json
"shop": {
  "text": "Buy a sword?",
  "options": [
    {
      "text": "Yes (10 diamonds)",
      "condition": { "type": "has_item", "item": "minecraft:diamond", "count": 10 },
      "lock_reason": "Need 10 diamonds",
      "next": "buy",
      "actions": [
        { "type": "remove_item", "item": "minecraft:diamond", "count": 10 },
        { "type": "give_item",   "item": "minecraft:diamond_sword" }
      ]
    },
    { "text": "No thanks", "next": null }
  ]
}
```

---

## 8. Actions

`actions` is an array of commands executed when a node is entered or an option is clicked. For example: "give the player 5 bread", "play a bell sound", "start a quest", "open another dialogue". They run top to bottom.

Usage:
```json
"actions": [
  { "type": "give_item", "item": "minecraft:bread", "count": 5 },
  { "type": "play_sound", "sound": "minecraft:entity.villager.yes" }
]
```

Below — all 28 types with examples. A `?` after a field means optional; the value in parentheses is the default.

### 8.1 Basic

#### `give_item` / `remove_item`
```json
{ "type": "give_item",   "item": "minecraft:apple", "count": 5 }
{ "type": "remove_item", "item": "minecraft:apple", "count": 3 }
```
`remove_item` works with any items, including modded ones and items with NBT (enchanted, renamed, or with durability).

#### `run_command`
```json
{ "type": "run_command", "command": "give @s minecraft:diamond 1" }
```
No leading slash. Runs as the server with perm-level 2. `@s` = the player.

#### `teleport`
```json
{ "type": "teleport", "x": 100, "y": 64, "z": 200 }
{ "type": "teleport", "x": 5, "y": 0, "z": -3, "mode": "relative" }
```
`mode`: `"absolute"` (default) or `"relative"`. Also accepts `yaw`, `pitch`.

#### `play_sound`
```json
{ "type": "play_sound", "sound": "minecraft:entity.villager.yes", "volume": 1.0, "pitch": 1.0 }
{ "type": "play_sound", "sound": "minecraft:block.bell.use", "target": "entity" }
```
`target`: `"player"` (default — only the player hears) or `"entity"` (plays at the NPC's position, audible to nearby players).

#### `give_effect` / `remove_effect`
```json
{ "type": "give_effect", "effect": "minecraft:regeneration", "duration": 400, "amplifier": 1 }
{ "type": "remove_effect", "effect": "minecraft:slowness" }
{ "type": "remove_effect" }  // remove all
```
`duration` in ticks (default 200), `amplifier` 0-255, `ambient`/`particles` are bools.

#### `spawn_particles`
```json
{ "type": "spawn_particles", "particle": "minecraft:happy_villager", "count": 20, "spread": 0.5 }
```
`target`: `"entity"` (default) or `"player"`.

#### `camera_shake`
```json
{ "type": "camera_shake", "intensity": 2.0, "duration": 30 }
```

#### `set_time` / `set_weather`
```json
{ "type": "set_time", "time": "night" }
{ "type": "set_time", "time": 6000 }  // exactly noon
{ "type": "set_weather", "weather": "thunder", "duration": 6000 }
```
`set_time.time`: `"day"` / `"noon"` / `"night"` / `"midnight"` or a tick number (0-23999).
`set_weather.weather`: `"clear"` / `"rain"` / `"thunder"`.

### 8.2 Scripting

#### `set_var`
```json
{ "type": "set_var", "name": "trust", "value": "1", "op": "set" }
{ "type": "set_var", "name": "trust", "op": "inc" }   // +1
{ "type": "set_var", "name": "trust", "op": "dec" }   // -1
```

#### `fire_event` — posts `DialogueChoiceEvent` for KubeJS/Forge
```json
{ "type": "fire_event", "tag": "started_quest_chain" }
```

#### `schedule_event` — delayed execution
```json
{
  "type": "schedule_event",
  "delay": 600,
  "actions": [
    { "type": "play_sound", "sound": "minecraft:entity.lightning_bolt.thunder" },
    { "type": "give_effect", "effect": "minecraft:slowness", "duration": 200 }
  ]
}
```
⚠️ Doesn't survive a server restart if the player is offline.

#### `force_dialogue` — open another dialogue
```json
{
  "type": "force_dialogue",
  "dialogue_id": "story/chapter_2/intro",
  "target_tag": "mayor",
  "radius": 32.0,
  "start_node": "greeting"
}
```
Finds the nearest NPC with `target_tag` in radius and opens the dialogue with them.

#### `notify_npc` — light up `!` over an NPC with the given dialogue
```json
{ "type": "notify_npc", "dialogue_id": "blacksmith" }
```

#### `summon_npc` — spawn an NPC during the dialogue
```json
{
  "type": "summon_npc",
  "entity": "minecraft:villager",
  "name": "Merchant",
  "tags": ["merchant"],
  "despawn": false,
  "walk_away": false,
  "start_dialogue": "merchant",
  "spawn_position": "behind_player"
}
```
`spawn_position`: `"behind_player"` (default), `"front_of_player"`, `"at_player"`.

### 8.3 Quests

See §11 for the full reference.

```json
{ "type": "start_quest", "quest": { "id": "...", "title": "...", ... } }
{ "type": "complete_objective", "quest_id": "harold_bread", "objective_number": 1 }
{ "type": "complete_quest", "quest_id": "harold_bread" }
{ "type": "fail_quest", "quest_id": "harold_bread" }
{ "type": "update_quest", "quest_id": "...", "objectives": [...] }
```

### 8.4 Social

#### `add_reputation`
```json
{ "type": "add_reputation", "id": "village", "value": 10, "label": "Helped the elder" }
```

#### `give_gift` — gift with 1-hour cooldown per NPC
```json
{
  "type": "give_gift",
  "character_id": "harold",
  "item": "minecraft:bread",
  "amount": 1,
  "reputation": 5,
  "label": "Gift",
  "success_message": "&aHarold accepts the bread.",
  "cooldown_message": "&7He's already eaten today."
}
```

#### `set_relationship` — set a relationship between two NPCs
```json
{ "type": "set_relationship", "npc_a": "mayor", "npc_b": "thief", "relationship": "enemy" }
```

#### `set_companion` — make NPC follow the player
```json
{ "type": "set_companion", "enable": true }
```
Only for `interactentity:custom_npc`.

#### `set_home` — set NPC's "home"
```json
{ "type": "set_home", "x": 100, "y": 64, "z": 200, "radius": 16 }
```
Without coordinates uses the NPC's current position. NPC will return to within `radius` blocks.

#### `play_emote` — play an animation
```json
{ "type": "play_emote", "emote": "wave", "duration_ticks": 40 }
{ "type": "play_emote", "emote": "six_seven" }
{ "type": "play_emote", "emote": "none" }  // clear
```
Only for `interactentity:custom_npc`. List in §18.

### 8.5 `scope` on individual actions

You can explicitly add `"scope": "global"` or `"per_player"` to any action. By default the scope is inherited from the dialogue's root — `DialogueTree.injectScope` auto-injects it into every action/condition that doesn't have its own `scope`.

```json
{ "type": "start_quest", "quest": { "id": "epic_quest" }, "scope": "global" }
```

Useful when a per-player dialogue needs to affect a global quest or vice versa.

---

## 9. Conditions

A condition checks some state of the world or player and returns true/false. Used in options (to hide/lock a button) and in `on_revisit` (to choose which branch to show).

Each condition is a JSON object with a `type` field and type-specific parameters. Example:
```json
{ "type": "has_item", "item": "minecraft:diamond", "count": 5 }
```

All 19 condition types:

| `type` | Fields | Meaning |
|--------|--------|---------|
| `has_item` | `item`, `count?` (1), `nbt?` | Counts any items — modded and NBT included. Supports matching specific NBT via optional `"nbt"` string (e.g., `"{GunId:\"tacz:deagle\"}"`) |
| `visited_node` | `dialogue`, `node` | Player visited this node |
| `quest_status` | `quest_id`, `status` (`"active"`/`"completed"`/`"failed"`/`"none"`) | |
| `if_var` | `name`, `op?` (`"eq"`/`"neq"`/`"gt"`/`"lt"`/`"gte"`/`"lte"`/`"exists"`), `value?` | |
| `reputation` | `id`, `op?` (default `"gte"`), `value` | |
| `killed_mob` | `entity`, `tag?`, `count?` (1) | Counter shared across all players on server |
| `has_effect` | `effect` | |
| `health_below` | `value`, `percent?` (false) | `percent: true` → value as % of max HP |
| `hunger_below` | `value` | 0–20 scale |
| `time_of_day` | `period?` (`"day"`/`"dusk"`/`"night"`/`"dawn"`) | |
| `weather` | `"clear"`/`"rain"`/`"thunder"` | |
| `dimension` | `minecraft:overworld` etc. | |
| `biome` | `minecraft:desert` etc. | |
| `can_give_gift` | `character_id` | Gift cooldown elapsed |
| `npc_relationship` | `npc_a`, `npc_b`, `relationship` | |
| `has_advancement` | `advancement` | Vanilla advancement id |
| `experience_level` | `level`, `op?` (default `"gte"`) | |
| `is_raining` | — | |
| `is_night` | — | true between ticks 13000–23000 |

### Examples

```json
{ "type": "if_var", "name": "met_harold", "value": "1", "op": "eq" }
{ "type": "reputation", "id": "village", "op": "gte", "value": 50 }
{ "type": "killed_mob", "entity": "minecraft:zombie", "count": 10 }
{ "type": "health_below", "value": 50, "percent": true }
{ "type": "quest_status", "quest_id": "harold_bread", "status": "completed" }
```

---

## 10. Variables

Variables let an NPC remember things about the player. For example: "this player already met me", "they brought me a gift 3 times", "trust level = 5".

Technically they're name → value pairs (value is always a string, but if it looks like a number the mod compares them as numbers). Stored either globally on the server or per-player — depends on the dialogue's scope (see §27).

### Setting

```json
{ "type": "set_var", "name": "trust", "value": "5", "op": "set" }
{ "type": "set_var", "name": "trust", "op": "inc" }  // +1 (numbers only)
{ "type": "set_var", "name": "trust", "op": "dec" }  // -1
```

### Reading in conditions

```json
{ "type": "if_var", "name": "met_harold", "value": "1", "op": "eq" }
{ "type": "if_var", "name": "trust", "value": "10", "op": "gte" }
{ "type": "if_var", "name": "secret_word", "op": "exists" }
```

### Reading in text via placeholder

```json
"text": "You have &e{var:trust}&r trust points."
```

### Example: NPC remembers first meeting

```json
"nodes": {
  "start": {
    "text": "...",
    "options": [
      {
        "text": "(Approach)",
        "condition": { "type": "if_var", "name": "met", "value": "1", "op": "neq" },
        "next": "first_meeting"
      },
      {
        "text": "(Approach again)",
        "condition": { "type": "if_var", "name": "met", "value": "1", "op": "eq" },
        "next": "return"
      }
    ]
  },
  "first_meeting": {
    "text": "Oh, a new face!",
    "actions": [{ "type": "set_var", "name": "met", "value": "1" }],
    "next": null
  },
  "return": {
    "text": "You again.",
    "next": null
  }
}
```

### Example: trust counter

```json
"options": [
  {
    "text": "(Help)",
    "next": "thanks",
    "actions": [{ "type": "set_var", "name": "trust", "op": "inc" }]
  },
  {
    "text": "(Refuse rudely)",
    "next": "rude",
    "actions": [{ "type": "set_var", "name": "trust", "op": "dec" }]
  }
]
```

Later:
```json
"options": [
  {
    "text": "Tell me the secret",
    "condition": { "type": "if_var", "name": "trust", "value": "5", "op": "gte" },
    "lock_reason": "Not enough trust",
    "next": "secret"
  }
]
```

---

## 11. Quests

Quests are tasks an NPC gives the player. A quest has a title, description, list of objectives (can be marked complete), status (active/completed/failed) and an optional deadline.

The player sees their quests in the journal (`J`) — they can see progress and pin a quest to the HUD (`K`) so it sticks on screen.

Like everything else, quests are stored either globally or per-player depending on the scope of the dialogue that started them. "Harold's personal quest for me" → `per_player`; "common server-wide story quest" → `global`.

### 11.1 start_quest

```json
{
  "type": "start_quest",
  "quest": {
    "id": "harold_bread",
    "title": "Bread for Harold",
    "description": "Bring Harold 3 loaves of bread.",
    "objectives": ["Bring 3 bread", "Talk to Harold"],
    "required_item": { "id": "minecraft:bread", "count": 3 },
    "giver": "Harold"
  }
}
```

### 11.2 Quest fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique ID |
| `title` | string | Short title |
| `description` | string | Journal description |
| `objectives` | array | List of strings. **Don't write `[ ]`/`[✓]` yourself** — the mod adds checkmarks |
| `required_item` | object | `{id, count?}` — if the player already has the count, the first objective auto-closes |
| `required_kills` | object | `{entity, tag?, count, objective?}` — auto-counter with `(N/M)` label |
| `deadline` | object | `{type, value?}`: `"ticks"`/`"game_days"` (need `value`), `"sunset"`/`"sunrise"` |
| `giver` | string | Quest giver's name. **Ignored when started from dialogue JSON** — the giver is taken from the NPC's `display_name`/CustomName. Only honored when a quest is started via the KubeJS API |

### 11.3 "Kill N mobs" objective

```json
{
  "type": "start_quest",
  "quest": {
    "id": "kill_zombies",
    "title": "Cleanup",
    "objectives": ["Kill zombies"],
    "required_kills": {
      "entity": "minecraft:zombie",
      "count": 10,
      "objective": 0
    }
  }
}
```

In the journal you'll see `Kill zombies (3/10)` — counter is automatic.

### 11.4 Marking an objective complete manually

Use **one** of three (mutually exclusive):

```json
{ "type": "complete_objective", "quest_id": "harold_bread", "objective": 0 }          // 0-indexed
{ "type": "complete_objective", "quest_id": "harold_bread", "objective_number": 1 }   // 1-indexed
{ "type": "complete_objective", "quest_id": "harold_bread", "objective_text": "Bring 3 bread" }
```

⚠️ The field `"index"` **does not work** — silently logs a warn.

### 11.5 Deadline

```json
"deadline": { "type": "game_days", "value": 3 }
"deadline": { "type": "ticks", "value": 12000 }
"deadline": { "type": "sunset" }
"deadline": { "type": "sunrise" }
```

When time runs out the quest status auto-flips to `"failed"`.

### 11.6 Complete or fail a quest

```json
{ "type": "complete_quest", "quest_id": "harold_bread" }
{ "type": "fail_quest", "quest_id": "harold_bread" }
```

`complete_quest` marks all objectives done and fires `QuestCompleteEvent`.

### 11.7 Update objectives

```json
{
  "type": "update_quest",
  "quest_id": "harold_bread",
  "objectives": ["Bring 5 bread (updated)", "Talk to Harold"]
}
```

⚠️ Replaces the **entire** objectives list — breaks the kills counter if it was tied to an index.

---

## 12. Linking NPCs

A storyline is rarely about a single NPC — usually it's a chain. The player talks to one, who sends them to a second, who unlocks a third. The mod gives you several tools to build these chains.

They all share one idea: one NPC leaves a "trace" (a flag, a visited node, a quest status), another NPC checks for that trace in its `condition`.

### Tool 1 — `visited_node`

```json
// In villager_b's dialogue
{
  "type": "visited_node",
  "dialogue": "villager_a",
  "node": "agreed_to_help"
}
```

### Tool 2 — `quest_status`

```json
{ "type": "quest_status", "quest_id": "main_story_1", "status": "completed" }
```

### Tool 3 — `set_var` / `if_var`

A sets a flag, B reads it.

### Tool 4 — `notify_npc`

Lights up the yellow `!` over the NPC with the given `dialogue_id`. Use it to highlight the "next" NPC after a chapter ends.

```json
"actions": [{ "type": "notify_npc", "dialogue_id": "blacksmith" }]
```

### Chains via `after_dialogue` summon

```json
// chapter_2.json
"summon": {
  "entity": "interactentity:custom_npc",
  "custom_name": "Sage",
  "tags": ["sage"],
  "trigger": { "type": "after_dialogue", "dialogue_id": "chapter_1", "delay": 100 },
  "spawn_position": "front_of_player"
}
```

When the player finishes `chapter_1` (reaches an end node), 100 ticks later the chapter 2 NPC spawns.

---

## 13. on_revisit

Triggers after the player has reached an end-node (the dialogue is marked completed). **ESC does NOT count as completion.**

```json
"on_revisit": {
  "default": "&7*silence*",
  "default_start_node": "hub",
  "conditions": [
    {
      "condition": { "type": "quest_status", "quest_id": "main", "status": "active" },
      "start_node": "quest_in_progress"
    },
    {
      "condition": { "type": "reputation", "id": "village", "value": 50, "op": "gte" },
      "text": "&aWelcome back, friend!"
    }
  ]
}
```

**Logic:**
1. Conditions are checked top-to-bottom.
2. First match: if there's a `start_node` — opens a full dialogue from it; otherwise shows the short `text` (no dialogue window).
3. If nothing matched: fallback to `default_start_node` (full dialogue) or `default` (text).

---

## 14. Auto-spawning NPCs (summon)

If you don't want to spawn NPCs manually (via commands or `/summon`) but want them to appear by themselves — add a `summon` block to the JSON. Specify which mob, on what trigger, and where.

Example: "when a player joins the world, 3 seconds later a merchant appears in front of them". Or: "when a player finishes the blacksmith's dialogue, spawn the sage 5 blocks in front of them".

| Field | Type | Req. | Description |
|-------|------|------|-------------|
| `entity` | string | yes | Entity type (vanilla names are auto-mapped — see §17) |
| `custom_name` | string | yes | Must match `target.name` |
| `tags` | array | — | Must contain `target.tag` |
| `trigger` | object | **yes** | See §14.1. Without it loading throws NPE |
| `spawn_position` | string | — | `"behind_player"` (default), `"front_of_player"`, `"at_player"` |
| `despawn_after_dialogue` | bool | — | Mob disappears after the dialogue |
| `walk_away_before_despawn` | bool | — | Walks ~10 blocks away before disappearing |

### 14.1 Spawn trigger types

| `type` | Fields | When |
|--------|--------|------|
| `on_join` | `delay?` (ticks) | `delay` ticks after the player joins |
| `after_dialogue` | `dialogue_id`, `delay?` | After the given dialogue is completed |
| `player_near` | `x`, `y`, `z`, `radius?` (8.0) | Player within radius |
| `player_entered_area` | `x`, `y`, `z`, `radius?` (8.0) | First entry into the zone |
| `player_looking_for_seconds` | `x`, `y`, `z`, `radius?` (8.0), `seconds?` (2) | Player looks for N seconds |
| `on_player_death` | `delay?` | After player death |

### 14.2 Example

```json
"summon": {
  "entity": "minecraft:zombie",
  "custom_name": "Harold",
  "tags": ["harold"],
  "trigger": { "type": "on_join", "delay": 60 },
  "spawn_position": "front_of_player",
  "despawn_after_dialogue": false
}
```

**Important:** for non-`repeatable` dialogues spawning is gated by an in-memory `TRIGGERED_DIALOGUES` set (reset only by full `/dialogue reload`) and by `hasVisited(entry)`.

---

## 15. Dialogue triggers

A top-level array. Starts a dialogue with an **already existing** NPC on an event.

| `type` | Fields | When |
|--------|--------|------|
| `proximity` | `radius?` (4.0) | Player within radius (polled every 10 ticks, 200-tick cooldown) |
| `on_hurt` | `radius?` (4.0) | Player hit the NPC |
| `on_death` | `radius?` (4.0) | Player killed the NPC |
| `health_below` | `threshold?` (0.5) | NPC HP fell below fraction of max (0..1) |

```json
"triggers": [
  { "type": "proximity", "radius": 5.0 }
]
```

**Do not confuse with `summon.trigger`** — different set, different role.

---

## 16. Routines

NPC behavior schedule across the in-game day (0..24000 ticks).

| Field | Type | Description |
|-------|------|-------------|
| `type` | string | `"idle_at"` / `"wander"` / `"patrol"` |
| `start` | int | Period start (default 0) |
| `end` | int | Period end (default 24000). If `start > end` wraps past midnight |
| `x`, `y`, `z` | int | Anchor point (for `idle_at`, `wander`) |
| `radius` | int | Wander radius (default 8) |
| `waypoints` | array | For `patrol`: `[{x,y,z}, ...]` |

### Example: merchant works by day, sleeps at night

```json
"routines": [
  { "type": "idle_at", "x": 100, "y": 64, "z": 200, "start": 0,     "end": 12000 },
  { "type": "idle_at", "x": 105, "y": 65, "z": 210, "start": 13000, "end": 23000 }
]
```

### Example: patrol

```json
"routines": [
  {
    "type": "patrol",
    "waypoints": [
      { "x": 100, "y": 64, "z": 200 },
      { "x": 120, "y": 64, "z": 200 },
      { "x": 120, "y": 64, "z": 220 },
      { "x": 100, "y": 64, "z": 220 }
    ]
  }
]
```

---

## 17. NPC entities

The mod registers 33 types. To prevent NPCs from attacking the player, the mod adds "peaceful" versions of every vanilla aggressive mob. They look and behave like the original but won't attack you. Plus a universal `custom_npc` type for important characters — with custom models, skins, scales and emotes.

### 17.1 Peaceful counterparts of vanilla mobs (32)

| Vanilla | Mod equivalent |
|---------|----------------|
| `minecraft:zombie` | `interactentity:npc_zombie` |
| `minecraft:skeleton` | `interactentity:npc_skeleton` |
| `minecraft:creeper` | `interactentity:npc_creeper` |
| `minecraft:spider`, `cave_spider` | `interactentity:npc_spider`, `npc_cave_spider` |
| `minecraft:enderman`, `endermite` | `npc_enderman`, `npc_endermite` |
| `minecraft:witch`, `evoker` | `npc_witch`, `npc_evoker` |
| `minecraft:piglin`, `piglin_brute`, `zombified_piglin` | `npc_piglin`, `npc_piglin_brute`, `npc_zombified_piglin` |
| `minecraft:pillager`, `vindicator`, `ravager` | `npc_pillager`, `npc_vindicator`, `npc_ravager` |
| `minecraft:husk`, `drowned`, `stray`, `wither_skeleton` | `npc_husk`, `npc_drowned`, `npc_stray`, `npc_wither_skeleton` |
| `minecraft:blaze`, `ghast`, `magma_cube`, `slime` | `npc_blaze`, `npc_ghast`, `npc_magma_cube`, `npc_slime` |
| `minecraft:phantom`, `vex`, `shulker` | `npc_phantom`, `npc_vex`, `npc_shulker` |
| `minecraft:guardian`, `elder_guardian` | `npc_guardian`, `npc_elder_guardian` |
| `minecraft:silverfish`, `hoglin`, `zoglin` | `npc_silverfish`, `npc_hoglin`, `npc_zoglin` |
| `minecraft:warden` | `npc_warden` |

When using `/npc spawn <id>` the mod auto-converts vanilla names to `interactentity:npc_*`. In JSON write vanilla.

### 17.2 `interactentity:custom_npc`

A universal entity with a player-shaped model. **Only this one** supports:
- Custom model and texture (`visual.model`, `visual.texture`)
- Custom scale (`visual.scale`)
- Emotes (`play_emote`)
- Being a companion (`set_companion`)
- Dynamic skins from a folder (see §20)

Use it for important characters.

### 17.3 Equipment Management (Armor and Items)

You can manage the equipment (held items and armor) of any registered NPC entity in the world:
- **Equipping items/armor:**
  - Hold an item/armor piece in your main hand, crouch (hold Shift), and right-click the NPC.
  - Armor items (helmets, chestplates, leggings, boots) are placed in their respective armor slots.
  - Shields are equipped in their offhand slot.
  - Elytras are placed in their chestplate slot.
  - Pumpkins and heads/skulls are placed in their helmet slot.
  - All other items are equipped in their mainhand slot.
  - If the NPC was already wearing/holding an item in that slot, the old item is returned to your inventory (or dropped on the ground if your inventory is full).
  - Plays the corresponding equip sound.
- **Removing items/armor:**
  - Crouch (hold Shift) and right-click the NPC with an empty hand.
  - This retrieves equipped items from the NPC one by one in sequence: Main Hand → Offhand → Head → Chest → Legs → Feet, and returns them to your inventory.
  - Plays a pickup sound.

---

## 18. Emotes and animations

NPCs of type `interactentity:custom_npc` can play one-shot animations — emotes. Wave, bow, surprised, shrug. Triggered via `play_emote`.

Useful for liveliness: when an NPC says "hi!" play `wave`; when they're surprised by a plot twist play `surprised`; when they're annoyed at the player play `crossed_arms`.

Regular peaceful mobs (zombie-NPC, skeleton-NPC etc.) **don't support emotes** — they lack the animations.

### Emote list

`beckon`, `bow`, `celebrate`, `clap`, `confused`, `crossed_arms`, `dismiss`, `facepalm`, `handshake`, `happy`, `laugh`, `no` (alias for `shake_head`), `nod`, `please`, `point`, `scared`, `shake_head`, `shrug`, `six_seven` (alias `67`), `surprised`, `think`, `wave`, `yawn`

Removed: `angry`, `sad`, `salute`.

### Usage

```json
{ "type": "play_emote", "emote": "wave", "duration_ticks": 40 }
{ "type": "play_emote", "emote": "six_seven" }
{ "type": "play_emote", "emote": "67" }       // alias
{ "type": "play_emote", "emote": "none" }     // clear
{ "type": "play_emote", "emote": "" }         // clear
```

`duration_ticks` is optional — it sets how many ticks the emote stays active before returning to idle. If omitted, a per-emote default (matching the animation length) is used. One-shot animations (wave, bow) play through once regardless of the value.

### Base animations (not emotes)

These play automatically:
- `animation.custom_npc.idle` — when standing
- `animation.custom_npc.walk` — when moving

Not configurable from JSON. Can only be replaced by overriding `custom_npc.animation.json` via a resource pack.

---

## 19. custom_npc visuals

```json
"visual": {
  "model": "interactentity:geo/custom_npc_default.geo.json",
  "texture": "harold",
  "scale": 1.0
}
```

| Field | Type | Description |
|-------|------|-------------|
| `model` | string | Path to a `.geo.json` model |
| `texture` | string | Simple name (dynamic skin, §20) or full path (`namespace:textures/entity/...png`) |
| `scale` | float | 0.1..5.0 |

### Built-in models

| Value | Arm style |
|-------|-----------|
| `interactentity:geo/custom_npc_default.geo.json` | thick (Steve) |
| `interactentity:geo/custom_npc_slim.geo.json` | slim (Alex, needed for slim skins) |

### Changing at runtime

```
/npc set_model @e[type=interactentity:custom_npc,limit=1] interactentity:geo/custom_npc_slim.geo.json
/npc set_texture @e[type=interactentity:custom_npc,limit=1] harold
/npc set_scale @e[type=interactentity:custom_npc,limit=1] 1.2
```

---

## 20. Skins — dynamic loading

The big feature for map makers. Normally, giving an NPC a custom skin meant building a resource pack, hosting it somewhere, wiring it into `server.properties` — a pain. Here it's simpler.

You just drop a PNG into one of two folders. The server reads it on startup, sends the PNG bytes to clients on login, the client displays it. Done. No resource packs, no hosting, no recompile.

The mod ships with only one fallback texture (`custom_npc_default.png`) — shown when nothing matches.

### 20.1 Where to put files

| Folder | When it's useful |
|--------|-----------------|
| `config/interactentity/skins/` | Global, shared across all worlds for one player |
| `<world>/interactentity/skins/` | Per-world, travels with the world when you zip-share it |

When names collide, **per-world wins** (logic: the map author intentionally put their skin there).

### 20.2 Rules

| Rule | Details |
|------|---------|
| Filename | `[a-z0-9_]+\.png` — lowercase letters, digits, underscores |
| Size | **64×64** or 64×32 (legacy) |
| Invalid file | Skipped, warn in log |

### 20.3 How it works

1. On startup the server scans both folders and reads PNG bytes into memory.
2. When a player logs in the server sends the batch via `SkinSyncPacket`.
3. The client creates a `DynamicTexture` and registers it under `interactentity:textures/entity/skins/<name>.png`.
4. `/dialogue reload` (no arg) rereads skins and broadcasts an update.

### 20.4 How to reference in JSON

| Spelling | What happens |
|----------|--------------|
| `"texture": "harold"` | Resolves to `interactentity:textures/entity/skins/harold.png` — dynamic skin or resource pack fallback |
| `"texture": "interactentity:textures/entity/foo.png"` | Used as-is |
| `"avatar": "interactentity:textures/entity/skins/harold.png"` | Avatar needs the **full path** — a bare skin name is not expanded |

**Recommendation:** for your own NPCs use simple names. For textures coming from resource packs use full namespaced paths.

### 20.5 Distributing a map with NPCs

1. Author drops PNGs into `<world>/interactentity/skins/harold.png`
2. JSON references `"texture": "harold"`
3. Author zips the world folder
4. Recipient unzips → server picks up skins on startup → everyone on the server sees them

### 20.6 Troubleshooting — skin not showing?

- Filename doesn't match `[a-z0-9_]+\.png`
- Size isn't 64×64 or 64×32
- JSON has a full path instead of a simple name
- You didn't run `/dialogue reload` after adding the file
- Check the server log for `[skins]` messages

---

## 21. Journal, quest HUD and the `!` icon

The mod has three UI elements the player can see:
- **Journal** — the big window with conversation history and quests (`J`)
- **Quest HUD** — a small on-screen panel with active quests (`K`)
- **`!` icon** — a yellow exclamation mark above NPCs that have something new for the player

### 21.1 Journal (`J`)

Opened with `J`. Three sections inside:

| Section | What |
|---------|------|
| **Characters** | All NPCs the player has talked to. Head icon, name, active-quest marker |
| **Dialogue history** | Lines the selected character ever said to you |
| **Quests** | Quests of the selected character. Each can be "tracked" (pinned to HUD) |

Character details: 3D model, faction, relationship status (from reputation), completed quests, lore.

"Track" button pins the quest to the HUD — limit 3 tracked quests at once.

### 21.2 Quest HUD (`K`)

Shows tracked quests on screen. Toggle with `K`.

### 21.3 "Current dialogue" overlay (`H` while in dialogue)

While a dialogue is open, `H` shows the scrollable history of replies in the current conversation. Useful if you missed something five lines back — scroll up without restarting the dialogue.

### 21.4 `!` icon above NPCs

The mod automatically draws a yellow `!` above an NPC's head in two cases:

1. **The player has never talked to this NPC** — they haven't opened its dialogue yet.
2. **`notify_npc` was triggered** — another NPC explicitly said "this one has something new".

Visible within 16 blocks, always faces the camera. Disappears the moment the player opens the dialogue (unless `notify_npc` was the cause — then it disappears after the next conversation).

Very useful for guiding the player: finished one quest → next NPC auto-lights up thanks to `notify_npc` in the closing option's actions.

---

## 22. NPC avatar in dialogue window

This is the head of the NPC shown to the left of the reply. Set in the dialogue's root via the `avatar` field:

```json
"avatar": "interactentity:textures/entity/skins/harold.png"
"avatar": "interactentity:textures/entity/mayor.png"
```

Unlike `visual.texture`, the `avatar` field needs a **full texture path** — a bare skin name like `"harold"` is not expanded into the skins folder and will render as a missing texture.

The mod takes the 8×8 region from coordinate (8,8) of the texture — that's the face in the standard 64×64 player-skin layout. So you can drop in regular player/NPC skins directly and the mod will crop out the head.

### Avatar via NBT (no JSON edit)

You can set the avatar of a specific mob via the `DialogueAvatar` NBT tag — it overrides the `avatar` from the JSON. Handy when you want two instances of the same NPC with different faces without two separate JSON files.

```
/data merge entity @e[name=NpcName,limit=1] {DialogueAvatar:"interactentity:textures/entity/skins/harold.png"}
```

Here you need the full texture path, not just a skin name.

> The visual style of the dialogue window (backgrounds, colors, button frames) is intentionally fixed and not configurable via JSON. This keeps every NPC looking consistent.

---

## 23. Reputation and factions

Reputation is just a number attached to a faction ID (e.g. `village`). The player can raise it (by helping NPCs) or lower it (by being rude / breaking promises). NPCs from that faction can check the current value in their conditions and react accordingly.

Four steps to set it up:

1. Declare the faction in the dialogue's root:
```json
"faction": "Village",
"reputation_id": "village"
```

2. Award reputation:
```json
{ "type": "add_reputation", "id": "village", "value": 10, "label": "Helped the elder" }
```

3. Check it:
```json
{ "type": "reputation", "id": "village", "op": "gte", "value": 50 }
```

4. Show it in text:
```json
"text": "You have &e{reputation:village}&r points with the village."
```

5. Gifts (see §24) automatically award reputation.

### Display scale

| Range | Status |
|-------|--------|
| ≤ -50 | hostile |
| -49..-20 | unfriendly |
| -19..19 | neutral |
| 20..49 | friendly |
| ≥ 50 | allied |

---

## 24. Gifts

Gifts are a special action that lets the player give an NPC something nice and get reputation in return. The main trick is the built-in cooldown: you can't gift the same NPC more often than once an hour (real time). This prevents spam — the player can't max out reputation by handing over 100 bread loaves in a minute.

If the cooldown hasn't elapsed, the action does nothing (just shows a message). Check separately with `can_give_gift`.

```json
{
  "type": "give_gift",
  "character_id": "harold",
  "item": "minecraft:bread",
  "amount": 1,
  "reputation": 5,
  "label": "Gift",
  "success_message": "&aHarold smiles.",
  "cooldown_message": "&7He's not hungry today."
}
```

Logic:
1. Check the player has 1 bread
2. Check the gift cooldown for `harold` has elapsed (1 hour real time)
3. If OK: item is taken, +5 reputation, `success_message` is shown
4. If cooldown not elapsed: `cooldown_message`, nothing else happens

Cooldown check separately:
```json
{ "type": "can_give_gift", "character_id": "harold" }
```

---

## 25. Relationships between NPCs

NPCs can "know" each other via relationship flags.

### Setting

```json
{
  "type": "set_relationship",
  "npc_a": "mayor",
  "npc_b": "thief",
  "relationship": "enemy"
}
```

`relationship` is an arbitrary string: `"ally"`, `"enemy"`, `"family"`, `"rival"`, whatever you want.

### Checking

```json
{
  "type": "npc_relationship",
  "npc_a": "mayor",
  "npc_b": "thief",
  "relationship": "enemy"
}
```

### Example: thief won't approach the mayor if they're enemies

```json
"options": [
  {
    "text": "Go talk to the mayor",
    "condition": {
      "type": "npc_relationship",
      "npc_a": "mayor",
      "npc_b": "thief",
      "relationship": "enemy"
    },
    "lock_reason": "He'd kill me",
    "next": "..."
  }
]
```

---

## 26. Companions and NPC home

### 26.1 set_companion

```json
{ "type": "set_companion", "enable": true }
```

Only for `interactentity:custom_npc`. The NPC starts following the player.

```json
{ "type": "set_companion", "enable": false }
```

Release them.

### 26.2 set_home

```json
{ "type": "set_home", "x": 100, "y": 64, "z": 200, "radius": 16 }
```

The NPC will return to within `radius` blocks of the point. Without coordinates uses the NPC's current position.

---

## 27. Scope

Scope is the "visibility" of a dialogue's progress. In plain words: is progress shared by all players on the server or does each player have their own?

Two options:

- `"global"` (default) — shared. If one player completes a quest, everyone else sees it as completed too. Good for co-op story maps and singleplayer.
- `"per_player"` (legacy `"player"`) — each player has their own. Good for multiplayer servers where each player goes through the story independently.

Scope affects: variables (`set_var` / `if_var`), reputation, visited nodes, quests, NPC-to-NPC relationships.

### What `global` means in practice

This matters because many people get surprised:

- `visited_node` is true if **at least one** player has visited it. If your friend visited the node, it counts for you too.
- `killed_mob` counts kills **summed across all players**. A 10-zombie quest could be split: ten people, one zombie each, done.
- Variables (`set_var`) are the same for everyone. One player sets `trust` to 5 — everyone sees 5.
- Quests show up in **every** player's journal but the status is shared.
- The `notify_npc` icon lights up **for everyone**.
- Storage is anchored to the Overworld — progress isn't lost crossing to Nether/End.

### What `per_player` means

Same thing but keyed by the player's UUID. Each player has their own variables, quests, visited nodes, reputation. Different journals show different progress.

For a proper multiplayer server where each player runs the story alone, this is almost always what you want.

---

## 28. Placeholders

In any text field (`text`, `display_name`, `random_text`, option texts) you can drop in placeholders — the mod substitutes the live value. `{player}` becomes the player's name, `{var:trust}` becomes your variable's value.

Super handy for lively lines: "Hi, Steve!" instead of a generic "Hi, traveler".

| Placeholder | Substitution |
|-------------|--------------|
| `{player}` | Player's name |
| `{player_uuid}` | Player's UUID |
| `{npc_uuid}` | NPC's UUID |
| `{var:NAME}` | Variable value |
| `{reputation:ID}` | Current faction reputation |

```json
"text": "Hi, &e{player}&r! You have {reputation:village} points and {var:trust} trust."
```

---

## 28a. Multilingual Dialogues in JSON

By default, writing a dialogue with plain text strings forces a single language on all players. To make your dialogues accessible worldwide, **InteractEntity** supports fully dynamic multilingual dialogue content directly inside the JSON files. 

For any user-facing text field (including `display_name` in the root, `text` in nodes, elements of the `random_text` array, option `text`, and option `lock_reason`), you can provide a **JSON object mapping locale codes** to their respective translations instead of a simple string.

### How it works
1. When a player opens a dialogue, the mod detects their active client language (e.g., `en_us`, `ru_ru`, `fr_fr`, `de_de`, `es_es`, `zh_cn`, etc.).
2. The mod looks up the matching locale key in your translation object.
3. **Fallback mechanism**: If the player's language is not defined in the object, the mod falls back to `en_us`. If `en_us` is also missing, the mod uses the first available language key defined in the object.
4. Standard **placeholders** (like `{player}`) and **formatting codes** (like `&6` or HEX colors) are fully supported and resolved on the chosen translation string dynamically, ensuring a polished, zero-mixture localized experience.

### JSON Example

Instead of a plain string:
```json
"text": "Hello, &e{player}&r! Do you want a diamond?"
```

You can write:
```json
"text": {
  "en_us": "Hello, &e{player}&r! Do you want a diamond?",
  "ru_ru": "Привет, &e{player}&r! Хочешь алмаз?",
  "fr_fr": "Bonjour, &e{player}&r ! Tu veux un diamant ?",
  "de_de": "Hallo, &e{player}&r! Möchtest du einen Diamanten?",
  "es_es": "¡Hola, &e{player}&r! ¿Quieres un diamante?",
  "zh_cn": "你好，&e{player}&r！你想要一颗钻石吗？"
}
```

### Full Node Example

Here is how a complete node looks with multilingual support:

```json
"nodes": {
  "start": {
    "text": {
      "en_us": "Welcome! I have a quest for you.",
      "ru_ru": "Добро пожаловать! У меня есть квест для тебя.",
      "fr_fr": "Bienvenue ! J'ai une quête pour toi.",
      "de_de": "Willkommen! Ich habe eine Quest für dich."
    },
    "options": [
      {
        "text": {
          "en_us": "Sure, let's do it!",
          "ru_ru": "Конечно, давай!",
          "fr_fr": "Bien sûr, c'est parti !",
          "de_de": "Klar, legen wir los!"
        },
        "next": "accept_quest"
      },
      {
        "text": {
          "en_us": "Not now (Requires 10 reputation)",
          "ru_ru": "Не сейчас (Требуется 10 репутации)"
        },
        "condition": {
          "type": "reputation",
          "id": "village",
          "op": "gte",
          "value": 10
        },
        "lock_reason": {
          "en_us": "You must be trusted in the village.",
          "ru_ru": "Вам должны доверять в деревне."
        },
        "next": "refuse_quest"
      }
    ]
  }
}
```

---

## 29. Text formatting

Inside any string you can color and style text. Standard Minecraft codes work (use `&` instead of `§`), plus a HEX color extension. Codes can be combined — `&l&c` is bold red.

| Code | Effect |
|------|--------|
| `&0`..`&9`, `&a`..`&f` | Minecraft colors |
| `&l` | Bold |
| `&o` | Italic |
| `&n` | Underline |
| `&m` | Strikethrough |
| `&k` | Obfuscated (flickering) |
| `&r` | Reset |
| `&#RRGGBB` | Custom HEX color |

```json
"text": "&#FFD700Gold&r and &lbold&r and &c&lbold red&r."
```

---

## 30. Commands

### `/dialogue`

| Command | What |
|---------|------|
| `/dialogue reload` | Reloads all dialogues + skins, resets progress and in-memory flags |
| `/dialogue reload <id>` | One dialogue + reset its progress (NOT in-memory spawn flags) |
| `/dialogue test <id> [node]` | Opens dialogue with the nearest mob without target checks |
| `/dialogue goto <node>` | Jump to a node inside the active dialogue |
| `/dialogue var set <name> <value> [target]` | Set a variable — global, or per-player if `target` is given |
| `/dialogue var get <name> [target]` | Print a variable's value (global, or the target player's) |

### `/npc`

| Command | What |
|---------|------|
| `/npc spawn <id>` | Spawn an NPC using `summon.entity` + `target.name/tag`. Accepts slashed IDs (`showcase/bob`) |
| `/npc tag <id>` | Assign name+tag to the nearest mob |
| `/npc remove` | Remove the nearest NPC |
| `/npc list [radius]` | List NPCs in radius (default 32) |
| `/npc set_model <targets> <path>` | Change `custom_npc` model |
| `/npc set_texture <targets> <name_or_path>` | Change texture (simple name or namespaced) |
| `/npc set_scale <targets> <0.1..5.0>` | Change `custom_npc` scale |
| `/npc set_name <targets> <name>` | Set visible CustomName |

---

## 31. Keybinds

The player has lots of hotkeys — you don't have to use the mouse inside a dialogue if you don't want to.

**Outside a dialogue:**

| Key | What |
|-----|------|
| `J` | Open journal |
| `K` | Toggle quest HUD |
| `RMB` on NPC | Open dialogue |

**Inside a dialogue:**

| Key | What |
|-----|------|
| `RMB` / `Space` / `Enter` | Advance (linear & after answering). If text is still typing, instantly finish typing |
| `1` ... `5` | Quick-pick answer by number |
| `↑` / `↓` | Highlight previous/next answer |
| `Enter` / `Space` | Confirm highlighted answer |
| `H` | Toggle "current dialogue" overlay with reply history |
| `ESC` | Close dialogue. **Note:** ESC does NOT count as completion — `on_revisit` won't fire, you need a real end node |

Journal and quest-HUD keys are rebindable via Minecraft's Options → Controls.

---

## 32. KubeJS integration

If KubeJS is installed alongside the mod, you can do things plain JSON can't. For example: dynamically generate quests based on world state, listen to dialogue events and inject custom logic, hand out rewards via your own loot tables.

The mod provides two integration points: a static Java API (methods callable from JS) and Forge events (subscribe and react).

### 32.1 Static API — `InteractEntityAPI`

```js
// kubejs/server_scripts/my_quests.js

const Api = Java.loadClass('net.ashpapi.interactentity.api.InteractEntityAPI')

PlayerEvents.loggedIn(event => {
  const player = event.player
  Api.addReputation(player, "village", 5, "global")
  Api.setVar(player, "intro_done", "1", "per_player")
})
```

#### Methods

```java
boolean startQuest(ServerPlayer player, String questJsonString)
boolean startQuest(ServerPlayer player, JsonObject questJson)
boolean completeQuest(ServerPlayer player, String questId)
boolean failQuest(ServerPlayer player, String questId)

void addReputation(ServerPlayer player, String factionId, int delta, String scope)
int   getReputation(ServerPlayer player, String factionId, String scope)

void   setVar(ServerPlayer player, String name, String value, String scope)
String getVar(ServerPlayer player, String name, String scope)

boolean openDialogue(ServerPlayer player, String dialogueId, LivingEntity entity)
```

`scope` is `"global"` or `"player"`.

### 32.2 Events — listen to dialogues from KubeJS

```js
ForgeEvents.onEvent('net.ashpapi.interactentity.api.DialogueChoiceEvent', event => {
  const player = event.player
  const tag = event.tag
  const source = event.source  // "option" or "action" (from fire_event)

  if (source === 'action' && tag === 'started_quest_chain') {
    player.tell('§eStory chain started!')
  }
})

ForgeEvents.onEvent('net.ashpapi.interactentity.api.QuestStartEvent', event => {
  console.log(`Player ${event.player.name.string} started quest ${event.questId} in ${event.scope}`)
})

ForgeEvents.onEvent('net.ashpapi.interactentity.api.QuestCompleteEvent', event => {
  event.player.give('minecraft:diamond')
})
```

### 32.3 Using `fire_event` to bridge with KubeJS

In JSON:
```json
"actions": [
  { "type": "fire_event", "tag": "village_saved" }
]
```

In KubeJS:
```js
ForgeEvents.onEvent('net.ashpapi.interactentity.api.DialogueChoiceEvent', event => {
  if (event.source === 'action' && event.tag === 'village_saved') {
    // custom KubeJS logic
  }
})
```

The cleanest way to integrate complex user logic without modifying Java code.

---

## 33. Forge API

### Events

| Event | When | Fields |
|-------|------|--------|
| `DialogueStartEvent` | Dialogue opened | `player`, `npc`, `dialogueId`, `startNodeId` |
| `DialogueChoiceEvent` | Option selected OR `fire_event` action fired | `player`, `npc`, `dialogueId`, `nodeId`, `source` (`"option"`/`"action"`), `tag` |
| `DialogueEndEvent` | Dialogue closed or completed | `player`, `npc`, `dialogueId`, `lastNodeId`, `completed` |
| `QuestStartEvent` | Quest started | `player`, `questId`, `scope` |
| `QuestCompleteEvent` | Quest completed | `player`, `questId`, `scope` |
| `QuestFailEvent` | Quest failed | `player`, `questId`, `scope` |

### Subscribing (Forge)

```java
@SubscribeEvent
public static void onQuestStart(QuestStartEvent event) {
    LOGGER.info("Player {} started quest {}", event.getPlayer().getName(), event.getQuestId());
}
```

---

## 34. Gotchas

1. **`summon` without `trigger`** → NPE on load. The trigger inside `summon` is mandatory.
2. **`update_quest` breaks the kills counter** — it replaces the entire `objectives[]`.
3. **`triggers[]` ≠ `summon.trigger`** — different sets and roles.
4. **Non-`repeatable` dialogues won't re-issue an `after_dialogue` spawn** of a child NPC if the child's entry node was already visited. Reset: `/dialogue reload` (no arg).
5. **ESC doesn't mark a dialogue completed** — `on_revisit` won't fire. You need a real end node.
6. **`has_item` counts any items** — modded items and items with NBT (enchanted, anvil-renamed, with durability) all count.
7. **Options support only one `condition`** — use an intermediate node for AND/OR.
8. **`fire_event` is caught via `DialogueChoiceEvent`** with `source == "action"`.
9. **`schedule_event` doesn't survive a server restart** if the player is offline.
10. **Skin not showing?** Check filename (only `[a-z0-9_]+`), size (64×64 or 64×32), folder, and that you ran `/dialogue reload` after changes.
11. **The `"index"` field in `complete_objective` is not supported** — use `objective` / `objective_number` / `objective_text`.
12. **`set_companion` works only with `interactentity:custom_npc`**.
13. **`play_emote` works only with `interactentity:custom_npc`**.
14. **`/dialogue reload <id>` does not reset in-memory `TRIGGERED_DIALOGUES`** — for a full spawn reset use `/dialogue reload` without arguments.
15. **JSON parser is strict about trailing commas** — an extra comma after the last field = load error, the dialogue won't appear. Check the log.
16. **Entity type and tag mismatch in `target` / `/summon`** — If you specify `entity_type` in the `target` block (e.g., `"interactentity:custom_npc"`), it must exactly match the type of entity spawned in the world. If you spawn a `"minecraft:villager"` but require `"interactentity:custom_npc"` in target (or vice versa), the dialogue will **not** start. Also, if you use the `/summon` command manually, remember to add the tag (e.g., `Tags:["tag_name"]`) and CustomName, otherwise the dialogue won't match.

---

<a id="35-example"></a>

## 35. Big example — story map

To put it all together, here's a fully working example with two linked NPCs. You can copy this into your world and it'll work.

Plot: the player meets Elsa (the herbalist) → she asks to find her lost pocket watch, starting the 'The Lost Amulet' quest → the player meets Harold (the hunter) → Harold asks for bread to treat his fox Rusty before handing over the watch, starting the 'Bread for Rusty' quest → the player brings bread to Harold, gets the watch, and returns it to Elsa for a reward.

### 35.1 `dialogues/story/elsa.json` (per-player)

```json
{
  "target": {
    "name": "Elsa",
    "tag": "story_elsa",
    "entity_type": "interactentity:custom_npc"
  },
  "display_name": "&d[&5Elsa&d]",
  "character_info": "Village herbalist. Collects rare roots and brews ointments.",
  "avatar": "interactentity:textures/entity/skins/elsa.png",
  "scope": "per_player",
  "disable_knockback": true,
  "disable_attacks": true,
  "entry": "start",
  "visual": {
    "model": "interactentity:geo/custom_npc_slim.geo.json",
    "texture": "elsa",
    "scale": 1.0
  },
  "summon": {
    "entity": "interactentity:custom_npc",
    "custom_name": "Elsa",
    "tags": ["story_elsa"],
    "spawn_position": "behind_player",
    "trigger": { "type": "on_join", "delay": 60 }
  },
  "on_revisit": {
    "default_start_node": "hub_idle",
    "conditions": [
      {
        "condition": { "type": "quest_status", "quest_id": "lost_amulet", "status": "completed" },
        "text": "&7Elsa smiles warmly at you. &dThank you for helping out, {player}."
      },
      {
        "condition": { "type": "has_item", "item": "minecraft:clock", "count": 1 },
        "start_node": "return_with_amulet"
      },
      {
        "condition": { "type": "quest_status", "quest_id": "lost_amulet", "status": "active" },
        "start_node": "hub_active"
      }
    ]
  },
  "nodes": {
    "start": {
      "text": "&fOh, traveler! Thank the gods, at least someone stopped by. I am in trouble, {player}...",
      "next": "explain",
      "actions": [
        { "type": "play_emote", "emote": "wave", "duration_ticks": 30 },
        { "type": "play_sound", "sound": "minecraft:entity.villager.ambient", "volume": 1.0, "pitch": 1.1 }
      ]
    },
    "explain": {
      "text": "&fMy &dpocket watch&f is gone. I left it on a tree stump this morning — got distracted by the garden bed, turned around, and it was gone. And there were fox tracks nearby...",
      "next": "ask_help",
      "actions": [
        { "type": "play_emote", "emote": "facepalm", "duration_ticks": 45 }
      ]
    },
    "ask_help": {
      "text": "&fOver in that part of the forest lives a hunter named &6Harold&f. Foxes often drag things to him — maybe he saw my watch. Will you help me?",
      "actions": [
        { "type": "play_emote", "emote": "please", "duration_ticks": 60 }
      ],
      "options": [
        { "text": "&aOf course, I'll find Harold.", "next": "accept",
          "actions": [
            { "type": "start_quest", "quest": {
              "id": "lost_amulet",
              "title": "The Lost Amulet",
              "description": "Elsa lost her pocket watch. A fox might have dragged it to Harold the hunter.",
              "objectives": [
                "Find Harold and ask him",
                "Return the watch to Elsa"
              ]
            }},
            { "type": "play_emote", "emote": "happy", "duration_ticks": 30 }
          ]
        },
        { "text": "&7Not up for this right now.", "next": "refuse",
          "actions": [
            { "type": "play_emote", "emote": "shrug", "duration_ticks": 25 }
          ]
        }
      ]
    },
    "accept": {
      "text": "&dThank you! Harold lives in the forest — you'll find him by the smoke above his cabin. He can be a bit gruff, but he's not mean.",
      "actions": [
        { "type": "play_emote", "emote": "bow", "duration_ticks": 40 }
      ]
    },
    "refuse": {
      "text": "&7I understand... If you change your mind, I'll be here."
    },
    "hub_idle": {
      "text": "&fYou again, {player}. Looking for herbs or just to chat?",
      "actions": [
        { "type": "play_emote", "emote": "nod", "duration_ticks": 25 }
      ],
      "options": [
        { "text": "&7Tell me about yourself.", "next": "lore" },
        { "text": "&7Leave.", "next": null }
      ]
    },
    "hub_active": {
      "text": "&fSo, did you find Harold? Does he have the amulet?",
      "options": [
        { "text": "&7Still looking.", "next": null },
        { "text": "&7Tell me about yourself.", "next": "lore" }
      ]
    },
    "return_with_amulet": {
      "text": "&dOh! My watch! Where did you find it, {player}?!",
      "next": "thanks",
      "actions": [
        { "type": "play_emote", "emote": "celebrate", "duration_ticks": 40 }
      ]
    },
    "thanks": {
      "text": "&fHarold, huh... Give him a jar of honey from me next time. And for you — here, take this.",
      "next": "reward",
      "actions": [
        { "type": "remove_item", "item": "minecraft:clock", "count": 1 },
        { "type": "complete_objective", "quest_id": "lost_amulet", "objective_number": 2 },
        { "type": "complete_quest", "quest_id": "lost_amulet" },
        { "type": "give_item", "item": "minecraft:emerald", "count": 4 },
        { "type": "give_item", "item": "minecraft:golden_apple", "count": 1 },
        { "type": "give_effect", "effect": "minecraft:regeneration", "duration": 200, "amplifier": 1 },
        { "type": "play_sound", "sound": "minecraft:entity.villager.celebrate", "volume": 1.0, "pitch": 1.2 }
      ]
    },
    "reward": {
      "text": "&e4 emeralds and an apple for the road. Stop by again, traveler.",
      "actions": [
        { "type": "play_emote", "emote": "bow", "duration_ticks": 45 }
      ]
    },
    "lore": {
      "text": "&7*Elsa weighs two bundles of herbs in her hands, pondering which is better* &fI was born right here in the village. My mother taught me about herbs, and my grandmother taught me charms. And the watch belonged to her, my grandmother. That's why I'm so worried.",
      "next": null,
      "actions": [
        { "type": "play_emote", "emote": "six_seven", "duration_ticks": 50 }
      ]
    }
  }
}
```

### 35.2 `dialogues/story/harold.json` (per-player)

```json
{
  "target": {
    "name": "Harold",
    "tag": "story_harold",
    "entity_type": "interactentity:custom_npc"
  },
  "display_name": "&6[&eHarold&6]",
  "character_info": "Hermit hunter. Lives in a forest cabin, has tamed a couple of foxes.",
  "avatar": "interactentity:textures/entity/skins/harold.png",
  "scope": "per_player",
  "entry": "start",
  "visual": {
    "model": "interactentity:geo/custom_npc_default.geo.json",
    "texture": "harold",
    "scale": 1.05
  },
  "summon": {
    "entity": "interactentity:custom_npc",
    "custom_name": "Harold",
    "tags": ["story_harold"],
    "trigger": { "type": "after_dialogue", "dialogue_id": "story/elsa", "delay": 200 }
  },
  "on_revisit": {
    "default_start_node": "hub_idle",
    "conditions": [
      {
        "condition": { "type": "visited_node", "dialogue": "story/harold", "node": "give_amulet" },
        "start_node": "hub_after_amulet"
      },
      {
        "condition": { "type": "quest_status", "quest_id": "harold_bread", "status": "active" },
        "start_node": "offer_bread"
      },
      {
        "condition": { "type": "quest_status", "quest_id": "lost_amulet", "status": "active" },
        "start_node": "ask_quest"
      }
    ]
  },
  "nodes": {
    "start": {
      "text": "&fHm. A stranger. &7*looks you up and down* &fWhat do you want in my forest, {player}?",
      "actions": [
        { "type": "play_emote", "emote": "crossed_arms", "duration_ticks": 60 }
      ],
      "options": [
        { "text": "&aElsa sent me. To ask about the watch.",
          "next": "knows_elsa",
          "condition": { "type": "quest_status", "quest_id": "lost_amulet", "status": "active" }
        },
        { "text": "&7Just passing by.", "next": "neutral" },
        { "text": "&7Sorry to bother you.", "next": null }
      ]
    },
    "neutral": {
      "text": "&fWell, passing by it is. The forest is big, don't get lost.",
      "next": null,
      "actions": [
        { "type": "play_emote", "emote": "shrug", "duration_ticks": 25 }
      ]
    },
    "ask_quest": {
      "text": "&fReturned? I guess the herbalist wouldn't let you go.",
      "actions": [
        { "type": "play_emote", "emote": "nod", "duration_ticks": 25 }
      ],
      "options": [
        { "text": "&aShe was asking about the watch.", "next": "knows_elsa" },
        { "text": "&7Just dropped in.", "next": "neutral" }
      ]
    },
    "knows_elsa": {
      "text": "&fElsa, huh... &7*scratches his beard* &fAn old pocket watch on a chain? With a crack on the glass?",
      "next": "confirm_amulet",
      "actions": [
        { "type": "complete_objective", "quest_id": "lost_amulet", "objective_number": 1 },
        { "type": "play_emote", "emote": "think", "duration_ticks": 40 }
      ]
    },
    "confirm_amulet": {
      "text": "&fMy little fox, Rusty, brought it this morning. I was wondering whose it was. Thought about taking it to the village, but had no time.",
      "next": "offer_amulet",
      "actions": [
        { "type": "play_emote", "emote": "think", "duration_ticks": 35 }
      ]
    },
    "offer_amulet": {
      "text": "&fSince you're from Elsa, go ahead and take it. But... &7*narrows his eyes* &fI won't let you go empty-handed. Bring me a piece of &6bread&f — to treat the fox.",
      "actions": [
        { "type": "play_emote", "emote": "crossed_arms", "duration_ticks": 40 }
      ],
      "options": [
        { "text": "&7Alright, I'll bring some.", "next": "come_back" },
        { "text": "&cAnd what if I take it by force?", "next": "threat" }
      ]
    },
    "come_back": {
      "text": "&fGo on. I'm not going anywhere. &7*nods toward the forest*",
      "next": null,
      "actions": [
        { "type": "play_emote", "emote": "nod", "duration_ticks": 20 },
        { "type": "start_quest", "quest": {
            "id": "harold_bread",
            "title": "Bread for Rusty",
            "description": "Harold asks to bring bread for his fox Rusty — only then will he hand over the found watch.",
            "objectives": ["Bring 1 bread to Harold"]
          }
        }
      ]
    },
    "threat": {
      "text": "&7*Harold puts his hand on his axe* &fGive it a try. Just don't complain later.",
      "next": null,
      "actions": [
        { "type": "play_emote", "emote": "crossed_arms", "duration_ticks": 50 }
      ]
    },
    "offer_bread": {
      "text": "&fAh, you're back. &7*looks with a squint* &fDid you bring the bread?",
      "actions": [
        { "type": "play_emote", "emote": "crossed_arms", "duration_ticks": 30 }
      ],
      "options": [
        { "text": "&aHere it is, as promised.",
          "next": "give_amulet",
          "condition": { "type": "has_item", "item": "minecraft:bread", "count": 1 },
          "lock_reason": "&8(requires 1× bread)"
        },
        { "text": "&7Haven't found it yet.", "next": "wait_more" },
        { "text": "&cCan we do without the bread?", "next": "threat" }
      ]
    },
    "wait_more": {
      "text": "&fNo rush. Rusty will wait, and so will I.",
      "next": null,
      "actions": [
        { "type": "play_emote", "emote": "shrug", "duration_ticks": 25 }
      ]
    },
    "give_amulet": {
      "text": "&fHere, take the watch. Rusty, come here, have a treat... &7*tosses the bread to the fox* &fSend Elsa my regards.",
      "next": "farewell",
      "actions": [
        { "type": "remove_item", "item": "minecraft:bread", "count": 1 },
        { "type": "complete_quest", "quest_id": "harold_bread" },
        { "type": "give_item", "item": "minecraft:clock", "count": 1 },
        { "type": "play_emote", "emote": "handshake", "duration_ticks": 35 },
        { "type": "play_sound", "sound": "minecraft:entity.fox.ambient", "volume": 1.0, "pitch": 1.0 }
      ]
    },
    "farewell": {
      "text": "&fAnd... tell her to send some honey next time. I respect her herbal tinctures.",
      "next": null,
      "actions": [
        { "type": "play_emote", "emote": "shrug", "duration_ticks": 30 }
      ]
    },
    "hub_idle": {
      "text": "&fYou again. What do you want?",
      "actions": [
        { "type": "play_emote", "emote": "crossed_arms", "duration_ticks": 30 }
      ],
      "options": [
        { "text": "&7Tell me about the forest.", "next": "lore" },
        { "text": "&7Bye.", "next": null }
      ]
    },
    "hub_after_amulet": {
      "text": "&fDelivered the watch to the herbalist? You did a good deed.",
      "actions": [
        { "type": "play_emote", "emote": "nod", "duration_ticks": 25 }
      ],
      "options": [
        { "text": "&7Tell me about the forest.", "next": "lore" },
        { "text": "&7Bye.", "next": null }
      ]
    },
    "lore": {
      "text": "&fI've lived here for twenty winters. My father before me. His father before him. The forest feeds us, and we don't touch it without need. That's all there is to say.",
      "next": null,
      "actions": [
        { "type": "play_emote", "emote": "shrug", "duration_ticks": 40 }
      ]
    }
  }
}
```

### 35.3 What to put in files

```
config/interactentity/dialogues/story/elsa.json
config/interactentity/dialogues/story/harold.json
config/interactentity/skins/elsa.png             ← 64x64
config/interactentity/skins/harold.png           ← 64x64
```

After starting: `/dialogue reload`, then wait for Elsa to spawn (or spawn her manually). Talk to Elsa → start quest → Elsa spawns Harold in the forest → find Harold → bring him bread to get the watch → return the watch to Elsa for the reward.

### 35.4 KubeJS hook on the event

```js
// kubejs/server_scripts/lost_amulet.js

ForgeEvents.onEvent('net.ashpapi.interactentity.api.QuestStartEvent', event => {
  if (event.questId === 'lost_amulet') {
    event.player.tell('§5A mysterious quest has begun...')
  }
})

ForgeEvents.onEvent('net.ashpapi.interactentity.api.QuestCompleteEvent', event => {
  if (event.questId === 'lost_amulet') {
    event.player.runCommandSilent('xp add @s 50 levels')
  }
})
```

---

## Four ways to add an NPC to the world

When your dialogue JSON is ready, you need to place a mob with the matching name and tag. There are four ways — pick whatever fits.

### Way 1 — `/npc spawn` (simplest)

```
/npc spawn my_dialogue
```

The mod creates a mob of the right type with the right name and tag at your feet. Type from `summon.entity`, name and tag from `target`. Downside: requires `summon` in JSON (for entity type). Upside: one click and done.

### Way 2 — `/npc tag` (convert an existing mob)

If you already have a mob of the right type and want to make it an NPC, walk up and:

```
/npc tag my_dialogue
```

The nearest mob is given the name and tag. Handy when mobs are already placed.

### Way 3 — manual vanilla

If you don't want mod commands at all:

```
/summon zombie ~ ~ ~ {CustomName:'"My Name"',CustomNameVisible:1b,Tags:["my_tag"]}
```

Or if the mob already exists:
```
/tag @e[type=minecraft:zombie,distance=..3,limit=1,sort=nearest] add my_tag
/data merge entity @e[type=minecraft:zombie,distance=..3,limit=1,sort=nearest] {CustomName:'"My Name"',CustomNameVisible:1b}
```

Works regardless of `summon` in JSON.

### Way 4 — auto-spawn via `summon`

Want the NPC to appear automatically (no commands)? Add a `summon` block with a trigger (see §14). The mob will appear when the condition fires — player joined, finished another dialogue, walked into a zone, etc. Best for story maps where you don't want the player doing anything manual.

---

## Cheatsheet for quickly making an NPC

1. **JSON** → `config/interactentity/dialogues/my_npc.json` (minimal — see §2)
2. **Skin** (if `custom_npc`) → `<world>/interactentity/skins/my_npc.png` or `config/interactentity/skins/my_npc.png` (64×64, name `[a-z0-9_]+`)
3. **target** → declare `name` and `tag` (see §5)
4. **Spawn** → one of:
   - `summon` block in JSON with a trigger (see §14)
   - `/npc spawn my_npc` (see §30)
   - Vanilla `/summon` with CustomName and Tags
5. **Reload** → `/dialogue reload`
6. **Test** → right-click the mob

If something doesn't work — check the server log, look for `[InteractEntity]`, `[skins]`, `WARN`.

---

<a id="русский"></a>

## Русский

> [🇬🇧 Switch to English](#english)

Полный справочник + туториал по моду **InteractEntity**: JSON-формат диалогов, квесты, репутация, NPC, скины, журнал, эмоции, KubeJS-интеграция и всё остальное.

> [!TIP]
> ### 💡 Лень читать? Пусть JSON-файлы напишет нейросеть!
> Вам не обязательно изучать всё руководство целиком! Вы можете поручить создание готовых JSON-файлов диалогов нейросети (например, Gemini или ChatGPT).
> 
> Просто **загрузите или скопируйте весь этот файл `README.md`** вашему ИИ-ассистенту и отправьте следующий промпт:
> 
> ```
> Ты — профессиональный сценарист квестов в Minecraft. Используя прикреплённое руководство по моду "InteractEntity", напиши полностью рабочий ветвящийся диалог в указанном JSON-формате.
> 
> Сюжет квеста: [Опишите здесь свой сюжет своими словами. Например: "Кузнец по имени Боргин просит принести ему 10 железных слитков. Если игрок приносит их, забрать железо, выдать в награду алмаз, дать +20 репутации гильдии кузнецов и заспавнить NPC Старца перед игроком. Иначе сказать, чтобы приходил позже."]
> 
> Выдай строго готовый и чистый JSON, соответствующий спецификации. Внимательно проверь синтаксис (запятые) и правильность типов действий (actions) и условий (conditions).
> ```
> 
> Скопируйте полученный JSON в файл вашего диалога в папке мира, напишите в игре `/dialogue reload` — и всё готово к тестированию!

---

## Оглавление

1. [О моде — что это и как работает](#1-о-моде)
2. [Быстрый старт — минимальный диалог за 30 секунд](#2-быстрый-старт)
3. [Расположение и ID диалогов](#3-расположение-и-id-диалогов)
4. [Корневые поля диалога](#4-корневые-поля-диалога)
5. [target — кого ищем](#5-target--кого-ищем)
6. [Узлы и переходы](#6-узлы-и-переходы)
7. [Опции (Option)](#7-опции-option)
8. [Действия (actions)](#8-действия-actions)
9. [Условия (conditions)](#9-условия-conditions)
10. [Переменные](#10-переменные)
11. [Квесты](#11-квесты)
12. [Связывание NPC между собой](#12-связывание-npc-между-собой)
13. [on_revisit — повторный разговор](#13-on_revisit--повторный-разговор)
14. [Авто-спавн NPC (summon)](#14-авто-спавн-npc-summon)
15. [Триггеры диалога (triggers)](#15-триггеры-диалога-triggers)
16. [Рутины — расписание NPC](#16-рутины--расписание-npc)
17. [NPC-сущности (32 типа + custom_npc)](#17-npc-сущности)
18. [Эмоции и анимации](#18-эмоции-и-анимации)
19. [Визуал custom_npc](#19-визуал-custom_npc)
20. [Скины — динамическая загрузка](#20-скины--динамическая-загрузка)
21. [Журнал и HUD квестов](#21-журнал-и-hud-квестов)
22. [Аватар NPC в окне диалога](#22-аватар-npc-в-окне-диалога)
23. [Репутация и фракции](#23-репутация-и-фракции)
24. [Подарки](#24-подарки)
25. [Отношения между NPC](#25-отношения-между-npc)
26. [Компаньоны и дом NPC](#26-компаньоны-и-дом-npc)
27. [Scope — global vs per_player](#27-scope)
28. [Плейсхолдеры](#28-плейсхолдеры)
28a. [Мультиязычные диалоги в JSON](#28a-мультиязычные-диалоги-в-json)
29. [Форматирование текста](#29-форматирование-текста)
30. [Команды](#30-команды)
31. [Клавиши](#31-клавиши)
32. [KubeJS интеграция](#32-kubejs-интеграция)
33. [Forge API — события и хуки](#33-forge-api)
34. [Подводные камни](#34-подводные-камни)
35. [Большой пример — сюжетная карта](#35-большой-пример)

---

## 1. О моде

**InteractEntity** — это мод для Forge 1.20.1, который позволяет делать в Minecraft нормальных NPC с диалогами. Не как у ванильных жителей (которые что-то бубнят и торгуют), а полноценные сценарные персонажи: с ветвистыми разговорами, квестами, репутацией, отношениями между собой и игроком.

Работает это так: ты ставишь в мире моба (любого — зомби, жителя, скелета, что угодно), даёшь ему имя через `CustomName` и какой-нибудь тег через scoreboard. Потом пишешь JSON-файл диалога, в котором указываешь это же имя и тег. Когда игрок кликнет ПКМ по такому мобу — откроется твой диалог.

Главная идея — **весь контент описывается JSON-файлами**. Не нужно ничего компилировать, не нужно лезть в код. Хочешь добавить нового персонажа? Кидаешь JSON в папку диалогов, кидаешь PNG-скин в папку скинов, пишешь `/dialogue reload` — и персонаж готов.

Что мод умеет:
- Ветвистые диалоги с условиями («если у игрока есть алмаз, показать опцию купить меч»)
- Квесты (личные и общие на сервере, с целями, дедлайнами, авто-счётчиком убийств)
- Репутацию и фракции
- Подарки с кулдауном
- Переменные (флаги памяти NPC — кто помнит что про игрока)
- Авто-спавн NPC (по триггерам типа «игрок вошёл в зону»)
- Кастомные скины через папку — не нужно собирать ресурспак
- Эмоции и анимации (для NPC типа `custom_npc`)
- Журнал персонажей и HUD активных квестов
- Связь с KubeJS — можно из JS-скриптов слушать события и менять прогресс

### Как мод устроен внутри

Чтобы понять что куда класть и где что искать, держи в голове такое разделение:

- **JSON-диалоги** — это твой контент. Лежат в папке конфигурации в `config/interactentity/dialogues/`. Каждый файл = один диалог.
- **Скины (PNG)** — лежат отдельно от мода, в папке игрока (`config/interactentity/skins/`) или в папке мира (`<world>/interactentity/skins/`). Подробности в §20.
- **Прогресс игроков** мод сохраняет автоматически в файлах мира. Тебе не нужно с этим возиться — главное, чтобы scope (см. §27) был указан правильно.
- **Журнал** игрок открывает клавишей `J` — там он видит всех NPC с которыми разговаривал, их историю реплик и список квестов.
- **Если что-то не работает** — смотри лог сервера. Мод пишет туда понятные warn-ы про невалидные JSON, скины с неправильным именем и т.п.

---

## 2. Быстрый старт

Самый простой способ убедиться что всё работает — сделать одного NPC с двумя репликами и проверить.

Создай файл `config/interactentity/dialogues/test.json`:

```json
{
  "target": { "name": "Тест", "tag": "test_npc" },
  "entry": "hi",
  "nodes": {
    "hi": {
      "text": "&aПривет, &e{player}&a!",
      "options": [
        { "text": "Дай хлеба", "next": "give" },
        { "text": "Пока", "next": null }
      ]
    },
    "give": {
      "text": "Держи.",
      "actions": [
        { "type": "give_item", "item": "minecraft:bread", "count": 3 }
      ]
    }
  }
}
```

В игре заспавни зомби с нужным именем и тегом:
```
/summon zombie ~ ~ ~ {CustomName:'"Тест"',CustomNameVisible:1b,Tags:["test_npc"]}
```

Теперь ПКМ по этому зомби — диалог откроется. Если не открылся: проверь что имя совпадает с `target.name` и тег с `target.tag`. Обычная типичная ошибка — лишний пробел в имени.

---

## 3. Расположение и ID диалогов

Все JSON-файлы диалогов лежат в папке конфигурации: `config/interactentity/dialogues/`. Подпапки разрешены — можно организовывать сюжет как удобно, например складывать главы в отдельные папки.

Пример структуры:
```
config/interactentity/dialogues/
  zombie.json                → ID диалога: "zombie"
  showcase/mayor.json        → ID диалога: "showcase/mayor"
  story/chapter_1/intro.json → ID диалога: "story/chapter_1/intro"
```

**ID диалога** — это просто его путь относительно папки `dialogues/`, без `.json`. Подпапки превращаются в слэши внутри ID. Этот ID ты будешь использовать в командах (например `/npc spawn showcase/mayor`) и в полях action'ов которые ссылаются на другой диалог (`force_dialogue`, `notify_npc`).

**После любой правки JSON** нужно сказать моду что-то изменилось. Для этого пиши в чат: `/dialogue reload` — он перечитает все файлы. Заодно эта команда сбросит весь прогресс и in-memory флаги — удобно когда тестируешь и хочешь начать с чистого листа. Если хочешь перечитать только один файл — `/dialogue reload <id>`, но имей в виду что этот вариант не сбросит флаги спавна (см. §34, пункт 14).

---

## 4. Корневые поля диалога

| Поле | Тип | Обяз. | Описание |
|------|-----|-------|----------|
| `target` | object | да | См. §5 |
| `entry` | string | да | ID стартового узла |
| `nodes` | object | да | `{id: NodeJson}` — словарь узлов |
| `display_name` | string | — | Имя в GUI диалога. По умолчанию = `target.name` |
| `scope` | string | — | `"global"` (default) или `"per_player"` — где хранится прогресс. См. §27 |
| `repeatable` | bool | — | `false` (default). Если `true` — диалог можно перепроходить |
| `invulnerable` | bool | — | `true` (default) — NPC неуязвим во время диалога |
| `disable_knockback` | bool | — | `false` (default). Если `true` — отключает отбрасывание и сдвиг для этого NPC при ударах |
| `disable_attacks` | bool | — | `false` (default). Если `true` — полностью отключает регистрацию атак (анимацию боли, звук боли и получение ударов) для этого NPC |
| `avatar` | string | — | Текстура аватара в диалоговом окне и журнале. **Нужен полный путь** (напр. `"interactentity:textures/entity/skins/harold.png"`). Простое имя скина здесь НЕ расширяется — этот шорткат работает только для `visual.texture` |
| `faction` | string | — | Название фракции (отображается в журнале) |
| `reputation_id` | string | — | ID фракции для накопления репутации. По умолчанию = `faction` |
| `character_info` | string | — | Описание персонажа для журнала |
| `visual` | object | — | См. §19 — модель/текстура/размер для `interactentity:custom_npc` |
| `summon` | object | — | См. §14 — авто-спавн NPC |
| `triggers` | array | — | См. §15 — авто-старт диалога с существующим NPC |
| `routines` | array | — | См. §16 — расписание поведения NPC |
| `on_revisit` | object | — | См. §13 — реакция на повторный заход |

Legacy: `start_trigger` (один триггер). Если есть `triggers[]` — `start_trigger` игнорируется.

---

## 5. target — кого ищем

| Поле | Тип | Обяз. | Описание |
|------|-----|-------|----------|
| `name` | string | да | Должен совпадать с `CustomName` моба |
| `tag` | string | да | Должен быть в scoreboard-тегах моба |
| `entity_type` | string | — | Тип сущности (`minecraft:zombie`, `interactentity:custom_npc`, …) |
| `faction` | string | — | Метаданные (не влияет на резолв) |

Все указанные поля должны совпасть. Иначе ПКМ ничего не делает.

> [!WARNING]
> ### ⚠️ Критическое правило совпадения: entity_type и теги
> - Если вы указываете `entity_type` в разделе `"target"`, он должен **строго** совпадать с типом сущности, заспавненной в мире. Например, если вы заспавнили `minecraft:villager`, а в `"target"` требуется `"interactentity:custom_npc"`, вы не сможете поговорить с NPC.
> - При ручном вызове команды `/summon` для тестирования, обязательно передавайте scoreboard-тег, совпадающий с `"target.tag"` (например, `/summon interactentity:custom_npc ~ ~ ~ {CustomName:'"Elsa"',Tags:["story_elsa"]}`), иначе диалог не начнется!

**Авто-маппинг при `/npc spawn`:** если `entity_type: minecraft:<mob>` указан для одного из 32 «мирных» мобов — мод подставит `interactentity:npc_<mob>` (см. §17). В JSON пиши vanilla-имя, конвертация автоматическая.

---

## 6. Узлы и переходы

Каждый диалог — это набор узлов (`nodes`). Узел — это одна реплика NPC. Игрок движется по узлам нажимая ПКМ или выбирая опцию.

Мод сам определяет тип узла по его полям — ничего вручную указывать не надо. Есть три типа:

| Тип | Признак | Поведение |
|-----|---------|-----------|
| **Линейный** | есть `next`, нет `options` | ПКМ → следующий узел |
| **Выбор** | есть `options` | Игрок выбирает кнопку |
| **Конец** | нет `next` и нет `options` | Диалог закрывается. `"next": null` тоже конец |

### Поля узла

| Поле | Тип | Описание |
|------|-----|----------|
| `text` | string | Реплика NPC. По умолчанию `""` |
| `random_text` | array | Массив строк — при входе выбирается случайная. Перекрывает `text` |
| `next` | string \| null | ID следующего узла |
| `auto_next_ticks` | int | Авто-переход через N тиков (20 = 1 сек) |
| `options` | array | См. §7 |
| `actions` | array | Действия при входе в узел (см. §8) |
| `camera` | string | Режим камеры. Default `"npc"` |
| `camera_yaw_offset` | float | Сдвиг ракурса по горизонтали |
| `camera_pitch_offset` | float | Сдвиг ракурса по вертикали |

### Пример: линейный → выбор → конец

```json
"nodes": {
  "intro": {
    "text": "Здравствуй.",
    "next": "main"
  },
  "main": {
    "text": "Чего хочешь?",
    "options": [
      { "text": "Подарок",  "next": "gift" },
      { "text": "Уйти",     "next": null }
    ]
  },
  "gift": {
    "text": "Держи яблоко.",
    "actions": [{ "type": "give_item", "item": "minecraft:apple" }]
  }
}
```

### Пример: random_text

```json
"greeting": {
  "random_text": [
    "Привет!",
    "Здарова.",
    "О, ты опять.",
    "&7*кивает*"
  ],
  "next": "hub"
}
```

---

## 7. Опции (Option)

Опции — это кнопки которые показываются игроку в узле типа «выбор». Каждая опция это объект внутри массива `options` у узла. У опции есть текст на кнопке и куда перейти при клике. Можно навешать на опцию условие (тогда она будет показана серой если не выполнено) и действия (выполнятся при клике).

| Поле | Тип | Описание |
|------|-----|----------|
| `text` | string | Текст на кнопке (обязат.) |
| `next` | string \| null | Куда переходить |
| `condition` | object | Если задано и false → кнопка отображается **серой** (locked) |
| `actions` | array | Действия при клике на эту опцию |
| `locked` | bool | Принудительно сделать заблокированной |
| `lock_reason` | string | Текст-причина блокировки (для подсказки) |

**Только одно condition на опцию.** Compound (and/or) не поддерживается — для составной логики делай промежуточный узел или фильтруй через action `set_var`+условный переход.

### Пример: условная опция

```json
"shop": {
  "text": "Купишь меч?",
  "options": [
    {
      "text": "Куплю (нужно 10 алмазов)",
      "condition": { "type": "has_item", "item": "minecraft:diamond", "count": 10 },
      "lock_reason": "Нужно 10 алмазов",
      "next": "buy",
      "actions": [
        { "type": "remove_item", "item": "minecraft:diamond", "count": 10 },
        { "type": "give_item",   "item": "minecraft:diamond_sword" }
      ]
    },
    { "text": "Нет, спасибо", "next": null }
  ]
}
```

---

## 8. Действия (actions)

`actions` — это массив команд, которые мод выполняет когда срабатывает узел (при входе в узел) или опция (при клике). Например: «дать игроку 5 хлеба», «запустить звук колокола», «начать квест», «открыть другой диалог». Действия выполняются последовательно сверху вниз.

Используются так:
```json
"actions": [
  { "type": "give_item", "item": "minecraft:bread", "count": 5 },
  { "type": "play_sound", "sound": "minecraft:entity.villager.yes" }
]
```

Ниже — все 28 типов с примерами. Поле помеченное `?` означает «не обязательно, в скобках указано значение по умолчанию».

### 8.1 Базовые

#### `give_item` / `remove_item`

```json
{ "type": "give_item",   "item": "minecraft:apple", "count": 5 }
{ "type": "remove_item", "item": "minecraft:apple", "count": 3 }
```

`remove_item` работает с любыми предметами, включая модовые и с NBT (зачарованные, переименованные, с прочностью).

#### `run_command`

```json
{ "type": "run_command", "command": "give @s minecraft:diamond 1" }
```

Без слэша в начале. Исполняется от сервера с perm-level 2. `@s` = игрок.

#### `teleport`

```json
{ "type": "teleport", "x": 100, "y": 64, "z": 200 }
{ "type": "teleport", "x": 5, "y": 0, "z": -3, "mode": "relative" }
```

`mode`: `"absolute"` (default) или `"relative"`. Можно также `yaw`, `pitch`.

#### `play_sound`

```json
{ "type": "play_sound", "sound": "minecraft:entity.villager.yes", "volume": 1.0, "pitch": 1.0 }
{ "type": "play_sound", "sound": "minecraft:block.bell.use", "target": "entity" }
```

`target`: `"player"` (default — звук слышит только игрок) или `"entity"` (играется в позиции NPC, слышат окружающие).

#### `give_effect` / `remove_effect`

```json
{ "type": "give_effect", "effect": "minecraft:regeneration", "duration": 400, "amplifier": 1 }
{ "type": "remove_effect", "effect": "minecraft:slowness" }
{ "type": "remove_effect" }  // снять все
```

`duration` в тиках (default 200), `amplifier` 0-255, `ambient`/`particles` — bool.

#### `spawn_particles`

```json
{ "type": "spawn_particles", "particle": "minecraft:happy_villager", "count": 20, "spread": 0.5 }
```

`target`: `"entity"` (default) или `"player"`.

#### `camera_shake`

```json
{ "type": "camera_shake", "intensity": 2.0, "duration": 30 }
```

#### `set_time` / `set_weather`

```json
{ "type": "set_time", "time": "night" }
{ "type": "set_time", "time": 6000 }  // ровно полдень
{ "type": "set_weather", "weather": "thunder", "duration": 6000 }
```

`set_time.time`: `"day"` / `"noon"` / `"night"` / `"midnight"` или число тиков (0-23999).
`set_weather.weather`: `"clear"` / `"rain"` / `"thunder"`.

### 8.2 Сценарные

#### `set_var`

```json
{ "type": "set_var", "name": "trust", "value": "1", "op": "set" }
{ "type": "set_var", "name": "trust", "op": "inc" }   // +1
{ "type": "set_var", "name": "trust", "op": "dec" }   // -1
```

#### `fire_event` — постит `DialogueChoiceEvent` для KubeJS/Forge

```json
{ "type": "fire_event", "tag": "started_quest_chain" }
```

#### `schedule_event` — отложенное исполнение

```json
{
  "type": "schedule_event",
  "delay": 600,
  "actions": [
    { "type": "play_sound", "sound": "minecraft:entity.lightning_bolt.thunder" },
    { "type": "give_effect", "effect": "minecraft:slowness", "duration": 200 }
  ]
}
```

⚠️ Если игрок офлайн — не сохраняется через рестарт.

#### `force_dialogue` — открыть другой диалог

```json
{
  "type": "force_dialogue",
  "dialogue_id": "story/chapter_2/intro",
  "target_tag": "mayor",
  "radius": 32.0,
  "start_node": "greeting"
}
```

Ищет ближайшего NPC с `target_tag` в радиусе и открывает с ним диалог.

#### `notify_npc` — зажечь `!` над NPC с указанным диалогом

```json
{ "type": "notify_npc", "dialogue_id": "blacksmith" }
```

#### `summon_npc` — создать NPC прямо во время диалога

```json
{
  "type": "summon_npc",
  "entity": "minecraft:villager",
  "name": "Купец",
  "tags": ["merchant"],
  "despawn": false,
  "walk_away": false,
  "start_dialogue": "merchant",
  "spawn_position": "behind_player"
}
```

`spawn_position`: `"behind_player"` (default), `"front_of_player"`, `"at_player"`.

### 8.3 Квесты

См. §11 для деталей.

```json
{ "type": "start_quest", "quest": { "id": "...", "title": "...", ... } }
{ "type": "complete_objective", "quest_id": "harold_bread", "objective_number": 1 }
{ "type": "complete_quest", "quest_id": "harold_bread" }
{ "type": "fail_quest", "quest_id": "harold_bread" }
{ "type": "update_quest", "quest_id": "...", "objectives": [...] }
```

### 8.4 Социальные

#### `add_reputation`

```json
{ "type": "add_reputation", "id": "village", "value": 10, "label": "Помощь старосте" }
```

#### `give_gift` — подарок с кулдауном 1 час

```json
{
  "type": "give_gift",
  "character_id": "harold",
  "item": "minecraft:bread",
  "amount": 1,
  "reputation": 5,
  "label": "Подарок",
  "success_message": "&aГарольд принимает хлеб.",
  "cooldown_message": "&7Сегодня он уже не голоден."
}
```

#### `set_relationship` — установить отношение между двумя NPC

```json
{ "type": "set_relationship", "npc_a": "mayor", "npc_b": "thief", "relationship": "enemy" }
```

#### `set_companion` — сделать NPC спутником игрока

```json
{ "type": "set_companion", "enable": true }
```

Только для `interactentity:custom_npc`. NPC начинает следовать за игроком.

#### `set_home` — задать «дом» NPC

```json
{ "type": "set_home", "x": 100, "y": 64, "z": 200, "radius": 16 }
```

Без координат — берётся текущая позиция NPC. NPC будет возвращаться в этот радиус.

#### `play_emote` — проиграть анимацию

```json
{ "type": "play_emote", "emote": "wave", "duration_ticks": 40 }
{ "type": "play_emote", "emote": "six_seven" }
{ "type": "play_emote", "emote": "none" }  // сбросить
```

Только для `interactentity:custom_npc`. Список см. §18.

### 8.5 Поле `scope` в action

Любому action можно явно указать `"scope": "global"` или `"per_player"`. По умолчанию scope наследуется из корня диалога — `DialogueTree.injectScope` автоматически вставляет его в каждый action/condition без своего `scope`.

```json
{ "type": "start_quest", "quest": { "id": "epic_quest" }, "scope": "global" }
```

Используется когда личный диалог должен повлиять на глобальный квест или наоборот.

---

## 9. Условия (conditions)

`condition` проверяет какое-то состояние мира или игрока и возвращает true/false. Используется в опциях (чтобы скрыть/заблокировать кнопку) и в `on_revisit` (чтобы выбрать какую ветку показать при повторном заходе).

Каждое условие — это JSON-объект с полем `type` и набором параметров, специфичных для типа. Например проверить наличие предмета:
```json
{ "type": "has_item", "item": "minecraft:diamond", "count": 5 }
```

Всего 19 типов условий. Ниже — все они:

| `type` | Поля | Семантика |
|--------|------|-----------|
| `has_item` | `item`, `count?` (1), `nbt?` | Считает любые предметы. Поддерживает проверку конкретных NBT-тегов через строковый параметр `"nbt"` (например, `"{GunId:\"tacz:deagle\"}"`) |
| `visited_node` | `dialogue`, `node` | Игрок проходил этот узел |
| `quest_status` | `quest_id`, `status` (`"active"`/`"completed"`/`"failed"`/`"none"`) | |
| `if_var` | `name`, `op?` (`"eq"`/`"neq"`/`"gt"`/`"lt"`/`"gte"`/`"lte"`/`"exists"`), `value?` | |
| `reputation` | `id`, `op?` (default `"gte"`), `value` | |
| `killed_mob` | `entity`, `tag?`, `count?` (1) | Счётчик общий на сервер |
| `has_effect` | `effect` | |
| `health_below` | `value`, `percent?` (false) | `percent: true` → value в % от max HP |
| `hunger_below` | `value` | Шкала 0–20 |
| `time_of_day` | `period?` (`"day"`/`"dusk"`/`"night"`/`"dawn"`) | |
| `weather` | `"clear"`/`"rain"`/`"thunder"` | |
| `dimension` | `minecraft:overworld` и т.п. | |
| `biome` | `minecraft:desert` и т.п. | |
| `can_give_gift` | `character_id` | Кулдаун подарка истёк |
| `npc_relationship` | `npc_a`, `npc_b`, `relationship` | |
| `has_advancement` | `advancement` | Vanilla advancement id |
| `experience_level` | `level`, `op?` (default `"gte"`) | |
| `is_raining` | — | |
| `is_night` | — | true 13000–23000 тиков |

### Примеры

```json
{ "type": "if_var", "name": "met_harold", "value": "1", "op": "eq" }
{ "type": "reputation", "id": "village", "op": "gte", "value": 50 }
{ "type": "killed_mob", "entity": "minecraft:zombie", "count": 10 }
{ "type": "health_below", "value": 50, "percent": true }
{ "type": "quest_status", "quest_id": "harold_bread", "status": "completed" }
```

---

## 10. Переменные

Переменные нужны чтобы NPC запоминал что-то про игрока. Например: «этот игрок уже встречался со мной», «он принёс мне 3 раза подарок», «уровень доверия = 5».

Технически это пары имя → значение (значение всегда строка, но если в нём число — мод умеет сравнивать как числа). Хранятся либо глобально для всего сервера, либо лично для игрока — зависит от scope диалога (см. §27).

### Установка

```json
{ "type": "set_var", "name": "trust", "value": "5", "op": "set" }
{ "type": "set_var", "name": "trust", "op": "inc" }  // +1 (только для числовых)
{ "type": "set_var", "name": "trust", "op": "dec" }  // -1
```

### Чтение в условиях

```json
{ "type": "if_var", "name": "met_harold", "value": "1", "op": "eq" }
{ "type": "if_var", "name": "trust", "value": "10", "op": "gte" }
{ "type": "if_var", "name": "secret_word", "op": "exists" }
```

### Чтение в тексте через плейсхолдер

```json
"text": "У тебя &e{var:trust}&r очков доверия."
```

### Пример: NPC запоминает первую встречу

```json
"nodes": {
  "start": {
    "text": "...",
    "options": [
      {
        "text": "(Подойти)",
        "condition": { "type": "if_var", "name": "met", "value": "1", "op": "neq" },
        "next": "first_meeting"
      },
      {
        "text": "(Подойти снова)",
        "condition": { "type": "if_var", "name": "met", "value": "1", "op": "eq" },
        "next": "return"
      }
    ]
  },
  "first_meeting": {
    "text": "О, новое лицо!",
    "actions": [{ "type": "set_var", "name": "met", "value": "1" }],
    "next": null
  },
  "return": {
    "text": "Снова ты.",
    "next": null
  }
}
```

### Пример: счётчик доверия и реакции

```json
"options": [
  {
    "text": "(Помочь)",
    "next": "thanks",
    "actions": [{ "type": "set_var", "name": "trust", "op": "inc" }]
  },
  {
    "text": "(Грубо отказать)",
    "next": "rude",
    "actions": [{ "type": "set_var", "name": "trust", "op": "dec" }]
  }
]
```

В дальнейшем:
```json
"options": [
  {
    "text": "Расскажи секрет",
    "condition": { "type": "if_var", "name": "trust", "value": "5", "op": "gte" },
    "lock_reason": "Недостаточно доверия",
    "next": "secret"
  }
]
```

---

## 11. Квесты

Квесты — это задания которые NPC выдаёт игроку. У квеста есть название, описание, список целей (можно отмечать как выполненные), статус (активен / завершён / провален) и опционально дедлайн.

Игрок видит свои квесты в журнале (клавиша `J`) — там можно посмотреть прогресс и закрепить квест в HUD (клавиша `K`), чтобы он висел на экране постоянно.

Квесты как и всё остальное хранятся либо глобально, либо лично — зависит от scope диалога который их выдал. То есть «личный квест Гарольда для меня» — это `per_player`, а «общий сюжетный квест на всю команду» — `global`.

### 11.1 start_quest

```json
{
  "type": "start_quest",
  "quest": {
    "id": "harold_bread",
    "title": "Хлеб для Гарольда",
    "description": "Принести Гарольду 3 буханки хлеба.",
    "objectives": ["Принести 3 хлеба", "Поговорить с Гарольдом"],
    "required_item": { "id": "minecraft:bread", "count": 3 },
    "giver": "Гарольд"
  }
}
```

### 11.2 Поля квеста

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | string | Уникальный ID |
| `title` | string | Краткое название |
| `description` | string | Описание для журнала |
| `objectives` | array | Список строк. **Не пиши `[ ]`/`[✓]`** — мод сам ставит галочки |
| `required_item` | object | `{id, count?}` — если у игрока уже есть нужное кол-во, первая цель закрывается сразу |
| `required_kills` | object | `{entity, tag?, count, objective?}` — авто-счётчик убийств с подписью `(N/M)` |
| `deadline` | object | `{type, value?}`: `"ticks"`/`"game_days"` (`value` обязат.), `"sunset"`/`"sunrise"` |
| `giver` | string | Имя выдавшего квест. **При запуске из диалогового JSON игнорируется** — берётся из `display_name`/CustomName NPC. Учитывается только при запуске квеста через KubeJS API |

### 11.3 Цель «убить N мобов»

```json
{
  "type": "start_quest",
  "quest": {
    "id": "kill_zombies",
    "title": "Зачистка",
    "objectives": ["Убить зомби"],
    "required_kills": {
      "entity": "minecraft:zombie",
      "count": 10,
      "objective": 0
    }
  }
}
```

В журнале появится `Убить зомби (3/10)` — счётчик автоматический.

### 11.4 Завершение цели вручную

Используй **одно** из трёх (взаимоисключающие):

```json
{ "type": "complete_objective", "quest_id": "harold_bread", "objective": 0 }          // индекс с нуля
{ "type": "complete_objective", "quest_id": "harold_bread", "objective_number": 1 }   // индекс с единицы
{ "type": "complete_objective", "quest_id": "harold_bread", "objective_text": "Принести 3 хлеба" }
```

⚠️ Поле `"index"` **не работает**, тихо логирует warn.

### 11.5 Дедлайн

```json
"deadline": { "type": "game_days", "value": 3 }
"deadline": { "type": "ticks", "value": 12000 }
"deadline": { "type": "sunset" }
"deadline": { "type": "sunrise" }
```

После истечения квест автоматически меняет статус на `"failed"`.

### 11.6 Завершить/провалить квест

```json
{ "type": "complete_quest", "quest_id": "harold_bread" }
{ "type": "fail_quest", "quest_id": "harold_bread" }
```

`complete_quest` ставит все цели в выполненные. Шлёт `QuestCompleteEvent`.

### 11.7 Обновить цели

```json
{
  "type": "update_quest",
  "quest_id": "harold_bread",
  "objectives": ["Принести 5 хлеба (обновлено)", "Поговорить с Гарольдом"]
}
```

⚠️ Заменяет **весь** список objectives — ломает счётчик kills, если он был привязан к индексу.

---

## 12. Связывание NPC между собой

Сюжет редко бывает про одного NPC — обычно цепочка. Игрок поговорил с одним, тот направил ко второму, второй открыл третьего. Мод даёт несколько инструментов, чтобы такие цепочки делать.

Все они построены на одной идее: один NPC оставляет «след» (флаг, посещённый узел, статус квеста), а другой проверяет этот след в своём `condition`.

### Инструмент 1 — `visited_node`

```json
// В диалоге villager_b
{
  "type": "visited_node",
  "dialogue": "villager_a",
  "node": "agreed_to_help"
}
```

### Инструмент 2 — `quest_status`

```json
{ "type": "quest_status", "quest_id": "main_story_1", "status": "completed" }
```

### Инструмент 3 — `set_var` / `if_var`

A зажигает флаг, B его читает.

### Инструмент 4 — `notify_npc`

Зажигает иконку `!` над NPC с указанным `dialogue_id`. Используй чтобы подсветить «следующего» NPC после диалога.

```json
"actions": [{ "type": "notify_npc", "dialogue_id": "blacksmith" }]
```

### Цепочка через `after_dialogue` summon

```json
// chapter_2.json
"summon": {
  "entity": "interactentity:custom_npc",
  "custom_name": "Мудрец",
  "tags": ["sage"],
  "trigger": { "type": "after_dialogue", "dialogue_id": "chapter_1", "delay": 100 },
  "spawn_position": "front_of_player"
}
```

После того как игрок завершит `chapter_1` (дойдёт до end-нода), через 100 тиков заспавнится NPC из `chapter_2`.

---

## 13. on_revisit — повторный разговор

Срабатывает после того как игрок дошёл до end-нода (диалог помечен завершённым). **ESC из диалога — НЕ завершение.**

```json
"on_revisit": {
  "default": "&7*тишина*",
  "default_start_node": "hub",
  "conditions": [
    {
      "condition": { "type": "quest_status", "quest_id": "main", "status": "active" },
      "start_node": "quest_in_progress"
    },
    {
      "condition": { "type": "reputation", "id": "village", "value": 50, "op": "gte" },
      "text": "&aДобро пожаловать, друг!"
    }
  ]
}
```

**Логика:**
1. Условия проверяются сверху вниз.
2. Первое match'нувшее: если есть `start_node` — открывается полный диалог с этого узла; иначе показывается короткое `text` (без диалогового окна).
3. Если ни одно не match: fallback на `default_start_node` (полный диалог) или `default` (текст).

---

## 14. Авто-спавн NPC (`summon`)

Если ты не хочешь спавнить NPC вручную каждый раз (через команды или `/summon`), а хочешь чтобы он появлялся автоматически — добавь в JSON блок `summon`. Внутри указывается какой моб появится, при каком событии и где.

Например: «когда игрок заходит в мир, через 3 секунды перед ним появляется купец». Или: «когда игрок завершит диалог с кузнецом, заспавнить старца в 5 блоках перед ним».

| Поле | Тип | Обяз. | Описание |
|------|-----|-------|----------|
| `entity` | string | да | Тип сущности (vanilla имя авто-маппится — см. §17) |
| `custom_name` | string | да | Должно совпадать с `target.name` |
| `tags` | array | — | Должно содержать `target.tag` |
| `trigger` | object | **да** | См. §14.1. Без триггера — NPE на загрузке |
| `spawn_position` | string | — | `"behind_player"` (default), `"front_of_player"`, `"at_player"` |
| `despawn_after_dialogue` | bool | — | Моб исчезает после диалога |
| `walk_away_before_despawn` | bool | — | Сперва уходит ~10 блоков, потом исчезает |

### 14.1 Типы spawn-триггеров

| `type` | Поля | Когда |
|--------|------|-------|
| `on_join` | `delay?` (тики) | Через delay после входа игрока |
| `after_dialogue` | `dialogue_id`, `delay?` | После завершения указанного диалога |
| `player_near` | `x`, `y`, `z`, `radius?` (8.0) | Игрок в радиусе |
| `player_entered_area` | `x`, `y`, `z`, `radius?` (8.0) | Первый вход в зону |
| `player_looking_for_seconds` | `x`, `y`, `z`, `radius?` (8.0), `seconds?` (2) | Смотрит N сек на точку |
| `on_player_death` | `delay?` | После смерти игрока |

### 14.2 Пример

```json
"summon": {
  "entity": "minecraft:zombie",
  "custom_name": "Гарольд",
  "tags": ["harold"],
  "trigger": { "type": "on_join", "delay": 60 },
  "spawn_position": "front_of_player",
  "despawn_after_dialogue": false
}
```

**Важно:** для не-`repeatable` диалогов спавн блокируется через in-memory `TRIGGERED_DIALOGUES` (сброс — полный `/dialogue reload`) и через `hasVisited(entry)`.

---

## 15. Триггеры диалога (`triggers`)

Если NPC уже стоит в мире и ты хочешь чтобы диалог открывался без ПКМ — добавь массив `triggers` в корень диалога. Тогда диалог будет стартовать сам когда сработает указанное событие. Например: «игрок подошёл ближе чем на 4 блока», «игрок ударил NPC», «у NPC осталось меньше половины HP».

Это полезно для боссов, для случайных встреч, для «эмбиентных» сцен типа «когда подходишь — слышишь шёпот».

| `type` | Поля | Когда |
|--------|------|-------|
| `proximity` | `radius?` (4.0) | Игрок в радиусе (опрос каждые 10 тиков, cooldown 200 тиков) |
| `on_hurt` | `radius?` (4.0) | Игрок ударил NPC |
| `on_death` | `radius?` (4.0) | Игрок убил NPC |
| `health_below` | `threshold?` (0.5) | HP NPC ниже доли от max (0..1) |

```json
"triggers": [
  { "type": "proximity", "radius": 5.0 }
]
```

**Не путать с `summon.trigger`** — там другой набор и оно только для спавна.

---

## 16. Рутины — расписание NPC

Расписание поведения в течение игрового дня (0..24000 тиков).

| Поле | Тип | Описание |
|------|-----|----------|
| `type` | string | `"idle_at"` / `"wander"` / `"patrol"` |
| `start` | int | Начало периода (default 0) |
| `end` | int | Конец (default 24000). Если `start > end` — перекрывает полночь |
| `x`, `y`, `z` | int | Опорная точка (для `idle_at`, `wander`) |
| `radius` | int | Радиус блуждания (default 8) |
| `waypoints` | array | Для `patrol`: `[{x,y,z}, ...]` |

### Пример: купец работает днём, спит ночью

```json
"routines": [
  { "type": "idle_at", "x": 100, "y": 64, "z": 200, "start": 0,     "end": 12000 },
  { "type": "idle_at", "x": 105, "y": 65, "z": 210, "start": 13000, "end": 23000 }
]
```

### Пример: патруль

```json
"routines": [
  {
    "type": "patrol",
    "waypoints": [
      { "x": 100, "y": 64, "z": 200 },
      { "x": 120, "y": 64, "z": 200 },
      { "x": 120, "y": 64, "z": 220 },
      { "x": 100, "y": 64, "z": 220 }
    ]
  }
]
```

---

## 17. NPC-сущности

Чтобы NPC не нападал на игрока, мод добавляет «мирные» версии всех агрессивных мобов. Они выглядят как оригинал и ведут себя похоже, но не атакуют. Плюс есть отдельный универсальный тип `custom_npc` для важных персонажей — с поддержкой кастомных моделей, скинов, размеров и эмоций.

Всего получается 33 типа:

### 17.1 Мирные аналоги ванильных мобов (32)

Каждый ванильный «враг» имеет «спокойную» версию — не атакует игрока, но в остальном выглядит и работает как обычный моб.

| Vanilla | Аналог мода |
|---------|-------------|
| `minecraft:zombie` | `interactentity:npc_zombie` |
| `minecraft:skeleton` | `interactentity:npc_skeleton` |
| `minecraft:creeper` | `interactentity:npc_creeper` |
| `minecraft:spider`, `cave_spider` | `interactentity:npc_spider`, `npc_cave_spider` |
| `minecraft:enderman`, `endermite` | `interactentity:npc_enderman`, `npc_endermite` |
| `minecraft:witch`, `evoker` | `npc_witch`, `npc_evoker` |
| `minecraft:piglin`, `piglin_brute`, `zombified_piglin` | `npc_piglin`, `npc_piglin_brute`, `npc_zombified_piglin` |
| `minecraft:pillager`, `vindicator`, `ravager` | `npc_pillager`, `npc_vindicator`, `npc_ravager` |
| `minecraft:husk`, `drowned`, `stray`, `wither_skeleton` | `npc_husk`, `npc_drowned`, `npc_stray`, `npc_wither_skeleton` |
| `minecraft:blaze`, `ghast`, `magma_cube`, `slime` | `npc_blaze`, `npc_ghast`, `npc_magma_cube`, `npc_slime` |
| `minecraft:phantom`, `vex`, `shulker` | `npc_phantom`, `npc_vex`, `npc_shulker` |
| `minecraft:guardian`, `elder_guardian` | `npc_guardian`, `npc_elder_guardian` |
| `minecraft:silverfish`, `hoglin`, `zoglin` | `npc_silverfish`, `npc_hoglin`, `npc_zoglin` |
| `minecraft:warden` | `npc_warden` |

При `/npc spawn <id>` мод авто-конвертирует ванильное имя в `interactentity:npc_*`. В JSON пиши vanilla.

### 17.2 `interactentity:custom_npc`

Универсальная сущность с player-моделью. **Только она** поддерживает:
- Кастомные модель и текстуру (`visual.model`, `visual.texture`)
- Кастомный размер (`visual.scale`)
- Эмоции (`play_emote`)
- Компаньонство (`set_companion`)
- Динамические скины из папки (см. §20)

Используй для важных персонажей.

### 17.3 Управление экипировкой (Броня и предметы)

Вы можете управлять экипировкой (оружием, щитами и броней) любого зарегистрированного NPC в мире:
- **Надевание предметов и брони:**
  - Возьмите предмет в основную руку, присядьте (зажмите Shift) и нажмите Правой кнопкой мыши по NPC.
  - Броня (шлемы, нагрудники, поножи, ботинки) автоматически наденется в нужные слоты брони.
  - Щиты экипируются во вторую руку (`OFFHAND`).
  - Элитры экипируются в слот нагрудника (`CHEST`).
  - Тыквы и головы/черепа экипируются в слот шлема (`HEAD`).
  - Все остальные предметы/блоки берутся в основную руку (`MAINHAND`).
  - Если слот NPC уже был занят, старый предмет автоматически возвращается в ваш инвентарь (или выпадает на землю, если ваш инвентарь переполнен).
  - Проигрывается соответствующий звук экипировки брони.
- **Снятие предметов и брони:**
  - Присядьте (зажмите Shift) и нажмите по NPC Правой кнопкой мыши с пустой рукой.
  - Это поочередно снимет с NPC надетые предметы в последовательности: Основная рука → Левая рука → Шлем → Нагрудник → Поножи → Ботинки, и вернет их в ваш инвентарь.
  - Проигрывается звук поднятия предмета.

---

## 18. Эмоции и анимации

NPC типа `interactentity:custom_npc` умеет проигрывать одноразовые анимации — эмоции. Например помахать рукой, поклониться, удивиться, пожать плечами. Запускаются через action `play_emote`.

Это удобно для оживления сцен — например когда NPC говорит «привет!», запусти ему `wave`; когда он удивлён сюжетным поворотом — `surprised`; когда зол на игрока — `crossed_arms`.

На обычных «мирных мобах» (зомби-NPC, скелет-NPC и т.п.) эмоции не работают — там нет нужных анимаций.

### Список эмоций

`beckon`, `bow`, `celebrate`, `clap`, `confused`, `crossed_arms`, `dismiss`, `facepalm`, `handshake`, `happy`, `laugh`, `no` (alias для `shake_head`), `nod`, `please`, `point`, `scared`, `shake_head`, `shrug`, `six_seven` (alias `67`), `surprised`, `think`, `wave`, `yawn`

Удалены: `angry`, `sad`, `salute`.

### Использование

```json
{ "type": "play_emote", "emote": "wave", "duration_ticks": 40 }
{ "type": "play_emote", "emote": "six_seven" }
{ "type": "play_emote", "emote": "67" }       // alias
{ "type": "play_emote", "emote": "none" }     // сброс
{ "type": "play_emote", "emote": "" }         // сброс
```

`duration_ticks` опционально — задаёт, сколько тиков эмоция остаётся активной до возврата в idle. Если не указано, берётся дефолт под каждую эмоцию (по длине анимации). Одноразовые анимации (wave, bow) в любом случае проигрываются один раз до конца.

### Базовые анимации (не эмоции)

Эти играются автоматически:
- `animation.custom_npc.idle` — когда стоит
- `animation.custom_npc.walk` — когда идёт

Они не настраиваются через JSON, заданы в коде. Можно заменить только подменив `custom_npc.animation.json` через ресурспак.

---

## 19. Визуал `custom_npc`

```json
"visual": {
  "model": "interactentity:geo/custom_npc_default.geo.json",
  "texture": "harold",
  "scale": 1.0
}
```

| Поле | Тип | Описание |
|------|-----|----------|
| `model` | string | Путь к `.geo.json` модели |
| `texture` | string | Простое имя (динамика, §20) или полный путь (`namespace:textures/entity/...png`) |
| `scale` | float | 0.1..5.0 |

### Доступные встроенные модели

| Значение | Стиль рук |
|----------|-----------|
| `interactentity:geo/custom_npc_default.geo.json` | толстые (Steve) |
| `interactentity:geo/custom_npc_slim.geo.json` | тонкие (Alex, нужно для slim-скинов) |

### Динамическое изменение

```
/npc set_model @e[type=interactentity:custom_npc,limit=1] interactentity:geo/custom_npc_slim.geo.json
/npc set_texture @e[type=interactentity:custom_npc,limit=1] harold
/npc set_scale @e[type=interactentity:custom_npc,limit=1] 1.2
```

---

## 20. Скины — динамическая загрузка

Главная фишка мода для создателей карт. Раньше чтобы дать NPC свой скин нужно было собирать ресурспак, заливать его куда-то на хостинг, прописывать в `server.properties` и так далее — морока. Тут всё проще.

Ты просто кидаешь PNG-файл в одну из двух папок. Сервер на старте сам прочитает его, при заходе игрока пошлёт PNG-байты клиенту, клиент покажет этот скин на NPC. Всё. Без ресурспаков, без хостинга, без перекомпиляции мода.

Внутри мода никаких скинов не лежит, кроме одного дефолтного `custom_npc_default.png` — он показывается если ничего не нашли.

### 20.1 Где хранить

| Папка | Когда удобно |
|-------|--------------|
| `config/interactentity/skins/` | глобальная, доступна во всех мирах одного игрока |
| `<world>/interactentity/skins/` | per-world, едет вместе с миром при zip-распространении |

При совпадении имён **per-world перекрывает глобальную**.

### 20.2 Правила

| Правило | Подробности |
|---------|-------------|
| Имя файла | `[a-z0-9_]+\.png` — только маленькие буквы, цифры, подчёркивания |
| Размер | **64×64** или 64×32 (legacy) |
| Невалидный файл | Пропускается, warn в лог |

### 20.3 Как это работает

1. Сервер при старте сканирует обе папки, читает байты PNG в память.
2. При заходе игрока шлёт пачку через `SkinSyncPacket`.
3. Клиент создаёт `DynamicTexture` и регистрирует под `interactentity:textures/entity/skins/<name>.png`.
4. `/dialogue reload` (без аргумента) перечитывает скины и рассылает обновление.

### 20.4 Как указывать в JSON

| Запись | Что произойдёт |
|--------|----------------|
| `"texture": "harold"` | резолвится в `interactentity:textures/entity/skins/harold.png` — динамика или fallback ресурспака |
| `"texture": "interactentity:textures/entity/foo.png"` | используется как есть |
| `"avatar": "interactentity:textures/entity/skins/harold.png"` | Для аватара нужен **полный путь** — простое имя скина не расширяется |

**Рекомендация:** для своих NPC используй простое имя. Для текстур из ресурспаков — полный namespaced путь.

### 20.5 Сценарий распространения карты

1. Автор кидает PNG в `<world>/interactentity/skins/harold.png`
2. В JSON указывает `"texture": "harold"`
3. Zip'ит папку мира
4. Скачавший распаковывает → сервер при старте подгрузит → у всех на сервере появится

### 20.6 Диагностика — почему не отображается?

- Имя файла не соответствует `[a-z0-9_]+\.png`
- Размер не 64×64 / 64×32
- В JSON указан полный путь вместо простого имени
- Не выполнен `/dialogue reload` после добавления файла
- Лог сервера: ищи `[skins]` сообщения

---

## 21. Журнал, HUD квестов и иконка над NPC

В моде есть три UI-элемента которые игрок видит:
- **Журнал** — большое окно с историей разговоров и квестами (`J`)
- **HUD квестов** — маленькая панель с активными квестами на экране (`K`)
- **Иконка `!`** — жёлтый восклицательный знак над головой NPC у которого есть для тебя что-то новое

### 21.1 Журнал (`J`)

Открывается клавишей `J`. Внутри три раздела:

| Раздел | Что |
|--------|-----|
| **Персонажи** | Все NPC с которыми игрок говорил. Иконка-голова, имя, маркер активного квеста |
| **История диалога** | Реплики выбранного персонажа (всё что говорил тебе) |
| **Задания** | Список квестов выбранного персонажа. Можно «отслеживать» (закрепить в HUD) |

Детали персонажа: 3D-модель, фракция, отношения (статус по репутации), завершённые квесты, lore.

Кнопка «Отслеживать» (закрепляет квест в HUD) — лимит 3 одновременно.

### 21.2 HUD квестов (`K`)

Показывает отслеживаемые квесты на экране. Toggle клавишей `K`.

### 21.3 Оверлей «Текущий диалог» (`H` в диалоге)

В открытом диалоге `H` показывает прокручиваемую историю реплик текущего разговора. Удобно если ты пропустил что NPC сказал пять реплик назад — можно вернуться и перечитать без перезахода.

### 21.4 Иконка `!` над NPC

Мод автоматически рисует жёлтый восклицательный знак над головой NPC в двух случаях:

1. **С этим NPC ещё не разговаривали** — игрок никогда не открывал его диалог.
2. **Сработал action `notify_npc`** — другой NPC явно сказал «у этого появилось новое».

Иконка видна в радиусе 16 блоков от игрока, всегда повёрнута к камере. Исчезает сразу после того как игрок открыл диалог (если только не было `notify_npc` — тогда исчезнет только после нового захода).

Это очень полезно для подсказок куда идти дальше: завершил один квест → следующий NPC автоматически загорелся `!` благодаря `notify_npc` в действиях завершающей опции.

---

## 22. Аватар NPC в окне диалога

Это голова NPC которая показывается слева от реплики. Указывается в корне диалога через поле `avatar`:

```json
"avatar": "interactentity:textures/entity/skins/harold.png"
"avatar": "interactentity:textures/entity/mayor.png"
```

В отличие от `visual.texture`, полю `avatar` нужен **полный путь к текстуре** — простое имя скина вроде `"harold"` здесь не расширяется в папку скинов и отрисуется как отсутствующая текстура.

Мод берёт из текстуры область 8×8 от координаты (8,8) — это лицо в стандартной player-skin раскладке (64×64). Поэтому удобно использовать обычные скины игроков и NPC напрямую — мод сам вырежет из них голову.

### Аватар через NBT (без правки JSON)

Можно задать аватар конкретному мобу через NBT-тег `DialogueAvatar` — он перекроет `avatar` из JSON. Удобно если хочешь два экземпляра одного NPC с разной внешностью без двух разных JSON-файлов.

```
/data merge entity @e[name=ИмяНПС,limit=1] {DialogueAvatar:"interactentity:textures/entity/skins/harold.png"}
```

Здесь нужен полный путь к текстуре, не просто имя скина.

> Стиль окна диалога (фоны, цвета, рамки кнопок) намеренно зафиксирован в моде и не настраивается через JSON. Это сделано чтобы все NPC выглядели единообразно.

---

## 23. Репутация и фракции

Репутация — это просто число привязанное к ID фракции (например `village`). Игрок может его повышать (помогая NPC) или понижать (грубя или нарушая обещания). NPC из этой фракции могут проверять текущее значение в своих условиях и реагировать по-разному.

Чтобы это работало, нужно сделать четыре простые вещи:

1. Объяви фракцию в корне диалога:
```json
"faction": "Деревня",
"reputation_id": "village"
```

2. Начисляй:
```json
{ "type": "add_reputation", "id": "village", "value": 10, "label": "Помощь старосте" }
```

3. Проверяй:
```json
{ "type": "reputation", "id": "village", "op": "gte", "value": 50 }
```

4. Показывай в тексте:
```json
"text": "У тебя &e{reputation:village}&r очков с деревней."
```

5. Подарки (см. §24) автоматически начисляют репутацию.

### Шкала отображения

| Диапазон | Статус |
|----------|--------|
| ≤ -50 | вражда |
| -49..-20 | неприязнь |
| -19..19 | нейтрально |
| 20..49 | дружелюбие |
| ≥ 50 | союзник |

---

## 24. Подарки

Подарки — это специальный action который позволяет игроку отдать NPC что-то приятное и получить за это репутацию. Главная фишка — встроенный кулдаун: одному и тому же NPC нельзя подарить чаще раза в час (реального времени). Это защищает от спама — игрок не может за минуту прокачать репутацию на максимум, отдавая 100 хлебов подряд.

Если кулдаун не истёк, action ничего не сделает (только покажет сообщение). Проверить можно отдельным условием `can_give_gift`.

```json
{
  "type": "give_gift",
  "character_id": "harold",
  "item": "minecraft:bread",
  "amount": 1,
  "reputation": 5,
  "label": "Подарок",
  "success_message": "&aГарольд улыбается.",
  "cooldown_message": "&7Сегодня он уже не голоден."
}
```

Логика:
1. Проверка — есть ли у игрока 1 хлеб
2. Проверка — истёк ли кулдаун подарка для `harold` (1 час реального времени)
3. Если ОК: предмет забирается, +5 к репутации, показывается `success_message`
4. Если кулдаун не истёк: показывается `cooldown_message`, ничего не происходит

Проверка кулдауна отдельно:
```json
{ "type": "can_give_gift", "character_id": "harold" }
```

---

## 25. Отношения между NPC

NPC могут «знать» друг о друге через relationship-флаги.

### Установка

```json
{
  "type": "set_relationship",
  "npc_a": "mayor",
  "npc_b": "thief",
  "relationship": "enemy"
}
```

`relationship` — произвольная строка: `"ally"`, `"enemy"`, `"family"`, `"rival"`, что угодно.

### Проверка

```json
{
  "type": "npc_relationship",
  "npc_a": "mayor",
  "npc_b": "thief",
  "relationship": "enemy"
}
```

### Пример: вор не подойдёт к мэру, если они враги

```json
"options": [
  {
    "text": "Поговорить с мэром",
    "condition": {
      "type": "npc_relationship",
      "npc_a": "mayor",
      "npc_b": "thief",
      "relationship": "enemy"
    },
    "lock_reason": "Он меня прибьёт",
    "next": "..."
  }
]
```

---

## 26. Компаньоны и дом NPC

### 26.1 set_companion

```json
{ "type": "set_companion", "enable": true }
```

Только для `interactentity:custom_npc`. NPC начинает следовать за игроком.

```json
{ "type": "set_companion", "enable": false }
```

Отпустить.

### 26.2 set_home

```json
{ "type": "set_home", "x": 100, "y": 64, "z": 200, "radius": 16 }
```

NPC будет возвращаться в радиус `radius` блоков от точки. Без координат — берётся текущая позиция NPC.

---

## 27. Scope

Scope — это «область видимости» прогресса диалога. Простыми словами: общий ли прогресс у всех игроков на сервере, или у каждого свой.

Есть два варианта:

- `"global"` (по умолчанию) — прогресс общий. Если один игрок прошёл квест, у всех остальных он тоже считается пройденным. Подходит для совместного сюжета (кооп-карты) или для одиночной игры.
- `"per_player"` — у каждого игрока свой прогресс. Подходит для мультиплеерных серверов, где каждый игрок проходит сюжет отдельно. Можно также писать `"player"` — это синоним.

Scope влияет на: переменные (`set_var` / `if_var`), репутацию, посещённые узлы (`visited_node`), квесты, отношения между NPC (`set_relationship`).

### Что конкретно происходит при `global`

Это важно понимать заранее, потому что многих удивляет:

- `visited_node` срабатывает если **хоть один** игрок прошёл этот узел. То есть если друг прошёл — у тебя тоже зачтётся.
- `killed_mob` считает убийства **всех игроков на сервере вместе**. Если квест «убить 10 зомби» — могут вдесятером по одному убить и квест закроется.
- Переменные (`set_var`) — одинаковы для всех. Один поменял `trust` на 5 — у всех такой же.
- Квесты видны в журнале **у каждого игрока**, но статус общий.
- Иконка `notify_npc` загорается **для всех** на сервере.
- Хранилище привязано к Overworld — прогресс не теряется при переходе в Nether/End.

### Что происходит при `per_player`

Всё то же самое, но привязано к UUID конкретного игрока. У каждого свои переменные, свои квесты, свои посещённые узлы, своя репутация. Журналы у разных игроков покажут разный прогресс.

Если делаешь нормальный мультиплеерный сервер где каждый идёт по сюжету сам — это твой выбор почти всегда.

### Cross-scope

Action в одном scope может ссылаться на квест из другого. `findQuestStore()` ищет квест во всех хранилищах. Если явно указан `"scope"` в action — берётся он.

### Когда что использовать

| Сценарий | Scope |
|----------|-------|
| Сюжетная карта на одного | `global` или `per_player` — без разницы |
| Мультиплеер кооп с общим сюжетом | `global` |
| Мультиплеер где каждый идёт по сюжету сам | `per_player` |
| Личные квесты вроде «Принеси мне меч» | `per_player` |
| Глобальный квест «Освободи деревню» | `global` |

---

## 28. Плейсхолдеры

В любом текстовом поле (`text`, `display_name`, `random_text`, тексты опций) можно вставлять плейсхолдеры — мод подменит их на актуальные значения. Например `{player}` превратится в ник игрока, `{var:trust}` — в значение твоей переменной.

Это очень удобно для живых реплик: «Привет, Стив!» вместо обезличенного «Привет, путник».

| Плейсхолдер | Что подставляется |
|-------------|-------------------|
| `{player}` | Имя игрока |
| `{player_uuid}` | UUID игрока |
| `{npc_uuid}` | UUID NPC |
| `{var:NAME}` | Значение переменной |
| `{reputation:ID}` | Текущая репутация фракции |

```json
"text": "Привет, &e{player}&r! У тебя {reputation:village} очков и {var:trust} доверия."
```

---

## 28a. Мультиязычные диалоги в JSON

По умолчанию, если писать диалоги обычными строками, все игроки будут видеть текст на одном языке. Чтобы сделать ваши диалоги доступными для игроков со всего мира, **InteractEntity** поддерживает полностью динамическую локализацию прямо внутри JSON-файлов.

Для любого текстового поля, отображаемого игроку (включая `display_name` в корне диалога, `text` в узлах, элементы массива `random_text`, тексты кнопок `text` в опциях и причины блокировки `lock_reason` в опциях), вместо простой строки вы можете указать **JSON-объект с кодами языков (локалей)** и соответствующими переводами.

### Как это работает
1. Когда игрок начинает диалог, мод автоматически определяет активный язык его клиента (например, `en_us`, `ru_ru`, `fr_fr`, `de_de`, `es_es`, `zh_cn` и т.д.).
2. Мод ищет соответствующий ключ локали в вашем объекте перевода.
3. **Механизм fallback (отката)**: Если язык игрока не найден в объекте, мод попробует использовать английский (`en_us`). Если и `en_us` отсутствует, будет взят первый доступный перевод из объекта.
4. Все **плейсхолдеры** (например `{player}`) и **коды форматирования** (такие как `&e` или HEX-цвета) полноценно поддерживаются и подставляются в выбранную строку перевода "на лету", гарантируя отсутствие смеси языков.

### Пример JSON

Вместо простой строки:
```json
"text": "Привет, &e{player}&r! Хочешь алмаз?"
```

Вы можете написать:
```json
"text": {
  "en_us": "Hello, &e{player}&r! Do you want a diamond?",
  "ru_ru": "Привет, &e{player}&r! Хочешь алмаз?",
  "fr_fr": "Bonjour, &e{player}&r ! Tu veux un diamant ?",
  "de_de": "Hallo, &e{player}&r! Möchtest du einen Diamanten?",
  "es_es": "¡Hola, &e{player}&r! ¿Quieres un diamante?",
  "zh_cn": "你好，&e{player}&r！你想要一颗钻石吗？"
}
```

### Пример полного узла

Вот как выглядит узел диалога с поддержкой мультиязычности:

```json
"nodes": {
  "start": {
    "text": {
      "en_us": "Welcome! I have a quest for you.",
      "ru_ru": "Добро пожаловать! У меня есть квест для тебя.",
      "fr_fr": "Bienvenue ! J'ai une quête pour toi.",
      "de_de": "Willkommen! Ich habe eine Quest für dich."
    },
    "options": [
      {
        "text": {
          "en_us": "Sure, let's do it!",
          "ru_ru": "Конечно, давай!",
          "fr_fr": "Bien sûr, c'est parti !",
          "de_de": "Klar, legen wir los!"
        },
        "next": "accept_quest"
      },
      {
        "text": {
          "en_us": "Not now (Requires 10 reputation)",
          "ru_ru": "Не сейчас (Требуется 10 репутации)"
        },
        "condition": {
          "type": "reputation",
          "id": "village",
          "op": "gte",
          "value": 10
        },
        "lock_reason": {
          "en_us": "You must be trusted in the village.",
          "ru_ru": "Вам должны доверять в деревне."
        },
        "next": "refuse_quest"
      }
    ]
  }
}
```

---

## 29. Форматирование текста

Внутри строки можно красить и стилить текст. Работают стандартные Minecraft-коды (через `&` вместо `§`) плюс расширение для HEX-цветов. Коды можно комбинировать — например `&l&c` даст жирный красный.

| Код | Эффект |
|-----|--------|
| `&0`..`&9`, `&a`..`&f` | Цвета как в Minecraft |
| `&l` | Жирный |
| `&o` | Курсив |
| `&n` | Подчёркивание |
| `&m` | Зачёркнутый |
| `&k` | Мерцающие символы |
| `&r` | Сброс форматирования |
| `&#RRGGBB` | Произвольный HEX-цвет |

```json
"text": "&#FFD700Золотой&r и &lжирный&r и &c&lкрасный жирный&r."
```

---

## 30. Команды

### `/dialogue`

| Команда | Что делает |
|---------|------------|
| `/dialogue reload` | Перезагружает все диалоги + скины, сбрасывает прогресс и in-memory флаги |
| `/dialogue reload <id>` | Один диалог + сброс прогресса (но НЕ in-memory флаги спавна) |
| `/dialogue test <id> [node]` | Открывает диалог с ближайшим мобом без проверок target |
| `/dialogue goto <node>` | В активном диалоге — прыжок к узлу |
| `/dialogue var set <name> <value> [target]` | Задать переменную — глобально или per-player, если указан `target` |
| `/dialogue var get <name> [target]` | Вывести значение переменной (глобальной или у игрока `target`) |

### `/npc`

| Команда | Что делает |
|---------|------------|
| `/npc spawn <id>` | Спавнит NPC по `summon.entity` + `target.name/tag`. Принимает slashed-ID (`showcase/bob`) |
| `/npc tag <id>` | Присваивает имя+тег ближайшему мобу |
| `/npc remove` | Удаляет ближайшего NPC |
| `/npc list [radius]` | Список NPC в радиусе (default 32) |
| `/npc set_model <targets> <path>` | Меняет модель `custom_npc` |
| `/npc set_texture <targets> <name_or_path>` | Меняет текстуру (простое имя или namespaced) |
| `/npc set_scale <targets> <0.1..5.0>` | Меняет размер `custom_npc` |
| `/npc set_name <targets> <name>` | Ставит видимый CustomName |

---

## 31. Клавиши

Игроку доступно довольно много горячих клавиш — мышью можно вообще не пользоваться внутри диалога если хочешь.

**Вне диалога:**

| Клавиша | Что |
|---------|-----|
| `J` | Открыть журнал |
| `K` | Показать/скрыть HUD активных квестов |
| `ПКМ` по NPC | Открыть диалог |

**Внутри диалога:**

| Клавиша | Что |
|---------|-----|
| `ПКМ` или `Пробел` или `Enter` | Перейти дальше (для линейных и при ответе игрока). Если текст ещё не дорисовался — мгновенно дорисовать |
| `1` ... `5` | Быстрый выбор варианта ответа по номеру |
| `↑` / `↓` | Подсветить предыдущий / следующий вариант |
| `Enter` или `Пробел` | Подтвердить подсвеченный вариант |
| `H` | Показать/скрыть «Текущий диалог» — оверлей с историей реплик внутри текущего разговора |
| `ESC` | Закрыть диалог. **Внимание:** ESC НЕ помечает диалог завершённым — `on_revisit` не сработает, нужен реальный end-нод |

Все клавиши открытия журнала и HUD переназначаются через стандартное меню Minecraft: Options → Controls.

---

## 32. KubeJS интеграция

Если поставить рядом KubeJS — можно делать вещи которые в чистом JSON не выразить. Например: динамически генерировать квесты от состояния мира, слушать события диалогов и встраивать кастомную логику, выдавать награды через свои таблицы.

Мод даёт два инструмента: статический Java-API (методы которые можно вызывать из JS) и Forge-события (на них можно подписаться и реагировать).

### 32.1 Статический API — `InteractEntityAPI`

```js
// kubejs/server_scripts/my_quests.js

ServerEvents.tick(event => {
  // например, выдать квест по таймеру
})

// Прямой вызов методов мода
const Api = Java.loadClass('net.ashpapi.interactentity.api.InteractEntityAPI')

PlayerEvents.loggedIn(event => {
  const player = event.player
  // Начислить репутацию
  Api.addReputation(player, "village", 5, "global")

  // Установить переменную
  Api.setVar(player, "intro_done", "1", "per_player")
})
```

#### Методы

```java
boolean startQuest(ServerPlayer player, String questJsonString)
boolean startQuest(ServerPlayer player, JsonObject questJson)
boolean completeQuest(ServerPlayer player, String questId)
boolean failQuest(ServerPlayer player, String questId)

void addReputation(ServerPlayer player, String factionId, int delta, String scope)
int   getReputation(ServerPlayer player, String factionId, String scope)

void   setVar(ServerPlayer player, String name, String value, String scope)
String getVar(ServerPlayer player, String name, String scope)

boolean openDialogue(ServerPlayer player, String dialogueId, LivingEntity entity)
```

`scope` — `"global"` или `"player"`.

### 32.2 События — слушаем диалоги из KubeJS

```js
// kubejs/server_scripts/dialogue_listener.js

ForgeEvents.onEvent('net.ashpapi.interactentity.api.DialogueChoiceEvent', event => {
  const player = event.player
  const tag = event.tag
  const source = event.source  // "option" или "action" (от fire_event)

  if (source === 'action' && tag === 'started_quest_chain') {
    player.tell('§eСюжетная цепочка началась!')
    // запустить свою логику
  }
})

ForgeEvents.onEvent('net.ashpapi.interactentity.api.QuestStartEvent', event => {
  console.log(`Player ${event.player.name.string} started quest ${event.questId} in ${event.scope}`)
})

ForgeEvents.onEvent('net.ashpapi.interactentity.api.QuestCompleteEvent', event => {
  // Выдать награду от себя
  event.player.give('minecraft:diamond')
})
```

### 32.3 Использование `fire_event` для связи с KubeJS

В JSON-диалоге:
```json
"actions": [
  { "type": "fire_event", "tag": "village_saved" }
]
```

В KubeJS:
```js
ForgeEvents.onEvent('net.ashpapi.interactentity.api.DialogueChoiceEvent', event => {
  if (event.source === 'action' && event.tag === 'village_saved') {
    // выдать любую KubeJS-логику
  }
})
```

Это самый чистый способ интегрировать сложную пользовательскую логику без модификации Java-кода.

---

## 33. Forge API

### События

| Event | Когда | Поля |
|-------|-------|------|
| `DialogueStartEvent` | Диалог открыт | `player`, `npc`, `dialogueId`, `startNodeId` |
| `DialogueChoiceEvent` | Выбран ответ ИЛИ сработал `fire_event` | `player`, `npc`, `dialogueId`, `nodeId`, `source` (`"option"`/`"action"`), `tag` |
| `DialogueEndEvent` | Диалог закрыт или завершен | `player`, `npc`, `dialogueId`, `lastNodeId`, `completed` |
| `QuestStartEvent` | Квест начат | `player`, `questId`, `scope` |
| `QuestCompleteEvent` | Квест завершён | `player`, `questId`, `scope` |
| `QuestFailEvent` | Квест провален | `player`, `questId`, `scope` |

### Подписка (Forge)

```java
@SubscribeEvent
public static void onQuestStart(QuestStartEvent event) {
    LOGGER.info("Player {} started quest {}", event.getPlayer().getName(), event.getQuestId());
}
```

---

## 34. Подводные камни

1. **`summon` без `trigger`** → NPE при загрузке. Триггер внутри `summon` обязателен.
2. **`update_quest` ломает счётчик kills**, потому что заменяет весь `objectives[]`.
3. **`triggers[]` ≠ `summon.trigger`** — разные наборы типов, разные роли.
4. **Не-repeatable диалог не перевыдаст after_dialogue-спавн** дочернего NPC, если энтри-нода дочернего уже посещался. Сброс: `/dialogue reload` (без аргумента).
5. **ESC не помечает диалог завершённым** — `on_revisit` не сработает. Нужен реальный end-нод.
6. **`has_item` считает любые предметы** — модовые и с NBT (зачарованные, переименованные через анвил, с прочностью) учитываются.
7. **Опции поддерживают только одно `condition`** — для AND/OR делай промежуточный узел.
8. **`fire_event` ловится через `DialogueChoiceEvent`** с `source == "action"`.
9. **`schedule_event` не сохраняется через рестарт** если игрок офлайн.
10. **Скин не отображается?** Проверь имя (только `[a-z0-9_]+`), размер (64×64 или 64×32), папку, наличие `/dialogue reload` после изменений.
11. **Поле `"index"` в `complete_objective` не поддерживается** — используй `objective` / `objective_number` / `objective_text`.
12. **`set_companion` работает только с `interactentity:custom_npc`**.
13. **`play_emote` работает только с `interactentity:custom_npc`**.
14. **`/dialogue reload <id>` не сбрасывает in-memory `TRIGGERED_DIALOGUES`** — для полного сброса спавна используй `/dialogue reload` без аргументов.
15. **JSON-парсер чувствителен к запятым** — лишняя запятая после последнего поля = ошибка загрузки, диалог не появится. Смотри лог.

---

<a id="35-большой-пример"></a>

## 35. Большой пример

Чтобы собрать всё вместе, ниже — полностью рабочий пример из двух связанных NPC. Можно скопировать в свой мир и оно заработает.

Сюжет: игрок встречает Эльзу (травницу) → она просит найти её потерянные карманные часы, начиная квест «Потерянный амулет» → игрок находит Гарольда (охотника) → Гарольд просит принести кусок хлеба для лиса Rusty перед тем, как отдать часы, начиная квест «Хлеб для Расти» → игрок приносит хлеб Гарольду, забирает часы и возвращает их Эльзе за награду.

### 35.1 `dialogues/story/elsa.json` (per-player)

```json
{
  "target": {
    "name": "Elsa",
    "tag": "story_elsa",
    "entity_type": "interactentity:custom_npc"
  },
  "display_name": "&d[&5Elsa&d]",
  "character_info": "Village herbalist. Collects rare roots and brews ointments.",
  "avatar": "interactentity:textures/entity/skins/elsa.png",
  "scope": "per_player",
  "disable_knockback": true,
  "disable_attacks": true,
  "entry": "start",
  "visual": {
    "model": "interactentity:geo/custom_npc_slim.geo.json",
    "texture": "elsa",
    "scale": 1.0
  },
  "summon": {
    "entity": "interactentity:custom_npc",
    "custom_name": "Elsa",
    "tags": ["story_elsa"],
    "spawn_position": "behind_player",
    "trigger": { "type": "on_join", "delay": 60 }
  },
  "on_revisit": {
    "default_start_node": "hub_idle",
    "conditions": [
      {
        "condition": { "type": "quest_status", "quest_id": "lost_amulet", "status": "completed" },
        "text": "&7Elsa smiles warmly at you. &dThank you for helping out, {player}."
      },
      {
        "condition": { "type": "has_item", "item": "minecraft:clock", "count": 1 },
        "start_node": "return_with_amulet"
      },
      {
        "condition": { "type": "quest_status", "quest_id": "lost_amulet", "status": "active" },
        "start_node": "hub_active"
      }
    ]
  },
  "nodes": {
    "start": {
      "text": "&fOh, traveler! Thank the gods, at least someone stopped by. I am in trouble, {player}...",
      "next": "explain",
      "actions": [
        { "type": "play_emote", "emote": "wave", "duration_ticks": 30 },
        { "type": "play_sound", "sound": "minecraft:entity.villager.ambient", "volume": 1.0, "pitch": 1.1 }
      ]
    },
    "explain": {
      "text": "&fMy &dpocket watch&f is gone. I left it on a tree stump this morning — got distracted by the garden bed, turned around, and it was gone. And there were fox tracks nearby...",
      "next": "ask_help",
      "actions": [
        { "type": "play_emote", "emote": "facepalm", "duration_ticks": 45 }
      ]
    },
    "ask_help": {
      "text": "&fOver in that part of the forest lives a hunter named &6Harold&f. Foxes often drag things to him — maybe he saw my watch. Will you help me?",
      "actions": [
        { "type": "play_emote", "emote": "please", "duration_ticks": 60 }
      ],
      "options": [
        { "text": "&aOf course, I'll find Harold.", "next": "accept",
          "actions": [
            { "type": "start_quest", "quest": {
              "id": "lost_amulet",
              "title": "The Lost Amulet",
              "description": "Elsa lost her pocket watch. A fox might have dragged it to Harold the hunter.",
              "objectives": [
                "Find Harold and ask him",
                "Return the watch to Elsa"
              ]
            }},
            { "type": "play_emote", "emote": "happy", "duration_ticks": 30 }
          ]
        },
        { "text": "&7Not up for this right now.", "next": "refuse",
          "actions": [
            { "type": "play_emote", "emote": "shrug", "duration_ticks": 25 }
          ]
        }
      ]
    },
    "accept": {
      "text": "&dThank you! Harold lives in the forest — you'll find him by the smoke above his cabin. He can be a bit gruff, but he's not mean.",
      "actions": [
        { "type": "play_emote", "emote": "bow", "duration_ticks": 40 }
      ]
    },
    "refuse": {
      "text": "&7I understand... If you change your mind, I'll be here."
    },
    "hub_idle": {
      "text": "&fYou again, {player}. Looking for herbs or just to chat?",
      "actions": [
        { "type": "play_emote", "emote": "nod", "duration_ticks": 25 }
      ],
      "options": [
        { "text": "&7Tell me about yourself.", "next": "lore" },
        { "text": "&7Leave.", "next": null }
      ]
    },
    "hub_active": {
      "text": "&fSo, did you find Harold? Does he have the amulet?",
      "options": [
        { "text": "&7Still looking.", "next": null },
        { "text": "&7Tell me about yourself.", "next": "lore" }
      ]
    },
    "return_with_amulet": {
      "text": "&dOh! My watch! Where did you find it, {player}?!",
      "next": "thanks",
      "actions": [
        { "type": "play_emote", "emote": "celebrate", "duration_ticks": 40 }
      ]
    },
    "thanks": {
      "text": "&fHarold, huh... Give him a jar of honey from me next time. And for you — here, take this.",
      "next": "reward",
      "actions": [
        { "type": "remove_item", "item": "minecraft:clock", "count": 1 },
        { "type": "complete_objective", "quest_id": "lost_amulet", "objective_number": 2 },
        { "type": "complete_quest", "quest_id": "lost_amulet" },
        { "type": "give_item", "item": "minecraft:emerald", "count": 4 },
        { "type": "give_item", "item": "minecraft:golden_apple", "count": 1 },
        { "type": "give_effect", "effect": "minecraft:regeneration", "duration": 200, "amplifier": 1 },
        { "type": "play_sound", "sound": "minecraft:entity.villager.celebrate", "volume": 1.0, "pitch": 1.2 }
      ]
    },
    "reward": {
      "text": "&e4 emeralds and an apple for the road. Stop by again, traveler.",
      "actions": [
        { "type": "play_emote", "emote": "bow", "duration_ticks": 45 }
      ]
    },
    "lore": {
      "text": "&7*Elsa weighs two bundles of herbs in her hands, pondering which is better* &fI was born right here in the village. My mother taught me about herbs, and my grandmother taught me charms. And the watch belonged to her, my grandmother. That's why I'm so worried.",
      "next": null,
      "actions": [
        { "type": "play_emote", "emote": "six_seven", "duration_ticks": 50 }
      ]
    }
  }
}
```

### 35.2 `dialogues/story/harold.json` (per-player)

```json
{
  "target": {
    "name": "Harold",
    "tag": "story_harold",
    "entity_type": "interactentity:custom_npc"
  },
  "display_name": "&6[&eHarold&6]",
  "character_info": "Hermit hunter. Lives in a forest cabin, has tamed a couple of foxes.",
  "avatar": "interactentity:textures/entity/skins/harold.png",
  "scope": "per_player",
  "entry": "start",
  "visual": {
    "model": "interactentity:geo/custom_npc_default.geo.json",
    "texture": "harold",
    "scale": 1.05
  },
  "summon": {
    "entity": "interactentity:custom_npc",
    "custom_name": "Harold",
    "tags": ["story_harold"],
    "trigger": { "type": "after_dialogue", "dialogue_id": "story/elsa", "delay": 200 }
  },
  "on_revisit": {
    "default_start_node": "hub_idle",
    "conditions": [
      {
        "condition": { "type": "visited_node", "dialogue": "story/harold", "node": "give_amulet" },
        "start_node": "hub_after_amulet"
      },
      {
        "condition": { "type": "quest_status", "quest_id": "harold_bread", "status": "active" },
        "start_node": "offer_bread"
      },
      {
        "condition": { "type": "quest_status", "quest_id": "lost_amulet", "status": "active" },
        "start_node": "ask_quest"
      }
    ]
  },
  "nodes": {
    "start": {
      "text": "&fHm. A stranger. &7*looks you up and down* &fWhat do you want in my forest, {player}?",
      "actions": [
        { "type": "play_emote", "emote": "crossed_arms", "duration_ticks": 60 }
      ],
      "options": [
        { "text": "&aElsa sent me. To ask about the watch.",
          "next": "knows_elsa",
          "condition": { "type": "quest_status", "quest_id": "lost_amulet", "status": "active" }
        },
        { "text": "&7Just passing by.", "next": "neutral" },
        { "text": "&7Sorry to bother you.", "next": null }
      ]
    },
    "neutral": {
      "text": "&fWell, passing by it is. The forest is big, don't get lost.",
      "next": null,
      "actions": [
        { "type": "play_emote", "emote": "shrug", "duration_ticks": 25 }
      ]
    },
    "ask_quest": {
      "text": "&fReturned? I guess the herbalist wouldn't let you go.",
      "actions": [
        { "type": "play_emote", "emote": "nod", "duration_ticks": 25 }
      ],
      "options": [
        { "text": "&aShe was asking about the watch.", "next": "knows_elsa" },
        { "text": "&7Just dropped in.", "next": "neutral" }
      ]
    },
    "knows_elsa": {
      "text": "&fElsa, huh... &7*scratches his beard* &fAn old pocket watch on a chain? With a crack on the glass?",
      "next": "confirm_amulet",
      "actions": [
        { "type": "complete_objective", "quest_id": "lost_amulet", "objective_number": 1 },
        { "type": "play_emote", "emote": "think", "duration_ticks": 40 }
      ]
    },
    "confirm_amulet": {
      "text": "&fMy little fox, Rusty, brought it this morning. I was wondering whose it was. Thought about taking it to the village, but had no time.",
      "next": "offer_amulet",
      "actions": [
        { "type": "play_emote", "emote": "think", "duration_ticks": 35 }
      ]
    },
    "offer_amulet": {
      "text": "&fSince you're from Elsa, go ahead and take it. But... &7*narrows his eyes* &fI won't let you go empty-handed. Bring me a piece of &6bread&f — to treat the fox.",
      "actions": [
        { "type": "play_emote", "emote": "crossed_arms", "duration_ticks": 40 }
      ],
      "options": [
        { "text": "&7Alright, I'll bring some.", "next": "come_back" },
        { "text": "&cAnd what if I take it by force?", "next": "threat" }
      ]
    },
    "come_back": {
      "text": "&fGo on. I'm not going anywhere. &7*nods toward the forest*",
      "next": null,
      "actions": [
        { "type": "play_emote", "emote": "nod", "duration_ticks": 20 },
        { "type": "start_quest", "quest": {
            "id": "harold_bread",
            "title": "Bread for Rusty",
            "description": "Harold asks to bring bread for his fox Rusty — only then will he hand over the found watch.",
            "objectives": ["Bring 1 bread to Harold"]
          }
        }
      ]
    },
    "threat": {
      "text": "&7*Harold puts his hand on his axe* &fGive it a try. Just don't complain later.",
      "next": null,
      "actions": [
        { "type": "play_emote", "emote": "crossed_arms", "duration_ticks": 50 }
      ]
    },
    "offer_bread": {
      "text": "&fAh, you're back. &7*looks with a squint* &fDid you bring the bread?",
      "actions": [
        { "type": "play_emote", "emote": "crossed_arms", "duration_ticks": 30 }
      ],
      "options": [
        { "text": "&aHere it is, as promised.",
          "next": "give_amulet",
          "condition": { "type": "has_item", "item": "minecraft:bread", "count": 1 },
          "lock_reason": "&8(requires 1× bread)"
        },
        { "text": "&7Haven't found it yet.", "next": "wait_more" },
        { "text": "&cCan we do without the bread?", "next": "threat" }
      ]
    },
    "wait_more": {
      "text": "&fNo rush. Rusty will wait, and so will I.",
      "next": null,
      "actions": [
        { "type": "play_emote", "emote": "shrug", "duration_ticks": 25 }
      ]
    },
    "give_amulet": {
      "text": "&fHere, take the watch. Rusty, come here, have a treat... &7*tosses the bread to the fox* &fSend Elsa my regards.",
      "next": "farewell",
      "actions": [
        { "type": "remove_item", "item": "minecraft:bread", "count": 1 },
        { "type": "complete_quest", "quest_id": "harold_bread" },
        { "type": "give_item", "item": "minecraft:clock", "count": 1 },
        { "type": "play_emote", "emote": "handshake", "duration_ticks": 35 },
        { "type": "play_sound", "sound": "minecraft:entity.fox.ambient", "volume": 1.0, "pitch": 1.0 }
      ]
    },
    "farewell": {
      "text": "&fAnd... tell her to send some honey next time. I respect her herbal tinctures.",
      "next": null,
      "actions": [
        { "type": "play_emote", "emote": "shrug", "duration_ticks": 30 }
      ]
    },
    "hub_idle": {
      "text": "&fYou again. What do you want?",
      "actions": [
        { "type": "play_emote", "emote": "crossed_arms", "duration_ticks": 30 }
      ],
      "options": [
        { "text": "&7Tell me about the forest.", "next": "lore" },
        { "text": "&7Bye.", "next": null }
      ]
    },
    "hub_after_amulet": {
      "text": "&fDelivered the watch to the herbalist? You did a good deed.",
      "actions": [
        { "type": "nod", "duration_ticks": 25 }
      ],
      "options": [
        { "text": "&7Tell me about the forest.", "next": "lore" },
        { "text": "&7Bye.", "next": null }
      ]
    },
    "lore": {
      "text": "&fI've lived here for twenty winters. My father before me. His father before him. The forest feeds us, and we don't touch it without need. That's all there is to say.",
      "next": null,
      "actions": [
        { "type": "play_emote", "emote": "shrug", "duration_ticks": 40 }
      ]
    }
  }
}
```

### 35.3 Что положить в файлы

```
config/interactentity/dialogues/story/elsa.json
config/interactentity/dialogues/story/harold.json
config/interactentity/skins/elsa.png             ← 64x64
config/interactentity/skins/harold.png           ← 64x64
```

После запуска: `/dialogue reload`, затем подождите спавна Эльзы (или призовите её вручную). Поговорите с Эльзой → начнется квест → Эльза заспавнит Гарольда в лесу → найдите Гарольда → принесите ему хлеб, чтобы забрать часы → верните часы Эльзе для получения награды.

### 35.4 KubeJS-хук на событие

```js
// kubejs/server_scripts/lost_amulet.js

ForgeEvents.onEvent('net.ashpapi.interactentity.api.QuestStartEvent', event => {
  if (event.questId === 'lost_amulet') {
    event.player.tell('§5Начался таинственный квест...')
  }
})

ForgeEvents.onEvent('net.ashpapi.interactentity.api.QuestCompleteEvent', event => {
  if (event.questId === 'lost_amulet') {
    event.player.runCommandSilent('xp add @s 50 levels')
  }
})
```

---

## Четыре способа добавить NPC в мир

Когда у тебя готов JSON-диалог, нужно как-то поставить моба с подходящим именем и тегом. Есть четыре способа — выбирай удобный.

### Способ 1 — `/npc spawn` (самый простой)

```
/npc spawn my_dialogue
```

Мод сам создаст моба нужного типа с нужным именем и тегом прямо у твоих ног. Тип берётся из `summon.entity` в JSON, имя и тег — из `target`. Минус: должен быть прописан блок `summon` (для типа сущности). Плюс: один клик и готово.

### Способ 2 — `/npc tag` (превратить существующего моба)

Если у тебя уже стоит моб подходящего типа и ты хочешь сделать из него NPC — подойди и пиши:

```
/npc tag my_dialogue
```

Ближайшему мобу присвоятся нужные имя и тег. Удобно когда мобы уже расставлены.

### Способ 3 — вручную через ваниль

Если хочется без всяких команд мода:

```
/summon zombie ~ ~ ~ {CustomName:'"Моё Имя"',CustomNameVisible:1b,Tags:["my_tag"]}
```

Или если моб уже стоит:
```
/tag @e[type=minecraft:zombie,distance=..3,limit=1,sort=nearest] add my_tag
/data merge entity @e[type=minecraft:zombie,distance=..3,limit=1,sort=nearest] {CustomName:'"Моё Имя"',CustomNameVisible:1b}
```

Этот способ работает всегда, даже если в JSON нет блока `summon`.

### Способ 4 — авто-спавн через `summon` в JSON

Если хочешь чтобы NPC появлялся автоматически (без команд) — добавь блок `summon` с триггером (см. §14). Тогда моб появится сам когда сработает условие — игрок зашёл в мир, прошёл другой диалог, подошёл к точке и т.д. Лучше всего для сюжетных карт где не хочешь чтобы игрок что-то вручную делал.

---

## Шпаргалка для быстрого создания NPC

1. **JSON** → `config/interactentity/dialogues/my_npc.json` (минимальный — см. §2)
2. **Скин** (если `custom_npc`) → `<world>/interactentity/skins/my_npc.png` или `config/interactentity/skins/my_npc.png` (64×64, имя `[a-z0-9_]+`)
3. **target** → объяви `name` и `tag` (см. §5)
4. **Спавн** → один из:
   - `summon` блок в JSON с триггером (см. §14)
   - `/npc spawn my_npc` (см. §30)
   - `/summon` ванилой с CustomName и Tags
5. **Reload** → `/dialogue reload`
6. **Тест** → ПКМ по мобу

Если что-то не работает — смотри лог сервера, ищи `[InteractEntity]`, `[skins]`, `WARN`.
