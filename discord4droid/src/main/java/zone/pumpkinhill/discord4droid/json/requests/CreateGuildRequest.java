package zone.pumpkinhill.discord4droid.json.requests;

import android.graphics.Bitmap;

import zone.pumpkinhill.discord4droid.util.ImageHelper;

/**
 * This is sent to create a new guild.
 */
public class CreateGuildRequest {

    /**
     * The name of the guild.
     */
    public String name;

    /**
     * The region for this guild. (OPTIONAL)
     */
    public String region;

    /**
     * The encoded icon for this guild. (OPTIONAL)
     */
    public String icon;

    public CreateGuildRequest(String name, String region, String icon) {
        this.name = name;
        this.region = region;
        this.icon = icon == null ? null : String.format("data:image/%s;base64,%s",
                "jpeg", icon);
    }
}
