# Changelog

## Unreleased

### Added
- Trading: the `merchant` node flag turns an NPC into a shopkeeper, shop files live in `config/interactentity/trades/`. Offers support `buy`/`sell`, multi-item prices, `stock` (`per_player`/`global`) and `condition`.
- Trade shop UI on top of the dialogue window: a pouch icon in the top-right corner opens a grid of goods, clicking an item slides an info panel out to the right (preview, price, stock, `description` with its own scrolling), the trade button sits on both the card and the panel.
- New `description` field on a trade offer — item description under the price.

### Changed
- Improved default NPC poses: richer idle/sitting/sleeping/sneaking/swimming animations (`custom_npc_default.animation.json`), extended pose logic in `CustomNpcEntity`, `CustomNpcModel`, `CustomNpcRenderer`, `PeacefulMobHandler`.
- README: documented `is_moving` and added a demo example for `set_pose`.

### Fixed
- Dedicated server crash on `RuntimeDistCleaner` when opening a dialogue: client-only code (`instanceof`/`new` of client classes) moved out of network packet classes into `ClientPacketHandler`, invoked via `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)`.
- `if_var` condition: a non-existent variable compared to a number in `eq`/`neq` is now treated as `0`, matching the existing behavior of `gt`/`lt`/`gte`/`lte`.

---

# Чейнджлог

## Unreleased

### Добавлено
- Торговля: метка `merchant` на ноде делает NPC торговцем, файлы витрин лежат в `config/interactentity/trades/`. У офферов есть `buy`/`sell`, цена из нескольких предметов, `stock` (`per_player`/`global`) и `condition`.
- Интерфейс витрины поверх окна диалога: иконка-мешочек в правом верхнем углу открывает сетку товаров, клик по товару выдвигает вправо панель с информацией (превью, цена, остаток, `description` со своей прокруткой), кнопка сделки есть и на карточке, и в панели.
- Новое поле оффера `description` — описание товара под ценой.

### Изменено
- Улучшены дефолтные позы NPC: более богатые анимации idle/sitting/sleeping/sneaking/swimming (`custom_npc_default.animation.json`), расширена логика поз в `CustomNpcEntity`, `CustomNpcModel`, `CustomNpcRenderer`, `PeacefulMobHandler`.
- README: добавлено пояснение `is_moving` и демо-пример для `set_pose`.

### Исправлено
- Краш на dedicated-сервере (`RuntimeDistCleaner`) при открытии диалога: клиентский код (`instanceof`/`new` клиентских классов) вынесен из классов сетевых пакетов в `ClientPacketHandler`, вызывается через `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)`.
- Условие `if_var`: несуществующая переменная при сравнении с числом в `eq`/`neq` теперь считается `0`, как уже было реализовано для `gt`/`lt`/`gte`/`lte`.
