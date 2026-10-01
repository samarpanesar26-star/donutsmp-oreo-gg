package gg.oreo;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class OreoClient implements ClientModInitializer {
    private static final KeyMapping.Category OREO_CATEGORY =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath("oreo_gg", "main"));

    private static final KeyMapping OPEN_GUI = KeyBindingHelper.registerKeyBinding(
        new KeyMapping("key.oreo.open_gui", GLFW.GLFW_KEY_RIGHT_SHIFT, OREO_CATEGORY)
    );

    private static final KeyMapping TOGGLE_FREECAM = KeyBindingHelper.registerKeyBinding(
        new KeyMapping("key.oreo.toggle_freecam", GLFW.GLFW_KEY_F6, OREO_CATEGORY)
    );

    @Override
    public void onInitializeClient() {
        OreoESP.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_GUI.consumeClick()) {
                if (client.screen == null) client.setScreen(new OreoScreen());
            }

            while (TOGGLE_FREECAM.consumeClick()) {
                boolean next = !OreoScreen.isModuleEnabled("StaffCamera");
                OreoScreen.setModuleEnabled("StaffCamera", next);
                if (next) Freecam.enable(client);
                else Freecam.disable(client);
            }

            Freecam.tick(client);
            OreoESP.tick(client);
        });
    }
}
