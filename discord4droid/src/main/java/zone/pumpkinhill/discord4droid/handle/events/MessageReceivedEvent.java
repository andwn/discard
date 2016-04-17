package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.Message;

/**
 * This event is dispatched whenever a message is received.
 */
public class MessageReceivedEvent extends Event {

    private final Message message;

    public MessageReceivedEvent(Message message) {
        this.message = message;
    }

    /**
     * Gets the message received.
     *
     * @return The message.
     */
    public Message getMessage() {
        return message;
    }
}
