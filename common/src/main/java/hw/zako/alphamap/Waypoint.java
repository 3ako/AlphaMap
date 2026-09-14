package hw.zako.alphamap;

public record Waypoint(double x, double y, double z, String name, int colour, boolean worldHidden) {

    public Waypoint(double x, double y, double z, String name, int colour) {
        this(x, y, z, name, colour, false);
    }

    public Waypoint renamed(String newName) {
        return new Waypoint(x, y, z, newName, colour, worldHidden);
    }

    public Waypoint coloured(int newColour) {
        return new Waypoint(x, y, z, name, newColour, worldHidden);
    }

    public Waypoint worldHidden(boolean hidden) {
        return new Waypoint(x, y, z, name, colour, hidden);
    }
}
