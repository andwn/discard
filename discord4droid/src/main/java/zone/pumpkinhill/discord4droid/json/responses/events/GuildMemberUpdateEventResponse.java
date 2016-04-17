package zone.pumpkinhill.discord4droid.json.responses.events;

import zone.pumpkinhill.discord4droid.json.responses.UserResponse;

/**
 * This event is received when a member is updated in a guild.
 */
public class GuildMemberUpdateEventResponse {

    /**
     * The guild affected.
     */
    public String guild_id;

    /**
     * The user's roles.
     */
    public String[] roles;

    /**
     * The user.
     */
    public UserResponse user;
}
