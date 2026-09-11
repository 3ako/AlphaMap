package hw.zako.alphamap;

public record Waypoint(double x, double y, double z, String name, int colour) {

    public Waypoint renamed(String newName) {
        return new Waypoint(x, y, z, newName, colour);
    }

    public Waypoint coloured(int newColour) {
        return new Waypoint(x, y, z, name, newColour);
    }
}
