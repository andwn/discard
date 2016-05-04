package zone.pumpkinhill.discord4droid.handle.obj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import zone.pumpkinhill.discord4droid.api.DiscordClient;
import zone.pumpkinhill.discord4droid.api.DiscordUtils;
import zone.pumpkinhill.discord4droid.handle.events.VoiceDisconnectedEvent;
import zone.pumpkinhill.discord4droid.json.requests.VoiceChannelRequest;
import zone.pumpkinhill.discord4droid.util.DiscordException;
import zone.pumpkinhill.discord4droid.util.HTTP429Exception;
import zone.pumpkinhill.discord4droid.util.MessageList;
import zone.pumpkinhill.discord4droid.util.MissingPermissionsException;

/**
 * Represents a voice channel.
 */
public class VoiceChannel extends Channel {

    public VoiceChannel(DiscordClient client, String name, String id, Guild parent,
                        String topic, int position) {
        this(client, name, id, parent, topic, position, new ArrayList<Message>(),
                new HashMap<String, PermissionOverride>(), new HashMap<String, PermissionOverride>());
    }

    public VoiceChannel(DiscordClient client, String name, String id, Guild parent,
                        String topic, int position, List<Message> messages,
                        Map<String, PermissionOverride> roleOverrides,
                        Map<String, PermissionOverride> userOverrides) {
        super(client, name, id, parent, topic, position, roleOverrides, userOverrides);
    }

    /**
     * Makes the bot user join this voice channel.
     */
    public void join() {
        if (client.isReady()) {

            if (client.voiceConnections.containsKey(parent)) {
                System.out.println("Attempting to join a multiple channels in the same guild! Moving channels instead...");
                try {
                    client.getOurUser().moveToVoiceChannel(this);
                } catch (DiscordException | HTTP429Exception | MissingPermissionsException e) {
                    System.out.println("Unable to switch voice channels! Aborting join request..." + e);
                    return;
                }
            } else if (client.getConnectedVoiceChannels().size() > 0)
                throw new UnsupportedOperationException("Must be a bot account to have multi-server voice support!");

            client.connectedVoiceChannels.add(this);
            client.ws.send(DiscordUtils.GSON.toJson(new VoiceChannelRequest(parent.getID(), id, false, false)));
        } else {
            System.out.println("Bot has not signed in yet!");
        }
    }

    /**
     * Makes the bot user leave this voice channel.
     */
    public void leave(){
        if (client.getConnectedVoiceChannels().contains(this)) {
            client.connectedVoiceChannels.remove(this);
            client.ws.send(DiscordUtils.GSON.toJson(new VoiceChannelRequest(parent.getID(), null, false, false)));
            client.voiceConnections.get(parent).disconnect(VoiceDisconnectedEvent.Reason.LEFT_CHANNEL);
        } else {
            System.out.println("Attempted to leave an not joined voice channel! Ignoring the method call...");
        }
    }

    /**
     * Checks if this voice channel is connected to by our user.
     *
     * @return True if connected, false if otherwise.
     */
    public boolean isConnected() {
        return client.getConnectedVoiceChannels().contains(this);
    }

    @Override
    public MessageList getMessages() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Message getMessageByID(String messageID) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTopic() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTopic(String topic) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Message sendMessage(String content) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Message sendMessage(String content, boolean tts) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLastReadMessageID() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Message getLastReadMessage() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLastReadMessageID(String lastReadMessageID) {
        throw new UnsupportedOperationException();
    }
}
