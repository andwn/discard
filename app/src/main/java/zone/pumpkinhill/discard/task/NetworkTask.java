package zone.pumpkinhill.discard.task;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discord4droid.handle.obj.Channel;
import zone.pumpkinhill.discord4droid.util.DiscordException;
import zone.pumpkinhill.discord4droid.util.HTTP429Exception;
import zone.pumpkinhill.discord4droid.util.MissingPermissionsException;
import zone.pumpkinhill.http.entity.ContentType;

public class NetworkTask extends AsyncTask<String, Void, Boolean> {
    private final static String TAG = NetworkTask.class.getCanonicalName();

    private final Context mContext;
    private String mErrorMsg;

    public NetworkTask(Context context) {
        mContext = context;
    }

    protected Boolean doInBackground(String... params) {
        try {
            switch(params[0]) {
                case "login": ClientHelper.login(params[1], params[2], params[3]); break;
                case "logout": ClientHelper.logout(); break;
                case "suspend": ClientHelper.client.suspend(); break;
                case "resume": ClientHelper.client.resume(); break;
                case "send-message": return doSendMessage(params[1], params[2]);
                case "send-file": return doSendFile(params[1], params[2]);
                case "create-invite":
                    ClientHelper.client.getChannelByID(params[1]).createInvite(0, 0, false, false);
                    break;
                case "create-guild":
                    ClientHelper.client.createGuild(params[1], params[2], params[3]);
                    break;
                case "change-profile":
                    ClientHelper.client.changeAccountInfo(params[1], params[2], params[3], params[4]);
                    break;
                default: mErrorMsg = "Unknown command: " + params[0]; return false;
            }
            return true;
        } catch(Exception e) {
            Log.d(TAG, "Exception: " + e);
            mErrorMsg = e.getLocalizedMessage();
            return false;
        }
    }

    protected void onPostExecute(Boolean result) {
        if(!result) Toast.makeText(mContext, mErrorMsg, Toast.LENGTH_LONG).show();
    }

    protected boolean doSendMessage(String channelId, String content) {
        Channel channel = ClientHelper.client.getChannelByID(channelId);
        if(channel == null) {
            mErrorMsg = "Unable to find channel with ID " + channelId;
            return false;
        }
        try {
            channel.sendMessage(content);
            return true;
        } catch(MissingPermissionsException e) {
            mErrorMsg = "Don't have permission to send messages in this channel";
        } catch(HTTP429Exception e) {
            mErrorMsg = "Unable to send message - traffic is being rate limited";
        } catch(DiscordException e) {
            mErrorMsg = "Error sending message: " + e;
        }
        return false;
    }

    protected boolean doSendFile(String channelId, String uri) {
        Channel channel = ClientHelper.client.getChannelByID(channelId);
        if(channel == null) {
            mErrorMsg = "Unable to find channel with ID " + channelId;
            return false;
        }
        try {
            // This is the real Rube Goldberg shit right here
            ContentResolver cr = mContext.getContentResolver();
            MimeTypeMap mime = MimeTypeMap.getSingleton();

            InputStream stream = cr.openInputStream(Uri.parse(uri));
            if(stream == null) {
                Log.e(TAG, "Failed to open stream for " + uri);
                return false;
            }

            String typeStr = cr.getType(Uri.parse(uri));
            ContentType type = typeStr == null ? ContentType.DEFAULT_BINARY : ContentType.parse(typeStr);

            String ext = mime.getExtensionFromMimeType(typeStr);
            String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.ENGLISH)
                    .format(new Date()) + "." + ext;

            channel.sendFile(stream, type, name, null);
            stream.close();
            return true;
        } catch(MissingPermissionsException e) {
            mErrorMsg = "Don't have permission to send files in this channel";
        } catch(HTTP429Exception e) {
            mErrorMsg = "Unable to send file - traffic is being rate limited";
        } catch(DiscordException e) {
            mErrorMsg = "Error sending file: " + e;
        } catch(IOException e) {
            mErrorMsg = "Error opening file to send: " + e;
        }
        return false;
    }
}
