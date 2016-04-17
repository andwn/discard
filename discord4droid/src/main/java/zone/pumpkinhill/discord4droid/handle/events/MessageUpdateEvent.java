package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.Message;

/**
 * This event is dispatched whenever a message is edited.
 */
public class MessageUpdateEvent extends Event {

    private final Message oldMessage, newMessage;

    public MessageUpdateEvent(Message oldMessage, Message newMessage) {
        this.oldMessage = oldMessage;
        this.newMessage = newMessage;
    }

    /**
     * The original message.
     *
     * @return The message.
     */
    public Message getOldMessage() {
        return oldMessage;
    }

    /**
     * The new message.
     *
     * @return The message.
     */
    public Message getNewMessage() {
        return newMessage;
    }
}
