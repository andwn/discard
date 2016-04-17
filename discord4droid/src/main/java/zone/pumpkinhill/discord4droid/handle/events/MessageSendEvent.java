package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.Message;

/**
 * This event is dispatched whenever a message is sent by the bot.
 */
public class MessageSendEvent extends Event {

    private Message message;

    public MessageSendEvent(Message message) {
        this.message = message;
    }

    /**
     * Gets the message sent.
     *
     * @return The message.
     */
    public Message getMessage() {
        return message;
    }
}
