package zone.pumpkinhill.discord4droid.json.responses.events;

import zone.pumpkinhill.discord4droid.json.responses.GuildResponse;

/**
 * This is returned when requesting additional guild members from a "large" guild.
 */
public class GuildMemberChunkEventResponse {

    /**
     * The guild id.
     */
    public String guild_id;

    /**
     * The members requested.
     */
    public GuildResponse.MemberResponse[] members;
}
