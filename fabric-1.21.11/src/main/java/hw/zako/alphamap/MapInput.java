package hw.zako.alphamap;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;

@UtilityClass
public class MapInput {

    private final double MIN_ZOOM = 1.0;
    private final double MAX_ZOOM = 8.0;
    private final double ZOOM_STEP = 1.25;

    private boolean cursor;
    private double zoom = MIN_ZOOM;

    private boolean released;

    private boolean dragging;
    private double dragX;
    private double dragY;

    private double focusX = 0.5;
    private double focusY = 0.5;

    private int viewLeft;
    private int viewTop;
    private int viewSide = 1;

    public boolean cursorActive() {
        return cursor;
    }

    public double zoom() {
        return zoom;
    }

    public double focusX() {
        return focusX;
    }

    public double focusY() {
        return focusY;
    }

    public void viewport(int left, int top, int side) {
        viewLeft = left;
        viewTop = top;
        viewSide = Math.max(1, side);
    }

    public void update(Minecraft client, boolean mapOpen) {
        cursor = mapOpen && client.screen == null && client.options.keyUse.isDown();

        if (cursor) {
            if (client.mouseHandler.isMouseGrabbed()) client.mouseHandler.releaseMouse();
            released = true;
            return;
        }

        if (!released) return;
        released = false;
        if (client.screen == null) client.mouseHandler.grabMouse();
    }

    public void drag(Minecraft client) {
        if (!cursor) {
            dragging = false;
            return;
        }

        double x = cursorX(client);
        double y = cursorY(client);
        boolean down = client.options.keyAttack.isDown();

        if (down && dragging) {
            focusX = clampFocus(focusX - (x - dragX) / (viewSide * zoom));
            focusY = clampFocus(focusY - (y - dragY) / (viewSide * zoom));
        }

        dragging = down;
        dragX = x;
        dragY = y;
    }

    public double cursorX(Minecraft client) {
        return client.mouseHandler.xpos() * guiScale(client);
    }

    public double cursorY(Minecraft client) {
        return client.mouseHandler.ypos() * guiScale(client);
    }

    private double guiScale(Minecraft client) {
        return (double) client.getWindow().getGuiScaledWidth() / client.getWindow().getWidth();
    }

    public void reset(Minecraft client) {
        cursor = false;
        if (released) {
            released = false;
            if (client.screen == null) client.mouseHandler.grabMouse();
        }
        dragging = false;
        zoom = MIN_ZOOM;
        focusX = 0.5;
        focusY = 0.5;
    }

    public void scroll(double delta) {
        if (delta == 0) return;

        Minecraft client = Minecraft.getInstance();
        double cursorX = cursorX(client);
        double cursorY = cursorY(client);

        double atX = atlasAt(focusX, cursorX, viewLeft);
        double atY = atlasAt(focusY, cursorY, viewTop);

        zoom = Math.clamp(delta > 0 ? zoom * ZOOM_STEP : zoom / ZOOM_STEP, MIN_ZOOM, MAX_ZOOM);

        focusX = clampFocus(atX - (cursorX - viewLeft - viewSide / 2.0) / (viewSide * zoom));
        focusY = clampFocus(atY - (cursorY - viewTop - viewSide / 2.0) / (viewSide * zoom));
    }

    private double atlasAt(double focus, double cursor, int edge) {
        return focus + (cursor - edge - viewSide / 2.0) / (viewSide * zoom);
    }

    private double clampFocus(double focus) {
        double half = 0.5 / zoom;
        return Math.clamp(focus, half, 1 - half);
    }
}
