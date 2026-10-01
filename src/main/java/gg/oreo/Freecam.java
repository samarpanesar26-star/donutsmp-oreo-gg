package gg.oreo;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class Freecam {
    private static RemotePlayer camera;
    private static Vec3 returnPos;
    private static float returnYaw;
    private static float returnPitch;

    private Freecam() {}

    public static void enable(Minecraft client) {
        if (camera != null || client.player == null || client.level == null) return;

        returnPos = client.player.position();
        returnYaw = client.player.getYRot();
        returnPitch = client.player.getXRot();

        GameProfile profile = new GameProfile(UUID.randomUUID(), "OreoCamera");
        camera = new RemotePlayer(client.level, profile);
        camera.setPos(returnPos);
        camera.setYRot(returnYaw);
        camera.setXRot(returnPitch);
        camera.setYHeadRot(returnYaw);
        camera.setNoGravity(true);
        camera.setInvisible(true);

        client.level.addEntity(camera);
        client.setCameraEntity(camera);
    }

    public static void disable(Minecraft client) {
        if (camera == null) return;

        if (client.level != null) {
            client.level.removeEntity(
                    camera.getId(),
                    net.minecraft.world.entity.Entity.RemovalReason.DISCARDED
            );
        }

        if (client.player != null && returnPos != null) {
            client.player.setPos(returnPos);
            client.player.setYRot(returnYaw);
            client.player.setXRot(returnPitch);
            client.player.setYHeadRot(returnYaw);
            client.player.setDeltaMovement(Vec3.ZERO);
            client.setCameraEntity(client.player);
        }

        camera = null;
        returnPos = null;
    }

    public static void tick(Minecraft client) {
        if (camera == null || client.player == null || client.level == null) return;

        if (client.screen != null) return;

        // Let the normal mouse input control the camera view while the real
        // player remains parked at the starting location.
        double speed = client.options.keySprint.isDown() ? 0.9D : 0.35D;
        double vertical = 0.0D;

        if (client.options.keyJump.isDown()) vertical += speed;
        if (client.options.keyShift.isDown()) vertical -= speed;

        float yaw = camera.getYRot();
        double radians = Math.toRadians(yaw);
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        double rightX = Math.cos(radians);
        double rightZ = Math.sin(radians);

        double x = 0.0D;
        double z = 0.0D;

        if (client.options.keyUp.isDown()) {
            x += forwardX;
            z += forwardZ;
        }
        if (client.options.keyDown.isDown()) {
            x -= forwardX;
            z -= forwardZ;
        }
        if (client.options.keyLeft.isDown()) {
            x -= rightX;
            z -= rightZ;
        }
        if (client.options.keyRight.isDown()) {
            x += rightX;
            z += rightZ;
        }

        double length = Math.sqrt(x * x + z * z);
        if (length > 0.0D) {
            x /= length;
            z /= length;
        }

        camera.setPos(
            camera.getX() + x * speed,
            camera.getY() + vertical,
            camera.getZ() + z * speed
        );
        camera.setDeltaMovement(Vec3.ZERO);

        if (returnPos != null) {
            client.player.setPos(returnPos);
            client.player.setDeltaMovement(Vec3.ZERO);
        }
    }

    public static boolean isActive() {
        return camera != null;
    }
}
