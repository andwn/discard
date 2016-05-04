package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.User;
import zone.pumpkinhill.discord4droid.handle.obj.VoiceChannel;

/**
 * This event is dispatched when a user moves from one voice channel to another.
 */
public class UserVoiceChannelMoveEvent extends Event {

    private final User user;
    private final VoiceChannel oldChannel;
    private final VoiceChannel newChannel;

    public UserVoiceChannelMoveEvent(User user, VoiceChannel oldChannel, VoiceChannel newChannel) {
        this.user = user;
        this.oldChannel = oldChannel;
        this.newChannel = newChannel;
    }

    public User getUser() {
        return user;
    }

    public VoiceChannel getOldChannel() {
        return oldChannel;
    }

    public VoiceChannel getNewChannel() {
        return newChannel;
    }
}
