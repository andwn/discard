package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;

/**
 * This event is dispatched when a guild becomes unavailable.
 * Note: this guild is removed from the guild list when this happens!
 */
public class GuildUnavailableEvent extends Event {

    private final Guild guild;

    public GuildUnavailableEvent(Guild guild) {
        this.guild = guild;
    }

    /**
     * Gets the guild that became unavailable.
     *
     * @return The guild.
     */
    public Guild getGuild() {
        return guild;
    }
}
