package gg.oreo;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class OreoESP {
    private static final List<BlockPos> STORAGE = new ArrayList<>();
    private static final List<BlockPos> SPAWNERS = new ArrayList<>();
    private static long lastScanTick = -1L;

    private OreoESP() {}

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(OreoESP::render);
    }

    public static void tick(Minecraft client) {
        if (client.level == null || client.player == null) return;
        if (!OreoScreen.isModuleEnabled("StorageFinder")
                && !OreoScreen.isModuleEnabled("SpawnerFinder")) {
            STORAGE.clear();
            SPAWNERS.clear();
            return;
        }

        if (client.level.getGameTime() - lastScanTick < 10L) return;
        lastScanTick = client.level.getGameTime();

        STORAGE.clear();
        SPAWNERS.clear();

        int centerChunkX = client.player.blockPosition().getX() >> 4;
        int centerChunkZ = client.player.blockPosition().getZ() >> 4;
        int radius = Math.min(8, Math.max(4, client.options.renderDistance().get() / 2));

        for (int cx = centerChunkX - radius; cx <= centerChunkX + radius; cx++) {
            for (int cz = centerChunkZ - radius; cz <= centerChunkZ + radius; cz++) {
                if (!client.level.hasChunk(cx, cz)) continue;

                LevelChunk chunk = client.level.getChunk(cx, cz);
                for (Map.Entry<BlockPos, net.minecraft.world.level.block.entity.BlockEntity> entry
                        : chunk.getBlockEntities().entrySet()) {
                    BlockPos pos = entry.getKey();
                    BlockState state = client.level.getBlockState(pos);
                    Block block = state.getBlock();

                    if (isSpawner(block)) {
                        SPAWNERS.add(pos.immutable());
                    } else if (isStorage(block)) {
                        STORAGE.add(pos.immutable());
                    }
                }
            }
        }
    }

    private static boolean isSpawner(Block block) {
        return block == Blocks.SPAWNER || block == Blocks.TRIAL_SPAWNER;
    }

    private static boolean isStorage(Block block) {
        return block instanceof ChestBlock
                || block instanceof BarrelBlock
                || block instanceof ShulkerBoxBlock
                || block instanceof EnderChestBlock
                || block instanceof HopperBlock
                || block instanceof DispenserBlock
                || block instanceof DropperBlock
                || block instanceof CrafterBlock;
    }

    private static void render(WorldRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        boolean storageOn = OreoScreen.isModuleEnabled("StorageFinder");
        boolean spawnerOn = OreoScreen.isModuleEnabled("SpawnerFinder");
        if (!storageOn && !spawnerOn) return;

        Vec3 camera = client.gameRenderer.getMainCamera().position();
        PoseStack matrices = context.matrices();
        MultiBufferSource buffers = context.consumers();

        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        if (storageOn) {
            renderList(matrices, buffers, STORAGE, client.level, 0.15f, 0.85f, 1.0f);
        }
        if (spawnerOn) {
            renderList(matrices, buffers, SPAWNERS, client.level, 1.0f, 0.35f, 0.15f);
        }

        matrices.popPose();
    }

    private static void renderList(
            PoseStack matrices,
            MultiBufferSource buffers,
            List<BlockPos> positions,
            net.minecraft.client.multiplayer.ClientLevel level,
            float red,
            float green,
            float blue) {

        var consumer = buffers.getBuffer(RenderTypes.lines());

        for (BlockPos pos : positions) {
            if (level.getBlockState(pos).isAir()) continue;

            AABB box = new AABB(
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    pos.getX() + 1.0D,
                    pos.getY() + 1.0D,
                    pos.getZ() + 1.0D
            );

            ShapeRenderer.renderLineBox(
                    matrices,
                    consumer,
                    box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ,
                    red, green, blue, 1.0f
            );
        }
    }
}
