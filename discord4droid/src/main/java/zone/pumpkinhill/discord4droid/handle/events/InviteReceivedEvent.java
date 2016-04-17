package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.Invite;
import zone.pumpkinhill.discord4droid.handle.obj.Message;

/**
 * This event is dispatched when a message the bot receives includes an invite link.
 */
public class InviteReceivedEvent extends Event {

    private final Invite invite;
    private final Message message;

    public InviteReceivedEvent(Invite invite, Message message) {
        this.invite = invite;
        this.message = message;
    }

    /**
     * Gets the invite received.
     *
     * @return The invite.
     */
    public Invite getInvite() {
        return invite;
    }

    /**
     * Gets the message which contains the invite.
     *
     * @return The message.
     */
    public Message getMessage() {
        return message;
    }
}
