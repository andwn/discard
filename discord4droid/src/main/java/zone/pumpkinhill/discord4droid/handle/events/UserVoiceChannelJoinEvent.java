package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.VoiceChannel;

/**
 * This event is dispatched when a user connects to a voice channel.
 */
public class UserVoiceChannelJoinEvent extends Event {

    /**
     * The channel the user joined.
     */
    private final VoiceChannel newChannel;

    public UserVoiceChannelJoinEvent(VoiceChannel newChannel) {
        this.newChannel = newChannel;
    }

    /**
     * Gets the voice channel this user joined.
     *
     * @return The voice channel.
     */
    public VoiceChannel getChannel() {
        return newChannel;
    }
}
