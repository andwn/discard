package zone.pumpkinhill.discord4droid.util;

import java.util.EnumSet;

import zone.pumpkinhill.discord4droid.handle.obj.Permissions;

/**
 * This exception is thrown when a user is missing the required permissions to perform an action.
 */
public class MissingPermissionsException extends Exception {

    private EnumSet<Permissions> missing;

    public MissingPermissionsException(EnumSet<Permissions> permissionsMissing) {
        super(getMessage(permissionsMissing));
        missing = permissionsMissing;
    }

    public MissingPermissionsException(String message) {
        super(message);
    }

    private static String getMessage(EnumSet<Permissions> permissions) {
        String rtn = "Missing permissions: ";
        for(Permissions p : permissions)  {
            rtn += p.name() + ", ";
        }
        return rtn + "!";
    }

    /**
     * Gets the formatted error message.
     *
     * @return The message.
     */
    public String getErrorMessage() {
        return missing == null ? getLocalizedMessage() : getMessage(missing);
    }
}
