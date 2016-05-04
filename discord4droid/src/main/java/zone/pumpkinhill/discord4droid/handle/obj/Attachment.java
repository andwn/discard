package zone.pumpkinhill.discord4droid.handle.obj;

import zone.pumpkinhill.discord4droid.api.DiscordClient;

/**
 * Represents an attachment included in the message.
 */
public class Attachment extends DiscordObject {
    protected final String filename;
    protected final int filesize;
    protected final String url;

    public Attachment(DiscordClient client, String filename, int filesize, String id, String url) {
        super(client, id);
        this.filename = filename;
        this.filesize = filesize;
        this.url = url;
    }

    /**
     * Gets the file name for the attachment.
     * @return The file name of the attachment.
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Gets the size of the attachment.
     * @return The size, in bytes of the attachment.
     */
    public int getFilesize() {
        return filesize;
    }

    /**
     * Gets the direct link to the attachment.
     * @return The download link for the attachment.
     */
    public String getUrl() {
        return url;
    }

    public Attachment copy() {
        return new Attachment(client, filename, filesize, id, url);
    }
}

