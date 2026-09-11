package hw.zako.alphamap;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class Vanilla {

    public @Nullable Screen screen(Minecraft client) {
        return client.gui.screen();
    }

    public void setScreen(Minecraft client, @Nullable Screen screen) {
        client.gui.setScreen(screen);
    }

    public boolean hudHidden(Minecraft client) {
        return client.gui.hud.isHidden();
    }

    public @Nullable Camera camera(Minecraft client) {
        Camera camera = client.gameRenderer.mainCamera();
        return camera.isInitialized() ? camera : null;
    }
}
