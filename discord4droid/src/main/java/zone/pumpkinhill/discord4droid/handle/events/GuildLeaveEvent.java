package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;

/**
 * This event is dispatched when a guild is deleted or the bot is kicked.
 */
public class GuildLeaveEvent extends Event {

    private final Guild guild;

    public GuildLeaveEvent(Guild guild) {
        this.guild = guild;
    }

    /**
     * Gets the guild involved.
     *
     * @return The guild.
     */
    public Guild getGuild() {
        return guild;
    }
}
