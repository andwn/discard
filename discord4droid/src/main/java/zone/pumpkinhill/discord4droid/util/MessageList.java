package zone.pumpkinhill.discord4droid.util;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import zone.pumpkinhill.discord4droid.api.DiscordClient;
import zone.pumpkinhill.discord4droid.api.DiscordUtils;
import zone.pumpkinhill.discord4droid.api.Endpoints;
import zone.pumpkinhill.discord4droid.api.Requests;
import zone.pumpkinhill.discord4droid.handle.obj.Channel;
import zone.pumpkinhill.discord4droid.handle.obj.Message;
import zone.pumpkinhill.discord4droid.handle.obj.Permissions;
import zone.pumpkinhill.discord4droid.json.responses.MessageResponse;
import zone.pumpkinhill.http.message.BasicNameValuePair;

/**
 * Wrapper around ArrayList to manage messages in a text channel
 */
public class MessageList {
    private final ArrayList<Message> messageCache = new ArrayList<>();
    private final DiscordClient client;
    private final Channel channel;

    /**
     * @param client The client for this list to respect.
     * @param channel The channel to retrieve messages from.
     */
    public MessageList(DiscordClient client, Channel channel) {
        this.client = client;
        this.channel = channel;
    }

    public synchronized Message get(int index) {
        return messageCache.get(index);
    }

    public boolean load(int messageCount, String before)
            throws DiscordException, HTTP429Exception, MissingPermissionsException {
        // Make sure we have read permission first
        DiscordUtils.checkPermissions(client, channel,
                EnumSet.of(Permissions.READ_MESSAGES, Permissions.READ_MESSAGE_HISTORY));
        int initialSize = size();
        String queryParams = "?limit="+messageCount;
        if(before != null) queryParams += "&before=" + before;
        String response = Requests.GET.makeRequest(
                client.getURL() + Endpoints.CHANNELS + channel.getID() + "/messages" + queryParams,
                new BasicNameValuePair("authorization", client.getToken()));
        if (response == null) return false;
        MessageResponse[] messages = DiscordUtils.GSON.fromJson(response, MessageResponse[].class);
        if (messages.length == 0) return false;
        for (MessageResponse messageResponse : messages) {
            add(DiscordUtils.getMessageFromJSON(client, channel, messageResponse));
        }
        return size() - initialSize <= messageCount;
    }

    public synchronized void add(Message message) {
        if(messageCache.contains(message)) return;
        if(messageCache.size() == 0) {
            messageCache.add(message);
        } else {
            for(int i = 0; i < messageCache.size(); i++) {
                if(messageCache.get(i).getTimestamp().before(message.getTimestamp())) {
                    messageCache.add(i, message);
                    return;
                }
            }
            messageCache.add(message);
        }
    }

    public void addAll(List<Message> messages) {
        for(Message m : messages) add(m);
    }

    public boolean contains(Message m) {
        return messageCache.contains(m);
    }

    public int size() {
        return messageCache.size();
    }

    public synchronized boolean remove(Message m) {
        return m.getChannel().equals(channel) && messageCache.remove(m);
    }

    public synchronized Message remove(int index) {
        if (index >= size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        Message message = get(index);
        remove(message);
        return message;
    }

    public List<Message> copy() {
        return new ArrayList<>(messageCache);
    }

    public List<Message> reverse() {
        List<Message> messages = new ArrayList<>();
        for(Message m : messageCache) {
            messages.add(0, m);
        }
        return messages;
    }

    public Message getEarliest() {
        return get(size()-1);
    }

    public Message getLatest() {
        return get(0);
    }

    public Message get(String id) {
        for(Message m : messageCache) {
            if(m.getID().equalsIgnoreCase(id)) return m;
        }
        return null;
    }

    public Channel getChannel() {
        return channel;
    }
}
