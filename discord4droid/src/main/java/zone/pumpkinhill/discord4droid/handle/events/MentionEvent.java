package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.Message;

/**
 * This event is dispatched whenever the bot is @mentioned.
 */
public class MentionEvent extends Event {

    private final Message message;

    public MentionEvent(Message message) {
        this.message = message;
    }

    /**
     * Gets the messaged which @mention'd the bot.
     *
     * @return The message.
     */
    public Message getMessage() {
        return message;
    }
}
