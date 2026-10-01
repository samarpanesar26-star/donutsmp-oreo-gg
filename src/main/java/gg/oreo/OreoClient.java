package gg.oreo;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class OreoClient implements ClientModInitializer {
    private static final KeyMapping.Category OREO_CATEGORY =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath("oreo_gg", "main"));

    private static final KeyMapping OPEN_GUI = KeyMappingHelper.registerKeyMapping(
        new KeyMapping(
            "key.oreo.open_gui",
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            OREO_CATEGORY
        )
    );

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_GUI.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new OreoScreen());
                }
            }
        });
    }
}
