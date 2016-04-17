package zone.pumpkinhill.discord4droid.json.responses.events;

import zone.pumpkinhill.discord4droid.json.responses.UserResponse;

/**
 * This is a generic object representing all guild ban events.
 */
public class GuildBanEventResponse {

    /**
     * The guild involved.
     */
    public String guild_id;

    /**
     * The user involved.
     */
    public UserResponse user;
}
