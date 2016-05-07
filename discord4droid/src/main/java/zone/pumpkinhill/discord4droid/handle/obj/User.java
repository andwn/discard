package zone.pumpkinhill.discord4droid.handle.obj;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import zone.pumpkinhill.discord4droid.api.DiscordClient;
import zone.pumpkinhill.discord4droid.api.DiscordUtils;
import zone.pumpkinhill.discord4droid.api.Endpoints;
import zone.pumpkinhill.discord4droid.api.Requests;
import zone.pumpkinhill.discord4droid.json.requests.MoveMemberRequest;
import zone.pumpkinhill.discord4droid.util.DiscordException;
import zone.pumpkinhill.discord4droid.util.HTTP429Exception;
import zone.pumpkinhill.discord4droid.util.MissingPermissionsException;
import zone.pumpkinhill.http.entity.StringEntity;
import zone.pumpkinhill.http.message.BasicNameValuePair;

/**
 * This class defines the Discord user.
 */
public class User extends DiscordObject {

    /**
     * Display name of the user.
     */
    protected String name;

    /**
     * The user's avatar location.
     */
    protected String avatar;

    /**
     * The game the user is playing.
     */
    protected String game;

    /**
     * User discriminator.
     * Distinguishes users with the same name.
     */
    protected String discriminator;

    /**
     * This user's presence.
     * One of [online/idle/offline].
     */
    protected Presences presence;

    /**
     * The user's avatar in URL form.
     */
    protected String avatarURL;

    /**
     * The roles the user is a part of. (Key = guild id).
     */
    protected HashMap<String, List<Role>> roles;

    /**
     * The voice channel this user is in.
     */
    protected VoiceChannel channel;

    protected UserVoiceState voiceState;

    protected boolean speaking;

    public User(DiscordClient client, String name, String id, String discriminator, String avatar, Presences presence) {
        super(client, id);
        this.name = name;
        this.discriminator = discriminator;
        this.avatar = avatar;
        this.avatarURL = String.format(client.getCDN() + Endpoints.AVATARS, this.id, this.avatar);
        this.presence = presence;
        this.roles = new HashMap<>();
        this.voiceState = new UserVoiceState(false, false, false, false);
        this.speaking = false;
    }

    /**
     * Gets the user's username.
     *
     * @return The username.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the game the user is playing, no value if the user isn't playing a game.
     *
     * @return The game.
     */
    public String getGame() {
        return game;
    }

    /**
     * Gets the user's avatar id.
     *
     * @return The avatar id.
     */
    public String getAvatar() {
        return avatar;
    }

    /**
     * Gets the user's avatar direct link.
     *
     * @return The avatar url.
     */
    public String getAvatarURL() {
        return avatarURL;
    }

    /**
     * Gets the user's presence.
     *
     * @return The user's presence.
     */
    public Presences getPresence() {
        return presence;
    }

    /**
     * Formats a string to @mention the user.
     *
     * @return The formatted string.
     */
    public String mention() {
        return "<@"+id+">";
    }

    /**
     * Gets the discriminator for the user. This is used by Discord to differentiate between two users with the same name.
     *
     * @return The discriminator.
     */
    public String getDiscriminator() {
        return discriminator;
    }

    /**
     * Gets the roles the user is a part of.
     *
     * @param guild The guild to check the roles for.
     * @return The roles.
     */
    public List<Role> getRolesForGuild(Guild guild) {
        if(roles != null) {
            List<Role> r = roles.get(guild.getID());
            if(r == null) return new ArrayList<>();
            return roles.get(guild.getID());
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Moves this user to a different voice channel.
     *
     * @param newChannel The new channel the user should move to.
     *
     * @throws DiscordException
     * @throws HTTP429Exception
     * @throws MissingPermissionsException
     */
    public void moveToVoiceChannel(VoiceChannel newChannel) throws DiscordException, HTTP429Exception, MissingPermissionsException {
        if (!client.getOurUser().equals(this))
            DiscordUtils.checkPermissions(client, newChannel.getGuild(), EnumSet.of(Permissions.VOICE_MOVE_MEMBERS));

        try {
            Requests.PATCH.makeRequest(client.getURL() + Endpoints.GUILDS + newChannel.getGuild().getID() + "/members/" + id,
                    new StringEntity(DiscordUtils.GSON.toJson(new MoveMemberRequest(newChannel.getID()))),
                    new BasicNameValuePair("authorization", client.getToken()),
                    new BasicNameValuePair("content-type", "application/json"));
        }catch (UnsupportedEncodingException e) {
            System.out.println("Discord4Droid Internal Exception: " + e);
        }
    }

    /**
     * Gets the voice channel this user is in (if in one).
     *
     * @return The (optional) voice channel.
     */
    public VoiceChannel getVoiceChannel() {
        return channel;
    }

    public UserVoiceState getVoiceState() {
        return voiceState;
    }

    public boolean isSpeaking() {
        return speaking;
    }

    // Local setters
    public void setGame(String game) {
        this.game = game;
    }
    public void setName(String name) {
        this.name = name;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
        this.avatarURL = String.format(client.getCDN() + Endpoints.AVATARS, this.id, this.avatar);
    }
    public void setPresence(Presences presence) {
        this.presence = presence;
    }
    public void setDiscriminator(String discriminator) {
        this.discriminator = discriminator;
    }
    public void addRole(String guildID, Role role) {
        if (!roles.containsKey(guildID)) {
            roles.put(guildID, new ArrayList<Role>());
        }
        roles.get(guildID).add(role);
    }
    public void setVoiceChannel(VoiceChannel channel) {
        this.channel = channel;
    }
    public void setVoiceState(UserVoiceState voiceState) {
        this.voiceState = voiceState;
    }
    public void setSpeaking(boolean speaking) {
        this.speaking = speaking;
    }

    // Abstract / Overrides
    @Override
    public String toString() {
        return mention();
    }
    public User copy() {
        return new User(client, name, id, discriminator, avatar, presence);
    }
}
