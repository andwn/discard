package zone.pumpkinhill.discord4droid.handle.obj;

import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import zone.pumpkinhill.discord4droid.api.DiscordClient;
import zone.pumpkinhill.discord4droid.api.DiscordUtils;
import zone.pumpkinhill.discord4droid.api.Endpoints;
import zone.pumpkinhill.discord4droid.api.Requests;
import zone.pumpkinhill.discord4droid.handle.events.MessageSendEvent;
import zone.pumpkinhill.discord4droid.json.generic.PermissionOverwrite;
import zone.pumpkinhill.discord4droid.json.requests.ChannelEditRequest;
import zone.pumpkinhill.discord4droid.json.requests.InviteRequest;
import zone.pumpkinhill.discord4droid.json.requests.MessageRequest;
import zone.pumpkinhill.discord4droid.json.responses.ExtendedInviteResponse;
import zone.pumpkinhill.discord4droid.json.responses.MessageResponse;
import zone.pumpkinhill.discord4droid.util.DiscordException;
import zone.pumpkinhill.discord4droid.util.HTTP429Exception;
import zone.pumpkinhill.discord4droid.util.MessageList;
import zone.pumpkinhill.discord4droid.util.MissingPermissionsException;
import zone.pumpkinhill.http.HttpEntity;
import zone.pumpkinhill.http.entity.ContentType;
import zone.pumpkinhill.http.entity.StringEntity;
import zone.pumpkinhill.http.entity.mime.MultipartEntityBuilder;
import zone.pumpkinhill.http.message.BasicNameValuePair;

/**
 * Defines a text channel in a guild/server.
 */
public class Channel {
    private final static String TAG = Channel.class.getCanonicalName();

    /**
     * User-friendly channel name (e.g. "general")
     */
    protected String name;

    /**
     * Channel ID.
     */
    protected final String id;

    /**
     * Messages that have been sent into this channel
     */
    protected final MessageList messages;

    /**
     * Indicates whether or not this channel is a PM channel.
     */
    protected boolean isPrivate;

    /**
     * The guild this channel belongs to.
     */
    protected final Guild parent;

    /**
     * The channel's topic message.
     */
    protected String topic;

    /**
     * The last read message.
     */
    protected String lastReadMessageID = null;

    /**
     * Whether the bot should send out a typing status
     */
    protected AtomicBoolean isTyping = new AtomicBoolean(false);

    /**
     * Manages all TimerTasks which send typing statuses.
     */
    protected static Timer typingTimer = new Timer("Typing Status Timer", true);

    /**
     * 5 seconds, the time it takes for one typing status to "wear off".
     */
    protected static final long TIME_FOR_TYPE_STATUS = 5000;

    /**
     * The position of this channel in the channel list.
     */
    protected int position;

    /**
     * The permission overrides for users (key = user id).
     */
    protected Map<String, PermissionOverride> userOverrides;

    /**
     * The permission overrides for roles (key = user id).
     */
    protected Map<String, PermissionOverride> roleOverrides;

    /**
     * The client that created this object.
     */
    protected final DiscordClient client;

    public Channel(DiscordClient client, String name, String id, Guild parent, String topic, int position) {
        this(client, name, id, parent, topic, position,
                new HashMap<String, PermissionOverride>(), new HashMap<String, PermissionOverride>());
    }

    public Channel(DiscordClient client, String name, String id, Guild parent, String topic,
                   int position, Map<String, PermissionOverride> roleOverrides,
                   Map<String, PermissionOverride> userOverrides) {
        this.client = client;
        this.name = name;
        this.id = id;
        this.parent = parent;
        this.isPrivate = false;
        this.topic = topic;
        this.position = position;
        this.roleOverrides = roleOverrides;
        this.userOverrides = userOverrides;
        if (!(this instanceof VoiceChannel))
            this.messages = new MessageList(client, this);
        else
            this.messages = null;
    }

    /**
     * Gets the name of this channel.
     *
     * @return The channel name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the CACHED name of the channel.
     *
     * @param name The name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the id of this channel.
     *
     * @return The channel id.
     */
    public String getID() {
        return id;
    }

    /**
     * Gets the messages in this channel.
     *
     * @return The list of messages in the channel.
     */
    public MessageList getMessages() {
        return messages;
    }

    /**
     * Gets a specific message by its id.
     *
     * @param messageID The message id.
     * @return The message (if found).
     */
    public Message getMessageByID(String messageID) {
        if (messages == null)
            return null;

        return messages.get(messageID);
    }

    /**
     * Gets the guild this channel is a part of.
     *
     * @return The guild.
     */
    public Guild getGuild() {
        return parent;
    }

    /**
     * Gets whether or not this channel is a private one–if it is a private one, this object is an instance of PrivateChannel.
     *
     * @return True if the channel is private, false if otherwise.
     */
    public boolean isPrivate() {
        return isPrivate;
    }

    /**
     * Gets the topic for the channel.
     *
     * @return The channel topic (null if not set).
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Sets the CACHED topic for the channel.
     *
     * @param topic The new channel topic
     */
    public void setTopic(String topic) {
        this.topic = topic;
    }

    /**
     * Formats a string to be able to #mention this channel.
     *
     * @return The formatted string.
     */
    public String mention() {
        return "<#"+this.getID()+">";
    }

    /**
     * Sends a message without tts to the desired channel.
     *
     * @param content The content of the message.
     * @return The message object representing the sent message
     *
     * @throws MissingPermissionsException
     * @throws HTTP429Exception
     * @throws DiscordException
     */
    public Message sendMessage(String content) throws MissingPermissionsException, HTTP429Exception, DiscordException {
        return sendMessage(content, false);
    }

    /**
     * Sends a message to the desired channel.
     *
     * @param content The content of the message.
     * @param tts Whether the message should use tts or not.
     * @return The message object representing the sent message
     *
     * @throws MissingPermissionsException
     * @throws HTTP429Exception
     * @throws DiscordException
     */
    public Message sendMessage(String content, boolean tts) throws MissingPermissionsException, HTTP429Exception, DiscordException {
        DiscordUtils.checkPermissions(client, this, EnumSet.of(Permissions.SEND_MESSAGES));
        if (client.isReady()) {
            MessageResponse response = DiscordUtils.GSON.fromJson(
                    Requests.POST.makeRequest(client.getURL() + Endpoints.CHANNELS + id + "/messages",
                    new StringEntity(DiscordUtils.GSON.toJson(new MessageRequest(content, tts)), "UTF-8"),
                    new BasicNameValuePair("authorization", client.getToken()),
                    new BasicNameValuePair("content-type", "application/json")), MessageResponse.class);
            Log.v(TAG, "Sending: " + response.toString());
            return DiscordUtils.getMessageFromJSON(client, this, response);
        } else {
            Log.w(TAG, "Trying to send message before logged in. Ignoring.");
            return null;
        }
    }

    /**
     * Sends a file to the channel.
     *
     * @param stream Open InputStream to send.
     * @param type MIME type of the file
     * @param name Name of the file (can be made up, just name sure the extension is right)
     * @param content The message to be sent with the file.
     * @return The message sent.
     *
     * @throws IOException
     * @throws MissingPermissionsException
     * @throws HTTP429Exception
     * @throws DiscordException
     */
    public Message sendFile(InputStream stream, ContentType type, String name, @Nullable String content)
            throws IOException, MissingPermissionsException, HTTP429Exception, DiscordException {
        DiscordUtils.checkPermissions(client, this, EnumSet.of(Permissions.SEND_MESSAGES, Permissions.ATTACH_FILES));
        if (!client.isReady()) {
            Log.w(TAG, "Trying to send file before logging in. Ignoring.");
            return null;
        }
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("file", stream, type, name);
        if (content != null) {
            builder.addTextBody("content", content);
        }
        HttpEntity fileEntity = builder.build();
        MessageResponse response = DiscordUtils.GSON.fromJson(Requests.POST.makeRequest(
                client.getURL() + Endpoints.CHANNELS + id + "/messages",
                fileEntity, new BasicNameValuePair("authorization", client.getToken())), MessageResponse.class);
        Message message = DiscordUtils.getMessageFromJSON(client, this, response);
        client.getDispatcher().dispatch(new MessageSendEvent(message));
        return message;
    }

    /**
     * Sends a file to the channel.
     *
     * @param file The file to send.
     * @param content The message to be sent with the file.
     * @return The message sent.
     *
     * @throws IOException
     * @throws MissingPermissionsException
     * @throws HTTP429Exception
     * @throws DiscordException
     */
    public Message sendFile(File file, @Nullable String content)
            throws IOException, MissingPermissionsException, HTTP429Exception, DiscordException {
        InputStream stream = new FileInputStream(file);
        ContentType type = ContentType.create(
                URLConnection.getFileNameMap().getContentTypeFor(file.getAbsolutePath()));
        String name = file.getName();
        Message msg = sendFile(stream, type, name, content);
        stream.close();
        return msg;
    }

    /**
     * Sends a file to the channel with a message attached.
     *
     * @param file The file to send.
     * @return The message sent.
     *
     * @throws IOException
     * @throws MissingPermissionsException
     * @throws HTTP429Exception
     * @throws DiscordException
     */
    public Message sendFile(File file)
            throws IOException, MissingPermissionsException, HTTP429Exception, DiscordException {
        return sendFile(file, null);
    }

    /**
     * Generates an invite for this channel.
     *
     * @param maxAge How long the invite should be valid, setting it to 0 makes it last forever.
     * @param maxUses The maximum uses for the invite, setting it to 0 makes the invite have unlimited uses.
     * @param temporary Whether users admitted with this invite are temporary.
     * @param useXkcdPass Whether to generate a human-readable code, maxAge cannot be 0 for this to work.
     * @return The newly generated invite.
     *
     * @throws MissingPermissionsException
     * @throws HTTP429Exception
     * @throws DiscordException
     */
    public Invite createInvite(int maxAge, int maxUses, boolean temporary, boolean useXkcdPass) throws MissingPermissionsException, HTTP429Exception, DiscordException {
        DiscordUtils.checkPermissions(client, this, EnumSet.of(Permissions.CREATE_INVITE));
        if (!client.isReady()) {
            Log.w(TAG, "Trying to invite before logging in. Ignoring.");
            return null;
        }
        try {
            ExtendedInviteResponse response = DiscordUtils.GSON.fromJson(
                    Requests.POST.makeRequest(client.getURL() + Endpoints.CHANNELS+getID()+"/invites",
                    new StringEntity(DiscordUtils.GSON.toJson(new InviteRequest(maxAge, maxUses, temporary, useXkcdPass))),
                    new BasicNameValuePair("authorization", client.getToken()),
                    new BasicNameValuePair("content-type", "application/json")), ExtendedInviteResponse.class);
            return DiscordUtils.getInviteFromJSON(client, response);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error encoding invite: " + e);
        }

        return null;
    }

    /**
     * Toggles whether the bot is "typing".
     */
    public synchronized void toggleTypingStatus() {
        isTyping.set(!isTyping.get());
        typingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isTyping.get())
                    this.cancel();
                try {
                    Requests.POST.makeRequest(client.getURL() + Endpoints.CHANNELS+getID()+"/typing",
                            new BasicNameValuePair("authorization", client.getToken()));
                } catch (HTTP429Exception | DiscordException e) {
                    Log.e(TAG, "Error setting typing status: " + e);
                }
            }
        }, 0, TIME_FOR_TYPE_STATUS);
    }

    /**
     * Gets whether the bot is "typing".
     *
     * @return True if the bot is typing, false if otherwise.
     */
    public synchronized boolean getTypingStatus() {
        return isTyping.get();
    }

    /**
     * Gets the last read message id.
     *
     * @return The message id.
     */
    public String getLastReadMessageID() {
        return lastReadMessageID;
    }

    /**
     * Gets the last read message.
     *
     * @return The message.
     */
    public Message getLastReadMessage() {
        return getMessageByID(lastReadMessageID);
    }

    private void edit(String name, Integer position, String topic) throws DiscordException, MissingPermissionsException, HTTP429Exception {
        DiscordUtils.checkPermissions(client, this,
                EnumSet.of(Permissions.MANAGE_CHANNEL, Permissions.MANAGE_CHANNELS));
        String newName = name == null ? this.name : name;
        int newPosition = position == null ? this.position : position;
        String newTopic = topic == null ? this.topic : topic;
        if (newName == null || newName.length() < 2 || newName.length() > 100) {
            throw new DiscordException("Channel name can only be between 2 and 100 characters!");
        }
        try {
            Requests.PATCH.makeRequest(client.getURL() + Endpoints.CHANNELS+id,
                    new StringEntity(DiscordUtils.GSON.toJson(new ChannelEditRequest(newName, newPosition, newTopic))),
                    new BasicNameValuePair("authorization", client.getToken()),
                    new BasicNameValuePair("content-type", "application/json"));
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error editing channel: " + e);
        }
    }

    /**
     * Changes the name of the channel
     *
     * @param name The new name of the channel.
     * @throws HTTP429Exception
     * @throws DiscordException
     * @throws MissingPermissionsException
     */
    public void changeName(String name) throws HTTP429Exception, DiscordException, MissingPermissionsException {
        edit(name, null, null);
    }

    /**
     * Changes the position of the channel
     *
     * @param position The new position of the channel.
     * @throws HTTP429Exception
     * @throws DiscordException
     * @throws MissingPermissionsException
     */
    public void changePosition(int position) throws HTTP429Exception, DiscordException, MissingPermissionsException {
        edit(null, position, null);
    }

    /**
     * Changes the topic of the channel
     *
     * @param topic The new topic of the channel.
     * @throws HTTP429Exception
     * @throws DiscordException
     * @throws MissingPermissionsException
     */
    public void changeTopic(String topic) throws HTTP429Exception, DiscordException, MissingPermissionsException {
        edit(null, null, topic);
    }

    /**
     * Gets the position of the channel on the channel list.
     *
     * @return The position.
     */
    public int getPosition() {
        return position;
    }

    /**
     * Sets the CACHED position of the channel.
     *
     * @param position The position.
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * Deletes this channel.
     *
     * @throws MissingPermissionsException
     * @throws HTTP429Exception
     * @throws DiscordException
     */
    public void delete() throws MissingPermissionsException, HTTP429Exception, DiscordException {
        DiscordUtils.checkPermissions(client, this, EnumSet.of(Permissions.MANAGE_CHANNELS));
        Requests.DELETE.makeRequest(client.getURL() + Endpoints.CHANNELS+id,
                new BasicNameValuePair("authorization", client.getToken()));
    }

    /**
     * Sets the CACHED last read message id.
     *
     * @param lastReadMessageID The message id.
     */
    public void setLastReadMessageID(String lastReadMessageID) {
        this.lastReadMessageID = lastReadMessageID;
    }

    /**
     * Gets the permissions overrides for users. (Key = User id).
     *
     * @return The user permissions overrides for this channel.
     */
    public Map<String, PermissionOverride> getUserOverrides() {
        return userOverrides;
    }

    /**
     * Gets the permissions overrides for users. (Key = User id).
     *
     * @return The user permissions overrides for this channel.
     */
    public Map<String, PermissionOverride> getRoleOverrides() {
        return roleOverrides;
    }

    /**
     * Gets the permissions available for a user with all permission overrides taken into account.
     *
     * @param user The user to get the permissions for.
     * @return The set of permissions.
     */
    public EnumSet<Permissions> getModifiedPermissions(User user) {
        if (isPrivate || getGuild().getOwnerID().equals(user.getID())) {
            return EnumSet.allOf(Permissions.class);
        }
        List<Role> roles = user.getRolesForGuild(parent);
        EnumSet<Permissions> permissions = EnumSet.noneOf(Permissions.class);
        for(Role role : roles) {
            EnumSet<Permissions> rolePermissions = getModifiedPermissions(role);
            for(Permissions p : rolePermissions) {
                if(!permissions.contains(p)) {
                    permissions.add(p);
                }
            }
        }
        PermissionOverride override = getUserOverrides().get(user.getID());
        if (override == null) {
            return permissions;
        }
        for(Permissions allow : override.allow()) {
            permissions.add(allow);
        }
        for(Permissions deny : override.deny()) {
            permissions.remove(deny);
        }
        return permissions;
    }

    /**
     * Gets the permissions available for a role with all permission overrides taken into account.
     *
     * @param role The role to get the permissions for.
     * @return The set of permissions.
     */
    public EnumSet<Permissions> getModifiedPermissions(Role role) {
        EnumSet<Permissions> base = role.getPermissions();
        PermissionOverride override = getRoleOverrides().get(role.getID());
        if (override == null) {
            if ((override = getRoleOverrides().get(parent.getEveryoneRole().getID())) == null)
                return base;
        }
        for(Permissions allow : override.allow()) {
            base.add(allow);
        }
        for(Permissions deny : override.deny()) {
            base.remove(deny);
        }
        return base;
    }

    /**
     * CACHES a permissions override for a user in this channel.
     *
     * @param userId The user the permissions override is for.
     * @param override The permissions override.
     */
    public void addUserOverride(String userId, PermissionOverride override) {
        userOverrides.put(userId, override);
    }

    /**
     * CACHES a permissions override for a role in this channel.
     *
     * @param roleId The role the permissions override is for.
     * @param override The permissions override.
     */
    public void addRoleOverride(String roleId, PermissionOverride override) {
        roleOverrides.put(roleId, override);
    }

    /**
     * Removes a permissions override on this channel.
     *
     * @param user The user whose override should be removed.
     *
     * @throws MissingPermissionsException
     * @throws HTTP429Exception
     * @throws DiscordException
     */
    public void removePermissionsOverride(User user) throws MissingPermissionsException, HTTP429Exception, DiscordException {
        DiscordUtils.checkPermissions(client, this, EnumSet.of(Permissions.MANAGE_PERMISSIONS));
        Requests.DELETE.makeRequest(client.getURL() + Endpoints.CHANNELS+getID()+"/permissions/"+user.getID(),
                new BasicNameValuePair("authorization", client.getToken()));
        userOverrides.remove(user.getID());
    }

    /**
     * Removes a permissions override on this channel.
     *
     * @param role The role whose override should be removed.
     *
     * @throws MissingPermissionsException
     * @throws HTTP429Exception
     * @throws DiscordException
     */
    public void removePermissionsOverride(Role role) throws MissingPermissionsException, HTTP429Exception, DiscordException {
        DiscordUtils.checkPermissions(client, this, EnumSet.of(Permissions.MANAGE_PERMISSIONS));
        Requests.DELETE.makeRequest(client.getURL() + Endpoints.CHANNELS+getID()+"/permissions/"+role.getID(),
                new BasicNameValuePair("authorization", client.getToken()));
        roleOverrides.remove(role.getID());
    }

    /**
     * Creates/edits permission overrides for this channel.
     *
     * @param role The role to create/edit the permission overrides for.
     * @param toAdd The permissions to add.
     * @param toRemove The permissions to remove.
     *
     * @throws MissingPermissionsException
     * @throws HTTP429Exception
     * @throws DiscordException
     */
    public void overrideRolePermissions(Role role, EnumSet<Permissions> toAdd, EnumSet<Permissions> toRemove) throws MissingPermissionsException, HTTP429Exception, DiscordException {
        overridePermissions("role", role.getID(), toAdd, toRemove);
    }

    /**
     * Creates/edits permission overrides for this channel.
     *
     * @param user The user to create/edit the permission overrides for.
     * @param toAdd The permissions to add.
     * @param toRemove The permissions to remove.
     *
     * @throws MissingPermissionsException
     * @throws HTTP429Exception
     * @throws DiscordException
     */
    public void overrideUserPermissions(User user, EnumSet<Permissions> toAdd, EnumSet<Permissions> toRemove) throws MissingPermissionsException, HTTP429Exception, DiscordException {
        overridePermissions("member", user.getID(), toAdd, toRemove);
    }

    private void overridePermissions(String type, String id, EnumSet<Permissions> toAdd, EnumSet<Permissions> toRemove) throws MissingPermissionsException, HTTP429Exception, DiscordException {
        DiscordUtils.checkPermissions(client, this, EnumSet.of(Permissions.MANAGE_PERMISSIONS));
        try {
            Requests.PUT.makeRequest(client.getURL() + Endpoints.CHANNELS+getID()+"/permissions/"+id,
                    new StringEntity(DiscordUtils.GSON.toJson(new PermissionOverwrite(type, id,
                            Permissions.generatePermissionsNumber(toAdd),
                            Permissions.generatePermissionsNumber(toRemove)))),
                    new BasicNameValuePair("authorization", client.getToken()),
                    new BasicNameValuePair("content-type", "application/json"));
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error encoding permissions: " + e);
        }
    }

    /**
     * This gets all the currently available invites for this channel.
     *
     * @return The list of all available invites.
     *
     * @throws DiscordException
     * @throws HTTP429Exception
     */
    public List<Invite> getInvites() throws DiscordException, HTTP429Exception {
        ExtendedInviteResponse[] response = DiscordUtils.GSON.fromJson(
                Requests.GET.makeRequest(client.getURL() + Endpoints.CHANNELS + id + "/invites",
                        new BasicNameValuePair("authorization", client.getToken()),
                        new BasicNameValuePair("content-type", "application/json")),
                ExtendedInviteResponse[].class);

        List<Invite> invites = new ArrayList<>();
        for (ExtendedInviteResponse inviteResponse : response) {
            invites.add(DiscordUtils.getInviteFromJSON(client, inviteResponse));
        }
        return invites;
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
        return client;
    }

    @Override
    public String toString() {
        return mention();
    }

    @Override
    public boolean equals(Object other) {
        return this.getClass().isAssignableFrom(other.getClass()) && ((Channel) other).getID().equals(getID());
    }

    /**
     * Sets the CACHED typing status.
     *
     * @param typingStatus The new typing status.
     */
    public void setTypingStatus(boolean typingStatus) {
        this.isTyping.set(typingStatus);
    }
}
