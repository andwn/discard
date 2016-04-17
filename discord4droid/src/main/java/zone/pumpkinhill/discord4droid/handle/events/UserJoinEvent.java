package zone.pumpkinhill.discord4droid.handle.events;

import java.util.Date;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.User;

/**
 * This is dispatched when a user is added/joins a guild.
 */
public class UserJoinEvent extends Event {

    private final Guild guild;
    private final Date joinTime;
    private final User userJoined;

    public UserJoinEvent(Guild guild, User user, Date when) {
        this.guild = guild;
        this.joinTime = when;
        this.userJoined = user;
    }

    /**
     * Gets the timestamp for when the user joined the guild.
     *
     * @return The timestamp.
     */
    public Date getJoinTime() {
        return joinTime;
    }

    /**
     * Gets the user involved.
     *
     * @return The user.
     */
    public User getUser() {
        return userJoined;
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
