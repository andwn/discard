package zone.pumpkinhill.discord4droid.json.requests;

import java.util.EnumSet;

import zone.pumpkinhill.discord4droid.handle.obj.Permissions;

/**
 * This is sent in order to edit a role.
 */
public class RoleEditRequest {

    /**
     * The new color for the role.
     */
    public int color;

    /**
     * Whether to hoist the role.
     */
    public boolean hoist;

    /**
     * The new name of the role.
     */
    public String name;

    /**
     * The new permissions of the role.
     */
    public int permissions;

    public RoleEditRequest(int color, boolean hoist, String name, EnumSet<Permissions> permissions) {
        this.color = color & 0x00ffffff; // & 0x00ffffff eliminates the alpha value;
        this.hoist = hoist;
        this.name = name;
        this.permissions = Permissions.generatePermissionsNumber(permissions);
    }
}
