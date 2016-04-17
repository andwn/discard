package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.Role;

/**
 * This event is dispatched whenever a guild role is modified.
 */
public class RoleUpdateEvent extends Event {

    private final Role oldRole, newRole;
    private final Guild guild;

    public RoleUpdateEvent(Role oldRole, Role newRole, Guild guild) {
        this.oldRole = oldRole;
        this.newRole = newRole;
        this.guild = guild;
    }

    /**
     * Gets the original version of the role.
     *
     * @return The old role.
     */
    public Role getOldRole() {
        return oldRole;
    }

    /**
     * Gets the new version of the role.
     *
     * @return The new role.
     */
    public Role getNewRole() {
        return newRole;
    }

    /**
     * Gets the guild the role is for.
     *
     * @return The guild.
     */
    public Guild getGuild() {
        return guild;
    }
}
