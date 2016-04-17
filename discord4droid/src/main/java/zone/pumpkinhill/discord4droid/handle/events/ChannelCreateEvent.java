package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.Channel;

/**
 * This event is dispatched when a channel is created.
 */
public class ChannelCreateEvent extends Event {

    private final Channel channel;

    public ChannelCreateEvent(Channel channel) {
        this.channel = channel;
    }

    /**
     * Gets the channel involved.
     *
     * @return The channel.
     */
    public Channel getChannel() {
        return channel;
    }
}
