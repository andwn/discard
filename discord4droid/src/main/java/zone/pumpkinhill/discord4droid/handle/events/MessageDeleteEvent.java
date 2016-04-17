package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.Message;

/**
 * This event is dispatched whenever a message is deleted.
 */
public class MessageDeleteEvent extends Event {

    private final Message message;

    public MessageDeleteEvent(Message message) {
        this.message = message;
    }

    /**
     * Gets the message deleted.
     *
     * @return The message.
     */
    public Message getMessage() {
        return message;
    }
}
