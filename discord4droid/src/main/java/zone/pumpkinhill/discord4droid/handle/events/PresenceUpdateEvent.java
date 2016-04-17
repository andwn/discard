package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.Presences;
import zone.pumpkinhill.discord4droid.handle.obj.User;

/**
 * This event is dispatched when a user changes his/her presence.
 */
public class PresenceUpdateEvent extends Event {

    private final Guild guild;
    private final User user;
    private final Presences oldPresence, newPresence;

    public PresenceUpdateEvent(Guild guild, User user, Presences oldPresence, Presences newPresence) {
        this.guild = guild;
        this.user = user;
        this.oldPresence = oldPresence;
        this.newPresence = newPresence;
    }

    /**
     * Gets the user's new presence.
     *
     * @return The presence.
     */
    public Presences getNewPresence() {
        return newPresence;
    }

    /**
     * Gets the user's old presence.
     *
     * @return The presence.
     */
    public Presences getOldPresence() {
        return oldPresence;
    }

    /**
     * Gets the user involved.
     *
     * @return The user.
     */
    public User getUser() {
        return user;
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
