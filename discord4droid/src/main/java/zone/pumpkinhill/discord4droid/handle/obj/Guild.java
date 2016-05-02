package zone.pumpkinhill.discord4droid.handle.obj;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import zone.pumpkinhill.discord4droid.api.DiscordClient;
import zone.pumpkinhill.discord4droid.api.DiscordUtils;
import zone.pumpkinhill.discord4droid.api.Endpoints;
import zone.pumpkinhill.discord4droid.api.Requests;
import zone.pumpkinhill.discord4droid.handle.AudioChannel;
import zone.pumpkinhill.discord4droid.json.generic.RoleResponse;
import zone.pumpkinhill.discord4droid.json.requests.ChannelCreateRequest;
import zone.pumpkinhill.discord4droid.json.requests.EditGuildRequest;
import zone.pumpkinhill.discord4droid.json.requests.MemberEditRequest;
import zone.pumpkinhill.discord4droid.json.requests.ReorderRolesRequest;
import zone.pumpkinhill.discord4droid.json.requests.TransferOwnershipRequest;
import zone.pumpkinhill.discord4droid.json.responses.ChannelResponse;
import zone.pumpkinhill.discord4droid.json.responses.ExtendedInviteResponse;
import zone.pumpkinhill.discord4droid.json.responses.GuildResponse;
import zone.pumpkinhill.discord4droid.json.responses.PruneResponse;
import zone.pumpkinhill.discord4droid.json.responses.UserResponse;
import zone.pumpkinhill.discord4droid.util.DiscordException;
import zone.pumpkinhill.discord4droid.util.HTTP429Exception;
import zone.pumpkinhill.discord4droid.util.MissingPermissionsException;
import zone.pumpkinhill.http.entity.StringEntity;
import zone.pumpkinhill.http.message.BasicNameValuePair;

/**
 * This class defines a guild/server/clan/whatever it's called.
 */
public class Guild {
    private final static String TAG = Guild.class.getCanonicalName();

    /**
     * All text channels in the guild.
     */
    protected final List<Channel> channels;

    /**
     * All voice channels in the guild.
     */
    protected final List<VoiceChannel> voiceChannels;

    /**
     * All users connected to the guild.
     */
    protected final List<User> users;

    /**
     * The name of the guild.
     */
    protected String name;

    /**
     * The ID of this guild.
     */
    protected final String id;

    /**
     * The location of the guild icon
     */
    protected String icon;

    /**
     * The url pointing to the guild icon
     */
    protected String iconURL;

    /**
     * The user id for the owner of the guild
     */
    protected String ownerID;

    /**
     * The roles the guild contains.
     */
    protected final List<Role> roles;

    /**
     * The channel where those who are afk are moved to.
     */
    protected String afkChannel;
    /**
     * The time in seconds for a user to be idle to be determined as "afk".
     */
    protected int afkTimeout;

    /**
     * The region this guild is located in.
     */
    protected String regionID;

    /**
     * This guild's audio channel.
     */
    protected AudioChannel audioChannel;

    /**
     * The client that created this object.
     */
    protected final DiscordClient client;

    public Guild(DiscordClient client, String name, String id, String icon, String ownerID,
                 String afkChannel, int afkTimeout, String region) {
        this(client, name, id, icon, ownerID, afkChannel, afkTimeout, region, new ArrayList<Role>(),
                new ArrayList<Channel>(), new ArrayList<VoiceChannel>(), new ArrayList<User>());
    }

    public Guild(DiscordClient client, String name, String id, String icon, String ownerID,
                 String afkChannel, int afkTimeout, String region, List<Role> roles,
                 List<Channel> channels, List<VoiceChannel> voiceChannels, List<User> users) {
        this.client = client;
        this.name = name;
        this.voiceChannels = voiceChannels;
        this.channels = channels;
        this.users = users;
        this.id = id;
        this.icon = icon;
        this.iconURL = String.format(client.getCDN() + Endpoints.ICONS, this.id, this.icon);
        this.ownerID = ownerID;
        this.roles = roles;
        this.afkChannel = afkChannel;
        this.afkTimeout = afkTimeout;
        this.regionID = region;
        this.audioChannel = new AudioChannel(client);
    }

    /**
     * Gets the user id for the owner of this guild.
     *
     * @return The owner id.
     */
    public String getOwnerID() {
        return ownerID;
    }

    /**
     * Gets the user object for the owner of this guild.
     *
     * @return The owner.
     */
    public User getOwner() {
        return client.getUserByID(ownerID);
    }

    /**
     * Sets the CACHED owner id.
     *
     * @param id The user if of the new owner.
     */
    public void setOwnerID(String id) {
        ownerID = id;
    }

    /**
     * Gets the icon id for this guild.
     *
     * @return The icon id.
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Gets the direct link to the guild's icon.
     *
     * @return The icon url.
     */
    public String getIconURL() {
        return iconURL;
    }

    /**
     * Sets the CACHED icon id for the guild.
     *
     * @param icon The icon id.
     */
    public void setIcon(String icon) {
        this.icon = icon;
        this.iconURL = String.format(client.getCDN() + Endpoints.ICONS, this.id, this.icon);
    }

    /**
     * Gets all the channels on the server.
     *
     * @return All channels on the server.
     */
    public List<Channel> getChannels() {
        return channels;
    }

    /**
     * Gets a channel on the guild by a specific channel id.
     *
     * @param id The ID of the channel you want to find.
     * @return The channel with given ID.
     */
    public Channel getChannelByID(String id) {
        for(Channel c : channels) {
            if(c.getID().equalsIgnoreCase(id)) return c;
        }
        return null;
    }

    /**
     * Gets all the users connected to the guild.
     *
     * @return All users connected to the guild.
     */
    public List<User> getUsers() {
        return users;
    }

    /**
     * Gets a user by its id in the guild.
     *
     * @param id ID of the user you want to find.
     * @return The user with given ID.
     */
    public User getUserByID(String id) {
        if (users == null)
            return null;
        for(User u : users) {
            if(u.getID().equalsIgnoreCase(id)) return u;
        }
        return null;
    }

    /**
     * Gets the name of the guild.
     *
     * @return The name of the guild
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the CACHED name of the guild.
     *
     * @param name The name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the id of the guild.
     *
     * @return The ID of this guild.
     */
    public String getID() {
        return id;
    }

    /**
     * CACHES a user to the guild.
     *
     * @param user The user.
     */
    public void addUser(User user) {
        if (user != null && !this.users.contains(user))
            this.users.add(user);
    }

    /**
     * CACHES a channel to the guild.
     *
     * @param channel The channel.
     */
    public void addChannel(Channel channel) {
        if (!this.channels.contains(channel) && !(channel instanceof VoiceChannel) && !(channel instanceof PrivateChannel))
            this.channels.add(channel);
    }

    /**
     * Gets the roles contained in this guild.
     *
     * @return The list of roles in the guild.
     */
    public List<Role> getRoles() {
        return roles;
    }

    /**
     * CACHES a role to the guild.
     *
     * @param role The role.
     */
    public void addRole(Role role) {
        if (!this.roles.contains(role))
            this.roles.add(role);
    }

    /**
     * Gets a role object for its unique id.
     *
     * @param id The role id of the desired role.
     * @return The role, or null if not found.
     */
    public Role getRoleByID(String id) {
        for(Role r : roles) {
            if(r.getID().equalsIgnoreCase(id)) return r;
        }
        return null;
    }

    /**
     * Gets the voice channels in this guild.
     *
     * @return The voice channels.
     */
    public List<VoiceChannel> getVoiceChannels() {
        return voiceChannels;
    }

    /**
     * Gets a voice channel for a give id.
     *
     * @param id The channel id.
     * @return The voice channel (or null if not found).
     */
    public VoiceChannel getVoiceChannelByID(String id) {
        for(VoiceChannel c : voiceChannels) {
            if(c.getID().equalsIgnoreCase(id)) return c;
        }
        return null;
    }

    /**
     * Gets the channel where afk users are placed.
     *
     * @return The voice channel (or null if nonexistant).
     */
    public VoiceChannel getAFKChannel() {
        return getVoiceChannelByID(afkChannel);
    }

    /**
     * Gets the timeout (in seconds) before a user is placed in the AFK channel.
     *
     * @return The timeout.
     */
    public int getAFKTimeout() {
        return afkTimeout;
    }

    public void setAFKChannel(String id) {
        this.afkChannel = id;
    }

    public void setAfkTimeout(int timeout) {
        this.afkTimeout = timeout;
    }

    public void addVoiceChannel(VoiceChannel channel) {
        if (!voiceChannels.contains(channel)) // && !(channel instanceof PrivateChannel))
            voiceChannels.add(channel);
    }

    /**
     * Creates a new role in this guild.
     *
     * @return The new role.
     *
     * @throws MissingPermissionsException
     * @throws HTTP429Exception
     * @throws DiscordException
     */
    public Role createRole() throws MissingPermissionsException, HTTP429Exception, DiscordException {
        DiscordUtils.checkPermissions(client, this, EnumSet.of(Permissions.MANAGE_ROLES));

        RoleResponse response = DiscordUtils.GSON.fromJson(
                Requests.POST.makeRequest(client.getURL() + Endpoints.GUILDS + id + "/roles",
                new BasicNameValuePair("authorization", client.getToken())), RoleResponse.class);
        return DiscordUtils.getRoleFromJSON(this, response);
    }

    /**
     * Retrieves the list of banned users from this guild.
     *
     * @return The list of banned users.
     *
     * @throws HTTP429Exception
     * @throws DiscordException
     */
    public List<User> getBannedUsers() throws HTTP429Exception, DiscordException {
        UserResponse[] users = DiscordUtils.GSON.fromJson(
                Requests.GET.makeRequest(client.getURL() + Endpoints.GUILDS + id + "/bans",
                new BasicNameValuePair("authorization", client.getToken())), UserResponse[].class);
        List<User> banned = new ArrayList<>();
        for (UserResponse user : users) {
            banned.add(DiscordUtils.getUserFromJSON(client, user));
        }
        return banned;
    }

    /**
     * Bans a user from this guild.
     *
     * @param user The user to ban.
     *
     * @throws MissingPermissionsException
     * @throws HTTP429Exception
     * @throws DiscordException
     */
    public void banUser(User user) throws MissingPermissionsException, HTTP429Exception, DiscordException {
        banUser(user, 0);
    }

    /**
     * Bans a user from this guild.
     *
     * @param user The user to ban.
     * @param deleteMessagesForDays The number of days to delete messages from this user for.
     *
     * @throws MissingPermissionsException
     * @throws HTTP429Exception
     * @throws DiscordException
     */
    public void banUser(User user, int deleteMessagesForDays) throws MissingPermissionsException, HTTP429Exception, DiscordException {
        DiscordUtils.checkPermissions(client, this, EnumSet.of(Permissions.BAN));

        Requests.PUT.makeRequest(client.getURL() + Endpoints.GUILDS + id + "/bans/" +
                user.getID() + "?delete-message-days=" + deleteMessagesForDays,
                new BasicNameValuePair("authorization", client.getToken()));
    }

    /**
     * This removes a ban on a user.
     *
     * @param userID The user to unban.
     *
     * @throws MissingPermissionsException
     * @throws HTTP429Exception
     * @throws DiscordException
     */
    public void pardonUser(String userID) throws MissingPermissionsException, HTTP429Exception, DiscordException {
        DiscordUtils.checkPermissions(client, this, EnumSet.of(Permissions.BAN));

        Requests.DELETE.makeRequest(client.getURL() + Endpoints.GUILDS + id + "/bans/" + userID,
                new BasicNameValuePair("authorization", client.getToken()));
    }

    /**
     * Kicks a user from the guild.
     *
     * @param user The user to kick.
     *
     * @throws MissingPermissionsException
     * @throws HTTP429Exception
     * @throws DiscordException
     */
    public void kickUser(User user) throws MissingPermissionsException, HTTP429Exception, DiscordException {
        DiscordUtils.checkPermissions(client, this, EnumSet.of(Permissions.KICK));

        Requests.DELETE.makeRequest(client.getURL() + Endpoints.GUILDS + id + "/members/" + user.getID(),
                new BasicNameValuePair("authorization", client.getToken()));
    }

    /**
     * Edits the roles a user is a part of.
     *
     * @param user The user to edit the roles for.
     * @param roles The roles for the user to have.
     *
     * @throws MissingPermissionsException
     * @throws HTTP429Exception
     * @throws DiscordException
     */
    public void editUserRoles(User user, Role[] roles) throws MissingPermissionsException, HTTP429Exception, DiscordException {
        DiscordUtils.checkPermissions(client, this, EnumSet.of(Permissions.MANAGE_ROLES));

        try {
            Requests.PATCH.makeRequest(client.getURL() + Endpoints.GUILDS + id + "/members/" + user.getID(),
                    new StringEntity(DiscordUtils.GSON.toJson(new MemberEditRequest(roles))),
                    new BasicNameValuePair("authorization", client.getToken()),
                    new BasicNameValuePair("content-type", "application/json"));
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error editing roles: " + e);
        }
    }

    public void edit(String name, String regionID, String icon, String afkChannelID, Integer afkTimeout) throws MissingPermissionsException, HTTP429Exception, DiscordException {
        DiscordUtils.checkPermissions(client, this, EnumSet.of(Permissions.MANAGE_SERVER));

        try {
            GuildResponse response = DiscordUtils.GSON.fromJson(
                    Requests.PATCH.makeRequest(client.getURL() + Endpoints.GUILDS + id,
                    new StringEntity(DiscordUtils.GSON.toJson(new EditGuildRequest(
                            name == null ? this.name : name,
                            regionID == null ? this.regionID : regionID,
                            icon == null ? this.icon : icon,
                            afkChannelID == null ? this.afkChannel : afkChannelID,
                            afkTimeout == null ? this.afkTimeout : afkTimeout))),
                    new BasicNameValuePair("authorization", client.getToken()),
                    new BasicNameValuePair("content-type", "application/json")), GuildResponse.class);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error editing guild: " + e);
        }
    }

    /**
     * This deletes this guild if and only if you are its owner, otherwise it throws a {@link MissingPermissionsException}.
     *
     * @throws DiscordException
     * @throws HTTP429Exception
     * @throws MissingPermissionsException
     */
    public void deleteGuild() throws DiscordException, HTTP429Exception, MissingPermissionsException {
        if(client.getOurUser() == null) {
            Log.w(TAG, "Trying to delete guild before logging in. Ignoring.");
            return;
        }
        if (!ownerID.equals(client.getOurUser().getID()))
            throw new MissingPermissionsException("You must be the guild owner to delete guilds!");

        Requests.DELETE.makeRequest(client.getURL() + Endpoints.GUILDS + id,
                new BasicNameValuePair("authorization", client.getToken()));
    }

    /**
     * This leaves the guild, NOTE: it throws a {@link DiscordException} if you are the guilds owner, use
     * {@link #deleteGuild()} instead!
     *
     * @throws DiscordException
     * @throws HTTP429Exception
     */
    public void leaveGuild() throws DiscordException, HTTP429Exception {
        if(client.getOurUser() == null) {
            Log.w(TAG, "Trying to leave guild before logging in. Ignoring.");
            return;
        }
        if (ownerID.equals(client.getOurUser().getID()))
            throw new DiscordException("Guild owners cannot leave their own guilds! Use deleteGuild() instead.");

        Requests.DELETE.makeRequest(client.getURL() + Endpoints.USERS + "@me/guilds/" + id,
                new BasicNameValuePair("authorization", client.getToken()));
    }

    /**
     * Creates a new channel.
     *
     * @param name The name of the new channel. MUST be between 2-100 characters long.
     * @return The new channel.
     *
     * @throws DiscordException
     * @throws MissingPermissionsException
     * @throws HTTP429Exception
     */
    public Channel createChannel(String name) throws DiscordException, MissingPermissionsException, HTTP429Exception {
        DiscordUtils.checkPermissions(client, this, EnumSet.of(Permissions.MANAGE_CHANNELS));

        if (!client.isReady()) {
            Log.w(TAG, "Trying to create channel before logging in. Ignoring.");
            return null;
        }

        if (name == null || name.length() < 2 || name.length() > 100)
            throw new DiscordException("Channel name can only be between 2 and 100 characters!");
        try {
            ChannelResponse response = DiscordUtils.GSON.fromJson(
                    Requests.POST.makeRequest(client.getURL() + Endpoints.GUILDS + getID() + "/channels",
                            new StringEntity(DiscordUtils.GSON.toJson(new ChannelCreateRequest(name, "text"))),
                            new BasicNameValuePair("authorization", client.getToken()),
                            new BasicNameValuePair("content-type", "application/json")),
                    ChannelResponse.class);

            Channel channel = DiscordUtils.getChannelFromJSON(client, this, response);
            addChannel(channel);

            return channel;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error creating channel: " + e);
        }
        return null;
    }

    /**
     * Creates a new voice channel.
     *
     * @param name The name of the new channel. MUST be between 2-100 characters long.
     * @return The new channel.
     *
     * @throws DiscordException
     * @throws MissingPermissionsException
     * @throws HTTP429Exception
     */
    public VoiceChannel createVoiceChannel(String name) throws DiscordException, MissingPermissionsException, HTTP429Exception {
        DiscordUtils.checkPermissions(client, this, EnumSet.of(Permissions.MANAGE_CHANNELS));

        if (!client.isReady()) {
            Log.w(TAG, "Trying to create voice channel before logging in. Ignoring.");
            return null;
        }

        if (name == null || name.length() < 2 || name.length() > 100)
            throw new DiscordException("Channel name can only be between 2 and 100 characters!");
        try {
            ChannelResponse response = DiscordUtils.GSON.fromJson(
                    Requests.POST.makeRequest(client.getURL() + Endpoints.GUILDS + getID() + "/channels",
                            new StringEntity(DiscordUtils.GSON.toJson(new ChannelCreateRequest(name, "voice"))),
                            new BasicNameValuePair("authorization", client.getToken()),
                            new BasicNameValuePair("content-type", "application/json")),
                    ChannelResponse.class);

            VoiceChannel channel = DiscordUtils.getVoiceChannelFromJSON(client, this, response);
            addVoiceChannel(channel);

            return channel;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error creating voice channel: " + e);
        }
        return null;
    }

    /**
     * Gets the region this guild is located in.
     *
     * @return The region.
     */
    public Region getRegion() {
        return client.getRegionByID(regionID);
    }

    /**
     * CACHES the region for this guild.
     *
     * @param regionID The region.
     */
    public void setRegion(String regionID) {
        this.regionID = regionID;
    }

    /**
     * Transfers the ownership of this guild to another user.
     *
     * @param newOwner The new owner.
     *
     * @throws HTTP429Exception
     * @throws MissingPermissionsException
     * @throws DiscordException
     */
    public void transferOwnership(User newOwner) throws HTTP429Exception, MissingPermissionsException, DiscordException {
        if(client.getOurUser() == null) {
            Log.w(TAG, "Trying to change guild owner before logging in. Ignoring.");
            return;
        }
        if (!getOwnerID().equals(client.getOurUser().getID()))
            throw new MissingPermissionsException("Cannot transfer ownership when you aren't the current owner!");
        try {
            GuildResponse response = DiscordUtils.GSON.fromJson(
                    Requests.PATCH.makeRequest(client.getURL() + Endpoints.GUILDS + id,
                    new StringEntity(DiscordUtils.GSON.toJson(new TransferOwnershipRequest(newOwner.getID()))),
                    new BasicNameValuePair("authorization", client.getToken()),
                    new BasicNameValuePair("content-type", "application/json")), GuildResponse.class);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error transferring guild: " + e);
        }
    }

    /**
     * This retrieves the @everyone role which exists on all guilds.
     *
     * @return The object representing the @everyone role.
     */
    public Role getEveryoneRole() {
        for(Role r : getRoles()) {
            if(r.getName().equals("@everyone")) return r;
        }
        return null;
    }

    /**
     * This gets all the currently available invites for this guild.
     *
     * @return The list of all available invites.
     *
     * @throws DiscordException
     * @throws HTTP429Exception
     */
    public List<Invite> getInvites() throws DiscordException, HTTP429Exception {
        ExtendedInviteResponse[] response = DiscordUtils.GSON.fromJson(
                Requests.GET.makeRequest(client.getURL() + Endpoints.GUILDS + id + "/invites",
                        new BasicNameValuePair("authorization", client.getToken()),
                        new BasicNameValuePair("content-type", "application/json")), ExtendedInviteResponse[].class);

        List<Invite> invites = new ArrayList<>();
        for (ExtendedInviteResponse inviteResponse : response)
            invites.add(DiscordUtils.getInviteFromJSON(client, inviteResponse));

        return invites;
    }

    /**
     * This reorders the position of the roles in this guild.
     *
     * @param rolesInOrder ALL the roles in the server, in the order of desired position. The first role gets position 1, second position 2, etc.
     *
     * @throws DiscordException
     * @throws HTTP429Exception
     * @throws MissingPermissionsException
     */
    public void reorderRoles(Role... rolesInOrder) throws DiscordException, HTTP429Exception, MissingPermissionsException {
        if (rolesInOrder.length != getRoles().size())
            throw new DiscordException("The number of roles to reorder does not equal the number of available roles!");

        DiscordUtils.checkPermissions(client, this, EnumSet.of(Permissions.MANAGE_ROLES));

        ReorderRolesRequest[] request = new ReorderRolesRequest[rolesInOrder.length];

        for (int i = 0; i < rolesInOrder.length; i++) {
            request[i] = new ReorderRolesRequest(rolesInOrder[i].getID(),
                    rolesInOrder[i].getName().equals("@everyone") ? -1 : i+1);
        }

        try {
            RoleResponse[] response = DiscordUtils.GSON.fromJson(Requests.PATCH.makeRequest(
                    client.getURL() + Endpoints.GUILDS + id + "/roles",
                    new StringEntity(DiscordUtils.GSON.toJson(request)),
                    new BasicNameValuePair("authorization", client.getToken()),
                    new BasicNameValuePair("content-type", "application/json")), RoleResponse[].class);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error reordering roles: " + e);
        }
    }

    /**
     * Gets the amount of users that would be pruned for the given amount of days.
     *
     * @param days The amount of days of inactivity to lead to a prune.
     * @return The amount of users.
     *
     * @throws DiscordException
     * @throws HTTP429Exception
     */
    public int getUsersToBePruned(int days) throws DiscordException, HTTP429Exception {
        PruneResponse response = DiscordUtils.GSON.fromJson(
                Requests.GET.makeRequest(client.getURL() + Endpoints.GUILDS + id + "/prune?days=" + days,
                        new BasicNameValuePair("authorization", client.getToken()),
                        new BasicNameValuePair("content-type", "application/json")), PruneResponse.class);
        return response.pruned;
    }

    /**
     * Prunes guild users for the given amount of days.
     *
     * @param days The amount of days of inactivity to lead to a prune.
     * @return The amount of users.
     *
     * @throws DiscordException
     * @throws HTTP429Exception
     */
    public int pruneUsers(int days) throws DiscordException, HTTP429Exception {
        PruneResponse response = DiscordUtils.GSON.fromJson(
                Requests.POST.makeRequest(client.getURL() + Endpoints.GUILDS + id + "/prune?days=" + days,
                        new BasicNameValuePair("authorization", client.getToken()),
                        new BasicNameValuePair("content-type", "application/json")), PruneResponse.class);
        return response.pruned;
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
     * Gets the audio channel of this guild. This throws an exception if the bot isn't in a channel yet.
     *
     * @return The audio channel.
     *
     * @throws DiscordException
     */
    public AudioChannel getAudioChannel() throws DiscordException {
        return audioChannel;
    }

    /**
     * This gets the client that this object is tied to.
     *
     * @return The client.
     */
    public DiscordClient getClient() {
        return client;
    }

    @Override
    public boolean equals(Object other) {
        return this.getClass().isAssignableFrom(other.getClass()) && ((Guild) other).getID().equals(getID());
    }
}
