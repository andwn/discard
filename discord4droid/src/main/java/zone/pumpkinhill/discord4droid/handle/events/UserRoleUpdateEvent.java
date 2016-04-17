package zone.pumpkinhill.discord4droid.handle.events;

import java.util.List;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.Role;
import zone.pumpkinhill.discord4droid.handle.obj.User;

/**
 * This event is dispatched when a guild updates a user's roles.
 */
public class UserRoleUpdateEvent extends Event {

    private final List<Role> oldRoles, newRoles;
    private final User user;
    private final Guild guild;

    public UserRoleUpdateEvent(List<Role> oldRoles, List<Role> newRoles, User user, Guild guild) {
        this.oldRoles = oldRoles;
        this.newRoles = newRoles;
        this.user = user;
        this.guild = guild;
    }

    /**
     * Gets the old roles for the user.
     *
     * @return The old roles.
     */
    public List<Role> getOldRoles() {
        return oldRoles;
    }

    /**
     * Gets the new roles for the user.
     *
     * @return The new roles.
     */
    public List<Role> getNewRoles() {
        return newRoles;
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
