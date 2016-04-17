package zone.pumpkinhill.discord4droid.api;

import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.neovisionaries.ws.client.PayloadGenerator;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.InflaterInputStream;

import zone.pumpkinhill.discord4droid.Constants;
import zone.pumpkinhill.discord4droid.handle.events.ChannelCreateEvent;
import zone.pumpkinhill.discord4droid.handle.events.ChannelDeleteEvent;
import zone.pumpkinhill.discord4droid.handle.events.ChannelUpdateEvent;
import zone.pumpkinhill.discord4droid.handle.events.DiscordDisconnectedEvent;
import zone.pumpkinhill.discord4droid.handle.events.GameChangeEvent;
import zone.pumpkinhill.discord4droid.handle.events.GuildCreateEvent;
import zone.pumpkinhill.discord4droid.handle.events.GuildLeaveEvent;
import zone.pumpkinhill.discord4droid.handle.events.GuildTransferOwnershipEvent;
import zone.pumpkinhill.discord4droid.handle.events.GuildUnavailableEvent;
import zone.pumpkinhill.discord4droid.handle.events.GuildUpdateEvent;
import zone.pumpkinhill.discord4droid.handle.events.InviteReceivedEvent;
import zone.pumpkinhill.discord4droid.handle.events.MentionEvent;
import zone.pumpkinhill.discord4droid.handle.events.MessageAcknowledgedEvent;
import zone.pumpkinhill.discord4droid.handle.events.MessageDeleteEvent;
import zone.pumpkinhill.discord4droid.handle.events.MessageReceivedEvent;
import zone.pumpkinhill.discord4droid.handle.events.MessageSendEvent;
import zone.pumpkinhill.discord4droid.handle.events.MessageUpdateEvent;
import zone.pumpkinhill.discord4droid.handle.events.PresenceUpdateEvent;
import zone.pumpkinhill.discord4droid.handle.events.ReadyEvent;
import zone.pumpkinhill.discord4droid.handle.events.RoleCreateEvent;
import zone.pumpkinhill.discord4droid.handle.events.RoleDeleteEvent;
import zone.pumpkinhill.discord4droid.handle.events.RoleUpdateEvent;
import zone.pumpkinhill.discord4droid.handle.events.TypingEvent;
import zone.pumpkinhill.discord4droid.handle.events.UserBanEvent;
import zone.pumpkinhill.discord4droid.handle.events.UserJoinEvent;
import zone.pumpkinhill.discord4droid.handle.events.UserLeaveEvent;
import zone.pumpkinhill.discord4droid.handle.events.UserPardonEvent;
import zone.pumpkinhill.discord4droid.handle.events.UserRoleUpdateEvent;
import zone.pumpkinhill.discord4droid.handle.events.UserUpdateEvent;
import zone.pumpkinhill.discord4droid.handle.events.UserVoiceChannelJoinEvent;
import zone.pumpkinhill.discord4droid.handle.events.UserVoiceChannelLeaveEvent;
import zone.pumpkinhill.discord4droid.handle.events.UserVoiceChannelMoveEvent;
import zone.pumpkinhill.discord4droid.handle.events.UserVoiceStateUpdateEvent;
import zone.pumpkinhill.discord4droid.handle.events.VoiceChannelCreateEvent;
import zone.pumpkinhill.discord4droid.handle.events.VoiceChannelDeleteEvent;
import zone.pumpkinhill.discord4droid.handle.events.VoiceChannelUpdateEvent;
import zone.pumpkinhill.discord4droid.handle.obj.Channel;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.Message;
import zone.pumpkinhill.discord4droid.handle.obj.Permissions;
import zone.pumpkinhill.discord4droid.handle.obj.Presences;
import zone.pumpkinhill.discord4droid.handle.obj.PrivateChannel;
import zone.pumpkinhill.discord4droid.handle.obj.Role;
import zone.pumpkinhill.discord4droid.handle.obj.User;
import zone.pumpkinhill.discord4droid.handle.obj.VoiceChannel;
import zone.pumpkinhill.discord4droid.json.requests.ConnectRequest;
import zone.pumpkinhill.discord4droid.json.requests.KeepAliveRequest;
import zone.pumpkinhill.discord4droid.json.requests.ResumeRequest;
import zone.pumpkinhill.discord4droid.json.responses.ChannelResponse;
import zone.pumpkinhill.discord4droid.json.responses.GuildResponse;
import zone.pumpkinhill.discord4droid.json.responses.MessageResponse;
import zone.pumpkinhill.discord4droid.json.responses.PrivateChannelResponse;
import zone.pumpkinhill.discord4droid.json.responses.RedirectResponse;
import zone.pumpkinhill.discord4droid.json.responses.VoiceStateResponse;
import zone.pumpkinhill.discord4droid.json.responses.VoiceUpdateResponse;
import zone.pumpkinhill.discord4droid.json.responses.events.ChannelUpdateEventResponse;
import zone.pumpkinhill.discord4droid.json.responses.events.GuildBanEventResponse;
import zone.pumpkinhill.discord4droid.json.responses.events.GuildMemberAddEventResponse;
import zone.pumpkinhill.discord4droid.json.responses.events.GuildMemberChunkEventResponse;
import zone.pumpkinhill.discord4droid.json.responses.events.GuildMemberRemoveEventResponse;
import zone.pumpkinhill.discord4droid.json.responses.events.GuildMemberUpdateEventResponse;
import zone.pumpkinhill.discord4droid.json.responses.events.GuildRoleDeleteEventResponse;
import zone.pumpkinhill.discord4droid.json.responses.events.GuildRoleEventResponse;
import zone.pumpkinhill.discord4droid.json.responses.events.MessageAcknowledgedEventResponse;
import zone.pumpkinhill.discord4droid.json.responses.events.MessageDeleteEventResponse;
import zone.pumpkinhill.discord4droid.json.responses.events.PresenceUpdateEventResponse;
import zone.pumpkinhill.discord4droid.json.responses.events.ReadyEventResponse;
import zone.pumpkinhill.discord4droid.json.responses.events.ResumedEventResponse;
import zone.pumpkinhill.discord4droid.json.responses.events.TypingEventResponse;
import zone.pumpkinhill.discord4droid.json.responses.events.UserUpdateEventResponse;

public class DiscordWS extends WebSocketAdapter {
    private final static String TAG = DiscordWS.class.getCanonicalName();

    private DiscordClient client;
    private WebSocket socket;
    public AtomicBoolean isConnected = new AtomicBoolean(true);
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
    private volatile boolean sentPing = false;
    private volatile long lastPingSent = -1L;
    private volatile long pingResponseTime = -1L;
    private final int timeoutTime;
    private final int maxMissedPingCount;
    private volatile int missedPingCount = 0;
    private static final String GATEWAY_VERSION = "4";

    private final Thread shutdownHook = new Thread() {//Ensures this websocket is closed properly
        @Override
        public void run() {
            isConnected.set(false);
            if (socket != null) socket.disconnect(); //Harsh disconnect to close the process ASAP
        }
    };

    /**
     * The amount of users a guild must have to be considered "large"
     */
    public static final int LARGE_THRESHOLD = 250; //250 is currently the max handled by discord

    public DiscordWS(DiscordClient client, String gateway, int timeout, int maxMissedPingCount) throws Exception {
        //Ensuring gateway is v4 ready
        if (!gateway.endsWith("/")) gateway += "/";
        gateway += "?encoding=json&v="+GATEWAY_VERSION;

        this.client = client;
        this.timeoutTime = timeout;
        this.maxMissedPingCount = maxMissedPingCount;

        Runtime.getRuntime().addShutdownHook(shutdownHook);

        WebSocketFactory factory = new WebSocketFactory();
        if(timeout != -1) factory.setConnectionTimeout(timeout);
        WebSocket socket = factory.createSocket(gateway);
        socket.addListener(this);
        socket.addHeader("Accept-Encoding", "gzip, deflate");
        socket.connectAsynchronously();
    }

    /**
     * Disconnects the client WS.
     */
    public void disconnect(DiscordDisconnectedEvent.Reason reason) {
        if (isConnected.get()) {
            client.dispatcher.dispatch(new DiscordDisconnectedEvent(reason));
            isConnected.set(false);
            executorService.shutdownNow();
            socket.disconnect();
            client.ws = null;
            clearCache();
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
            //Thread.currentThread().interrupt();
        }
    }

    private void clearCache() {
        client.sessionId = null;
        client.connectedVoiceChannels.clear();
        client.voiceConnections.clear();
        client.guildList.clear();
        client.heartbeat = 0;
        client.lastSequence = 0;
        client.ourUser = null;
        client.privateChannels.clear();
        client.REGIONS.clear();
    }

    /**
     * Sends a message through the websocket.
     *
     * @param message The json message to send.
     */
    public void send(String message) {
        if (socket == null || !socket.isOpen()) {
            Log.w(TAG, "Attempt to send message with no connection: " + message);
            return;
        }
        if (isConnected.get()) {
            socket.sendText(message);
        }
    }

    /**
     * Sends a message through the websocket.
     *
     * @param object This object is converted to json and sent to the websocket.
     */
    public void send(Object object) {
        send(DiscordUtils.GSON.toJson(object));
    }

    private void startKeepalive() {
        final Runnable keepAlive = new Runnable() {
            public void run() {
                if (isConnected.get()) {
                    long l = System.currentTimeMillis()-client.timer;
                    Log.d(TAG, "Sending keep alive... ("+System.currentTimeMillis()+") after "+l+"ms.");
                    send(DiscordUtils.GSON.toJson(new KeepAliveRequest(client.lastSequence)));
                    client.timer = System.currentTimeMillis();
                }
            }
        };
        executorService.scheduleAtFixedRate(keepAlive,
                client.timer + client.heartbeat - System.currentTimeMillis(),
                client.heartbeat, TimeUnit.MILLISECONDS);
    }

    /**
     * Called after the opening handshake of the web socket connection succeeded.
     *
     * @param websocket The web socket.
     * @param headers   HTTP headers received from the server. Keys of the map are
     *                  HTTP header names such as {@code "Sec-WebSocket-Accept"}.
     *                  Note that the comparator used by the map is {@link
     *                  String#CASE_INSENSITIVE_ORDER}.
     * @throws Exception An exception thrown by an implementation of this method.
     *                   The exception is passed to {@link #handleCallbackError(WebSocket, Throwable)}.
     */
    @Override
    public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
        super.onConnected(websocket, headers);
        this.socket = websocket;
        if (client.sessionId != null) {
            send(DiscordUtils.GSON.toJson(new ResumeRequest(client.sessionId, client.lastSequence, client.getToken())));
            Log.i(TAG, "Reconnected to the Discord websocket.");
        } else if (!client.getToken().isEmpty()) {
            send(DiscordUtils.GSON.toJson(new ConnectRequest(client.getToken(), "Android",
                    Constants.APP_NAME, Constants.APP_NAME, "", "", LARGE_THRESHOLD, true)));
            Log.i(TAG, "Connected to the Discord websocket.");
        } else {
            Log.e(TAG, "Use the login() method to set your token first!");
            return;
        }
        socket.setPingPayloadGenerator(new PayloadGenerator() {
            @Override
            public byte[] generate() {
                return DiscordUtils.GSON.toJson(new KeepAliveRequest(client.lastSequence)).getBytes();
            }
        });
        socket.setPingInterval(5000);
    }

    /**
     * Called when a text message was received.
     *
     * @param websocket The web socket.
     * @param text      The text message.
     * @throws Exception An exception thrown by an implementation of this method.
     *                   The exception is passed to {@link #handleCallbackError(WebSocket, Throwable)}.
     */
    @Override
    public void onTextMessage(WebSocket websocket, String text) throws Exception {
        JsonParser parser = new JsonParser();
        JsonObject object = parser.parse(text).getAsJsonObject();
        if (object.has("message")) {
            String msg = object.get("message").getAsString();
            if (msg == null || msg.isEmpty()) {
                Log.e(TAG, "Received unknown error from Discord. Frame: " + text);
            } else {
                Log.e(TAG, "Received error from Discord: " + msg + ". Frame: " + text);
            }
        }
        int op = object.get("op").getAsInt();

        if (op != GatewayOps.RECONNECT.ordinal()) //Not a redirect op, so cache the last sequence value
            client.lastSequence = object.get("s").getAsInt();

        if (op == GatewayOps.DISPATCH.ordinal()) { //Event dispatched
            String type = object.get("t").getAsString();
            JsonElement eventObject = object.get("d");

            switch (type) {
                case "RESUMED":
                    resumed(eventObject);
                    break;

                case "READY":
                    ready(eventObject);
                    break;

                case "MESSAGE_CREATE":
                    messageCreate(eventObject);
                    break;

                case "TYPING_START":
                    typingStart(eventObject);
                    break;

                case "GUILD_CREATE":
                    guildCreate(eventObject);
                    break;

                case "GUILD_MEMBER_ADD":
                    guildMemberAdd(eventObject);
                    break;

                case "GUILD_MEMBER_REMOVE":
                    guildMemberRemove(eventObject);
                    break;

                case "GUILD_MEMBER_UPDATE":
                    guildMemberUpdate(eventObject);
                    break;

                case "MESSAGE_UPDATE":
                    messageUpdate(eventObject);
                    break;

                case "MESSAGE_DELETE":
                    messageDelete(eventObject);
                    break;

                case "PRESENCE_UPDATE":
                    presenceUpdate(eventObject);
                    break;

                case "GUILD_DELETE":
                    guildDelete(eventObject);
                    break;

                case "CHANNEL_CREATE":
                    channelCreate(eventObject);
                    break;

                case "CHANNEL_DELETE":
                    channelDelete(eventObject);
                    break;

                case "USER_UPDATE":
                    userUpdate(eventObject);
                    break;

                case "CHANNEL_UPDATE":
                    channelUpdate(eventObject);
                    break;

                case "MESSAGE_ACK":
                    messageAck(eventObject);
                    break;

                case "GUILD_MEMBERS_CHUNK":
                    guildMembersChunk(eventObject);
                    break;

                case "GUILD_UPDATE":
                    guildUpdate(eventObject);
                    break;

                case "GUILD_ROLE_CREATE":
                    guildRoleCreate(eventObject);
                    break;

                case "GUILD_ROLE_UPDATE":
                    guildRoleUpdate(eventObject);
                    break;

                case "GUILD_ROLE_DELETE":
                    guildRoleDelete(eventObject);
                    break;

                case "GUILD_BAN_ADD":
                    guildBanAdd(eventObject);
                    break;

                case "GUILD_BAN_REMOVE":
                    guildBanRemove(eventObject);
                    break;

                case "VOICE_STATE_UPDATE":
                    voiceStateUpdate(eventObject);
                    break;

                case "VOICE_SERVER_UPDATE":
                    voiceServerUpdate(eventObject);
                    break;

                default:
                    Log.w(TAG, "Unknown message received: "+type+", (ignoring): " + text);
            }
        } else if (op == GatewayOps.RECONNECT.ordinal()) { //Gateway is redirecting us
            RedirectResponse redirectResponse = DiscordUtils.GSON.fromJson(object.getAsJsonObject("d"), RedirectResponse.class);
            Log.d("onTextMessage", "Received a gateway redirect request, closing the socket at reopening at " + redirectResponse.url);
            try {
                client.ws = new DiscordWS(client, redirectResponse.url, timeoutTime, maxMissedPingCount);
                disconnect(DiscordDisconnectedEvent.Reason.RECONNECTING);
            } catch (Exception e) {
                Log.e(TAG, "Error with reconnect request: " + e);
            }
        } else if (op == GatewayOps.INVALID_SESSION.ordinal()) { //Invalid session ABANDON EVERYTHING!!!
            Log.e("onTextMessage", "Invalid session! Attempting to clear caches and reconnect...");
            disconnect(DiscordDisconnectedEvent.Reason.RECONNECTING);
        } else {
            Log.w(TAG, "Unhandled opcode received: "+op+" (ignoring)");
        }
    }

    private void resumed(JsonElement eventObject) {
        ResumedEventResponse event = DiscordUtils.GSON.fromJson(eventObject, ResumedEventResponse.class);
        client.heartbeat = event.heartbeat_interval;
        startKeepalive();
    }

    private void ready(JsonElement eventObject) {
        ReadyEventResponse event = DiscordUtils.GSON.fromJson(eventObject, ReadyEventResponse.class);

        client.sessionId = event.session_id;

        client.ourUser = DiscordUtils.getUserFromJSON(client, event.user);

        client.heartbeat = event.heartbeat_interval;
        Log.d(TAG, "Received heartbeat interval of " + client.heartbeat);

        startKeepalive();

        client.isReady = true;

        // I hope you like loops.
        for (GuildResponse guildResponse : event.guilds) {
            if (guildResponse.unavailable) { //Guild can't be reached, so we ignore it
                Log.w(TAG, "Guild with id "+guildResponse.id+" is unavailable, ignoring it.");
                continue;
            }

            Guild guild = DiscordUtils.getGuildFromJSON(client, guildResponse);
            if (guild != null)
                client.guildList.add(guild);
        }
        for (PrivateChannelResponse privateChannelResponse : event.private_channels) {
            PrivateChannel channel = DiscordUtils.getPrivateChannelFromJSON(client, privateChannelResponse);
            client.privateChannels.add(channel);
        }
        for (ReadyEventResponse.ReadStateResponse readState : event.read_state) {
            Channel channel = client.getChannelByID(readState.id);
            if (channel != null)
                channel.setLastReadMessageID(readState.last_message_id);
        }
        Log.i(TAG, "Logged in as "+client.ourUser.getName()+" (ID "+client.ourUser.getID()+").");

        client.dispatcher.dispatch(new ReadyEvent());
    }

    private void messageCreate(JsonElement eventObject) {
        MessageResponse event = DiscordUtils.GSON.fromJson(eventObject, MessageResponse.class);
        boolean mentioned = event.mention_everyone || event.content.contains("<@"+client.ourUser.getID()+">");

        Channel channel = client.getChannelByID(event.channel_id);

        if (null != channel) {
            Message message = DiscordUtils.getMessageFromJSON(client, channel, event);
            if (!channel.getMessages().contains(message)) {
                Log.i(TAG, "Message from: "+message.getAuthor().getName()+" ("
                        +event.author.id+") in channel ID "+event.channel_id+": "+event.content);

                if (event.content.contains("discord.gg/")) {
                    String inviteCode = event.content.split("discord\\.gg/")[1].split(" ")[0];
                    Log.i(TAG, "Received invite code \""+inviteCode+"\"");
                    client.dispatcher.dispatch(new InviteReceivedEvent(client.getInviteForCode(inviteCode), message));
                } else if (event.content.contains("discordapp.com/invite/")) {
                    String inviteCode = event.content.split("discordapp\\.com/invite/")[1].split(" ")[0];
                    Log.i(TAG, "Received invite code \""+inviteCode+"\"");
                    client.dispatcher.dispatch(new InviteReceivedEvent(client.getInviteForCode(inviteCode), message));
                }

                if (mentioned) {
                    client.dispatcher.dispatch(new MentionEvent(message));
                }

                if (message.getAuthor().equals(client.getOurUser())) {
                    client.dispatcher.dispatch(new MessageSendEvent(message));
                    message.getChannel().setTypingStatus(false); //Messages being sent should stop the bot from typing
                } else {
                    client.dispatcher.dispatch(new MessageReceivedEvent(message));
                }
            }
        }
    }

    private void typingStart(JsonElement eventObject) {
        TypingEventResponse event = DiscordUtils.GSON.fromJson(eventObject, TypingEventResponse.class);
        User user;
        Channel channel = client.getChannelByID(event.channel_id);
        if (channel != null) {
            if (channel.isPrivate()) {
                user = ((PrivateChannel) channel).getRecipient();
            } else {
                user = channel.getGuild().getUserByID(event.user_id);
            }
            if (user != null) {
                client.dispatcher.dispatch(new TypingEvent(user, channel));
            }
        }
    }

    private void guildCreate(JsonElement eventObject) {
        GuildResponse event = DiscordUtils.GSON.fromJson(eventObject, GuildResponse.class);
        if (event.unavailable) { //Guild can't be reached, so we ignore it
            Log.w(TAG, "Guild with id "+event.id+" is unavailable, ignoring it.");
            return;
        }
        Guild guild = DiscordUtils.getGuildFromJSON(client, event);
        client.guildList.add(guild);
        client.dispatcher.dispatch(new GuildCreateEvent(guild));
        Log.i(TAG, "New guild has been created/joined! \""+guild.getName()+"\" with ID " + guild.getID());
    }

    private void guildMemberAdd(JsonElement eventObject) {
        GuildMemberAddEventResponse event = DiscordUtils.GSON.fromJson(eventObject, GuildMemberAddEventResponse.class);
        String guildID = event.guild_id;
        Guild guild = client.getGuildByID(guildID);
        if (guild != null) {
            User user = DiscordUtils.getUserFromGuildMemberResponse(client, guild, new GuildResponse.MemberResponse(event.user, event.roles));
            guild.addUser(user);
            Log.i(TAG, "User \""+user.getName()+"\" joined guild \""+guild.getName()+"\".");
            client.dispatcher.dispatch(new UserJoinEvent(guild, user, DiscordUtils.convertFromTimestamp(event.joined_at)));
        }
    }

    private void guildMemberRemove(JsonElement eventObject) {
        GuildMemberRemoveEventResponse event = DiscordUtils.GSON.fromJson(eventObject, GuildMemberRemoveEventResponse.class);
        String guildID = event.guild_id;
        Guild guild = client.getGuildByID(guildID);
        if (guild != null) {
            User user = guild.getUserByID(event.user.id);
            if (user != null) {
                guild.getUsers().remove(user);
                Log.i(TAG, "User \""+user.getName()+"\" joined guild \""+guild.getName()+"\".");
                client.dispatcher.dispatch(new UserLeaveEvent(guild, user));
            }
        }
    }

    private void guildMemberUpdate(JsonElement eventObject) {
        GuildMemberUpdateEventResponse event = DiscordUtils.GSON.fromJson(eventObject, GuildMemberUpdateEventResponse.class);
        Guild guild = client.getGuildByID(event.guild_id);
        User user = client.getUserByID(event.user.id);
        if (guild != null && user != null) {
            List<Role> oldRoles = new ArrayList<>(user.getRolesForGuild(guild));
            user.getRolesForGuild(guild).clear();
            for (String role : event.roles) {
                user.addRole(guild.getID(), guild.getRoleByID(role));
            }
            user.addRole(guild.getID(), guild.getRoleByID(guild.getID())); //@everyone role
            client.dispatcher.dispatch(new UserRoleUpdateEvent(oldRoles, user.getRolesForGuild(guild), user, guild));
        }
    }

    private void messageUpdate(JsonElement eventObject) {
        MessageResponse event = DiscordUtils.GSON.fromJson(eventObject, MessageResponse.class);
        String id = event.id;
        String channelID = event.channel_id;
        String content = event.content;
        Channel channel = client.getChannelByID(channelID);
        if (channel == null) return;
        Message toUpdate = channel.getMessageByID(id);
        if (toUpdate != null) {
            Message oldMessage = new Message(client, toUpdate.getID(), toUpdate.getContent(), toUpdate.getAuthor(),
                    toUpdate.getChannel(), toUpdate.getTimestamp(), toUpdate.getEditedTimestamp(),
                    toUpdate.mentionsEveryone(), toUpdate.getRawMentions(), toUpdate.getAttachments());
            toUpdate = DiscordUtils.getMessageFromJSON(client, channel, event);
            client.dispatcher.dispatch(new MessageUpdateEvent(oldMessage, toUpdate));
        }
    }

    private void messageDelete(JsonElement eventObject) {
        MessageDeleteEventResponse event = DiscordUtils.GSON.fromJson(eventObject, MessageDeleteEventResponse.class);
        String id = event.id;
        String channelID = event.channel_id;
        Channel channel = client.getChannelByID(channelID);
        if (channel != null) {
            Message message = channel.getMessageByID(id);
            if (message != null) {
                client.dispatcher.dispatch(new MessageDeleteEvent(message));
            }
        }
    }

    private void presenceUpdate(JsonElement eventObject) {
        PresenceUpdateEventResponse event = DiscordUtils.GSON.fromJson(eventObject, PresenceUpdateEventResponse.class);
        Presences presences = Presences.valueOf(event.status.toUpperCase());
        String gameName = event.game == null ? null : event.game.name;
        Guild guild = client.getGuildByID(event.guild_id);
        if (guild != null
                && presences != null) {
            User user = guild.getUserByID(event.user.id);
            if (user != null) {
                if (!user.getPresence().equals(presences)) {
                    Presences oldPresence = user.getPresence();
                    user.setPresence(presences);
                    client.dispatcher.dispatch(new PresenceUpdateEvent(guild, user, oldPresence, presences));
                    Log.i(TAG, "User \""+user.getName()+"\" changed presence to "+ user.getPresence());
                }
                if (user.getGame() != null && !user.getGame().equals(gameName)) {
                    String oldGame = user.getGame();
                    user.setGame(gameName);
                    client.dispatcher.dispatch(new GameChangeEvent(guild, user, oldGame, gameName));
                    Log.i(TAG, "User \""+user.getName()+"\" changed game to "+ gameName);
                }
            }
        }
    }

    private void guildDelete(JsonElement eventObject) {
        GuildResponse event = DiscordUtils.GSON.fromJson(eventObject, GuildResponse.class);
        Guild guild = client.getGuildByID(event.id);
        client.getGuilds().remove(guild);
        if (event.unavailable) { //Guild can't be reached
            Log.w(TAG, "Guild with id "+event.id+" is unavailable.");
            client.dispatcher.dispatch(new GuildUnavailableEvent(guild));
        } else {
            Log.i(TAG, "You have been kicked from or left \""+guild.getName()+"\"! :O");
            client.dispatcher.dispatch(new GuildLeaveEvent(guild));
        }
    }

    private void channelCreate(JsonElement eventObject) {
        boolean isPrivate = eventObject.getAsJsonObject().get("is_private").getAsBoolean();

        if (isPrivate) { // PM channel.
            PrivateChannelResponse event = DiscordUtils.GSON.fromJson(eventObject, PrivateChannelResponse.class);
            String id = event.id;
            boolean contained = false;
            for (PrivateChannel privateChannel : client.privateChannels) {
                if (privateChannel.getID().equalsIgnoreCase(id))
                    contained = true;
            }

            if (contained)
                return; // we already have this PM channel; no need to create another.

            client.privateChannels.add(DiscordUtils.getPrivateChannelFromJSON(client, event));

        } else { // Regular channel.
            ChannelResponse event = DiscordUtils.GSON.fromJson(eventObject, ChannelResponse.class);
            String type = event.type;
            Guild guild = client.getGuildByID(event.guild_id);
            if (guild != null) {
                if (type.equalsIgnoreCase("text")) { //Text channel
                    Channel channel = DiscordUtils.getChannelFromJSON(client, guild, event);
                    guild.addChannel(channel);
                    client.dispatcher.dispatch(new ChannelCreateEvent(channel));
                } else if (type.equalsIgnoreCase("voice")) {
                    VoiceChannel channel = DiscordUtils.getVoiceChannelFromJSON(client, guild, event);
                    guild.addVoiceChannel(channel);
                    client.dispatcher.dispatch(new VoiceChannelCreateEvent(channel));
                }
            }
        }
    }

    private void channelDelete(JsonElement eventObject) {
        ChannelResponse event = DiscordUtils.GSON.fromJson(eventObject, ChannelResponse.class);
        if (event.type.equalsIgnoreCase("text")) {
            Channel channel = client.getChannelByID(event.id);
            if (channel != null) {
                channel.getGuild().getChannels().remove(channel);
                client.dispatcher.dispatch(new ChannelDeleteEvent(channel));
            }
        } else if (event.type.equalsIgnoreCase("voice")) {
            VoiceChannel channel = client.getVoiceChannelByID(event.id);
            if (channel != null) {
                channel.getGuild().getVoiceChannels().remove(channel);
                client.dispatcher.dispatch(new VoiceChannelDeleteEvent(channel));
            }
        }
    }

    private void userUpdate(JsonElement eventObject) {
        UserUpdateEventResponse event = DiscordUtils.GSON.fromJson(eventObject, UserUpdateEventResponse.class);
        User newUser = client.getUserByID(event.id);
        if (newUser != null) {
            User oldUser = new User(client, newUser.getName(), newUser.getID(), newUser.getDiscriminator(), newUser.getAvatar(), newUser.getPresence(), newUser.isBot());
            newUser = DiscordUtils.getUserFromJSON(client, event);
            client.dispatcher.dispatch(new UserUpdateEvent(oldUser, newUser));
        }
    }

    private void channelUpdate(JsonElement eventObject) {
        ChannelUpdateEventResponse event = DiscordUtils.GSON.fromJson(eventObject, ChannelUpdateEventResponse.class);
        if (!event.is_private) {
            if (event.type.equalsIgnoreCase("text")) {
                Channel toUpdate = client.getChannelByID(event.id);
                if (toUpdate != null) {
                    Channel oldChannel = new Channel(client, toUpdate.getName(),
                            toUpdate.getID(), toUpdate.getGuild(), toUpdate.getTopic(), toUpdate.getPosition(),
                            toUpdate.getRoleOverrides(), toUpdate.getUserOverrides());
                    oldChannel.setLastReadMessageID(toUpdate.getLastReadMessageID());

                    toUpdate = DiscordUtils.getChannelFromJSON(client, toUpdate.getGuild(), event);

                    client.getDispatcher().dispatch(new ChannelUpdateEvent(oldChannel, toUpdate));
                }
            } else if (event.type.equalsIgnoreCase("voice")) {
                VoiceChannel toUpdate = client.getVoiceChannelByID(event.id);
                if (toUpdate != null) {
                    VoiceChannel oldChannel = new VoiceChannel(client, toUpdate.getName(),
                            toUpdate.getID(), toUpdate.getGuild(), "", toUpdate.getPosition(),
                            null, toUpdate.getRoleOverrides(), toUpdate.getUserOverrides());

                    toUpdate = DiscordUtils.getVoiceChannelFromJSON(client, toUpdate.getGuild(), event);

                    client.getDispatcher().dispatch(new VoiceChannelUpdateEvent(oldChannel, toUpdate));
                }
            }
        }
    }

    private void messageAck(JsonElement eventObject) {
        MessageAcknowledgedEventResponse event = DiscordUtils.GSON.fromJson(eventObject, MessageAcknowledgedEventResponse.class);
        Channel channelAck = client.getChannelByID(event.channel_id);
        if (channelAck != null) {
            Message messageAck = channelAck.getMessageByID(event.message_id);
            if (messageAck != null)
                client.getDispatcher().dispatch(new MessageAcknowledgedEvent(messageAck));
        }
    }

    private void guildMembersChunk(JsonElement eventObject) {
        GuildMemberChunkEventResponse event = DiscordUtils.GSON.fromJson(eventObject, GuildMemberChunkEventResponse.class);
        Guild guildToUpdate = client.getGuildByID(event.guild_id);
        if (guildToUpdate == null) {
            Log.w(TAG, "Can't receive guild members chunk for guild id "+event.guild_id+", the guild is null!");
            return;
        }

        for (GuildResponse.MemberResponse member : event.members) {
            guildToUpdate.addUser(DiscordUtils.getUserFromGuildMemberResponse(client, guildToUpdate, member));
        }
    }

    private void guildUpdate(JsonElement eventObject) {
        GuildResponse guildResponse = DiscordUtils.GSON.fromJson(eventObject, GuildResponse.class);
        Guild toUpdate = client.getGuildByID(guildResponse.id);

        if (toUpdate != null) {
            Guild oldGuild = new Guild(client, toUpdate.getName(), toUpdate.getID(), toUpdate.getIcon(),
                    toUpdate.getOwnerID(), toUpdate.getAFKChannel() == null ? null : toUpdate.getAFKChannel().getID(),
                    toUpdate.getAFKTimeout(), toUpdate.getRegion().getID(), toUpdate.getRoles(), toUpdate.getChannels(), toUpdate.getVoiceChannels(),
                    toUpdate.getUsers());

            toUpdate = DiscordUtils.getGuildFromJSON(client, guildResponse);

            if (!toUpdate.getOwnerID().equals(oldGuild.getOwnerID())) {
                client.dispatcher.dispatch(new GuildTransferOwnershipEvent(oldGuild.getOwner(), toUpdate.getOwner(), toUpdate));
            } else {
                client.dispatcher.dispatch(new GuildUpdateEvent(oldGuild, toUpdate));
            }
        }
    }

    private void guildRoleCreate(JsonElement eventObject) {
        GuildRoleEventResponse event = DiscordUtils.GSON.fromJson(eventObject, GuildRoleEventResponse.class);
        Guild guild = client.getGuildByID(event.guild_id);
        if (guild != null) {
            Role role = DiscordUtils.getRoleFromJSON(guild, event.role);
            guild.addRole(role);
            client.dispatcher.dispatch(new RoleCreateEvent(role, guild));
        }
    }

    private void guildRoleUpdate(JsonElement eventObject) {
        GuildRoleEventResponse event = DiscordUtils.GSON.fromJson(eventObject, GuildRoleEventResponse.class);
        Guild guild = client.getGuildByID(event.guild_id);
        if (guild != null) {
            Role toUpdate = guild.getRoleByID(event.role.id);
            if (toUpdate != null) {
                Role oldRole = new Role(toUpdate.getPosition(),
                        Permissions.generatePermissionsNumber(toUpdate.getPermissions()), toUpdate.getName(),
                        toUpdate.isManaged(), toUpdate.getID(), toUpdate.isHoisted(), toUpdate.getColor(), guild);
                toUpdate = DiscordUtils.getRoleFromJSON(guild, event.role);
                client.dispatcher.dispatch(new RoleUpdateEvent(oldRole, toUpdate, guild));
            }
        }
    }

    private void guildRoleDelete(JsonElement eventObject) {
        GuildRoleDeleteEventResponse event = DiscordUtils.GSON.fromJson(eventObject, GuildRoleDeleteEventResponse.class);
        Guild guild = client.getGuildByID(event.guild_id);
        if (guild != null) {
            Role role = guild.getRoleByID(event.role_id);
            if (role != null) {
                guild.getRoles().remove(role);
                client.dispatcher.dispatch(new RoleDeleteEvent(role, guild));
            }
        }
    }

    private void guildBanAdd(JsonElement eventObject) {
        GuildBanEventResponse event = DiscordUtils.GSON.fromJson(eventObject, GuildBanEventResponse.class);
        Guild guild = client.getGuildByID(event.guild_id);
        if (guild != null) {
            User user = DiscordUtils.getUserFromJSON(client, event.user);
            if (client.getUserByID(user.getID()) != null)
                guild.getUsers().remove(user);

            client.dispatcher.dispatch(new UserBanEvent(user, guild));
        }
    }

    private void guildBanRemove(JsonElement eventObject) {
        GuildBanEventResponse event = DiscordUtils.GSON.fromJson(eventObject, GuildBanEventResponse.class);
        Guild guild = client.getGuildByID(event.guild_id);
        if (guild != null) {
            User user = DiscordUtils.getUserFromJSON(client, event.user);

            client.dispatcher.dispatch(new UserPardonEvent(user, guild));
        }
    }

    private void voiceStateUpdate(JsonElement eventObject) {
        VoiceStateResponse event = DiscordUtils.GSON.fromJson(eventObject, VoiceStateResponse.class);
        Guild guild = client.getGuildByID(event.guild_id);

        if (guild != null) {
            VoiceChannel channel = guild.getVoiceChannelByID(event.channel_id);
            User user = guild.getUserByID(event.user_id);
            VoiceChannel oldChannel = user.getVoiceChannel();
            user.setVoiceChannel(channel);
            if (channel != oldChannel) {
                if (channel == null) {
                    client.dispatcher.dispatch(new UserVoiceChannelLeaveEvent(oldChannel));
                } else if (oldChannel == null) {
                    client.dispatcher.dispatch(new UserVoiceChannelJoinEvent(channel));
                } else {
                    client.dispatcher.dispatch(new UserVoiceChannelMoveEvent(oldChannel, channel));
                }
            } else {
                client.dispatcher.dispatch(new UserVoiceStateUpdateEvent(user, channel, event.self_mute, event.self_deaf, event.mute, event.deaf, event.suppress));
            }
        }
    }

    private void voiceServerUpdate(JsonElement eventObject) {
        VoiceUpdateResponse event = DiscordUtils.GSON.fromJson(eventObject, VoiceUpdateResponse.class);
        try {
            event.endpoint = event.endpoint.substring(0, event.endpoint.indexOf(":"));
            client.voiceConnections.put(client.getGuildByID(event.guild_id), new DiscordVoiceWS(event, client));
        } catch (Exception e) {
            Log.e(TAG, "Error updating voice server: " + e);
            e.printStackTrace();
        }
    }

    @Override
    public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception {
        //Converts binary data to readable string data
        try {
            InflaterInputStream inputStream = new InflaterInputStream(new ByteArrayInputStream(binary));
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder sb = new StringBuilder();
            String read;
            while ((read = reader.readLine()) != null) {
                sb.append(read);
            }

            String data = sb.toString();
            reader.close();
            inputStream.close();

            onTextMessage(websocket, data);
        } catch (IOException e) {
            Log.d(TAG, "Binary message error: " + e);
        }
    }

    @Override
    public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
        super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
        disconnect(DiscordDisconnectedEvent.Reason.UNKNOWN);
    }

    @Override
    public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
        super.onError(websocket, cause);
        cause.printStackTrace();
        disconnect(DiscordDisconnectedEvent.Reason.UNKNOWN);
    }

    /**
     * Gets the most recent ping response time by discord.
     *
     * @return The response time (in ms).
     */
    public long getResponseTime() {
        return pingResponseTime;
    }
}
