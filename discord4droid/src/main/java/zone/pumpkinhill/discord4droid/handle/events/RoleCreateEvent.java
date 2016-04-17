package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.Role;

/**
 * This event is dispatched whenever a role is created.
 */
public class RoleCreateEvent extends Event {

    private final Role role;
    private final Guild guild;

    public RoleCreateEvent(Role role, Guild guild) {
        this.role = role;
        this.guild = guild;
    }

    /**
     * Gets the newly created role.
     *
     * @return The role.
     */
    public Role getRole() {
        return role;
    }

    /**
     * Gets the guild the role was created for.
     *
     * @return The guild.
     */
    public Guild getGuild() {
        return guild;
    }
}
