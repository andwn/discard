package zone.pumpkinhill.discord4droid.json.requests;

/**
 * This is the request sent in order to edit a channel's information.
 */
public class ChannelEditRequest {

    /**
     * The new name (2-100 characters long) of the channel.
     */
    public String name;

    /**
     * The new position of the channel.
     */
    public int position;

    /**
     * The new topic of the channel.
     */
    public String topic;

    public ChannelEditRequest(String name, int position, String topic) {
        this.name = name;
        this.position = position;
        this.topic = topic;
    }
}
