/**
 * Copyright 2015-2016 Austin Keener & Michael Ritter
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package zone.pumpkinhill.discord4droid.api;

import org.jitsi.impl.neomedia.codec.audio.opus.Opus;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import zone.pumpkinhill.discord4droid.util.TweetNaCl;

/**
 * Represents the contents of a audio packet that was either received from Discord or
 * will be sent to discord.
 */
public class AudioPacket {
    private static long stereoOpusEncoder;
    private static long monoOpusEncoder;
    private static long stereoOpusDecoder;
    private static long monoOpusDecoder;

    static {
        try {
            stereoOpusEncoder = Opus.encoder_create(DiscordVoiceWS.OPUS_SAMPLE_RATE,
                    DiscordVoiceWS.OPUS_STEREO_CHANNEL_COUNT);
            monoOpusEncoder = Opus.encoder_create(DiscordVoiceWS.OPUS_SAMPLE_RATE,
                    DiscordVoiceWS.OPUS_MONO_CHANNEL_COUNT);
            stereoOpusDecoder = Opus.decoder_create(DiscordVoiceWS.OPUS_SAMPLE_RATE,
                    DiscordVoiceWS.OPUS_STEREO_CHANNEL_COUNT);
            monoOpusDecoder = Opus.decoder_create(DiscordVoiceWS.OPUS_SAMPLE_RATE,
                    DiscordVoiceWS.OPUS_MONO_CHANNEL_COUNT);
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
            stereoOpusEncoder = 0;
            stereoOpusDecoder = 0;
            monoOpusEncoder = 0;
            monoOpusDecoder = 0;
        }
    }

    private final char seq;
    private final int timestamp;
    private final int ssrc;
    private final byte[] encodedAudio;
    private final byte[] rawPacket;
    private final byte[] rawAudio;

    public AudioPacket(DatagramPacket packet) {
        this(Arrays.copyOf(packet.getData(), packet.getLength()));
    }

    public AudioPacket(byte[] rawPacket) { //FIXME: Support mono & decryption
        this.rawPacket = rawPacket;

        ByteBuffer buffer = ByteBuffer.wrap(rawPacket);
        this.seq = buffer.getChar(2);
        this.timestamp = buffer.getInt(4);
        this.ssrc = buffer.getInt(8);

        byte[] audio = new byte[buffer.array().length-12];
        System.arraycopy(buffer.array(), 12, audio, 0, audio.length);
        this.encodedAudio = audio;
        this.rawAudio = decodeToPCM(encodedAudio);
    }

    public AudioPacket(char seq, int timestamp, int ssrc, byte[] rawAudio, int channels, byte[] secret) {
        this.seq = seq;
        this.ssrc = ssrc;
        this.timestamp = timestamp;
        this.rawAudio = rawAudio;

        ByteBuffer nonceBuffer = ByteBuffer.allocate(12);
        nonceBuffer.put(0, (byte) 0x80);
        nonceBuffer.put(1, (byte) 0x78);
        nonceBuffer.putChar(2, seq);
        nonceBuffer.putInt(4, timestamp);
        nonceBuffer.putInt(8, ssrc);
        this.encodedAudio = TweetNaCl.secretbox(encodeToOpus(rawAudio, channels),
                Arrays.copyOf(nonceBuffer.array(), 24), //encryption nonce is 24 bytes long while discord's is 12 bytes long
                secret);

        byte[] packet = new byte[nonceBuffer.capacity()+encodedAudio.length];
        System.arraycopy(nonceBuffer.array(), 0, packet, 0, 12); //Add nonce
        System.arraycopy(encodedAudio, 0, packet, 12, encodedAudio.length); //Add audio

        this.rawPacket = packet;
    }

    public byte[] getRawPacket() {
        return Arrays.copyOf(rawPacket, rawPacket.length);
    }

    public DatagramPacket asUdpPacket(InetSocketAddress address) throws SocketException {
        return new DatagramPacket(getRawPacket(), rawPacket.length, address);
    }

    public static byte[] encodeToOpus(byte[] rawAudio, int channels) {
        int result;
        byte[] output = new byte[4096];
        if (channels == 1) {
            result = Opus.encode(monoOpusEncoder, rawAudio, 0, DiscordVoiceWS.OPUS_FRAME_SIZE, output, 0, output.length);
        } else {
            result = Opus.encode(stereoOpusEncoder, rawAudio, 0, DiscordVoiceWS.OPUS_FRAME_SIZE, output, 0, output.length);
        }
        byte[] audio = new byte[result];
        System.arraycopy(output, 0, audio, 0, result);
        return audio;
    }

    public byte[] decodeToPCM(byte[] opusAudio) {
        byte[] output = new byte[8192];
        int result = Opus.decode(stereoOpusDecoder, opusAudio, 0, opusAudio.length, output, 0, output.length, 0);
        byte[] audio = new byte[result];
        System.arraycopy(output, 0, audio, 0, result);
        return audio;
    }

    public byte[] getRawAudio() {
        return rawAudio;
    }
}