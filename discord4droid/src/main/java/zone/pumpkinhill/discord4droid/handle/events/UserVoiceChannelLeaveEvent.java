package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.VoiceChannel;

/**
 * This event is dispatched when a user disconnects from a voice channel.
 */
public class UserVoiceChannelLeaveEvent extends Event {

    /**
     * The channel the user left.
     */
    private final VoiceChannel oldChannel;

    public UserVoiceChannelLeaveEvent(VoiceChannel oldChannel) {
        this.oldChannel = oldChannel;
    }

    /**
     * Gets the voice channel this user left.
     *
     * @return The voice channel.
     */
    public VoiceChannel getChannel() {
        return oldChannel;
    }
}
