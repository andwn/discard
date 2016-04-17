package zone.pumpkinhill.discord4droid.api;

/**
 * Used to represent an event.
 */
public abstract class Event {

    protected DiscordClient client;

    /**
     * Gets the client instance this event was fired from.
     *
     * @return The client instance.
     */
    public DiscordClient getClient() {
        return client;
    }
}
