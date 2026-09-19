package hw.zako.alphamap;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class MobListScreenBase extends Screen {

    private static final int HEIGHT = 20;
    private static final int GAP = 22;
    private static final int COLUMN_GAP = 6;
    private static final int WIDEST = 150;
    private static final int FACE = 16;
    private static final int MIN_SPAN = 70;
    private static final int PAGER = 20;
    private static final int DOT = 6;
    private static final int HOSTILE_DOT = 0xFFFF4040;
    private static final int PASSIVE_DOT = 0xFF7BE07B;

    private final @Nullable Screen parent;
    private final List<Placed> placed = new ArrayList<>();
    private int page;
    private String filter = "";

    private record Placed(String id, int x, int y) {
    }

    protected MobListScreenBase(@Nullable Screen parent) {
        super(Component.translatable("alphamap.mobs.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        MapSettings settings = MapSettings.get();
        String query = filter.trim().toLowerCase(Locale.ROOT);
        List<String> all = MobHeads.known().stream()
                .filter(id -> query.isEmpty() || id.contains(query)
                        || mobName(id).getString().toLowerCase(Locale.ROOT).contains(query))
                .toList();
        placed.clear();

        int maxRows = Math.max(1, (height - 28 - GAP * 4) / GAP);
        int widest = Math.clamp((width - 20 + COLUMN_GAP) / (MIN_SPAN + COLUMN_GAP), 1, 6);
        int columns = Math.min(3, widest);
        while (columns < widest && (all.size() + columns - 1) / columns > maxRows) columns++;
        int span = Math.min(WIDEST, (width - 20 - (columns - 1) * COLUMN_GAP) / columns);

        int perPage = maxRows * columns;
        int pages = Math.max(1, (all.size() + perPage - 1) / perPage);
        page = Math.clamp(page, 0, pages - 1);
        List<String> mobs = all.subList(page * perPage, Math.min(all.size(), (page + 1) * perPage));
        int rows = Math.min(maxRows, (mobs.size() + columns - 1) / columns);
        int left = (width - span * columns - (columns - 1) * COLUMN_GAP) / 2;
        int top = Math.max(28, (height - (rows + 3) * GAP - GAP * 2) / 2);

        master(left, top, span, settings, true);
        master(left + span + COLUMN_GAP, top, span, settings, false);

        EditBox search = new EditBox(font, left, top + GAP, span * 2 + COLUMN_GAP, HEIGHT,
                Component.translatable("alphamap.mobs.search"));
        search.setHint(Component.translatable("alphamap.mobs.search"));
        search.setValue(filter);
        search.setResponder(text -> {
            if (text.equals(filter)) return;
            filter = text;
            page = 0;
            rebuildWidgets();
        });
        addRenderableWidget(search);
        setInitialFocus(search);

        int listTop = top + GAP * 2 + GAP / 2;
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

        int bottom = listTop + rows * GAP + GAP / 2;
        int doneLeft = (width - span) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(doneLeft, bottom, span, HEIGHT)
                .build());
        if (pages > 1) {
            pager(doneLeft - COLUMN_GAP - PAGER, bottom, "<", page > 0, -1);
            pager(doneLeft + span + COLUMN_GAP, bottom, ">", page < pages - 1, 1);
        }
    }

    private void pager(int x, int y, String label, boolean active, int step) {
        Button button = Button.builder(Component.literal(label), widget -> {
            page += step;
            rebuildWidgets();
        }).bounds(x, y, PAGER, HEIGHT).build();
        button.active = active;
        addRenderableWidget(button);
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
            boolean off = MapSettings.OFF.equals(settings.minimapMob(spot.id()));
            if (head == null || head.sample() == null) {
                int colour = MobHeads.hostile(spot.id()) ? HOSTILE_DOT : PASSIVE_DOT;
                int x = spot.x() + 4 + (FACE - DOT) / 2;
                int y = spot.y() + 2 + (FACE - DOT) / 2;
                canvas.fill(x, y, x + DOT, y + DOT, off ? colour & 0x60FFFFFF : colour);
                continue;
            }

            int tint = off ? 0x60FFFFFF : 0xFFFFFFFF;
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
