package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.User;

/**
 * This event is dispatched when a guild member is removed/leaves from a guild
 */
public class UserLeaveEvent extends Event {
    private final Guild guild;
    private final User user;

    public UserLeaveEvent(Guild guild, User user) {
        this.guild = guild;
        this.user = user;
    }

    /**
     * The user involved.
     *
     * @return The user.
     */
    public User getUser() {
        return user;
    }

    /**
     * The guild involved.
     *
     * @return The guild.
     */
    public Guild getGuild() {
        return guild;
    }
}
