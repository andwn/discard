package zone.pumpkinhill.discord4droid.json.responses;

import zone.pumpkinhill.discord4droid.json.generic.GameObject;

/**
 * Represents a user's presence
 */
public class PresenceResponse {

    /**
     * The user this represents
     */
    public UserResponse user;

    /**
     * The status for the user, either: "idle" or "online"
     */
    public String status;

    /**
     * The game the user is playing (or null if no game being played)
     */
    public GameObject game;

    /**
     * Represents a user
     */
    public class UserResponse {

        /**
         * The user's id
         */
        public String id;
    }
}
