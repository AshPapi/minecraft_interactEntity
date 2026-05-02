package net.ashpapi.interactentity.npc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class NpcRoutine {
    private final String type; // "idle_at", "wander", "patrol"
    private final int startTime; // game time of day (0-24000)
    private final int endTime;
    @Nullable
    private final BlockPos position; // for idle_at
    private final int radius; // for wander
    private final List<BlockPos> waypoints; // for patrol

    public NpcRoutine(String type, int startTime, int endTime, @Nullable BlockPos position, int radius, List<BlockPos> waypoints) {
        this.type = type;
        this.startTime = startTime;
        this.endTime = endTime;
        this.position = position;
        this.radius = radius;
        this.waypoints = waypoints;
    }

    public String getType() { return type; }
    public int getStartTime() { return startTime; }
    public int getEndTime() { return endTime; }
    @Nullable public BlockPos getPosition() { return position; }
    public int getRadius() { return radius; }
    public List<BlockPos> getWaypoints() { return waypoints; }

    public boolean isActiveAt(long dayTime) {
        int time = (int) (dayTime % 24000);
        if (startTime <= endTime) {
            return time >= startTime && time < endTime;
        } else {
            return time >= startTime || time < endTime;
        }
    }

    public static NpcRoutine fromJson(JsonObject json) {
        String type = json.get("type").getAsString();
        int start = json.has("start") ? json.get("start").getAsInt() : 0;
        int end = json.has("end") ? json.get("end").getAsInt() : 24000;

        BlockPos pos = null;
        if (json.has("x") && json.has("y") && json.has("z")) {
            pos = new BlockPos(json.get("x").getAsInt(), json.get("y").getAsInt(), json.get("z").getAsInt());
        }

        int radius = json.has("radius") ? json.get("radius").getAsInt() : 8;

        List<BlockPos> waypoints = new ArrayList<>();
        if (json.has("waypoints")) {
            JsonArray arr = json.getAsJsonArray("waypoints");
            for (int i = 0; i < arr.size(); i++) {
                JsonObject wp = arr.get(i).getAsJsonObject();
                waypoints.add(new BlockPos(wp.get("x").getAsInt(), wp.get("y").getAsInt(), wp.get("z").getAsInt()));
            }
        }

        return new NpcRoutine(type, start, end, pos, radius, waypoints);
    }
}
