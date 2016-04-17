package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.User;

/**
 * This is dispatched when a user starts or stops speaking
 */
public class VoiceUserSpeakingEvent extends Event {
    /**
     * The user involved
     */
    private final User user;

    private final int ssrc;
    private final boolean speaking;

    public VoiceUserSpeakingEvent(User user, int ssrc, boolean speaking) {
        this.user = user;
        this.ssrc = ssrc;
        this.speaking = speaking;
    }

    /**
     * Gets the user who started/ended speaking.
     *
     * @return The user.
     */
    public User getUser() {
        return user;
    }

    /**
     * Gets the ssrc-a unique number per user.
     *
     * @return The ssrc.
     */
    public int getSsrc() {
        return ssrc;
    }

    /**
     * Whether the user is now speaking or not.
     *
     * @return True if the user is speaking, false if otherwise.
     */
    public boolean isSpeaking() {
        return speaking;
    }
}
