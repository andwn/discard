package zone.pumpkinhill.discord4droid.handle;

import java.io.IOException;
import java.io.InputStream;

/**
 * This is used to interface with voice channels.
 */
public class AudioChannel {
    private InputStream audioStream;

    public AudioChannel() { }

    public void setAudioStream(InputStream stream) {
        audioStream = stream;
    }

    /**
     * Gets the PCM data that needs to be sent.
     *
     * @param length : How many MS of data needed to be sent.
     * @return : The PCM data
     */
    public byte[] getAudioData(int length) {
        if (audioStream == null) return null;
        try {
            byte[] audio = new byte[length*48000];
            int amountRead = audioStream.read(audio, 0, audio.length);
            if (amountRead > -1) {
                return audio;
            } else {
                return getAudioData(length);
            }
        } catch (IOException e) {
            return null;
        }
    }
}
