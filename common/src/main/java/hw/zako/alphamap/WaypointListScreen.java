package hw.zako.alphamap;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class WaypointListScreen extends Screen {

    private static final int HEIGHT = 20;
    private static final int GAP = 22;
    private static final int COLUMN_GAP = 4;

    private static final int NAME = 150;
    private static final int WORLD = 110;
    private static final int DELETE = 80;
    private static final int PAGE = 20;

    private static final int RED = 0xFF5555;

    private final @Nullable Screen parent;

    private int page;

    public WaypointListScreen(@Nullable Screen parent) {
        super(Component.translatable("alphamap.waypoints.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        List<Waypoint> all = Waypoints.all();
        int span = NAME + WORLD + DELETE + COLUMN_GAP * 2;
        int left = (width - span) / 2;
        int top = height / 6;

        if (all.isEmpty()) {
            Button empty = Button.builder(Component.translatable("alphamap.waypoints.empty"), button -> {
            }).bounds(left, top, span, HEIGHT).build();
            empty.active = false;
            addRenderableWidget(empty);
            done(top + GAP * 2, span);
            return;
        }

        int rows = Math.max(1, (height - top - GAP * 3) / GAP);
        int pages = (all.size() + rows - 1) / rows;
        page = Math.clamp(page, 0, pages - 1);
        int from = page * rows;
        int to = Math.min(all.size(), from + rows);

        for (int i = from; i < to; i++) {
            int index = i;
            int y = top + (i - from) * GAP;

            addRenderableWidget(Button.builder(name(all.get(i)),
                            button -> Vanilla.setScreen(minecraft, WaypointScreen.of(index, this)))
                    .bounds(left, y, NAME, HEIGHT).build());

            addRenderableWidget(Button.builder(world(all.get(i)), button -> {
                Waypoint waypoint = Waypoints.all().get(index);
                Waypoints.replace(index, waypoint.worldHidden(!waypoint.worldHidden()));
                button.setMessage(world(Waypoints.all().get(index)));
            }).bounds(left + NAME + COLUMN_GAP, y, WORLD, HEIGHT).build());

            addRenderableWidget(Button.builder(
                    Component.translatable("alphamap.waypoints.delete").withColor(RED), button -> {
                        Waypoints.remove(index);
                        rebuildWidgets();
                    }).bounds(left + NAME + WORLD + COLUMN_GAP * 2, y, DELETE, HEIGHT).build());
        }

        int bottom = top + (to - from) * GAP + GAP / 2;
        if (pages > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), button -> {
                page = (page + pages - 1) % pages;
                rebuildWidgets();
            }).bounds(left, bottom, PAGE, HEIGHT).build());

            addRenderableWidget(Button.builder(Component.literal(">"), button -> {
                page = (page + 1) % pages;
                rebuildWidgets();
            }).bounds(left + span - PAGE, bottom, PAGE, HEIGHT).build());
        }
        done(bottom, span - (pages > 1 ? (PAGE + COLUMN_GAP) * 2 : 0));
    }

    @Override
    public void onClose() {
        Vanilla.setScreen(minecraft, parent);
    }

    private void done(int y, int span) {
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds((width - span) / 2, y, span, HEIGHT)
                .build());
    }

    private static Component name(Waypoint waypoint) {
        Component text = waypoint.name().isBlank()
                ? Component.translatable("alphamap.waypoint.title")
                : Component.literal(waypoint.name());
        return text.copy().withColor(waypoint.colour());
    }

    private static Component world(Waypoint waypoint) {
        return Component.translatable(waypoint.worldHidden()
                ? "alphamap.waypoints.world.off"
                : "alphamap.waypoints.world.on");
    }
}
