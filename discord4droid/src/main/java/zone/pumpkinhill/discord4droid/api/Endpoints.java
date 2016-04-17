package zone.pumpkinhill.discord4droid.api;

/**
 * Static class that contains
 * URLs useful to us.
 */
public interface Endpoints {
    // REST URIs
    String API = "api";
    String USERS = API + "/users/";
    String LOGIN = API + "/auth/login";
    String LOGOUT = API + "/auth/logout";
    String GATEWAY = API + "/gateway";
    String GUILDS = API + "/guilds/";
    String CHANNELS = API + "/channels/";
    String INVITE = API + "/invite/";
    String VOICE = API + "/voice/";

    // Content URIs
    String AVATARS = "avatars/%s/%s.jpg";
    String ICONS = "icons/%s/%s.jpg";

    // API metrics (response time)
    String METRICS = "https://srhpyqt94yxb.statuspage.io/metrics-display/d5cggll8phl5/%s.json";

    // Scheduled maintenance information
    String STATUS = "https://status.discordapp.com/api/v2/scheduled-maintenances/%s.json";
}
