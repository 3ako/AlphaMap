package hw.zako.alphamap;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class Vanilla {

    public @Nullable Screen screen(Minecraft client) {
        return client.screen;
    }

    public void setScreen(Minecraft client, @Nullable Screen screen) {
        client.setScreen(screen);
    }

    public boolean hudHidden(Minecraft client) {
        return client.options.hideGui;
    }

    public @Nullable String mobId(Entity entity) {
        return EntityType.getKey(entity.getType()).getPath();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public @Nullable Identifier mobTexture(Minecraft client, Entity entity) {
        EntityRenderer renderer = client.getEntityRenderDispatcher().getRenderer(entity);
        if (!(renderer instanceof LivingEntityRenderer living)) return null;
        try {
            return living.getTextureLocation(
                    (LivingEntityRenderState) renderer.createRenderState(entity, 1.0f));
        } catch (RuntimeException e) {
            return null;
        }
    }

    public @Nullable Camera camera(Minecraft client) {
        Camera camera = client.gameRenderer.getMainCamera();
        return camera.isInitialized() ? camera : null;
    }
}
