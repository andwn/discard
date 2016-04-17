package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.VoiceChannel;

/**
 * This event is dispatched when a user moves from one voice channel to another.
 */
public class UserVoiceChannelMoveEvent extends Event {

    /**
     * The channel the user left.
     */
    private final VoiceChannel oldChannel;
    /**
     * The channel the user joined.
     */
    private final VoiceChannel newChannel;

    public UserVoiceChannelMoveEvent(VoiceChannel oldChannel, VoiceChannel newChannel) {
        this.oldChannel = oldChannel;
        this.newChannel = newChannel;
    }

    /**
     * Gets the voice channel this user left.
     *
     * @return The voice channel.
     */
    public VoiceChannel getOldChannel() {
        return oldChannel;
    }

    /**
     * Gets the voice channel this user joined.
     *
     * @return The voice channel.
     */
    public VoiceChannel getNewChannel() {
        return newChannel;
    }
}
