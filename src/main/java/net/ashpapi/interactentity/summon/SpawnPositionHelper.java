package net.ashpapi.interactentity.summon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class SpawnPositionHelper {

    @Nullable
    public static Vec3 findBehindPlayer(ServerPlayer player, ServerLevel level) {
        float yaw = player.getYRot();
        // Behind player = opposite of look direction
        float behindYaw = yaw + 180.0f;
        float radians = behindYaw * Mth.DEG_TO_RAD;

        double baseX = player.getX() - Mth.sin(radians) * 3.0;
        double baseZ = player.getZ() + Mth.cos(radians) * 3.0;

        // Try distances 3, 2, 4 blocks behind
        for (double dist : new double[]{3.0, 2.0, 4.0}) {
            double x = player.getX() - Mth.sin(radians) * dist;
            double z = player.getZ() + Mth.cos(radians) * dist;
            BlockPos groundPos = findSafeGround(level, x, player.getY(), z);
            if (groundPos != null) {
                return new Vec3(x, groundPos.getY() + 1.0, z);
            }
        }

        // Fallback: just use 3 blocks behind at player Y
        return new Vec3(baseX, player.getY(), baseZ);
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
