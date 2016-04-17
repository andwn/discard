package zone.pumpkinhill.discord4droid.api;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.InflaterInputStream;

import zone.pumpkinhill.discord4droid.handle.events.VoiceDisconnectedEvent;
import zone.pumpkinhill.discord4droid.handle.events.VoicePingEvent;
import zone.pumpkinhill.discord4droid.handle.events.VoiceUserSpeakingEvent;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.User;
import zone.pumpkinhill.discord4droid.json.requests.VoiceConnectRequest;
import zone.pumpkinhill.discord4droid.json.requests.VoiceKeepAliveRequest;
import zone.pumpkinhill.discord4droid.json.requests.VoiceSpeakingRequest;
import zone.pumpkinhill.discord4droid.json.requests.VoiceUDPConnectRequest;
import zone.pumpkinhill.discord4droid.json.responses.VoiceUpdateResponse;

public class DiscordVoiceWS extends WebSocketAdapter {
    private final static String TAG = DiscordVoiceWS.class.getCanonicalName();

    public static final int OPUS_SAMPLE_RATE = 48000;   //(Hz) We want to use the highest of qualities! All the bandwidth!
    public static final int OPUS_FRAME_SIZE = 960;
    public static final int OPUS_FRAME_TIME_AMOUNT = OPUS_FRAME_SIZE*1000/OPUS_SAMPLE_RATE;
    public static final int OPUS_MONO_CHANNEL_COUNT = 1;
    public static final int OPUS_STEREO_CHANNEL_COUNT = 2;

    public static final int OP_INITIAL_CONNECTION = 2;
    public static final int OP_HEARTBEAT_RETURN = 3;
    public static final int OP_CONNECTING_COMPLETED = 4;
    public static final int OP_USER_SPEAKING_UPDATE = 5;

    public AtomicBoolean isConnected = new AtomicBoolean(true);
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(3);

    private DiscordClient client;

    private Guild guild;

    private int ssrc;
    private VoiceUpdateResponse event;
    private DatagramSocket udpSocket;

    private InetSocketAddress addressPort;
    private boolean isSpeaking;

    private byte[] secret;

    private WebSocket socket;

    public DiscordVoiceWS(VoiceUpdateResponse response, DiscordClient client) throws Exception {
        this.client = client;
        this.event = response;
        this.guild = client.getGuildByID(event.guild_id);
        WebSocketFactory factory = new WebSocketFactory();
        WebSocket socket = factory.createSocket("wss://" + response.endpoint);
        socket.addListener(this);
        socket.addHeader("Accept-Encoding", "gzip, deflate");
        socket.connect();
    }

    @Override
    public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
        this.socket = websocket;
        send(DiscordUtils.GSON.toJson(new VoiceConnectRequest(event.guild_id, client.ourUser.getID(), client.sessionId, event.token)));
        Log.i(TAG, "Connected to the Discord Voice websocket.");
    }

    @Override
    public void onTextMessage(WebSocket websocket, String text) throws Exception {
        JsonParser parser = new JsonParser();
        JsonObject object = parser.parse(text).getAsJsonObject();

        int op = object.get("op").getAsInt();

        switch (op) {
            case OP_INITIAL_CONNECTION: {
                try {
                    JsonObject eventObject = (JsonObject) object.get("d");
                    ssrc = eventObject.get("ssrc").getAsInt();

                    udpSocket = new DatagramSocket();
                    addressPort = new InetSocketAddress(event.endpoint, eventObject.get("port").getAsInt());

                    ByteBuffer buffer = ByteBuffer.allocate(70);
                    buffer.putInt(ssrc);

                    DatagramPacket discoveryPacket = new DatagramPacket(buffer.array(), buffer.array().length, addressPort);
                    udpSocket.send(discoveryPacket);

                    DatagramPacket receivedPacket = new DatagramPacket(new byte[70], 70);
                    udpSocket.receive(receivedPacket);

                    byte[] data = receivedPacket.getData();

                    int ourPort = ((0x000000FF & ((int) data[receivedPacket.getLength()-1])) << 8) | ((0x000000FF & ((int) data[receivedPacket.getLength()-2])));

                    String ourIP = new String(data);
                    ourIP = ourIP.substring(4, ourIP.length()-2);
                    ourIP = ourIP.trim();

                    send(DiscordUtils.GSON.toJson(new VoiceUDPConnectRequest(ourIP, ourPort)));

                    startKeepalive(eventObject.get("heartbeat_interval").getAsInt());
                } catch (IOException e) {
                    Log.e(TAG, "Error reading text message: " + e);
                }
                break;
            }
            case OP_HEARTBEAT_RETURN: {
                long timePingSent = object.get("d").getAsLong();
                client.dispatcher.dispatch(new VoicePingEvent((System.currentTimeMillis()-timePingSent)));
                break;
            }
            case OP_CONNECTING_COMPLETED: {
                isConnected.set(true);

                JsonArray array = object.get("d").getAsJsonObject().get("secret_key").getAsJsonArray();
                secret = new byte[array.size()];
                for (int i = 0; i < array.size(); i++)
                    secret[i] = (byte) array.get(i).getAsInt();

                setupSendThread();
                setupReceiveThread();
                break;
            }
            case OP_USER_SPEAKING_UPDATE: {
                JsonObject eventObject = (JsonObject) object.get("d");
                boolean isSpeaking = eventObject.get("speaking").getAsBoolean();
                int ssrc = eventObject.get("ssrc").getAsInt();
                String userId = eventObject.get("user_id").getAsString();

                User user = client.getUserByID(userId);
                if (user == null) {
                    Log.w(TAG, "Got an Audio USER_SPEAKING_UPDATE for a non-existent User. JSON: " + object.toString());
                    return;
                }

                client.dispatcher.dispatch(new VoiceUserSpeakingEvent(user, ssrc, isSpeaking));
                break;
            }
            default: {
                Log.w(TAG, "Unknown voice packet: " + object);
            }
        }
    }

    private void setupSendThread() {
        Runnable sendThread = new Runnable() {
            //char seq = 0;
            //int timestamp = 0;      //Used to sync up our packets within the same timeframe of other people talking.

            @Override
            public void run() {
                try {
                    if (isConnected.get()) {
                        /*
                        AudioChannel.AudioData data = guild.getAudioChannel().getAudioData(OPUS_FRAME_SIZE);
                        if (data != null) {
                            client.timer = System.currentTimeMillis();
                            // TODO: Fix audio
                            AudioPacket packet = new AudioPacket(seq, timestamp, ssrc, data.rawData, data.metaData.channels, secret);
                            if (!isSpeaking)
                                setSpeaking(true);
                            udpSocket.send(packet.asUdpPacket(addressPort));

                            if (seq+1 > Character.MAX_VALUE)
                                seq = 0;
                            else
                                seq++;

                            timestamp += OPUS_FRAME_SIZE;
                        } else if (isSpeaking)
                        */
                            setSpeaking(false);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error creating thread to send audio: " + e);
                }
            }
        };
        executorService.scheduleAtFixedRate(sendThread, 0, OPUS_FRAME_TIME_AMOUNT, TimeUnit.MILLISECONDS);
    }

    private void setupReceiveThread() {
//		Runnable receiveThread = ()->{
//			if (isConnected.get()) {
//				DatagramPacket receivedPacket = new DatagramPacket(new byte[1920], 1920);
//				try {
//					udpSocket.receive(receivedPacket);
//
//					AudioPacket packet = new AudioPacket(receivedPacket);
//					client.getDispatcher().dispatch(new AudioReceiveEvent(packet));
//				} catch (SocketException e) {
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		};
//		executorService.scheduleAtFixedRate(receiveThread, 0, OPUS_FRAME_TIME_AMOUNT, TimeUnit.MILLISECONDS);
    }

    private void startKeepalive(int hearbeat_interval) {
        final Runnable keepAlive = new Runnable() {
            public void run () {
                if(isConnected.get()) {
                    long l = System.currentTimeMillis() - client.timer;
                    Log.d(TAG, "Sending keep alive... (" + System.currentTimeMillis() + "). Took " + l + " ms.");
                    send(DiscordUtils.GSON.toJson(new VoiceKeepAliveRequest(System.currentTimeMillis())));
                    client.timer = System.currentTimeMillis();
                }
            }
        };
        executorService.scheduleAtFixedRate(keepAlive,
                client.timer + hearbeat_interval - System.currentTimeMillis(),
                hearbeat_interval, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception {
        //Converts binary data to readable string data
        try {
            InflaterInputStream inputStream = new InflaterInputStream(new ByteArrayInputStream(binary));
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder sb = new StringBuilder();
            String read;
            while ((read = reader.readLine()) != null) {
                sb.append(read);
            }

            String data = sb.toString();
            reader.close();
            inputStream.close();

            onTextMessage(websocket, data);
        } catch (IOException e) {
            Log.e(TAG, "Error decoding binary message: " + e);
        }
    }

    @Override
    public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
        super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
        disconnect(VoiceDisconnectedEvent.Reason.UNKNOWN);
    }

    @Override
    public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
        super.onError(websocket, cause);
        disconnect(VoiceDisconnectedEvent.Reason.UNKNOWN);
    }

    public void send(String message) {
        if (socket == null) {
            Log.w(TAG, "Socket attempting to send a message (" + message + ") without a valid session.");
            return;
        }
        if (isConnected.get()) {
            socket.sendText(message);
        }
    }

    public void send(Object object) {
        send(DiscordUtils.GSON.toJson(object));
    }

    /**
     * Disconnects the client WS.
     */
    public void disconnect(VoiceDisconnectedEvent.Reason reason) {
        if (isConnected.get()) {
            client.dispatcher.dispatch(new VoiceDisconnectedEvent(reason));
            isConnected.set(false);
            udpSocket.close();
            socket.disconnect();
            client.voiceConnections.remove(guild);
            executorService.shutdownNow();
//			Thread.currentThread().interrupt();
        }
    }

    /**
     * Updates the speaking status
     *
     * @param speaking: is voice currently being sent
     */
    public void setSpeaking(boolean speaking) {
        this.isSpeaking = speaking;

        send(DiscordUtils.GSON.toJson(new VoiceSpeakingRequest(speaking)));
    }
}
