package zone.pumpkinhill.discord4droid.api;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import zone.pumpkinhill.discord4droid.handle.obj.Attachment;
import zone.pumpkinhill.discord4droid.handle.obj.Channel;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.Invite;
import zone.pumpkinhill.discord4droid.handle.obj.Message;
import zone.pumpkinhill.discord4droid.handle.obj.PermissionOverride;
import zone.pumpkinhill.discord4droid.handle.obj.Permissions;
import zone.pumpkinhill.discord4droid.handle.obj.Presences;
import zone.pumpkinhill.discord4droid.handle.obj.PrivateChannel;
import zone.pumpkinhill.discord4droid.handle.obj.Region;
import zone.pumpkinhill.discord4droid.handle.obj.Role;
import zone.pumpkinhill.discord4droid.handle.obj.User;
import zone.pumpkinhill.discord4droid.handle.obj.UserVoiceState;
import zone.pumpkinhill.discord4droid.handle.obj.VoiceChannel;
import zone.pumpkinhill.discord4droid.json.generic.PermissionOverwrite;
import zone.pumpkinhill.discord4droid.json.generic.RoleResponse;
import zone.pumpkinhill.discord4droid.json.requests.GuildMembersRequest;
import zone.pumpkinhill.discord4droid.json.responses.ChannelResponse;
import zone.pumpkinhill.discord4droid.json.responses.GuildResponse;
import zone.pumpkinhill.discord4droid.json.responses.InviteJSONResponse;
import zone.pumpkinhill.discord4droid.json.responses.MessageResponse;
import zone.pumpkinhill.discord4droid.json.responses.PresenceResponse;
import zone.pumpkinhill.discord4droid.json.responses.PrivateChannelResponse;
import zone.pumpkinhill.discord4droid.json.responses.RegionResponse;
import zone.pumpkinhill.discord4droid.json.responses.UserResponse;
import zone.pumpkinhill.discord4droid.json.responses.VoiceStateResponse;
import zone.pumpkinhill.discord4droid.util.MissingPermissionsException;

/**
 * Collection of internal Discord4J utilities.
 */
public class DiscordUtils {
    private final static String TAG = DiscordUtils.class.getSimpleName();

    /**
     * Re-usable instance of Gson.
     */
    public static final Gson GSON = new GsonBuilder().serializeNulls().create();
    /**
     * Like {@link #GSON} but it doesn't serialize nulls.
     */
    public static final Gson GSON_NO_NULLS = new GsonBuilder().create();

    /**
     * Converts a String timestamp into a java object timestamp.
     *
     * @param time The String timestamp.
     * @return The java object representing the timestamp.
     */
    public static Date convertFromTimestamp(String time) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZZZZZ", Locale.US).parse(time);
        } catch(ParseException e) {
            Log.d(TAG, "" + e);
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(time);
        } catch(ParseException e) {
            Log.w(TAG, "Error parsing time: " + e);
            return new Date();
        }
    }

    /**
     * Returns a user from the java form of the raw JSON data.
     */
    public static User getUserFromJSON(DiscordClient client, UserResponse response) {
        if (response == null)
            return null;

        User user;
        if ((user = client.getUserByID(response.id)) != null) {
            user.setAvatar(response.avatar);
            if(response.username != null && !response.username.isEmpty())
                user.setName(response.username);
            user.setDiscriminator(response.discriminator);
        } else {
            user = new User(client, response.username, response.id, response.discriminator, response.avatar, Presences.OFFLINE);
        }
        return user;
    }

    /**
     * Creates a java {@link Invite} object for a json response.
     *
     * @param client The discord client to use.
     * @param json The json response to use.
     * @return The java invite object.
     */
    public static Invite getInviteFromJSON(DiscordClient client, InviteJSONResponse json) {
        return new Invite(client, json.code, json.xkcdpass);
    }

    /**
     * Gets the users mentioned from a message json object.
     *
     * @param json The json response to use.
     * @return The list of mentioned users.
     */
    public static List<String> getMentionsFromJSON(MessageResponse json) {
        List<String> mentions = new ArrayList<>();
        if (json.mentions != null)
            for (UserResponse response : json.mentions)
                mentions.add(response.id);

        return mentions;
    }

    /**
     * Gets the attachments on a message.
     *
     * @param json The json response to use.
     * @return The attached messages.
     */
    public static List<Attachment> getAttachmentsFromJSON(DiscordClient client, MessageResponse json) {
        List<Attachment> attachments = new ArrayList<>();
        if(json.attachments != null) {
            for (MessageResponse.AttachmentResponse response : json.attachments) {
                attachments.add(new Attachment(client, response.filename, response.size,
                        response.id, response.url));
            }
        }
        return attachments;
    }

    /**
     * Creates a guild object from a json response.
     *
     * @param client The discord client.
     * @param json The json response.
     * @return The guild object.
     */
    public static Guild getGuildFromJSON(DiscordClient client, GuildResponse json) {
        Guild guild;

        if ((guild = client.getGuildByID(json.id)) != null) {
            guild.setIcon(json.icon);
            guild.setName(json.name);
            guild.setOwnerID(json.owner_id);
            guild.setAFKChannel(json.afk_channel_id);
            guild.setAfkTimeout(json.afk_timeout);
            guild.setRegion(json.region);

            List<Role> newRoles = new ArrayList<>();
            for (RoleResponse roleResponse : json.roles) {
                newRoles.add(getRoleFromJSON(client, guild, roleResponse));
            }
            guild.getRoles().clear();
            guild.getRoles().addAll(newRoles);

            for (User user : guild.getUsers()) { //Removes all deprecated roles
                for (Role role : user.getRolesForGuild(guild)) {
                    if (guild.getRoleByID(role.getID()) == null) {
                        user.getRolesForGuild(guild).remove(role);
                    }
                }
            }
        } else {
            guild = new Guild(client, json.name, json.id, json.icon, json.owner_id, json.afk_channel_id, json.afk_timeout, json.region);

            if (json.roles != null)
                for (RoleResponse roleResponse : json.roles) {
                    getRoleFromJSON(client, guild, roleResponse); //Implicitly adds the role to the guild.
                }

            if (json.members != null)
                for (GuildResponse.MemberResponse member : json.members) {
                    User u = getUserFromGuildMemberResponse(client, guild, member);
                    if(u != null) guild.addUser(u);
                }

            if (json.large) { //The guild is large, we have to send a request to get the offline users
                client.ws.send(DiscordUtils.GSON.toJson(new GuildMembersRequest(json.id)));
            }

            if (json.presences != null)
                for (PresenceResponse presence : json.presences) {
                    User user = guild.getUserByID(presence.user.id);
                    if(user != null) {
                        user.setPresence(Presences.valueOf((presence.status).toUpperCase()));
                        user.setGame(presence.game == null ? null : presence.game.name);
                    }
                }

            if (json.channels != null)
                for (ChannelResponse channelResponse : json.channels) {
                    String channelType = channelResponse.type;
                    if (channelType.equalsIgnoreCase("text")) {
                        guild.addChannel(getChannelFromJSON(client, guild, channelResponse));
                    } else if (channelType.equalsIgnoreCase("voice")) {
                        guild.addVoiceChannel(getVoiceChannelFromJSON(client, guild, channelResponse));
                    }
                }

            if (json.voice_states != null) {
                for (VoiceStateResponse voiceState : json.voice_states) {
                    User u = guild.getUserByID(voiceState.user_id);
                    u.setVoiceChannel(guild.getVoiceChannelByID(voiceState.channel_id));
                    u.setVoiceState(new UserVoiceState(voiceState.self_mute, voiceState.self_deaf,
                            voiceState.mute, voiceState.deaf));
                }
            }
        }

        return guild;
    }

    /**
     * Creates a user object from a guild member json response.
     *
     * @param client The discord client.
     * @param guild The guild the member belongs to.
     * @param json The json response.
     * @return The user object.
     */
    public static User getUserFromGuildMemberResponse(DiscordClient client, Guild guild, GuildResponse.MemberResponse json) {
        User user = getUserFromJSON(client, json.user);
        for (String role : json.roles) {
            Role roleObj = guild.getRoleByID(role);
            if (roleObj != null && !user.getRolesForGuild(guild).contains(roleObj))
                user.addRole(guild.getID(), roleObj);
        }
        user.addRole(guild.getID(), guild.getRoleByID(guild.getID())); //@everyone role
        return user;
    }

    /**
     * Creates a private channel object from a json response.
     *
     * @param client The discord client.
     * @param json The json response.
     * @return The private channel object.
     */
    public static PrivateChannel getPrivateChannelFromJSON(DiscordClient client, PrivateChannelResponse json) {
        String id = json.id;
        User recipient = client.getUserByID(id);
        if (recipient == null) {
            recipient = getUserFromJSON(client, json.recipient);
        }
        PrivateChannel channel = null;
        for (PrivateChannel privateChannel : client.privateChannels) {
            if (privateChannel.getRecipient().equals(recipient)) {
                channel = privateChannel;
                break;
            }
        }
        if (channel == null) {
            channel = new PrivateChannel(client, recipient, id);
        }
        channel.setLastReadMessageID(json.last_message_id);
        return channel;
    }

    /**
     * Creates a message object from a json response.
     *
     * @param client The discord client.
     * @param channel The channel.
     * @param json The json response.
     * @return The message object.
     */
    public static Message getMessageFromJSON(DiscordClient client, Channel channel, MessageResponse json) {
        Message message;
        if ((message = channel.getMessageByID(json.id)) != null) {
            message.setAttachments(getAttachmentsFromJSON(client, json));
            message.setContent(json.content);
            message.setMentionsEveryone(json.mention_everyone);
            message.setMentions(getMentionsFromJSON(json));
            message.setTimestamp(convertFromTimestamp(json.timestamp));
            message.setEditedTimestamp(json.edited_timestamp == null ? null : convertFromTimestamp(json.edited_timestamp));
            return message;
        } else
            return new Message(client, json.id, json.content, getUserFromJSON(client, json.author),
                    channel, convertFromTimestamp(json.timestamp),
                    json.edited_timestamp == null ? null : convertFromTimestamp(json.edited_timestamp),
                    json.mention_everyone, getMentionsFromJSON(json), getAttachmentsFromJSON(client, json));
    }

    /**
     * Creates a channel object from a json response.
     *
     * @param client The discord client.
     * @param guild the guild.
     * @param json The json response.
     * @return The channel object.
     */
    public static Channel getChannelFromJSON(DiscordClient client, Guild guild, ChannelResponse json) {
        Channel channel;

        if ((channel = guild.getChannelByID(json.id)) != null) {
            channel.setName(json.name);
            channel.setPosition(json.position);
            channel.setTopic(json.topic);
            HashMap<String, PermissionOverride> userOverrides = new HashMap<>();
            HashMap<String, PermissionOverride> roleOverrides = new HashMap<>();
            for (PermissionOverwrite overrides : json.permission_overwrites) {
                if (overrides.type.equalsIgnoreCase("role")) {
                    if (channel.getRoleOverrides().containsKey(overrides.id)) {
                        roleOverrides.put(overrides.id, channel.getRoleOverrides().get(overrides.id));
                    } else {
                        roleOverrides.put(overrides.id, new PermissionOverride(
                                Permissions.getAllowedPermissionsForNumber(overrides.allow),
                                Permissions.getDeniedPermissionsForNumber(overrides.deny)));
                    }
                } else if (overrides.type.equalsIgnoreCase("member")) {
                    if (channel.getUserOverrides().containsKey(overrides.id)) {
                        userOverrides.put(overrides.id, channel.getUserOverrides().get(overrides.id));
                    } else {
                        userOverrides.put(overrides.id, new PermissionOverride(
                                Permissions.getAllowedPermissionsForNumber(overrides.allow),
                                Permissions.getDeniedPermissionsForNumber(overrides.deny)));
                    }
                } else {
                    Log.w(TAG, "Unknown permissions overwrite type \"" + overrides.type + "\"!");
                }
            }
            channel.getUserOverrides().clear();
            channel.getUserOverrides().putAll(userOverrides);
            channel.getRoleOverrides().clear();
            channel.getRoleOverrides().putAll(roleOverrides);
        } else {
            channel = new Channel(client, json.name, json.id, guild, json.topic, json.position);

            for (PermissionOverwrite overrides : json.permission_overwrites) {
                PermissionOverride override = new PermissionOverride(
                        Permissions.getAllowedPermissionsForNumber(overrides.allow),
                        Permissions.getDeniedPermissionsForNumber(overrides.deny));
                if (overrides.type.equalsIgnoreCase("role")) {
                    channel.addRoleOverride(overrides.id, override);
                } else if (overrides.type.equalsIgnoreCase("member")) {
                    channel.addUserOverride(overrides.id, override);
                } else {
                    Log.w(TAG, "Unknown permissions overwrite type \"" + overrides.type + "\"!");
                }
            }
        }

        channel.setLastReadMessageID(json.last_message_id);

        return channel;
    }

    /**
     * Creates a role object from a json response.
     *
     * @param guild the guild.
     * @param json The json response.
     * @return The role object.
     */
    public static Role getRoleFromJSON(DiscordClient client, Guild guild, RoleResponse json) {
        Role role;
        if ((role = guild.getRoleByID(json.id)) != null) {
            role.setColor(json.color);
            role.setHoist(json.hoist);
            role.setName(json.name);
            role.setPermissions(json.permissions);
            role.setPosition(json.position);
        } else {
            role = new Role(client, json.position, json.permissions, json.name, json.managed, json.id, json.hoist, json.color, guild);
            guild.addRole(role);
        }
        return role;
    }

    /**
     * Creates a region object from a json response.
     *
     * @param json The json response.
     * @return The region object.
     */
    public static Region getRegionFromJSON(RegionResponse json) {
        return new Region(json.id, json.name, json.vip);
    }

    /**
     * Creates a channel object from a json response.
     *
     * @param client The discord client.
     * @param guild the guild.
     * @param json The json response.
     * @return The channel object.
     */
    public static VoiceChannel getVoiceChannelFromJSON(DiscordClient client, Guild guild, ChannelResponse json) {
        VoiceChannel channel;

        if ((channel = guild.getVoiceChannelByID(json.id)) != null) {
            channel.setName(json.name);
            channel.setPosition(json.position);
            HashMap<String, PermissionOverride> userOverrides = new HashMap<>();
            HashMap<String, PermissionOverride> roleOverrides = new HashMap<>();
            for (PermissionOverwrite overrides : json.permission_overwrites) {
                if (overrides.type.equalsIgnoreCase("role")) {
                    if (channel.getRoleOverrides().containsKey(overrides.id)) {
                        roleOverrides.put(overrides.id, channel.getRoleOverrides().get(overrides.id));
                    } else {
                        roleOverrides.put(overrides.id, new PermissionOverride(
                                Permissions.getAllowedPermissionsForNumber(overrides.allow),
                                Permissions.getDeniedPermissionsForNumber(overrides.deny)));
                    }
                } else if (overrides.type.equalsIgnoreCase("member")) {
                    if (channel.getUserOverrides().containsKey(overrides.id)) {
                        userOverrides.put(overrides.id, channel.getUserOverrides().get(overrides.id));
                    } else {
                        userOverrides.put(overrides.id, new PermissionOverride(
                                Permissions.getAllowedPermissionsForNumber(overrides.allow),
                                Permissions.getDeniedPermissionsForNumber(overrides.deny)));
                    }
                } else {
                    Log.w(TAG, "Unknown permissions overwrite type \"" + overrides.type + "\"!");
                }
            }
            channel.getUserOverrides().clear();
            channel.getUserOverrides().putAll(userOverrides);
            channel.getRoleOverrides().clear();
            channel.getRoleOverrides().putAll(roleOverrides);
        } else {
            channel = new VoiceChannel(client, json.name, json.id, guild, json.topic, json.position);

            for (PermissionOverwrite overrides : json.permission_overwrites) {
                PermissionOverride override = new PermissionOverride(
                        Permissions.getAllowedPermissionsForNumber(overrides.allow),
                        Permissions.getDeniedPermissionsForNumber(overrides.deny));
                if (overrides.type.equalsIgnoreCase("role")) {
                    channel.addRoleOverride(overrides.id, override);
                } else if (overrides.type.equalsIgnoreCase("member")) {
                    channel.addUserOverride(overrides.id, override);
                } else {
                    Log.w(TAG, "Unknown permissions overwrite type \"" + overrides.type + "\"!");
                }
            }
        }

        return channel;
    }

    /**
     * Checks a set of permissions provided by a channel against required permissions.
     */
    public static void checkPermissions(DiscordClient client, Channel channel, EnumSet<Permissions> required)
            throws MissingPermissionsException {
        if(channel instanceof PrivateChannel) return;
        EnumSet<Permissions> contained = channel.getModifiedPermissions(client.getOurUser());
        checkPermissions(contained, required);
    }

    /**
     * Checks a set of permissions provided by a guild against required permissions.
     */
    public static void checkPermissions(DiscordClient client, Guild guild, EnumSet<Permissions> required)
            throws MissingPermissionsException {
        EnumSet<Permissions> contained = EnumSet.noneOf(Permissions.class);
        List<Role> roles = client.getOurUser().getRolesForGuild(guild);
        for (Role role : roles) {
            contained.addAll(role.getPermissions());
        }
        checkPermissions(contained, required);
    }

    /**
     * Checks a set of permissions against required permissions.
     */
    public static void checkPermissions(EnumSet<Permissions> contained, EnumSet<Permissions> required)
            throws MissingPermissionsException {
        EnumSet<Permissions> missing = EnumSet.noneOf(Permissions.class);
        for (Permissions requiredPermission : required) {
            if (!contained.contains(requiredPermission)) {
                missing.add(requiredPermission);
            }
        }
        if (missing.size() > 0) {
            throw new MissingPermissionsException(missing);
        }
    }
}
