package net.ashpapi.interactentity.summon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Locale;

public class SpawnPositionHelper {

    @Nullable
    public static Vec3 findForConfig(ServerPlayer player, ServerLevel level, String spawnPosition) {
        String normalized = spawnPosition == null ? "behind_player" : spawnPosition.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "front_player", "in_front_of_player" -> findInFrontOfPlayer(player, level);
            case "at_player", "player" -> findAtPlayer(player, level);
            default -> findBehindPlayer(player, level);
        };
    }

    @Nullable
    public static Vec3 findBehindPlayer(ServerPlayer player, ServerLevel level) {
        return findRelativeToPlayer(player, level, player.getYRot() + 180.0f);
    }

    @Nullable
    public static Vec3 findInFrontOfPlayer(ServerPlayer player, ServerLevel level) {
        return findRelativeToPlayer(player, level, player.getYRot());
    }

    @Nullable
    public static Vec3 findAtPlayer(ServerPlayer player, ServerLevel level) {
        BlockPos groundPos = findSafeGround(level, player.getX(), player.getY(), player.getZ());
        if (groundPos != null) {
            return new Vec3(player.getX(), groundPos.getY() + 1.0, player.getZ());
        }
        return player.position();
    }

    @Nullable
    private static Vec3 findRelativeToPlayer(ServerPlayer player, ServerLevel level, float yaw) {
        float radians = yaw * Mth.DEG_TO_RAD;
        double fallbackX = player.getX() - Mth.sin(radians) * 3.0;
        double fallbackZ = player.getZ() + Mth.cos(radians) * 3.0;

        for (double dist : new double[]{3.0, 2.0, 4.0}) {
            double x = player.getX() - Mth.sin(radians) * dist;
            double z = player.getZ() + Mth.cos(radians) * dist;
            BlockPos groundPos = findSafeGround(level, x, player.getY(), z);
            if (groundPos != null) {
                return new Vec3(x, groundPos.getY() + 1.0, z);
            }
        }

        return new Vec3(fallbackX, player.getY(), fallbackZ);
    }

    @Nullable
    private static BlockPos findSafeGround(ServerLevel level, double x, double playerY, double z) {
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        int startY = Mth.floor(playerY);

        // Search up/down from player Y for solid ground + 2 air blocks above
        for (int dy = 0; dy <= 5; dy++) {
            for (int sign : new int[]{0, -1, 1}) {
                int y = startY + dy * (sign == 0 ? 0 : sign);
                if (dy == 0 && sign != 0) continue;

                BlockPos below = new BlockPos(bx, y - 1, bz);
                BlockPos feet = new BlockPos(bx, y, bz);
                BlockPos head = new BlockPos(bx, y + 1, bz);

                BlockState belowState = level.getBlockState(below);
                BlockState feetState = level.getBlockState(feet);
                BlockState headState = level.getBlockState(head);

                if (belowState.isSolidRender(level, below)
                        && !feetState.isSolidRender(level, feet)
                        && !headState.isSolidRender(level, head)) {
                    return below;
                }
            }
        }

        return null;
    }
}
