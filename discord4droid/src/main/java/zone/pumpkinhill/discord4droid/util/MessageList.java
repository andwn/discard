package zone.pumpkinhill.discord4droid.util;

import android.util.Log;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import zone.pumpkinhill.discord4droid.api.DiscordClient;
import zone.pumpkinhill.discord4droid.api.DiscordUtils;
import zone.pumpkinhill.discord4droid.api.Endpoints;
import zone.pumpkinhill.discord4droid.api.EventSubscriber;
import zone.pumpkinhill.discord4droid.api.Requests;
import zone.pumpkinhill.discord4droid.handle.events.ChannelDeleteEvent;
import zone.pumpkinhill.discord4droid.handle.events.ChannelUpdateEvent;
import zone.pumpkinhill.discord4droid.handle.events.GuildLeaveEvent;
import zone.pumpkinhill.discord4droid.handle.events.GuildTransferOwnershipEvent;
import zone.pumpkinhill.discord4droid.handle.events.GuildUpdateEvent;
import zone.pumpkinhill.discord4droid.handle.events.MessageDeleteEvent;
import zone.pumpkinhill.discord4droid.handle.events.MessageReceivedEvent;
import zone.pumpkinhill.discord4droid.handle.events.MessageSendEvent;
import zone.pumpkinhill.discord4droid.handle.events.RoleUpdateEvent;
import zone.pumpkinhill.discord4droid.handle.events.UserRoleUpdateEvent;
import zone.pumpkinhill.discord4droid.handle.obj.Channel;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.Message;
import zone.pumpkinhill.discord4droid.handle.obj.Permissions;
import zone.pumpkinhill.discord4droid.handle.obj.PrivateChannel;
import zone.pumpkinhill.discord4droid.handle.obj.Role;
import zone.pumpkinhill.discord4droid.handle.obj.VoiceChannel;
import zone.pumpkinhill.discord4droid.json.responses.MessageResponse;
import zone.pumpkinhill.http.message.BasicNameValuePair;

/**
 * This class is a custom implementation of {@link List} for retrieving discord messages.
 *
 * The list gets a message on demand, it either fetches it from the cache or it requests the message from Discord
 * if not cached.
 */
public class MessageList extends AbstractList<Message> implements List<Message> {
    private final static String TAG = MessageList.class.getCanonicalName();

    /**
     * This is used to cache message objects to prevent unnecessary queries.
     */
    private final LinkedBlockingDeque<Message> messageCache = new LinkedBlockingDeque<>();

    /**
     * This represents the amount of messages to fetch from discord every time the index goes out of bounds.
     */
    public static final int MESSAGE_CHUNK_COUNT = 100; //100 is the max amount discord lets you retrieve at one time

    /**
     * The client that this list is respecting.
     */
    private final DiscordClient client;

    /**
     * The channel the messages are from.
     */
    private final Channel channel;

    /**
     * This is true if the client object has permission to read this channel's messages.
     */
    private volatile boolean hasPermission;

    /**
     * This is the maximum amount of messages that will be cached by this list. If negative, it'll store unlimited
     * messages.
     */
    private volatile int capacity = 256;

    /**
     * @param client The client for this list to respect.
     * @param channel The channel to retrieve messages from.
     */
    public MessageList(DiscordClient client, Channel channel) {
        if (channel instanceof VoiceChannel) {
            throw new UnsupportedOperationException();
        }
        this.client = client;
        this.channel = channel;
        updatePermissions();
    }

    /**
     * This implementation of {@link List#get(int)} first checks if the requested message is cached, if so it retrieves
     * that object, otherwise it requests messages from discord in chunks of {@link #MESSAGE_CHUNK_COUNT} until it gets
     * the requested object. If the object cannot be found, it throws an {@link ArrayIndexOutOfBoundsException}.
     *
     * @param index The index (starting at 0) of the message in this list.
     * @return The message object for this index.
     */
    @Override
    public synchronized Message get(int index) {
        while (size() <= index) {
            try {
                if (!load(MESSAGE_CHUNK_COUNT))
                    throw new ArrayIndexOutOfBoundsException();
            } catch (Exception e) {
                throw new ArrayIndexOutOfBoundsException("Error querying for additional messages. (Cause: "+e.getClass().getSimpleName()+")");
            }
        }
        Message message = (Message) messageCache.toArray()[index];
        purge();
        return message;
    }

    /**
     * This purges the list's internal message cache so that the oldest messages are removed until the list's capacity
     * requirements are met.
     *
     * @return The amount of messages cleared.
     */
    public int purge() {
        if (capacity >= 0) {
            int start = size();
            for (int i = start-1; i >= capacity; i--) {
                messageCache.remove(messageCache.toArray()[i]);
            }
            return start-size();
        }
        return 0;
    }

    private boolean queryMessages(int messageCount) throws DiscordException, HTTP429Exception {
        if (!hasPermission) return false;
        int initialSize = size();
        String queryParams = "?limit="+messageCount;
        if (initialSize != 0) {
            queryParams += "&before=" + messageCache.getLast().getID();
        }
        String response = Requests.GET.makeRequest(
                client.getURL() + Endpoints.CHANNELS + channel.getID() + "/messages" + queryParams,
                new BasicNameValuePair("authorization", client.getToken()));
        if (response == null) return false;
        MessageResponse[] messages = DiscordUtils.GSON.fromJson(response, MessageResponse[].class);
        if (messages.length == 0) return false;
        for (MessageResponse messageResponse : messages) {
            if (!add(DiscordUtils.getMessageFromJSON(client, channel, messageResponse), true)) {
                return false;
            }
        }
        return size() - initialSize <= messageCount;
    }

    /**
     * This adds a message object to the internal message cache.
     *
     * @param message The message object to cache.
     * @return True if the object was successfully cached, false if otherwise.
     */
    @Override
    public synchronized boolean add(Message message) {
        return add(message, false);
    }

    /**
     * This method was delegated so that the {@link #get(int)} method won't be broken if queried messages exceed the
     * list's capacity.
     *
     * @param message The message to cache.
     * @param skipPurge Whether to skip purging the cache, true to skip, false to purge.
     * @return True if the object was successfully cached, false if otherwise.
     */
    private boolean add(Message message, boolean skipPurge) {
        if (messageCache.contains(message)) return false;
        int initialSize = size();
        if (initialSize == 0) {
            messageCache.add(message);
        } else {
            if (MessageComparator.REVERSED.compare(message, messageCache.getFirst()) > -1)
                messageCache.addLast(message);
            else
                messageCache.addFirst(message);
        }
        boolean cacheChanged = initialSize != size();
        if (!skipPurge) purge();
        return cacheChanged;
    }

    /**
     * This implementation of {@link List#size()} gets the size of the internal message cache NOT the total amount of
     * messages which exist in a channel in total.
     *
     * @return The amount of messages in the internal message cache.
     */
    @Override
    public int size() {
        return messageCache.size();
    }

    @Override
    public synchronized boolean remove(Object o) {
        return o instanceof Message &&
                ((Message)o).getChannel().equals(channel) &&
                messageCache.remove(o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Message remove(int index) {
        if (index >= size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        Message message = get(index);
        remove(message);
        return message;
    }

    /**
     * This creates a new {@link List} from this message list.
     *
     * @return The copied list. Note: This list is a copy of the current message cache, not a copy of this specific
     * instance of {@link MessageList}. It will ONLY ever contain the contents of the current message cache.
     */
    public List<Message> copy() {
        return new ArrayList<>(this);
    }

    /**
     * A utility method to reverse the order of this list.
     *
     * @return A reversed COPY of this list.
     *
     * @see #copy()
     */
    public List<Message> reverse() {
        List<Message> messages = new ArrayList<>(); // = copy();
        for(Message m : this) {
            messages.add(0, m);
        }
        return messages;
    }

    /**
     * This retrieves the earliest CACHED message.
     *
     * @return The earliest message. A cleaner version of {@link #get(int)} with an index of {@link #size()}-1.
     */
    public Message getEarliestMessage() {
        return get(size()-1);
    }

    /**
     * This retrieves the latest CACHED message.
     *
     * @return The latest message. A cleaner version of {@link #get(int)} with an index of 0.
     */
    public Message getLatestMessage() {
        return get(0);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * This retrieves a message object with the specified message id.
     *
     * @param id The message id to search for.
     * @return The message object found, or null if nonexistent.
     */
    public Message get(String id) {
        for(Message m : this) {
            if(m.getID().equalsIgnoreCase(id)) return m;
        }
        return null;
    }

    /**
     * This attempts to load the specified number of messages into the list's cache. NOTE: this calls {@link #purge()}
     * after loading.
     *
     * @param messageCount The amount of messages to load.
     * @return True if this action was successful, false if otherwise.
     *
     * @throws HTTP429Exception
     */
    public boolean load(int messageCount) throws HTTP429Exception {
        try {
            boolean success = queryMessages(messageCount);
            purge();
            return success;
        } catch (DiscordException e) {
            Log.e(TAG, "Error loading messages: " + e);
        }
        return false;
    }

    /**
     * Sets the maximum amount of messages to be cached by this list. NOTE: This purges immediately after changing the
     * capacity.
     *
     * @param capacity The capacity, if negative the capacity will be unlimited.
     */
    public void setCacheCapacity(int capacity) {
        this.capacity = capacity;
        purge();
    }

    /**
     * Gets the maximum amount of messages to be cached by this list.
     *
     * @return The capacity, if negative the capacity will be unlimited.
     */
    public int getCacheCapacity() {
        return capacity;
    }

    private void updatePermissions() {
        try {
            DiscordUtils.checkPermissions(client, channel, EnumSet.of(Permissions.READ_MESSAGES, Permissions.READ_MESSAGE_HISTORY));
            hasPermission = true;
        } catch (MissingPermissionsException e) {
            System.out.println("LOL");
            Log.w(TAG, "Missing permissions required to read channel " + channel.getName() + ".");
            hasPermission = false;
        }
    }

    /**
     * This is used to automatically update the message list.
     */
    public static class MessageListEventListener {

        private volatile MessageList list;

        public MessageListEventListener(MessageList list) {
            this.list = list;
        }

        @EventSubscriber
        public void onMessageReceived(MessageReceivedEvent event) {
            if (event.getMessage().getChannel().equals(list.channel)) {
                list.add(event.getMessage());
            }
        }

        @EventSubscriber
        public void onMessageSent(MessageSendEvent event) {
            if (event.getMessage().getChannel().equals(list.channel)) {
                list.add(event.getMessage());
            }
        }

        @EventSubscriber
        public void onMessageDelete(MessageDeleteEvent event) {
            if (event.getMessage().getChannel().equals(list.channel)) {
                list.remove(event.getMessage());
            }
        }

        //The following are to unregister this listener to optimize the event dispatcher.

        @EventSubscriber
        public void onChannelDelete(ChannelDeleteEvent event) {
            if (event.getChannel().equals(list.channel)) {
                list.client.getDispatcher().unregisterListener(this);
            }
        }

        @EventSubscriber
        public void onGuildRemove(GuildLeaveEvent event) {
            if (!(list.channel instanceof PrivateChannel) &&
                    event.getGuild().equals(list.channel.getGuild())) {
                list.client.getDispatcher().unregisterListener(this);
            }
        }

        //The following are to update the hasPermission boolean

        @EventSubscriber
        public void onRoleUpdate(RoleUpdateEvent event) {
            if(list.channel instanceof PrivateChannel) return;
            Guild thisGuild = list.channel.getGuild();
            if(!event.getGuild().equals(thisGuild)) return;
            List<Role> roles = list.client.getOurUser().getRolesForGuild(thisGuild);
            if (roles.contains(event.getNewRole())) list.updatePermissions();
        }

        @EventSubscriber
        public void onGuildUpdate(GuildUpdateEvent event) {
            if (!(list.channel instanceof PrivateChannel) &&
                    event.getNewGuild().equals(list.channel.getGuild())) {
                list.updatePermissions();
            }
        }

        @EventSubscriber
        public void onUserRoleUpdate(UserRoleUpdateEvent event) {
            if (!(list.channel instanceof PrivateChannel) &&
                    event.getUser().equals(list.client.getOurUser()) &&
                    event.getGuild().equals(list.channel.getGuild())) {
                list.updatePermissions();
            }
        }

        @EventSubscriber
        public void onGuildTransferOwnership(GuildTransferOwnershipEvent event) {
            if (!(list.channel instanceof PrivateChannel) &&
                    event.getGuild().equals(list.channel.getGuild())) {
                list.updatePermissions();
            }
        }

        @EventSubscriber
        public void onChannelUpdateEvent(ChannelUpdateEvent event) {
            if (event.getNewChannel().equals(list.channel))
                list.updatePermissions();
        }
    }
}
