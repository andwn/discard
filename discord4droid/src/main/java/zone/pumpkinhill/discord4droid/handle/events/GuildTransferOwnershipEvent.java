package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.User;

/**
 * This event is dispatched when a guild's ownership is transferred.
 */
public class GuildTransferOwnershipEvent extends Event {

    private final User oldOwner, newOwner;
    private final Guild guild;

    public GuildTransferOwnershipEvent(User oldOwner, User newOwner, Guild guild) {
        this.oldOwner = oldOwner;
        this.newOwner = newOwner;
        this.guild = guild;
    }

    /**
     * Gets the original owner of the guild.
     *
     * @return The original owner.
     */
    public User getOldOwner() {
        return oldOwner;
    }

    /**
     * Gets the new owner of the guild.
     *
     * @return The new owner.
     */
    public User getNewOwner() {
        return newOwner;
    }

    /**
     * Gets the guild that ownership was transferred in.
     *
     * @return The effected guild.
     */
    public Guild getGuild() {
        return guild;
    }
}
