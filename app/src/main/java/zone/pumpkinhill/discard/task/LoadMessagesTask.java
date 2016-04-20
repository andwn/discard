package zone.pumpkinhill.discard.task;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.EditText;
import android.widget.ListView;

import java.lang.ref.WeakReference;

import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discard.adapter.ChatMessageAdapter;
import zone.pumpkinhill.discord4droid.handle.obj.Channel;
import zone.pumpkinhill.discord4droid.util.DiscordException;
import zone.pumpkinhill.discord4droid.util.HTTP429Exception;
import zone.pumpkinhill.discord4droid.util.MessageList;
import zone.pumpkinhill.discord4droid.util.MissingPermissionsException;

public class LoadMessagesTask extends AsyncTask<String, Void, Boolean> {
    private final static String TAG = LoadMessagesTask.class.getCanonicalName();

    private final WeakReference<ListView> mChatView;
    private final MessageList mMessageList;

    public LoadMessagesTask(MessageList messageList, ListView chatView) {
        mChatView = new WeakReference<>(chatView);
        mMessageList = messageList;
    }

    protected Boolean doInBackground(String... params) {
        try {
            mMessageList.load(MessageList.MESSAGE_CHUNK_COUNT);
            return true;
        } catch(HTTP429Exception e) {
            Log.e(TAG, "Error fetching latest messages: " + e);
            return false;
        }
    }

    protected void onPostExecute(Boolean result) {
        ListView cb = mChatView.get();
        if(result) {
            // On success refresh chat
            if(cb != null) {
                Log.v(TAG, "Refreshing list adapter.");
                ((ChatMessageAdapter) cb.getAdapter()).notifyDataSetChanged();
            }
        } else {
            Log.e(TAG, "Failed to download message list.");
        }
    }
}
