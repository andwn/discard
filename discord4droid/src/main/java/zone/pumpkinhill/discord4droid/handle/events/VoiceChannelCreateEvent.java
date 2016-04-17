package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.VoiceChannel;

/**
 * This event is dispatched when a voice channel is created.
 */
public class VoiceChannelCreateEvent extends Event {

    private final VoiceChannel channel;

    public VoiceChannelCreateEvent(VoiceChannel channel) {
        this.channel = channel;
    }

    /**
     * Gets the channel involved.
     *
     * @return The channel.
     */
    public VoiceChannel getChannel() {
        return channel;
    }
}
