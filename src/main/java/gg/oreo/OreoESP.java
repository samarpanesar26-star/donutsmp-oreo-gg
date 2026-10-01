package gg.oreo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
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
        // Debug-style line overlays are safest in this render stage on 1.21.11.
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(OreoESP::render);
    }

    public static void tick(Minecraft client) {
        if (client.level == null || client.player == null) return;

        boolean storageOn = OreoScreen.isModuleEnabled("StorageFinder");
        boolean spawnerOn = OreoScreen.isModuleEnabled("SpawnerFinder");

        if (!storageOn && !spawnerOn) {
            STORAGE.clear();
            SPAWNERS.clear();
            return;
        }

        long gameTime = client.level.getGameTime();
        if (gameTime - lastScanTick < 10L) return;
        lastScanTick = gameTime;

        STORAGE.clear();
        SPAWNERS.clear();

        int centerChunkX = client.player.blockPosition().getX() >> 4;
        int centerChunkZ = client.player.blockPosition().getZ() >> 4;
        int radius = Math.min(10, Math.max(4, client.options.renderDistance().get() / 2));

        for (int cx = centerChunkX - radius; cx <= centerChunkX + radius; cx++) {
            for (int cz = centerChunkZ - radius; cz <= centerChunkZ + radius; cz++) {
                if (!client.level.hasChunk(cx, cz)) continue;

                LevelChunk chunk = client.level.getChunk(cx, cz);
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    BlockPos pos = entry.getKey();
                    BlockState state = client.level.getBlockState(pos);
                    BlockEntity entity = entry.getValue();

                    if (spawnerOn && (state.is(Blocks.SPAWNER) || state.is(Blocks.TRIAL_SPAWNER))) {
                        SPAWNERS.add(pos.immutable());
                    } else if (storageOn && entity instanceof Container) {
                        STORAGE.add(pos.immutable());
                    }
                }
            }
        }
    }

    private static void render(WorldRenderContext context) {
        if (!OreoScreen.isModuleEnabled("StorageFinder")
                && !OreoScreen.isModuleEnabled("SpawnerFinder")) return;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        Vec3 camera = context.worldState().cameraRenderState.pos;
        MultiBufferSource consumers = context.consumers();
        if (consumers == null) return;

        VertexConsumer consumer = consumers.getBuffer(RenderTypes.lines());
        PoseStack matrices = context.matrices();
        PoseStack.Pose pose = matrices.last();

        if (OreoScreen.isModuleEnabled("StorageFinder")) {
            renderBoxesRelative(pose, consumer, STORAGE, camera, 0.15f, 0.85f, 1.0f);
        }
        if (OreoScreen.isModuleEnabled("SpawnerFinder")) {
            renderBoxesRelative(pose, consumer, SPAWNERS, camera, 1.0f, 0.35f, 0.15f);
        }
    }

    private static void renderBoxesRelative(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            List<BlockPos> positions,
            Vec3 camera,
            float red,
            float green,
            float blue) {

        for (BlockPos pos : positions) {
            float x0 = (float) (pos.getX() - camera.x);
            float y0 = (float) (pos.getY() - camera.y);
            float z0 = (float) (pos.getZ() - camera.z);
            float x1 = x0 + 1.0f;
            float y1 = y0 + 1.0f;
            float z1 = z0 + 1.0f;

            line(consumer, pose, x0,y0,z0, x1,y0,z0, red,green,blue);
            line(consumer, pose, x0,y0,z0, x0,y0,z1, red,green,blue);
            line(consumer, pose, x1,y0,z0, x1,y0,z1, red,green,blue);
            line(consumer, pose, x0,y0,z1, x1,y0,z1, red,green,blue);

            line(consumer, pose, x0,y1,z0, x1,y1,z0, red,green,blue);
            line(consumer, pose, x0,y1,z0, x0,y1,z1, red,green,blue);
            line(consumer, pose, x1,y1,z0, x1,y1,z1, red,green,blue);
            line(consumer, pose, x0,y1,z1, x1,y1,z1, red,green,blue);

            line(consumer, pose, x0,y0,z0, x0,y1,z0, red,green,blue);
            line(consumer, pose, x1,y0,z0, x1,y1,z0, red,green,blue);
            line(consumer, pose, x0,y0,z1, x0,y1,z1, red,green,blue);
            line(consumer, pose, x1,y0,z1, x1,y1,z1, red,green,blue);
        }
    }

    private static void line(VertexConsumer consumer, PoseStack.Pose pose,
                             float x0,float y0,float z0, float x1,float y1,float z1,
                             float red,float green,float blue) {
        consumer.addVertex(pose, x0,y0,z0).setColor(red,green,blue,1.0f);
        consumer.addVertex(pose, x1,y1,z1).setColor(red,green,blue,1.0f);
    }
}
