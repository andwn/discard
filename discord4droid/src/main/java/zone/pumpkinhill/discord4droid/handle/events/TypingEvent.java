package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.Channel;
import zone.pumpkinhill.discord4droid.handle.obj.User;

/**
 * This event is dispatched if a user is typing.
 */
public class TypingEvent extends Event {

    private final User user;
    private final Channel channel;

    public TypingEvent(User user, Channel channel) {
        this.user = user;
        this.channel = channel;
    }

    /**
     * The user involved.
     *
     * @return The user.
     */
    public User getUser() {
        return user;
    }

    /**
     * The channel involved.
     *
     * @return The channel.
     */
    public Channel getChannel() {
        return channel;
    }
}
