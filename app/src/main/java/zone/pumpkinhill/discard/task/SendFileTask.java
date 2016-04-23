package zone.pumpkinhill.discard.task;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ListView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discard.adapter.ChatMessageAdapter;
import zone.pumpkinhill.discord4droid.handle.obj.Channel;
import zone.pumpkinhill.discord4droid.util.DiscordException;
import zone.pumpkinhill.discord4droid.util.HTTP429Exception;
import zone.pumpkinhill.discord4droid.util.MissingPermissionsException;
import zone.pumpkinhill.http.entity.ContentType;

public class SendFileTask extends AsyncTask<String, Void, Boolean> {
    private final static String TAG = SendFileTask.class.getCanonicalName();

    private final Context mContext;
    private final Uri mUri;
    private final WeakReference<ListView> mChatBox;


    public SendFileTask(Context context, Uri uri, ListView chatView) {
        mContext = context;
        mUri = uri;
        mChatBox = new WeakReference<>(chatView);
    }

    protected Boolean doInBackground(String... params) {
        try {
            Channel channel = ClientHelper.client.getChannelByID(params[0]);
            if(channel == null) {
                Log.e(TAG, "Attempt to upload to unloaded channel.");
                return false;
            }
            // This is the real Rube Goldberg shit right here
            ContentResolver cr = mContext.getContentResolver();
            MimeTypeMap mime = MimeTypeMap.getSingleton();

            InputStream stream = cr.openInputStream(mUri);
            if(stream == null) {
                Log.e(TAG, "Failed to open stream for " + mUri.toString());
                return false;
            }

            String typeStr = cr.getType(mUri);
            ContentType type = typeStr == null ? ContentType.DEFAULT_BINARY : ContentType.parse(typeStr);

            String ext = mime.getExtensionFromMimeType(typeStr);
            String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.ENGLISH)
                    .format(new Date()) + "." + ext;

            channel.sendFile(stream, type, name, null);
            stream.close();
            return true;
        } catch (DiscordException | MissingPermissionsException | HTTP429Exception | IOException e) {
            Log.e(TAG, "Failed to send message: " + e);
            return false;
        }
    }

    protected void onPostExecute(Boolean result) {
        ListView cb = mChatBox.get();
        if(result) {
            // On success refresh chat (D4J doesn't dispatch an event for our own message)
            if(cb != null) {
                Log.v(TAG, "Refreshing list adapter.");
                ((ChatMessageAdapter) cb.getAdapter()).notifyDataSetChanged();
            }
        }
    }
}
