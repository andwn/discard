package zone.pumpkinhill.discord4droid.json.requests;

import zone.pumpkinhill.discord4droid.handle.obj.Role;

/**
 * This request is sent to modify a user's roles.
 */
public class MemberEditRequest {

    /**
     * Roles for the user to have.
     */
    public String[] roles;

    public MemberEditRequest(String[] roles) {
        this.roles = roles;
    }

    public MemberEditRequest(Role[] roles) {
        this.roles = new String[roles.length];
        for (int i = 0; i < roles.length; i++)
            this.roles[i] = roles[i].getID();
    }
}
