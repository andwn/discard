package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.User;

/**
 * This event is dispatched when a user is pardoned from a ban.
 */
public class UserPardonEvent extends Event {

    private final User user;
    private final Guild guild;

    public UserPardonEvent(User user, Guild guild) {
        this.user = user;
        this.guild = guild;
    }

    /**
     * Gets the user that was pardoned.
     *
     * @return The pardoned user.
     */
    public User getUser() {
        return user;
    }

    /**
     * Gets the guild the user was pardoned from.
     *
     * @return The guild.
     */
    public Guild getGuild() {
        return guild;
    }
}
