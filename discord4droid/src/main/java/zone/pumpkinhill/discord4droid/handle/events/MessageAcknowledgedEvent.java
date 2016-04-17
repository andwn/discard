package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.Message;

/**
 * This event is dispatched when this bot's account acknowledges a message.
 */
public class MessageAcknowledgedEvent extends Event {

    private final Message acknowledged;

    public MessageAcknowledgedEvent(Message acknowledged) {
        this.acknowledged = acknowledged;
    }

    /**
     * Gets the message that was acknowledged.
     *
     * @return The acknowledged message.
     */
    public Message getAcknowledgedMessage() {
        return acknowledged;
    }
}
