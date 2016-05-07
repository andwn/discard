package zone.pumpkinhill.discord4droid.handle.obj;

import java.math.BigInteger;
import java.util.Date;

import zone.pumpkinhill.discord4droid.api.DiscordClient;

public abstract class DiscordObject {
    protected final DiscordClient client;
    protected final String id;
    // Used to determine age based on discord ids
    protected static final BigInteger DISCORD_EPOCH = new BigInteger("1420070400000");

    public DiscordObject(DiscordClient client, String id) {
        this.client = client;
        this.id = id;
    }

    public final DiscordClient getClient() { return client; }

    // Gets the snowflake ID of this object
    public final String getID() { return id; }

    public final Date getCreationDate() {
        long milliseconds = DISCORD_EPOCH.add(new BigInteger(id).shiftRight(22)).longValue();
        Date date = new Date();
        date.setTime(milliseconds);
        return date;
    }

    public abstract DiscordObject copy();

    @Override
    public String toString() {
        return "[OBJ:" + client + ":" + id + "]";
    }

    @Override
    public final boolean equals(Object other) {
        return other != null && this.getClass().isAssignableFrom(other.getClass()) &&
                id.equals(((DiscordObject) other).getID());
    }
}
