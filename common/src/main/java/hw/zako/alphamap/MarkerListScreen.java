package hw.zako.alphamap;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public final class MarkerListScreen extends Screen {

    private static final int HEIGHT = 20;
    private static final int GAP = 22;
    private static final int COLUMN_GAP = 6;
    private static final int WIDEST = 150;

    private final @Nullable Screen parent;
    private final Predicate<String> shown;
    private final BiConsumer<String, Boolean> show;

    private MarkerListScreen(@Nullable Screen parent, String title,
                             Predicate<String> shown, BiConsumer<String, Boolean> show) {
        super(Component.translatable(title));
        this.parent = parent;
        this.shown = shown;
        this.show = show;
    }

    public static MarkerListScreen minimap(@Nullable Screen parent) {
        MapSettings settings = MapSettings.get();
        return new MarkerListScreen(parent, "alphamap.markers.minimap",
                settings::minimapMarkerShown, settings::minimapMarkerShown);
    }

    public static MarkerListScreen world(@Nullable Screen parent) {
        MapSettings settings = MapSettings.get();
        return new MarkerListScreen(parent, "alphamap.markers.world",
                settings::worldMarkerShown, settings::worldMarkerShown);
    }

    @Override
    protected void init() {
        List<String> kinds = kinds();
        if (kinds.isEmpty()) {
            Button empty = Button.builder(Component.translatable("alphamap.markers.empty"), button -> {
            }).bounds((width - WIDEST) / 2, height / 3, WIDEST, HEIGHT).build();
            empty.active = false;
            addRenderableWidget(empty);
            done(height / 3 + GAP * 2, WIDEST);
            return;
        }

        int columns = kinds.size() > 16 ? 3 : 2;
        int span = Math.min(WIDEST, (width - 20 - (columns - 1) * COLUMN_GAP) / columns);
        int rows = (kinds.size() + columns - 1) / columns;
        int left = (width - span * columns - (columns - 1) * COLUMN_GAP) / 2;
        int top = Math.max(20, (height - rows * GAP - GAP * 2) / 2);

        for (int i = 0; i < kinds.size(); i++) {
            String icon = kinds.get(i);
            int x = left + (i / rows) * (span + COLUMN_GAP);
            int y = top + (i % rows) * GAP;

            addRenderableWidget(Button.builder(label(icon), button -> {
                show.accept(icon, !shown.test(icon));
                MapSettings.get().clampAndSave();
                button.setMessage(label(icon));
            }).bounds(x, y, span, HEIGHT).build());
        }

        done(top + rows * GAP + GAP / 2, span);
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

    private Component label(String icon) {
        Component name = Component.translatableWithFallback("alphamap.marker." + icon, icon);
        return Component.translatable(shown.test(icon)
                ? "alphamap.markers.shown"
                : "alphamap.markers.hidden", name);
    }

    private static List<String> kinds() {
        MapSettings settings = MapSettings.get();
        var kinds = new TreeSet<>(MarkerIcons.seen());
        kinds.addAll(settings.minimapHidden());
        kinds.addAll(settings.worldShown());
        return List.copyOf(kinds);
    }
}
