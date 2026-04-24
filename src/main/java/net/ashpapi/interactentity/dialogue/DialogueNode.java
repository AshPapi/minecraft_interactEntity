package net.ashpapi.interactentity.dialogue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DialogueNode {
    private final String id;
    private final String text;
    private final List<String> randomTexts;
    @Nullable
    private final String nextNodeId;
    private final List<DialogueOption> options;
    private final List<JsonObject> actions;
    private final int autoNextTicks;
    private final String cameraMode;
    private final float cameraYawOffset;
    private final float cameraPitchOffset;

    public DialogueNode(String id, String text, List<String> randomTexts, @Nullable String nextNodeId,
                        List<DialogueOption> options, List<JsonObject> actions, int autoNextTicks,
                        String cameraMode, float cameraYawOffset, float cameraPitchOffset) {
        this.id = id;
        this.text = text;
        this.randomTexts = randomTexts;
        this.nextNodeId = nextNodeId;
        this.options = options;
        this.actions = actions;
        this.autoNextTicks = autoNextTicks;
        this.cameraMode = cameraMode;
        this.cameraYawOffset = cameraYawOffset;
        this.cameraPitchOffset = cameraPitchOffset;
    }

    public String getCameraMode() { return cameraMode; }
    public float getCameraYawOffset() { return cameraYawOffset; }
    public float getCameraPitchOffset() { return cameraPitchOffset; }

    public String getId() { return id; }
    public String getText() {
        if (randomTexts != null && !randomTexts.isEmpty()) {
            return randomTexts.get(ThreadLocalRandom.current().nextInt(randomTexts.size()));
        }
        return text;
    }
    @Nullable public String getNextNodeId() { return nextNodeId; }
    public List<DialogueOption> getOptions() { return options; }
    public List<JsonObject> getActions() { return actions; }
    public int getAutoNextTicks() { return autoNextTicks; }

    public boolean isLinear() { return nextNodeId != null && options.isEmpty(); }
    public boolean isChoice() { return !options.isEmpty(); }
    public boolean isEnd() { return nextNodeId == null && options.isEmpty(); }

    public static DialogueNode fromJson(String id, JsonObject json) {
        String text = json.has("text") ? json.get("text").getAsString() : "";

        List<String> randomTexts = null;
        if (json.has("random_text")) {
            randomTexts = new ArrayList<>();
            JsonArray arr = json.getAsJsonArray("random_text");
            for (JsonElement el : arr) randomTexts.add(el.getAsString());
        }

        String next = json.has("next") && !json.get("next").isJsonNull() ? json.get("next").getAsString() : null;
        int autoNext = json.has("auto_next_ticks") ? json.get("auto_next_ticks").getAsInt() : 0;

        List<DialogueOption> options = new ArrayList<>();
        if (json.has("options")) {
            JsonArray arr = json.getAsJsonArray("options");
            for (JsonElement el : arr) {
                options.add(DialogueOption.fromJson(el.getAsJsonObject()));
            }
        }

        List<JsonObject> actions = new ArrayList<>();
        if (json.has("actions")) {
            JsonArray arr = json.getAsJsonArray("actions");
            for (JsonElement el : arr) {
                actions.add(el.getAsJsonObject());
            }
        }

        String cameraMode = json.has("camera") ? json.get("camera").getAsString() : "npc";
        float yawOff = json.has("camera_yaw_offset") ? json.get("camera_yaw_offset").getAsFloat() : 0f;
        float pitchOff = json.has("camera_pitch_offset") ? json.get("camera_pitch_offset").getAsFloat() : 0f;

        return new DialogueNode(id, text,
                randomTexts == null ? null : Collections.unmodifiableList(randomTexts),
                next, Collections.unmodifiableList(options), Collections.unmodifiableList(actions),
                autoNext, cameraMode, yawOff, pitchOff);
    }
}
