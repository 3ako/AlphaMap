package hw.zako.alphamap;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class Canvas {

    GuiGraphicsExtractor graphics;

    public int width() {
        return graphics.guiWidth();
    }

    public int height() {
        return graphics.guiHeight();
    }

    public void scissorOn(int left, int top, int right, int bottom) {
        graphics.enableScissor(left, top, right, bottom);
    }

    public void scissorOff() {
        graphics.disableScissor();
    }

    public void push(float x, float y, float scale) {
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
    }

    public void push(float x, float y, float scale, float radians) {
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.rotate(radians);
        pose.scale(scale, scale);
    }

    public void pop() {
        graphics.pose().popMatrix();
    }

    public void fill(int left, int top, int right, int bottom, int colour) {
        graphics.fill(left, top, right, bottom, colour);
    }

    public void blit(Identifier texture, int x, int y, int size, int tint) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0f, 0.0f, size, size, size, size, tint);
    }

    public void text(Font font, String text, int x, int y, int colour) {
        graphics.text(font, text, x, y, colour);
    }

    public void centered(Font font, String text, int x, int y, int colour) {
        graphics.centeredText(font, text, x, y, colour);
    }

    public void centered(Font font, Component text, int x, int y, int colour) {
        graphics.centeredText(font, text, x, y, colour);
    }
}
