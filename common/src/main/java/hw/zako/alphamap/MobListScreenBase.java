package hw.zako.alphamap;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class MobListScreenBase extends Screen {

    private static final int HEIGHT = 20;
    private static final int GAP = 22;
    private static final int COLUMN_GAP = 6;
    private static final int WIDEST = 150;
    private static final int FACE = 16;

    private final @Nullable Screen parent;
    private final List<Placed> placed = new ArrayList<>();

    private record Placed(String id, int x, int y) {
    }

    protected MobListScreenBase(@Nullable Screen parent) {
        super(Component.translatable("alphamap.mobs.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        MapSettings settings = MapSettings.get();
        List<String> mobs = MobHeads.known();
        placed.clear();

        int maxRows = Math.max(1, (height - 28 - GAP * 3) / GAP);
        int columns = 3;
        while (columns < 6 && (mobs.size() + columns - 1) / columns > maxRows) columns++;
        int span = Math.min(WIDEST, (width - 20 - (columns - 1) * COLUMN_GAP) / columns);
        int rows = (mobs.size() + columns - 1) / columns;
        int left = (width - span * columns - (columns - 1) * COLUMN_GAP) / 2;
        int top = Math.max(28, (height - (rows + 2) * GAP - GAP * 2) / 2);

        master(left, top, span, settings, true);
        master(left + span + COLUMN_GAP, top, span, settings, false);

        int listTop = top + GAP + GAP / 2;
        for (int i = 0; i < mobs.size(); i++) {
            String id = mobs.get(i);
            int x = left + (i / rows) * (span + COLUMN_GAP);
            int y = listTop + (i % rows) * GAP;

            placed.add(new Placed(id, x, y));
            Button button = Button.builder(mobLabel(settings, id), widget -> {
                settings.minimapMob(id, next(settings.minimapMob(id)));
                settings.clampAndSave();
                widget.setMessage(mobLabel(settings, id));
            }).bounds(x, y, span, HEIGHT).build();
            button.setTooltip(Tooltip.create(mobName(id)));
            addRenderableWidget(button);
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds((width - span) / 2, listTop + rows * GAP + GAP / 2, span, HEIGHT)
                .build());
    }

    private void master(int x, int y, int span, MapSettings settings, boolean hostile) {
        addRenderableWidget(Button.builder(masterLabel(settings, hostile), button -> {
            settings.minimapKind(hostile, next(settings.minimapKind(hostile)));
            settings.clampAndSave();
            rebuildWidgets();
        }).bounds(x, y, span, HEIGHT).build());
    }

    private static String next(String state) {
        return switch (state) {
            case MapSettings.OFF -> MapSettings.DOTS;
            case MapSettings.DOTS -> MapSettings.HEADS;
            default -> MapSettings.OFF;
        };
    }

    private Component masterLabel(MapSettings settings, boolean hostile) {
        return Component.translatable(
                "alphamap.mobs." + (hostile ? "hostile" : "passive") + "." + settings.minimapKind(hostile));
    }

    private Component mobLabel(MapSettings settings, String id) {
        return Component.translatable("alphamap.state." + settings.minimapMob(id));
    }

    private Component mobName(String id) {
        return MobHeads.isGroup(id)
                ? Component.translatable("alphamap.mob." + id)
                : Component.translatableWithFallback("entity.minecraft." + id, id);
    }

    protected void paint(Canvas canvas) {
        MapSettings settings = MapSettings.get();
        for (Placed spot : placed) {
            MobHeads.Head head = MobHeads.of(spot.id());
            if (head == null || head.sample() == null) continue;

            int tint = MapSettings.OFF.equals(settings.minimapMob(spot.id())) ? 0x60FFFFFF : 0xFFFFFFFF;
            canvas.blitRegion(head.sample(), spot.x() + 4, spot.y() + 2, FACE, FACE,
                    head.u(), head.v(), head.width(), head.height(),
                    head.sheetWidth(), head.sheetHeight(), tint);
        }
    }

    @Override
    public void onClose() {
        Vanilla.setScreen(minecraft, parent);
    }
}
