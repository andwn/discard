package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.Role;

/**
 * This event is dispatched after a role has been deleted from a guild.
 */
public class RoleDeleteEvent extends Event {

    private final Role role;
    private final Guild guild;

    public RoleDeleteEvent(Role role, Guild guild) {
        this.role = role;
        this.guild = guild;
    }

    /**
     * Gets the role that was deleted.
     *
     * @return The deleted role.
     */
    public Role getRole() {
        return role;
    }

    /**
     * Gets the guild the role was from.
     *
     * @return The guild.
     */
    public Guild getGuild() {
        return guild;
    }
}
