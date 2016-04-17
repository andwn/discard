package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.User;

/**
 * This is dispatched whenever a user updates his/her info
 */
public class UserUpdateEvent extends Event {

    private User oldUser, newUser;

    public UserUpdateEvent(User oldUser, User newUser) {
        this.oldUser = oldUser;
        this.newUser = newUser;
    }

    /**
     * Gets the old user info
     *
     * @return The old user object
     */
    public User getOldUser() {
        return oldUser;
    }

    /**
     * Gets the new user info
     *
     * @return The new user object
     */
    public User getNewUser() {
        return newUser;
    }
}
