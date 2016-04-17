package zone.pumpkinhill.discord4droid.handle.obj;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.EnumSet;

import zone.pumpkinhill.discord4droid.api.DiscordClient;
import zone.pumpkinhill.discord4droid.api.DiscordUtils;
import zone.pumpkinhill.discord4droid.api.Endpoints;
import zone.pumpkinhill.discord4droid.api.Requests;
import zone.pumpkinhill.discord4droid.json.generic.RoleResponse;
import zone.pumpkinhill.discord4droid.json.requests.RoleEditRequest;
import zone.pumpkinhill.discord4droid.util.DiscordException;
import zone.pumpkinhill.discord4droid.util.HTTP429Exception;
import zone.pumpkinhill.discord4droid.util.MissingPermissionsException;
import zone.pumpkinhill.http.entity.StringEntity;
import zone.pumpkinhill.http.message.BasicNameValuePair;

/**
 * Represents a role.
 */
public class Role {

    /**
     * Where the role should be displayed. -1 is @everyone, it is always last
     */
    protected int position;

    /**
     * The permissions the role has.
     */
    protected EnumSet<Permissions> permissions;

    /**
     * The role name
     */
    protected String name;

    /**
     * Whether this role is managed via plugins like twitch
     */
    protected boolean managed;

    /**
     * The role id
     */
    protected String id;

    /**
     * Whether to display this role separately from others
     */
    protected boolean hoist;

    /**
     * The DECIMAL format for the color
     */
    protected int color;

    /**
     * The guild this role belongs to
     */
    protected Guild guild;

    public Role(int position, int permissions, String name, boolean managed, String id,
                boolean hoist, int color, Guild guild) {
        this.position = position;
        this.permissions = Permissions.getAllowedPermissionsForNumber(permissions);
        this.name = name;
        this.managed = managed;
        this.id = id;
        this.hoist = hoist;
        this.color = color;
        this.guild = guild;
    }

    /**
     * Gets the position of the role, the higher the number the higher priority it has on sorting. @everyone is always -1
     *
     * @return The position.
     */
    public int getPosition() {
        return position;
    }

    /**
     * Sets the CACHED role position.
     *
     * @param position The role position.
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * Gets the position the role allows.
     *
     * @return The set of enabled permissions.
     */
    public EnumSet<Permissions> getPermissions() {
        return permissions.clone();
    }

    /**
     * Sets the CACHED enabled permissions.
     *
     * @param permissions The permissions number.
     */
    public void setPermissions(int permissions) {
        this.permissions = Permissions.getAllowedPermissionsForNumber(permissions);
    }

    /**
     * Gets the name of the role.
     *
     * @return The name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the CACHED role name.
     *
     * @param name The name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Checks whether the role is managed by an external plugin like twitch.
     *
     * @return True if managed, false if otherwise.
     */
    public boolean isManaged() {
        return managed;
    }

    /**
     * Gets the unique id of the role.
     *
     * @return The role id.
     */
    public String getID() {
        return id;
    }

    /**
     * Gets whether the role is hoisted–meaning that it is displayed separately from the @everyone role.
     *
     * @return True if hoisted, false if otherwise.
     */
    public boolean isHoisted() {
        return hoist;
    }

    /**
     * Sets whether this role is hoisted in the CACHE.
     *
     * @param hoist True if hoisted, false if otherwise.
     */
    public void setHoist(boolean hoist) {
        this.hoist = hoist;
    }

    /**
     * Gets the color for this role.
     *
     * @return The color.
     */
    public int getColor() {
        return color;
    }

    /**
     * Sets the CACHED role color.
     *
     * @param color The color decimal number.
     */
    public void setColor(int color) {
        this.color = color;
    }

    /**
     * Gets the guild this role belongs to.
     *
     * @return The guild.
     */
    public Guild getGuild() {
        return guild;
    }

    private void edit(Integer color, Boolean hoist, String name, EnumSet<Permissions> permissions) throws MissingPermissionsException, HTTP429Exception, DiscordException {
        DiscordUtils.checkPermissions(guild.client, guild, EnumSet.of(Permissions.MANAGE_ROLES));

        try {
            RoleResponse response = DiscordUtils.GSON.fromJson(Requests.PATCH.makeRequest(
                    guild.client.getURL() + Endpoints.GUILDS+guild.getID()+"/roles/"+id,
                    new StringEntity(DiscordUtils.GSON.toJson(new RoleEditRequest(
                            color == null ? getColor() : color,
                            hoist == null ? isHoisted() : hoist,
                            name == null ? getName() : name,
                            permissions == null ? getPermissions() : permissions))),
                    new BasicNameValuePair("authorization", guild.client.getToken()),
                    new BasicNameValuePair("content-type", "application/json")), RoleResponse.class);
        } catch (UnsupportedEncodingException e) {
            System.out.println("Discord4Droid Internal Exception: " + e);
        }
    }

    /**
     * Changes the color of the role.
     *
     * @param color The new color for the role.
     *
     * @throws HTTP429Exception
     * @throws DiscordException
     * @throws MissingPermissionsException
     */
    public void changeColor(int color) throws HTTP429Exception, DiscordException, MissingPermissionsException {
        edit(color, null, null, null);
    }

    /**
     * Changes whether to hoist the role.
     *
     * @param hoist Whether to hoist the role.
     *
     * @throws HTTP429Exception
     * @throws DiscordException
     * @throws MissingPermissionsException
     */
    public void changeHoist(boolean hoist) throws HTTP429Exception, DiscordException, MissingPermissionsException {
        edit(null, hoist, null, null);
    }

    /**
     * Changes the name of the role.
     *
     * @param name The new name for the role.
     *
     * @throws HTTP429Exception
     * @throws DiscordException
     * @throws MissingPermissionsException
     */
    public void changeName(String name) throws HTTP429Exception, DiscordException, MissingPermissionsException {
        edit(null, null, name, null);
    }

    /**
     * Changes the permissions of the role.
     *
     * @param permissions The new permissions for the role.
     *
     * @throws HTTP429Exception
     * @throws DiscordException
     * @throws MissingPermissionsException
     */
    public void changePermissions(EnumSet<Permissions> permissions) throws HTTP429Exception, DiscordException, MissingPermissionsException {
        edit(null, null, null, permissions);
    }

    /**
     * Attempts to delete this role.
     *
     * @throws MissingPermissionsException
     * @throws HTTP429Exception
     * @throws DiscordException
     */
    public void delete() throws MissingPermissionsException, HTTP429Exception, DiscordException {
        DiscordUtils.checkPermissions(guild.client, guild, EnumSet.of(Permissions.MANAGE_ROLES));

        Requests.DELETE.makeRequest(guild.client.getURL() + Endpoints.GUILDS+guild.getID()+"/roles/"+id,
                new BasicNameValuePair("authorization", ((Guild) guild).client.getToken()));
    }

    /**
     * This calculates the time at which this object has been created by analyzing its Discord ID.
     *
     * @return The time at which this object was created.
     */
    public Date getCreationDate() {
        return DiscordUtils.getSnowflakeTimeFromID(id);
    }

    /**
     * This gets the client that this object is tied to.
     *
     * @return The client.
     */
    public DiscordClient getClient() {
        return guild.getClient();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        return this.getClass().isAssignableFrom(other.getClass()) && ((Role) other).getID().equals(getID());
    }
}
