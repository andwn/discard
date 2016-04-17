package zone.pumpkinhill.discord4droid.handle.obj;

/**
 * This represents a discord server region, used for voice and guild management.
 */
public class Region {

    private final String id, name;
    private final boolean vip;

    public Region(String id, String name, boolean vip) {
        this.id = id;
        this.name = name;
        this.vip = vip;
    }

    /**
     * Gets the region id.
     *
     * @return The id.
     */
    public String getID() {
        return id;
    }

    /**
     * Gets the name of the region.
     *
     * @return The region name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets whether the region is for VIPs.
     *
     * @return True if it's for VIPs, false if otherwise.
     */
    public boolean isVIPOnly() {
        return vip;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object other) {
        return this.getClass().isAssignableFrom(other.getClass()) && ((Region) other).getID().equals(getID());
    }
}
