package zone.pumpkinhill.discord4droid.handle.obj;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import zone.pumpkinhill.discord4droid.api.DiscordClient;
import zone.pumpkinhill.discord4droid.util.DiscordException;
import zone.pumpkinhill.discord4droid.util.HTTP429Exception;
import zone.pumpkinhill.discord4droid.util.MissingPermissionsException;

public class PrivateChannel extends Channel {

    /**
     * The recipient of this private channel.
     */
    protected final User recipient;

    public PrivateChannel(DiscordClient client, User recipient, String id) {
        super(client, recipient.getName(), id, null, null, 0, new HashMap<String, PermissionOverride>(), new HashMap<String, PermissionOverride>());
        this.recipient = recipient;
        this.isPrivate = true;
    }

    @Override
    public Map<String, PermissionOverride> getUserOverrides() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, PermissionOverride> getRoleOverrides() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EnumSet<Permissions> getModifiedPermissions(User user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EnumSet<Permissions> getModifiedPermissions(Role role) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addUserOverride(String userId, PermissionOverride override) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addRoleOverride(String roleId, PermissionOverride override) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removePermissionsOverride(User user) throws MissingPermissionsException, HTTP429Exception, DiscordException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removePermissionsOverride(Role role) throws MissingPermissionsException, HTTP429Exception, DiscordException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void overrideRolePermissions(Role role, EnumSet<Permissions> toAdd, EnumSet<Permissions> toRemove) throws MissingPermissionsException, HTTP429Exception, DiscordException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void overrideUserPermissions(User user, EnumSet<Permissions> toAdd, EnumSet<Permissions> toRemove) throws MissingPermissionsException, HTTP429Exception, DiscordException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Invite> getInvites() throws DiscordException, HTTP429Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPosition(int position) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getPosition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String mention() {
        return recipient.mention();
    }

    @Override
    public Invite createInvite(int maxAge, int maxUses, boolean temporary, boolean useXkcdPass) throws MissingPermissionsException, HTTP429Exception, DiscordException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTopic(String topic) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTopic() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Guild getGuild() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return recipient.getName();
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException();
    }

    /**
     * Indicates the user with whom you are communicating.
     *
     * @return The user.
     */
    public User getRecipient() {
        return recipient;
    }

    @Override
    public String toString() {
        return recipient.toString();
    }
}
