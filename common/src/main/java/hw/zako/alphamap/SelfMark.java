package hw.zako.alphamap;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SelfMark {

    private final int OUTLINE = 0x000000;

    public void draw(Canvas canvas, SelfShape shape, int x, int y,
                     double scale, int colour, int alpha, float turn) {
        int fill = colour | alpha;
        int edge = OUTLINE | alpha;
        int arm = Math.max(2, (int) Math.round(4 * scale));

        switch (shape) {
            case CROSS -> {
                canvas.fill(x - arm, y - 1, x + arm + 1, y + 2, edge);
                canvas.fill(x - 1, y - arm, x + 2, y + arm + 1, edge);
                canvas.fill(x - arm + 1, y, x + arm, y + 1, fill);
                canvas.fill(x, y - arm + 1, x + 1, y + arm, fill);
            }
            case SQUARE -> {
                canvas.fill(x - arm - 1, y - arm - 1, x + arm + 1, y + arm + 1, edge);
                canvas.fill(x - arm, y - arm, x + arm, y + arm, fill);
            }
            case DOT -> {
                disc(canvas, x, y, arm, edge);
                disc(canvas, x, y, arm - 1, fill);
            }
            case ARROW -> arrow(canvas, x, y, (float) scale, turn, fill, edge);
        }
    }

    private void disc(Canvas canvas, int x, int y, int radius, int colour) {
        for (int row = -radius; row <= radius; row++) {
            int span = (int) Math.round(Math.sqrt((double) radius * radius - row * row));
            canvas.fill(x - span, y + row, x + span + 1, y + row + 1, colour);
        }
    }

    private void arrow(Canvas canvas, int x, int y, float scale, float turn,
                       int colour, int outline) {
        canvas.push(x, y, scale, turn);
        for (int row = -4; row <= 3; row++) {
            int half = (row + 4) / 2;
            canvas.fill(-half - 1, row, half + 2, row + 1, outline);
        }
        for (int row = -3; row <= 2; row++) {
            int half = (row + 3) / 2;
            canvas.fill(-half, row, half + 1, row + 1, colour);
        }
        canvas.pop();
    }
}
