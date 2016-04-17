package zone.pumpkinhill.discord4droid.handle.obj;

/**
 * Represents an attachment included in the message.
 */
public class Attachment {

    /**
     * The file name of the attachment.
     */
    protected final String filename;

    /**
     * The size, in bytes of the attachment.
     */
    protected final int filesize;

    /**
     * The attachment id.
     */
    protected final String id;

    /**
     * The download link for the attachment.
     */
    protected final String url;

    public Attachment(String filename, int filesize, String id, String url) {
        this.filename = filename;
        this.filesize = filesize;
        this.id = id;
        this.url = url;
    }

    /**
     * Gets the file name for the attachment.
     *
     * @return The file name of the attachment.
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Gets the size of the attachment.
     *
     * @return The size, in bytes of the attachment.
     */
    public int getFilesize() {
        return filesize;
    }

    /**
     * Gets the id of the attachment.
     *
     * @return The attachment id.
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the direct link to the attachment.
     *
     * @return The download link for the attachment.
     */
    public String getUrl() {
        return url;
    }
}

