package gg.oreo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
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

        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        if (storageOn) {
            submitBoxes(context, matrices, STORAGE, 0.15f, 0.85f, 1.0f);
        }
        if (spawnerOn) {
            submitBoxes(context, matrices, SPAWNERS, 1.0f, 0.35f, 0.15f);
        }

        matrices.popPose();
    }

    private static void submitBoxes(
            WorldRenderContext context,
            PoseStack matrices,
            List<BlockPos> positions,
            float red,
            float green,
            float blue) {

        context.commandQueue().submitCustomGeometry(
                matrices,
                RenderTypes.lines(),
                (pose, consumer) -> renderBoxes(pose, consumer, positions, red, green, blue)
        );
    }

    private static void renderBoxes(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            List<BlockPos> positions,
            float red,
            float green,
            float blue) {

        consumer.setLineWidth(2.0f);

        for (BlockPos pos : positions) {
            float x0 = pos.getX();
            float y0 = pos.getY();
            float z0 = pos.getZ();
            float x1 = x0 + 1.0f;
            float y1 = y0 + 1.0f;
            float z1 = z0 + 1.0f;

            line(consumer, pose, x0, y0, z0, x1, y0, z0, red, green, blue);
            line(consumer, pose, x0, y0, z0, x0, y0, z1, red, green, blue);
            line(consumer, pose, x1, y0, z0, x1, y0, z1, red, green, blue);
            line(consumer, pose, x0, y0, z1, x1, y0, z1, red, green, blue);

            line(consumer, pose, x0, y1, z0, x1, y1, z0, red, green, blue);
            line(consumer, pose, x0, y1, z0, x0, y1, z1, red, green, blue);
            line(consumer, pose, x1, y1, z0, x1, y1, z1, red, green, blue);
            line(consumer, pose, x0, y1, z1, x1, y1, z1, red, green, blue);

            line(consumer, pose, x0, y0, z0, x0, y1, z0, red, green, blue);
            line(consumer, pose, x1, y0, z0, x1, y1, z0, red, green, blue);
            line(consumer, pose, x0, y0, z1, x0, y1, z1, red, green, blue);
            line(consumer, pose, x1, y0, z1, x1, y1, z1, red, green, blue);
        }
    }

    private static void line(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float red, float green, float blue) {

        consumer.addVertex(pose, x0, y0, z0).setColor(red, green, blue, 1.0f);
        consumer.addVertex(pose, x1, y1, z1).setColor(red, green, blue, 1.0f);
    }

}
