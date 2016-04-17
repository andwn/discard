package zone.pumpkinhill.discord4droid.json.responses.events;

import zone.pumpkinhill.discord4droid.json.responses.UserResponse;

/**
 * This response is received when a user leaves a guild
 */
public class GuildMemberRemoveEventResponse {

    /**
     * The user who left
     */
    public UserResponse user;

    /**
     * The guild the user left
     */
    public String guild_id;
}
