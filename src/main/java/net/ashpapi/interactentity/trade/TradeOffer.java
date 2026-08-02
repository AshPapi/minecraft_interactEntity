package net.ashpapi.interactentity.trade;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.ashpapi.interactentity.InteractEntityMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Одна позиция витрины торговца.
 *
 * type = "buy": игрок платит priceStacks, получает resultStack.
 * type = "sell": игрок отдаёт merchandiseStack, получает priceStacks.
 *
 * stock: -1 = бесконечно, иначе лимит числа сделок (хранится в прогрессе).
 * stockScope: "per_player" (дефолт) или "global" — где хранить счётчик.
 * condition: необязательное условие из ConditionRegistry (фильтруется на сервере).
 *
 * В сеть condition не шлётся — клиент получает уже отфильтрованный список.
 * При отправке поле stock заменяется на ТЕКУЩИЙ остаток (для отображения).
 */
public class TradeOffer {

    public static final int INFINITE_STOCK = -1;

    private final String type;                  // "buy" | "sell"
    private final ItemStack resultStack;        // для buy
    private final ItemStack merchandiseStack;   // для sell
    private final List<ItemStack> priceStacks;
    private final String info;
    private final String description;
    private final int stock;                    // -1 = бесконечно (исходный лимит)
    private final String stockScope;            // "per_player" | "global"
    @Nullable
    private final JsonObject condition;
    private final int sourceIndex;              // позиция в исходном файле витрины; -1 = не задана

    public TradeOffer(String type, ItemStack resultStack, ItemStack merchandiseStack,
                      List<ItemStack> priceStacks, String info, String description,
                      int stock, String stockScope, @Nullable JsonObject condition) {
        this(type, resultStack, merchandiseStack, priceStacks, info, description, stock, stockScope, condition, -1);
    }

    public TradeOffer(String type, ItemStack resultStack, ItemStack merchandiseStack,
                      List<ItemStack> priceStacks, String info, String description,
                      int stock, String stockScope, @Nullable JsonObject condition, int sourceIndex) {
        this.type = type;
        this.resultStack = resultStack;
        this.merchandiseStack = merchandiseStack;
        this.priceStacks = priceStacks;
        this.info = info;
        this.description = description;
        this.stock = stock;
        this.stockScope = stockScope;
        this.condition = condition;
        this.sourceIndex = sourceIndex;
    }

    public String getType() { return type; }
    public boolean isBuy() { return "buy".equals(type); }
    public boolean isSell() { return "sell".equals(type); }
    public ItemStack getResultStack() { return resultStack; }
    public ItemStack getMerchandiseStack() { return merchandiseStack; }
    public List<ItemStack> getPriceStacks() { return priceStacks; }
    public String getInfo() { return info; }
    /** Описание товара для панели деталей (прокручивается, если длинное). */
    public String getDescription() { return description; }
    public int getStock() { return stock; }
    public boolean isInfinite() { return stock == INFINITE_STOCK; }
    public String getStockScope() { return stockScope; }
    @Nullable public JsonObject getCondition() { return condition; }
    /** Индекс оффера в файле витрины: клиент шлёт его обратно, т.к. его список отфильтрован. */
    public int getSourceIndex() { return sourceIndex; }

    /** «Товар» для покупателя — то, что он получает/видит как главный предмет строки. */
    public ItemStack getDisplayStack() {
        return isBuy() ? resultStack : merchandiseStack;
    }

    public static TradeOffer fromJson(JsonObject json) {
        String type = json.has("type") ? json.get("type").getAsString() : "buy";

        ItemStack result = ItemStack.EMPTY;
        if (json.has("result")) {
            result = parseStack(json.getAsJsonObject("result"));
        }

        ItemStack merchandise = ItemStack.EMPTY;
        if (json.has("merchandise")) {
            merchandise = parseStack(json.getAsJsonObject("merchandise"));
        }

        List<ItemStack> prices = new ArrayList<>();
        if (json.has("price")) {
            for (JsonElement el : json.getAsJsonArray("price")) {
                prices.add(parseStack(el.getAsJsonObject()));
            }
        }

        String info = json.has("info") ? json.get("info").getAsString() : "";
        String description = json.has("description") ? json.get("description").getAsString() : "";
        int stock = json.has("stock") ? json.get("stock").getAsInt() : INFINITE_STOCK;
        String stockScope = json.has("stock_scope") ? json.get("stock_scope").getAsString() : "per_player";
        JsonObject condition = json.has("condition") && json.get("condition").isJsonObject()
                ? json.getAsJsonObject("condition") : null;

        return new TradeOffer(type, result, merchandise, prices, info, description, stock, stockScope, condition);
    }

    /** Парсит { "item": "minecraft:emerald", "count": 2, "nbt": "{...}" } в ItemStack. */
    public static ItemStack parseStack(JsonObject json) {
        String itemId = json.get("item").getAsString();
        int count = json.has("count") ? json.get("count").getAsInt() : 1;
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
        if (item == null) {
            InteractEntityMod.LOGGER.warn("Unknown item in trade offer: {}", itemId);
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item, count);
        if (json.has("nbt") && json.get("nbt").isJsonPrimitive()) {
            try {
                CompoundTag tag = TagParser.parseTag(json.get("nbt").getAsString());
                stack.setTag(tag);
            } catch (Exception e) {
                InteractEntityMod.LOGGER.warn("Failed to parse nbt for trade item '{}': {}", itemId, e.getMessage());
            }
        }
        return stack;
    }

    // ===================== Сетевая сериализация =====================
    // condition не шлётся (клиент получает уже отфильтрованный список).
    // stock здесь — ТЕКУЩИЙ остаток для отображения (или -1).

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(type);
        buf.writeItem(resultStack);
        buf.writeItem(merchandiseStack);
        buf.writeVarInt(priceStacks.size());
        for (ItemStack price : priceStacks) buf.writeItem(price);
        buf.writeUtf(info);
        buf.writeUtf(description);
        buf.writeInt(stock);
        buf.writeInt(sourceIndex);
    }

    public static TradeOffer decode(FriendlyByteBuf buf) {
        String type = buf.readUtf();
        ItemStack result = buf.readItem();
        ItemStack merchandise = buf.readItem();
        int priceCount = buf.readVarInt();
        List<ItemStack> prices = new ArrayList<>(priceCount);
        for (int i = 0; i < priceCount; i++) prices.add(buf.readItem());
        String info = buf.readUtf();
        String description = buf.readUtf();
        int stock = buf.readInt();
        int sourceIndex = buf.readInt();
        // condition = null, stockScope не нужен на клиенте (отображение только)
        return new TradeOffer(type, result, merchandise, prices, info, description, stock, "per_player", null, sourceIndex);
    }

    /**
     * Копия оффера для отправки клиенту: stock = текущий остаток (или -1), condition = null,
     * sourceIndex = позиция в файле витрины (список у клиента отфильтрован, его индексы не совпадают).
     */
    public TradeOffer forClientView(int sourceIndex, int currentStockRemaining) {
        return new TradeOffer(type, resultStack.copy(), merchandiseStack.copy(),
                priceStacks.stream().map(ItemStack::copy).toList(),
                info, description, currentStockRemaining, stockScope, null, sourceIndex);
    }
}
