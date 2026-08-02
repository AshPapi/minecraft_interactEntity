package net.ashpapi.interactentity.trade;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Файл-витрина: shop_name + список офферов.
 * Не содержит target — привязка к персонажу делается меткой "merchant" в файле диалога.
 */
public class TradeShop {

    private final String id;
    private final String shopName;
    private final List<TradeOffer> offers;

    public TradeShop(String id, String shopName, List<TradeOffer> offers) {
        this.id = id;
        this.shopName = shopName;
        this.offers = offers;
    }

    public String getId() { return id; }
    public String getShopName() { return shopName; }
    public List<TradeOffer> getOffers() { return offers; }

    public static TradeShop fromJson(String id, JsonObject json) {
        String shopName = json.has("shop_name") ? json.get("shop_name").getAsString() : "";
        List<TradeOffer> offers = new ArrayList<>();
        if (json.has("offers")) {
            for (JsonElement el : json.getAsJsonArray("offers")) {
                offers.add(TradeOffer.fromJson(el.getAsJsonObject()));
            }
        }
        return new TradeShop(id, shopName, Collections.unmodifiableList(offers));
    }
}
