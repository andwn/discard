package zone.pumpkinhill.discord4droid.handle.obj;

public class UserVoiceState {
    protected boolean selfMute, selfDeaf, modMute, modDeaf;
    public UserVoiceState(boolean selfMute, boolean selfDeaf, boolean modMute, boolean modDeaf) {
        this.selfMute = selfMute;
        this.selfDeaf = selfDeaf;
        this.modMute = modMute;
        this.modDeaf = modDeaf;
    }
    public boolean isMute() {
        return selfMute | modMute;
    }
    public boolean isDeaf() {
        return selfDeaf | modDeaf;
    }
    public boolean isSelfMuted() {
        return selfMute;
    }
    public boolean isSelfDeafened() {
        return selfDeaf;
    }
    public boolean isMutedByMod() {
        return modMute;
    }
    public boolean isDeafenedByMod() {
        return modDeaf;
    }
}

