package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.VoiceChannel;

/**
 * This event is dispatched when a voice channel is updated.
 */
public class VoiceChannelUpdateEvent extends Event {

    private final VoiceChannel oldVoiceChannel, newVoiceChannel;

    public VoiceChannelUpdateEvent(VoiceChannel oldVoiceChannel, VoiceChannel newVoiceChannel) {
        this.oldVoiceChannel = oldVoiceChannel;
        this.newVoiceChannel = newVoiceChannel;
    }

    /**
     * Gets the original voice channel.
     *
     * @return The un-updated instance of the voice channel.
     */
    public VoiceChannel getOldVoiceChannel() {
        return oldVoiceChannel;
    }

    /**
     * Gets the new voice channel.
     *
     * @return The updated instance of the voice channel.
     */
    public VoiceChannel getNewVoiceChannel() {
        return newVoiceChannel;
    }
}
