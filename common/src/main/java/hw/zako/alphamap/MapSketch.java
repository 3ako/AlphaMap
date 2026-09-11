package hw.zako.alphamap;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class MapSketch {

    private final double MIN_STEP = 1.5;

    private final List<List<double[]>> strokes = new ArrayList<>();

    private List<double[]> current;

    public List<List<double[]>> strokes() {
        return strokes;
    }

    public boolean isEmpty() {
        return strokes.isEmpty();
    }

    public void start() {
        current = new ArrayList<>();
        strokes.add(current);
    }

    public void extend(double atlasX, double atlasY) {
        if (current == null) start();

        if (!current.isEmpty()) {
            double[] last = current.get(current.size() - 1);
            if (Math.abs(last[0] - atlasX) < MIN_STEP && Math.abs(last[1] - atlasY) < MIN_STEP) return;
        }
        current.add(new double[]{atlasX, atlasY});
    }

    public void finish() {
        if (current != null && current.size() < 2) strokes.remove(current);
        current = null;
    }

    public void clear() {
        strokes.clear();
        current = null;
    }
}
