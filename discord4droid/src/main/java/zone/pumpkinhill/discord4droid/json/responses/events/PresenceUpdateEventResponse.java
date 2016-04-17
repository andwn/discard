package zone.pumpkinhill.discord4droid.json.responses.events;

import zone.pumpkinhill.discord4droid.json.generic.GameObject;
import zone.pumpkinhill.discord4droid.json.responses.PresenceResponse;

/**
 * This is received when a user's presence changes
 */
public class PresenceUpdateEventResponse {

    /**
     * The user this represents
     */
    public PresenceResponse.UserResponse user;

    /**
     * The status for the user, either: "idle" or "online"
     */
    public String status;

    /**
     * The game the user is playing (or null if no game being played)
     */
    public GameObject game;

    /**
     * The roles the user is a part of
     */
    public String[] roles;

    /**
     * The guild the presence updated in
     */
    public String guild_id;
}
