package zone.pumpkinhill.discord4droid.json.responses;

/**
 * The json response received from logging in
 */
public class LoginResponse {

    /**
     * The access token for the bot to use.
     */
    public String token;

    /**
     * The URL to server's CDN for avatars and icons.
     * Not part of Discord official API
     */
    public String content;
}
