package zone.pumpkinhill.discord4droid.json.requests;

/**
 * This is sent to request that the connection between the client and the Discord Servers stay alive.
 */
public class KeepAliveRequest {

    /**
     * The opcode, 1.
     */
    public int op = 1;

    /**
     * The last received sequence id.
     */
    public long d;

    public KeepAliveRequest(int seq) {
        this.d = seq;
    }
}
