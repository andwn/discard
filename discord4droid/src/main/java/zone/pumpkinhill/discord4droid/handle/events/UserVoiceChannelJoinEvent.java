package zone.pumpkinhill.discord4droid.handle.events;

import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.handle.obj.User;
import zone.pumpkinhill.discord4droid.handle.obj.VoiceChannel;

/**
 * This event is dispatched when a user connects to a voice channel.
 */
public class UserVoiceChannelJoinEvent extends Event {

    private final User user;
    private final VoiceChannel newChannel;

    public UserVoiceChannelJoinEvent(User user, VoiceChannel newChannel) {
        this.user = user;
        this.newChannel = newChannel;
    }

    public User getUser() {
        return user;
    }

    public VoiceChannel getChannel() {
        return newChannel;
    }
}
