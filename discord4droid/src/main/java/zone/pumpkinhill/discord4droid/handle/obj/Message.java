package zone.pumpkinhill.discord4droid.handle.obj;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import zone.pumpkinhill.discord4droid.api.DiscordClient;
import zone.pumpkinhill.discord4droid.api.DiscordUtils;
import zone.pumpkinhill.discord4droid.api.Endpoints;
import zone.pumpkinhill.discord4droid.api.Requests;
import zone.pumpkinhill.discord4droid.handle.events.MessageUpdateEvent;
import zone.pumpkinhill.discord4droid.json.requests.MessageRequest;
import zone.pumpkinhill.discord4droid.json.responses.MessageResponse;
import zone.pumpkinhill.discord4droid.util.DiscordException;
import zone.pumpkinhill.discord4droid.util.HTTP429Exception;
import zone.pumpkinhill.discord4droid.util.MissingPermissionsException;
import zone.pumpkinhill.http.entity.StringEntity;
import zone.pumpkinhill.http.message.BasicNameValuePair;

/**
 * Represents a discord message.
 */
public class Message extends DiscordObject {

    /**
     * The actual message (what you see
     * on your screen, the content).
     */
    protected String content;

    /**
     * The User who sent the message.
     */
    protected final User author;

    /**
     * The ID of the channel the message was sent in.
     */
    protected final Channel channel;

    /**
     * The time the message was received.
     */
    protected Date timestamp;

    /**
     * The time (if it exists) that the message was edited.
     */
    protected Date editedTimestamp;

    /**
     * The list of users mentioned by this message.
     */
    protected List<String> mentions;

    /**
     * The attachments, if any, on the message.
     */
    protected List<Attachment> attachments;

    /**
     * Whether the
     */
    protected boolean mentionsEveryone;

    public Message(DiscordClient client, String id, String content, User user, Channel channel,
                   Date timestamp, Date editedTimestamp, boolean mentionsEveryone,
                   List<String> mentions, List<Attachment> attachments) {
        super(client, id);
        this.content = content;
        this.author = user;
        this.channel = channel;
        this.timestamp = timestamp;
        this.editedTimestamp = editedTimestamp;
        this.mentions = mentions;
        this.attachments = attachments;
        this.mentionsEveryone = mentionsEveryone;
    }

    /**
     * Gets the string content of the message.
     *
     * @return The content of the message
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the CACHED content of the message.
     *
     * @param content The new message content.
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Sets the CACHED mentions in this message.
     *
     * @param mentions The new mentions.
     */
    public void setMentions(List<String> mentions) {
        this.mentions = mentions;
    }

    /**
     * Sets the CACHED attachments in this message.
     *
     * @param attachments The new attachements.
     */
    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    /**
     * Gets the channel that this message belongs to.
     *
     * @return The channel.
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * Gets the user who authored this message.
     *
     * @return The author.
     */
    public User getAuthor() {
        return author;
    }

    /**
     * Sets the CACHED version of the message timestamp.
     *
     * @param timestamp The timestamp.
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the timestamp for when this message was sent/edited.
     *
     * @return The timestamp.
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the users mentioned in this message.
     *
     * @return The users mentioned.
     */
    public List<User> getMentions() {
        if (mentionsEveryone) {
            return channel.getGuild().getUsers();
        } else {
            ArrayList<User> user = new ArrayList<>();
            for(String m : mentions) {
                user.add(client.getUserByID(m));
            }
            return user;
        }
    }

    /**
     * Gets the attachments in this message.
     *
     * @return The attachments.
     */
    public List<Attachment> getAttachments() {
        return attachments;
    }

    /**
     * Adds an "@mention," to the author of the referenced Message
     * object before your content
     *
     * @param content Message to send.
     * @throws MissingPermissionsException
     * @throws HTTP429Exception
     * @throws DiscordException
     */
    public void reply(String content) throws MissingPermissionsException, HTTP429Exception, DiscordException {
        getChannel().sendMessage(String.format("%s, %s", this.getAuthor(), content));
    }

    /**
     * Edits the message. NOTE: Discord only supports editing YOUR OWN messages!
     *
     * @param content The new content for the message to contain.
     * @return The new message (this).
     *
     * @throws MissingPermissionsException
     * @throws DiscordException
     */
    public Message edit(String content) throws MissingPermissionsException, HTTP429Exception, DiscordException {
        if (!this.getAuthor().equals(client.getOurUser()))
            throw new MissingPermissionsException("Cannot edit other users' messages!");
        if (client.isReady()) {
            MessageResponse response = DiscordUtils.GSON.fromJson(
                    Requests.PATCH.makeRequest(client.getURL() + Endpoints.CHANNELS+channel.getID()+"/messages/"+id,
                    new StringEntity(DiscordUtils.GSON.toJson(new MessageRequest(content, false)), "UTF-8"),
                    new BasicNameValuePair("authorization", client.getToken()),
                    new BasicNameValuePair("content-type", "application/json")), MessageResponse.class);

            Message oldMessage = copy();
            DiscordUtils.getMessageFromJSON(client, channel, response);
            //Event dispatched here because otherwise there'll be an NPE as for some reason when the bot edits a message,
            // the event chain goes like this:
            //Original message edited to null, then the null message edited to the new content
            client.getDispatcher().dispatch(new MessageUpdateEvent(oldMessage, this));

        } else {
            System.out.println("Bot has not signed in yet!");
        }
        return this;
    }

    /**
     * Gets the raw list of mentioned user ids.
     *
     * @return Mentioned user list.
     */
    public List<String> getRawMentions() {
        return mentions;
    }

    /**
     * Returns whether this message mentions everyone.
     *
     * @return True if it mentions everyone, false if otherwise.
     */
    public boolean mentionsEveryone() {
        return mentionsEveryone;
    }

    /**
     * CACHES whether the message mentions everyone.
     *
     * @param mentionsEveryone True to mention everyone false if otherwise.
     */
    public void setMentionsEveryone(boolean mentionsEveryone) {
        this.mentionsEveryone = mentionsEveryone;
    }

    /**
     * Deletes the message.
     *
     * @throws MissingPermissionsException
     * @throws HTTP429Exception
     * @throws DiscordException
     */
    public void delete() throws MissingPermissionsException, HTTP429Exception, DiscordException {
        if (!getAuthor().equals(client.getOurUser()))
            DiscordUtils.checkPermissions(client, getChannel(), EnumSet.of(Permissions.MANAGE_MESSAGES));

        if (client.isReady()) {
            Requests.DELETE.makeRequest(client.getURL() + Endpoints.CHANNELS+channel.getID()+"/messages/"+id,
                    new BasicNameValuePair("authorization", client.getToken()));
        } else {
            System.out.println("Bot has not signed in yet!");
        }
    }

    /**
     * Gets the time that this message was last edited.
     *
     * @return The edited timestamp.
     */
    public Date getEditedTimestamp() {
        return editedTimestamp;
    }

    /**
     * This sets the CACHED edited timestamp.
     *
     * @param editedTimestamp The new timestamp.
     */
    public void setEditedTimestamp(Date editedTimestamp) {
        this.editedTimestamp = editedTimestamp;
    }

    /**
     * Gets the guild this message is from.
     *
     * @return The guild.
     */
    public Guild getGuild() {
        return getChannel().getGuild();
    }

    @Override
    public String toString() {
        return content;
    }

    public Message copy() {
        return new Message(client, id, content, author, channel, timestamp,
                editedTimestamp, mentionsEveryone, mentions, attachments);
    }
}
