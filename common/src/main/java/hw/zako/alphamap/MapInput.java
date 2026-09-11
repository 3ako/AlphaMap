package hw.zako.alphamap;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;

@UtilityClass
public class MapInput {

    private final double MIN_ZOOM = 1.0;
    private final double MAX_MAGNIFICATION = 2.0;
    private final double ZOOM_CEILING = 32.0;
    private final double ZOOM_STEP = 1.25;

    private boolean cursor;
    private boolean engaged;
    private double zoom = MIN_ZOOM;

    private boolean released;

    private MapTool tool = MapTool.PAN;

    private boolean holding;
    private boolean pressed;
    private boolean letGo;

    private boolean useHeld;
    private boolean usePressed;
    // Карту фиксируют тем же ПКМ, что ставит метки: это нажатие метку ставить не должно.
    private boolean useSwallowed;
    private double pointerX;
    private double pointerY;
    private double dragX;
    private double dragY;

    private double focusX = 0.5;
    private double focusY = 0.5;

    private int viewLeft;
    private int viewTop;
    private int viewSide = 1;
    private double viewPixels = 1;

    public boolean cursorActive() {
        return cursor;
    }

    public boolean engaged() {
        return engaged;
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

    public MapTool tool() {
        return tool;
    }

    public void tool(MapTool chosen) {
        tool = chosen;
    }

    public boolean pressed() {
        return pressed;
    }

    public boolean holding() {
        return holding;
    }

    public boolean letGo() {
        return letGo;
    }

    public boolean usePressed() {
        return usePressed;
    }

    public double pointerX() {
        return pointerX;
    }

    public double pointerY() {
        return pointerY;
    }

    public void viewport(int left, int top, int side, double atlasPixels) {
        viewLeft = left;
        viewTop = top;
        viewSide = Math.max(1, side);
        viewPixels = Math.max(1, atlasPixels);
        zoom = Math.min(zoom, maxZoom());
    }

    public double maxZoom() {
        return Math.clamp(viewPixels * MAX_MAGNIFICATION / viewSide, MIN_ZOOM, ZOOM_CEILING);
    }

    public void update(Minecraft client, boolean mapOpen) {
        boolean use = client.options.keyUse.isDown();
        boolean asked = use || tool == MapTool.PENCIL;
        if (mapOpen && Vanilla.screen(client) == null && asked) {
            if (!engaged) useSwallowed = use;
            engaged = true;
        }

        cursor = mapOpen && Vanilla.screen(client) == null && engaged;

        if (cursor) {
            if (client.mouseHandler.isMouseGrabbed()) client.mouseHandler.releaseMouse();
            released = true;
            return;
        }

        if (!released || Vanilla.screen(client) != null) return;
        released = false;
        client.mouseHandler.grabMouse();
    }

    public void mouse(Minecraft client) {
        pointerX = cursorX(client);
        pointerY = cursorY(client);

        if (!cursor) {
            pressed = false;
            letGo = holding;
            holding = false;
            usePressed = false;
            return;
        }

        boolean use = client.options.keyUse.isDown();
        usePressed = use && !useHeld && !useSwallowed;
        if (!use) useSwallowed = false;
        useHeld = use;

        boolean down = client.options.keyAttack.isDown();
        pressed = down && !holding;
        letGo = !down && holding;

        if (down && !pressed && tool == MapTool.PAN) {
            focusX = clampFocus(focusX - (pointerX - dragX) / (viewSide * zoom));
            focusY = clampFocus(focusY - (pointerY - dragY) / (viewSide * zoom));
        }

        holding = down;
        dragX = pointerX;
        dragY = pointerY;
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
        engaged = false;
        if (released) {
            released = false;
            if (Vanilla.screen(client) == null) client.mouseHandler.grabMouse();
        }
        holding = false;
        pressed = false;
        letGo = false;
        useHeld = false;
        usePressed = false;
        useSwallowed = false;
        zoom = MIN_ZOOM;
        focusX = 0.5;
        focusY = 0.5;
        tool = MapTool.PAN;
    }

    public void scroll(double delta) {
        if (delta == 0) return;

        Minecraft client = Minecraft.getInstance();
        double cursorX = cursorX(client);
        double cursorY = cursorY(client);

        double atX = atlasAt(focusX, cursorX, viewLeft);
        double atY = atlasAt(focusY, cursorY, viewTop);

        zoom = Math.clamp(delta > 0 ? zoom * ZOOM_STEP : zoom / ZOOM_STEP, MIN_ZOOM, maxZoom());

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
