package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.User;

/**
 * This event is dispatched when a user is banned from a guild.
 */
public class UserBanEvent extends Event {

    private final User user;
    private final Guild guild;

    public UserBanEvent(User user, Guild guild) {
        this.user = user;
        this.guild = guild;
    }

    /**
     * Gets the user that was banned.
     *
     * @return The banned user.
     */
    public User getUser() {
        return user;
    }

    /**
     * Gets the guild the user was banned from.
     *
     * @return The guild.
     */
    public Guild getGuild() {
        return guild;
    }
}
