package zone.pumpkinhill.discord4droid.json.responses.events;

/**
 * This is received when the connection is resumed after a reconnect.
 */
public class ResumedEventResponse {

    /**
     * The new heartbeat interval to use.
     */
    public long heartbeat_interval;
}
