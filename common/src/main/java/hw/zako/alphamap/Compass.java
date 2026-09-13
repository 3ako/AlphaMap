package hw.zako.alphamap;

import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

@UtilityClass
public class Compass {

    private final int COLOUR = 0xE0FFFFFF;
    private final int LINE = 4;

    private final String[] SIDES = {"north", "east", "south", "west"};

    private final int[][] WAYS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    public void around(Canvas canvas, Font font, float centreX, float centreY,
                       double reach, float turn, boolean square) {
        double cos = Math.cos(-turn);
        double sin = Math.sin(-turn);

        for (int i = 0; i < SIDES.length; i++) {
            double across = WAYS[i][0] * cos - WAYS[i][1] * sin;
            double down = WAYS[i][0] * sin + WAYS[i][1] * cos;
            double push = square ? reach / Math.max(Math.abs(across), Math.abs(down)) : reach;

            canvas.centered(font, letter(i),
                    (int) Math.round(centreX + across * push),
                    (int) Math.round(centreY + down * push) - LINE,
                    COLOUR);
        }
    }

    public void square(Canvas canvas, Font font, int left, int top, int side) {
        int centre = left + side / 2;
        int middle = top + side / 2 - LINE;

        canvas.centered(font, letter(0), centre, top + 3, COLOUR);
        canvas.centered(font, letter(1), left + side - 8, middle, COLOUR);
        canvas.centered(font, letter(2), centre, top + side - 12, COLOUR);
        canvas.centered(font, letter(3), left + 8, middle, COLOUR);
    }

    private Component letter(int side) {
        return Component.translatable("alphamap.compass." + SIDES[side]);
    }
}
