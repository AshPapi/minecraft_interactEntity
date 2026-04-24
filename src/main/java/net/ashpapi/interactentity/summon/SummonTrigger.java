package net.ashpapi.interactentity.summon;

import com.google.gson.JsonObject;

public class SummonTrigger {
    private final String type;
    private final String dialogueId;
    private final int delay;
    private final double x, y, z;
    private final double radius;
    private final int seconds;

    public SummonTrigger(String type, String dialogueId, int delay,
                         double x, double y, double z, double radius, int seconds) {
        this.type = type;
        this.dialogueId = dialogueId;
        this.delay = delay;
        this.x = x; this.y = y; this.z = z;
        this.radius = radius;
        this.seconds = seconds;
    }

    public String getType() { return type; }
    public String getDialogueId() { return dialogueId; }
    public int getDelay() { return delay; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public double getRadius() { return radius; }
    public int getSeconds() { return seconds; }

    public static SummonTrigger fromJson(JsonObject json) {
        String type = json.get("type").getAsString();
        String dialogueId = json.has("dialogue_id") ? json.get("dialogue_id").getAsString() : "";
        int delay = json.has("delay") ? json.get("delay").getAsInt() : 0;
        double x = json.has("x") ? json.get("x").getAsDouble() : 0;
        double y = json.has("y") ? json.get("y").getAsDouble() : 0;
        double z = json.has("z") ? json.get("z").getAsDouble() : 0;
        double radius = json.has("radius") ? json.get("radius").getAsDouble() : 8.0;
        int seconds = json.has("seconds") ? json.get("seconds").getAsInt() : 2;
        return new SummonTrigger(type, dialogueId, delay, x, y, z, radius, seconds);
    }
}
