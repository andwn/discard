package zone.pumpkinhill.discord4droid.api;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import zone.pumpkinhill.discord4droid.Constants;
import zone.pumpkinhill.discord4droid.handle.AudioChannel;
import zone.pumpkinhill.discord4droid.handle.events.DiscordDisconnectedEvent;
import zone.pumpkinhill.discord4droid.handle.events.VoiceDisconnectedEvent;
import zone.pumpkinhill.discord4droid.handle.obj.Channel;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.Invite;
import zone.pumpkinhill.discord4droid.handle.obj.Presences;
import zone.pumpkinhill.discord4droid.handle.obj.PrivateChannel;
import zone.pumpkinhill.discord4droid.handle.obj.Region;
import zone.pumpkinhill.discord4droid.handle.obj.User;
import zone.pumpkinhill.discord4droid.handle.obj.VoiceChannel;
import zone.pumpkinhill.discord4droid.json.requests.AccountInfoChangeRequest;
import zone.pumpkinhill.discord4droid.json.requests.CreateGuildRequest;
import zone.pumpkinhill.discord4droid.json.requests.LoginRequest;
import zone.pumpkinhill.discord4droid.json.requests.PresenceUpdateRequest;
import zone.pumpkinhill.discord4droid.json.requests.PrivateChannelRequest;
import zone.pumpkinhill.discord4droid.json.responses.AccountInfoChangeResponse;
import zone.pumpkinhill.discord4droid.json.responses.GatewayResponse;
import zone.pumpkinhill.discord4droid.json.responses.GuildResponse;
import zone.pumpkinhill.discord4droid.json.responses.InviteJSONResponse;
import zone.pumpkinhill.discord4droid.json.responses.LoginResponse;
import zone.pumpkinhill.discord4droid.json.responses.PrivateChannelResponse;
import zone.pumpkinhill.discord4droid.json.responses.RegionResponse;
import zone.pumpkinhill.discord4droid.util.DiscordException;
import zone.pumpkinhill.discord4droid.util.HTTP429Exception;
import zone.pumpkinhill.discord4droid.util.ImageHelper;
import zone.pumpkinhill.http.entity.StringEntity;
import zone.pumpkinhill.http.message.BasicNameValuePair;

/**
 * Defines the client.
 * This class receives and sends messages, as well as holds our user data.
 */
public final class DiscordClient {
    private final static String TAG = DiscordClient.class.getCanonicalName();

    /**
     * Used for keep alive. Keeps last time (in ms)
     * that we sent the keep alive so we can accurately
     * time our keep alive messages.
     */
    protected volatile long timer = System.currentTimeMillis();

    /**
     * User we are logged in as
     */
    protected User ourUser;

    /**
     * Our token, so we can send XHR to Discord.
     */
    protected String token;

    /**
     * Time (in ms) between keep alive messages.
     */
    protected volatile long heartbeat;

    /**
     * Local copy of all guilds/servers.
     */
    protected final List<Guild> guildList = new ArrayList<>();

    /**
     * Entrypoint URL
     */
    protected String url;

    /**
     * Content URL
     */
    protected String cdn;

    /**
     * Private copy of the email you logged in with.
     */
    protected String email;

    /**
     * Private copy of the password you used to log in.
     */
    protected String password;

    /**
     * WebSocket over which to communicate with Discord.
     */
    public DiscordWS ws;

    /**
     * Holds the active connections to voice sockets.
     */
    public final Map<Guild, DiscordVoiceWS> voiceConnections = new HashMap<>();

    /**
     * Event dispatcher.
     */
    protected EventDispatcher dispatcher;

    /**
     * All of the private message channels that the bot is connected to.
     */
    protected final List<PrivateChannel> privateChannels = new ArrayList<>();

    /**
     * The voice channels the bot is currently in.
     */
    public List<VoiceChannel> connectedVoiceChannels = new ArrayList<>();

    /**
     * Whether the api is logged in.
     */
    protected boolean isReady = false;

    /**
     * The websocket session id.
     */
    protected String sessionId;

    /**
     * Caches the last operation done by the websocket, required for handling redirects.
     */
    protected int lastSequence = 0;

    /**
     * Caches the available regions for discord.
     */
    protected final List<Region> REGIONS = new ArrayList<>();

    /**
     * The time for the client to timeout.
     */
    protected final int timeoutTime = -1;

    /**
     * The maximum amount of pings discord can miss.
     */
    protected final int maxMissedPingCount = -1;

    /**
     * When this client was logged into. Useful for determining uptime.
     */
    protected Date launchTime;

    /**
     * Suspended state disconnects from websockets to save battery
     */
    protected boolean suspended;

    public DiscordClient(String email, String password) {
        this.email = email;
        this.password = password;
        this.dispatcher = new EventDispatcher(this);
    }

    public DiscordClient(String email, String password, String url) {
        this(email, password);
        this.url = url;
        if(url.equals(Constants.URL_OFFICIAL)) this.cdn = Constants.CDN_OFFICIAL;
    }

    /**
     * Gets the {@link EventDispatcher} instance for this client. Use this to handle events.
     *
     * @return The event dispatcher instance.
     */
    public EventDispatcher getDispatcher() {
        return dispatcher;
    }

    /**
     * Gets the audio channel instance for this client.
     *
     * @return The audio channel.
     * @deprecated See {@link VoiceChannel#getAudioChannel()} or {@link Guild#getAudioChannel()}
     */
    public AudioChannel getAudioChannel() {
        if (getConnectedVoiceChannels().get(0) != null)
            try {
                return getConnectedVoiceChannels().get(0).getAudioChannel();
            } catch (DiscordException e) {
                Log.e(TAG, "Error getting audio channel: " + e);
            }
        return null;
    }

    /**
     * Gets the authorization token for this client.
     *
     * @return The authorization token.
     */
    public String getToken() {
        return token;
    }

    /**
     * Logs the client in as the provided account.
     *
     * @throws DiscordException This is thrown if there is an error logging in.
     */
    public void login() throws DiscordException {
        try {
            if (ws != null) {
                ws.disconnect(DiscordDisconnectedEvent.Reason.RECONNECTING);
                for (DiscordVoiceWS vws : voiceConnections.values()) {
                    vws.disconnect(VoiceDisconnectedEvent.Reason.RECONNECTING);
                }
                lastSequence = 0;
                sessionId = null; //Prevents the websocket from sending a resume request.
            }
            LoginResponse response = DiscordUtils.GSON.fromJson(
                    Requests.POST.makeRequest(url + Endpoints.LOGIN,
                    new StringEntity(DiscordUtils.GSON.toJson(new LoginRequest(email, password))),
                    new BasicNameValuePair("content-type", "application/json")), LoginResponse.class);
            this.token = response.token;
            if(response.content != null) this.cdn = response.content;
            this.ws = new DiscordWS(this, obtainGateway(getToken()), timeoutTime, maxMissedPingCount);
            launchTime = new Date();
            suspended = false;
        } catch (Exception e) {
            throw new DiscordException("Login error occurred! Are your login details correct?");
        }
    }

    /**
     * Logs out the client.
     *
     * @throws HTTP429Exception
     */
    public void logout() throws HTTP429Exception, DiscordException {
        if (isReady()) {
            ws.disconnect(DiscordDisconnectedEvent.Reason.LOGGED_OUT);

            for (DiscordVoiceWS vws : voiceConnections.values())
                vws.disconnect(VoiceDisconnectedEvent.Reason.LOGGED_OUT);

            lastSequence = 0;
            sessionId = null; //Prevents the websocket from sending a resume request.

            Requests.POST.makeRequest(url + Endpoints.LOGOUT,
                    new BasicNameValuePair("authorization", token));
        } else {
            Log.w(TAG, "Trying to logout before login. Ignoring.");
        }
    }

    /**
     * Gets the WebSocket gateway
     *
     * @param token Our login token
     * @return the WebSocket URL of which to connect
     */
    private String obtainGateway(String token) {
        String gateway = null;
        try {
            GatewayResponse response = DiscordUtils.GSON.fromJson(
                    Requests.GET.makeRequest(url + Endpoints.GATEWAY,
                    new BasicNameValuePair("authorization", token)), GatewayResponse.class);
            gateway = response.url;
        } catch (HTTP429Exception | DiscordException e) {
            Log.e(TAG, "Gateway Error: " + e);
        }
        Log.i(TAG, "Obtained gateway: " + gateway);
        return gateway;
    }

    public void suspend() {
        if(suspended) return;
        suspended = true;
        Log.i(TAG, "Suspending websocket connection.");
        if (ws != null) {
            ws.disconnect(DiscordDisconnectedEvent.Reason.SUSPENDED);
            for (DiscordVoiceWS vws : voiceConnections.values()) {
                vws.disconnect(VoiceDisconnectedEvent.Reason.LOGGED_OUT);
            }
            lastSequence = 0;
            sessionId = null; //Prevents the websocket from sending a resume request.
        }

    }

    public void resume() throws DiscordException {
        if(!suspended) return;
        suspended = false;
        Log.i(TAG, "Resuming websocket connection.");
        try {
            this.ws = new DiscordWS(this, obtainGateway(getToken()), timeoutTime, maxMissedPingCount);
            launchTime = new Date();
        } catch(Exception e) {
            throw new DiscordException("Error occurred resuming websocket connection: " + e);
        }
    }

    public void changeAccountInfo(String username, String email, String password, String avatar) throws HTTP429Exception, DiscordException {
        if(!isReady() || ourUser == null) return;
        try {
            AccountInfoChangeResponse response = DiscordUtils.GSON.fromJson(
                    Requests.PATCH.makeRequest(url + Endpoints.USERS + "@me",
                    new StringEntity(DiscordUtils.GSON.toJson(new AccountInfoChangeRequest(
                            email == null ? this.email : email, this.password,
                            password == null ? this.password : password,
                            username == null ? ourUser.getName() : username,
                            avatar == null ? ourUser.getAvatar() : avatar))),
                    new BasicNameValuePair("Authorization", token),
                    new BasicNameValuePair("content-type", "application/json; charset=UTF-8")), AccountInfoChangeResponse.class);

            if (!this.token.equals(response.token)) {
                Log.d(TAG, "Token changed, updating it.");
                this.token = response.token;
            }
        } catch (UnsupportedEncodingException | NullPointerException e) {
            Log.e(TAG, "Error changing account info: " + e);
        }
    }

    /**
     * Updates the bot's presence.
     *
     * @param isIdle If true, the bot will be "idle", otherwise the bot will be "online".
     * @param game The optional name of the game the bot is playing. If empty, the bot simply won't be playing a game.
     */
    public void updatePresence(boolean isIdle, String game) {
        if (!isReady()) {
            Log.w(TAG, "Trying to update presence before logged in. Ignoring.");
            return;
        }
        ws.send(DiscordUtils.GSON.toJson(new PresenceUpdateRequest(isIdle ? System.currentTimeMillis() : null, game)));
        getOurUser().setPresence(isIdle ? Presences.IDLE : Presences.ONLINE);
        getOurUser().setGame(game);
    }

    /**
     * Checks if the api is ready to be interacted with (if it is logged in).
     *
     * @return True if ready, false if otherwise.
     */
    public boolean isReady() {
        return isReady /* && ws != null */ ;
    }

    /**
     * Gets the {@link User} this bot is representing.
     *
     * @return The user object.
     */
    public User getOurUser() {
        return ourUser;
    }

    /**
     * Gets a set of all channels visible to the bot user.
     *
     * @param priv Whether to include private channels in the set.
     * @return A {@link Collection} of all {@link Channel} objects.
     */
    public Collection<Channel> getChannels(boolean priv) {
        Collection<Channel> channels = new ArrayList<>();
        for(Guild g : guildList) {
            channels.addAll(g.getChannels());
        }
        if (priv)
            channels.addAll(privateChannels);
        return channels;
    }

    /**
     * Gets a channel by its unique id.
     *
     * @param id The id of the desired channel.
     * @return The {@link Channel} object with the provided id.
     */
    public Channel getChannelByID(String id) {
        for(Channel c : getChannels(true)) {
            if(c.getID().equalsIgnoreCase(id)) return c;
        }
        return null;
    }

    /**
     * Gets a set of all voice channels visible to the bot user.
     *
     * @return A {@link Collection} of all {@link VoiceChannel} objects.
     */
    public Collection<VoiceChannel> getVoiceChannels() {
        Collection<VoiceChannel> channels = new ArrayList<>();
        for(Guild g : guildList) {
            channels.addAll(g.getVoiceChannels());
        }
        return channels;
    }

    /**
     * Gets a voice channel from a given id.
     *
     * @param id The voice channel id.
     * @return The voice channel (or null if not found).
     */
    public VoiceChannel getVoiceChannelByID(String id) {
        for(VoiceChannel c : getVoiceChannels()) {
            if(c.getID().equalsIgnoreCase(id)) return c;
        }
        return null;
    }

    /**
     * Gets a guild by its unique id.
     *
     * @param guildID The id of the desired guild.
     * @return The {@link Guild} object with the provided id.
     */
    public Guild getGuildByID(String guildID) {
        for(Guild g : guildList) {
            if(g.getID().equalsIgnoreCase(guildID)) return g;
        }
        return null;
    }

    /**
     * Gets all the guilds the user the api represents is connected to.
     *
     * @return The list of {@link Guild}s the api is connected to.
     */
    public List<Guild> getGuilds() {
        return guildList;
    }

    /**
     * Gets a user by its unique id.
     *
     * @param userID The id of the desired user.
     * @return The {@link User} object with the provided id.
     */
    public User getUserByID(String userID) {
        User user = null;
        for (Guild guild : guildList) {
            if (user == null)
                user = guild.getUserByID(userID);
            else
                break;
        }

        return ourUser != null && ourUser.getID().equals(userID) ? ourUser : user;
    }

    /**
     * Gets a {@link PrivateChannel} for the provided recipient.
     *
     * @param user The user who will be the recipient of the private channel.
     * @return The {@link PrivateChannel} object.
     *
     * @throws DiscordException
     * @throws HTTP429Exception
     */
    public PrivateChannel getOrCreatePMChannel(User user) throws DiscordException, HTTP429Exception {
        if (!isReady()) {
            Log.w(TAG, "Trying to get private channel before logging in. Ignoring.");
            return null;
        }

        for(PrivateChannel opt : privateChannels) {
            if(opt.getRecipient().getID().equalsIgnoreCase(user.getID())) return opt;
        }

        PrivateChannelResponse response = null;
        try {
            response = DiscordUtils.GSON.fromJson(
                    Requests.POST.makeRequest(url + Endpoints.USERS + this.ourUser.getID() + "/channels",
                    new StringEntity(DiscordUtils.GSON.toJson(new PrivateChannelRequest(user.getID()))),
                    new BasicNameValuePair("authorization", this.token),
                    new BasicNameValuePair("content-type", "application/json")), PrivateChannelResponse.class);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error creating creating a private channel: " + e);
        }

        PrivateChannel channel = DiscordUtils.getPrivateChannelFromJSON(this, response);
        privateChannels.add(channel);
        return channel;
    }

    /**
     * Gets the invite for a code.
     *
     * @param code The invite code or xkcd pass.
     * @return The invite, or null if it doesn't exist.
     */
    public Invite getInviteForCode(String code) {
        if (!isReady()) {
            Log.w(TAG, "Trying to get invite before logging in. Ignoring.");
            return null;
        }

        try {
            InviteJSONResponse response = DiscordUtils.GSON.fromJson(
                    Requests.GET.makeRequest(url + Endpoints.INVITE + code,
                    new BasicNameValuePair("authorization", token)), InviteJSONResponse.class);

            return DiscordUtils.getInviteFromJSON(this, response);
        } catch (HTTP429Exception | DiscordException e) {
            Log.e(TAG, "Error getting invite: " + e);
        }
        return null;
    }

    /**
     * Gets the regions available for discord.
     *
     * @return The list of available regions.
     *
     * @throws HTTP429Exception
     * @throws DiscordException
     */
    public List<Region> getRegions() throws HTTP429Exception, DiscordException {
        if (REGIONS.isEmpty()) {
            RegionResponse[] regions = DiscordUtils.GSON.fromJson(Requests.GET.makeRequest(
                            url + Endpoints.VOICE+"regions",
                            new BasicNameValuePair("authorization", this.token)),
                    RegionResponse[].class);

            for(RegionResponse r : regions) {
                REGIONS.add(DiscordUtils.getRegionFromJSON(r));
            }
        }
        return REGIONS;
    }

    /**
     * Gets the corresponding region for a given id.
     *
     * @param regionID The region id.
     * @return The region (or null if not found).
     */
    public Region getRegionByID(String regionID) {
        try {
            for(Region r : getRegions()) {
                if(r.getID().equals(regionID)) return r;
            }
        } catch (HTTP429Exception | DiscordException e) {
            Log.e(TAG, "Error getting region: " + e);
        }
        return null;
    }

    /**
     * Creates a new guild.
     *
     * @param name The name of the guild.
     * @param region The region for the guild.
     * @param icon The icon for the guild.
     * @return The new guild's id.
     *
     * @throws HTTP429Exception
     * @throws DiscordException
     */
    public Guild createGuild(String name, String region, String icon) throws HTTP429Exception, DiscordException {
        try {
            if(getRegionByID(region) == null) {
                throw new DiscordException("Unable to find region with ID " + region);
            }
            GuildResponse guildResponse = DiscordUtils.GSON.fromJson(
                    Requests.POST.makeRequest(url + Endpoints.API + "/guilds",
                    new StringEntity(DiscordUtils.GSON_NO_NULLS.toJson(
                            new CreateGuildRequest(name, region, icon))),
                    new BasicNameValuePair("authorization", this.token),
                    new BasicNameValuePair("content-type", "application/json")), GuildResponse.class);
            Guild guild = DiscordUtils.getGuildFromJSON(this, guildResponse);
            guildList.add(guild);
            return guild;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error creating guild: " + e);
        }
        return null;
    }

    /**
     * Gets the latest response time by the discord websocket to a ping.
     *
     * @return The response time (in ms).
     */
    public long getResponseTime() {
        return ws.getResponseTime();
    }

    /**
     * Gets the connected voice channels.
     *
     * @return The voice channels.
     */
    public List<VoiceChannel> getConnectedVoiceChannels() {
        return connectedVoiceChannels;
    }

    /**
     * Gets the time when this client was last logged into. Useful for keeping track of uptime.
     *
     * @return The launch time.
     */
    public Date getLaunchTime() {
        return launchTime;
    }

    public String getURL() {
        return url;
    }

    public String getCDN() {
        return cdn;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public String getEmail() {
        return email;
    }
}
