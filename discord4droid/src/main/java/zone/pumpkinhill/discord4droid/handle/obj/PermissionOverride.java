package zone.pumpkinhill.discord4droid.handle.obj;

import java.util.EnumSet;

/**
 * Represents specific permission overrides for a user/role in the channel.
 */
public class PermissionOverride {

    /**
     * Permissions to add.
     */
    protected final EnumSet<Permissions> allow;

    /**
     * Permissions to remove.
     */
    protected final EnumSet<Permissions> deny;

    public PermissionOverride(EnumSet<Permissions> allow, EnumSet<Permissions> deny) {
        this.allow = allow;
        this.deny = deny;
    }

    /**
     * Gets the permissions to add to the user/role.
     *
     * @return The permissions.
     */
    public EnumSet<Permissions> allow() {
        return allow;
    }

    /**
     * Gets the permissions to remove from the user/role.
     *
     * @return The permissions.
     */
    public EnumSet<Permissions> deny() {
        return deny;
    }
}