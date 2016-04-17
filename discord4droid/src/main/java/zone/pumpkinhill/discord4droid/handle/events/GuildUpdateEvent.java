package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;

/**
 * This event is dispatched when a guild is edited by its owner.
 */
public class GuildUpdateEvent extends Event {

    private final Guild oldGuild, newGuild;

    public GuildUpdateEvent(Guild oldGuild, Guild newGuild) {
        this.oldGuild = oldGuild;
        this.newGuild = newGuild;
    }

    /**
     * Gets the unupdated guild.
     *
     * @return The old guild.
     */
    public Guild getOldGuild() {
        return oldGuild;
    }

    /**
     * Gets the updated guild.
     *
     * @return The new guild.
     */
    public Guild getNewGuild() {
        return newGuild;
    }
}
