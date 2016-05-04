package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.User;
import zone.pumpkinhill.discord4droid.handle.obj.VoiceChannel;

/**
 * This event is dispatched when a user disconnects from a voice channel.
 */
public class UserVoiceChannelLeaveEvent extends Event {

    private final User user;
    private final VoiceChannel oldChannel;

    public UserVoiceChannelLeaveEvent(User user, VoiceChannel oldChannel) {
        this.user = user;
        this.oldChannel = oldChannel;
    }

    public User getUser() {
        return user;
    }

    public VoiceChannel getChannel() {
        return oldChannel;
    }
}
