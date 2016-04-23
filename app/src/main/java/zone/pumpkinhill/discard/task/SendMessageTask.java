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
import zone.pumpkinhill.discord4droid.util.MissingPermissionsException;

public class SendMessageTask extends AsyncTask<String, Void, Boolean> {
    private final static String TAG = SendMessageTask.class.getCanonicalName();

    private final WeakReference<EditText> mTextBox;
    private final WeakReference<ListView> mChatBox;
    private String mMessage;

    public SendMessageTask(EditText textBox, ListView chatView) {
        mTextBox = new WeakReference<>(textBox);
        mChatBox = new WeakReference<>(chatView);
        mMessage = textBox.getText().toString();
        textBox.setText("");
        textBox.setEnabled(false);
    }

    protected Boolean doInBackground(String... params) {
        try {
            Channel channel = ClientHelper.client.getChannelByID(params[0]);
            if(channel == null) {
                Log.e(TAG, "Attempt to send message to unloaded channel.");
                return false;
            }
            channel.sendMessage(mMessage);
            return true;
        } catch (DiscordException | MissingPermissionsException | HTTP429Exception e) {
            Log.e(TAG, "Failed to send message: " + e);
            return false;
        }
    }

    protected void onPostExecute(Boolean result) {
        ListView cb = mChatBox.get();
        EditText tb = mTextBox.get();
        if(result) {
            // On success refresh chat (D4J doesn't dispatch an event for our own message)
            if(cb != null) {
                Log.v(TAG, "Refreshing list adapter.");
                ((ChatMessageAdapter) cb.getAdapter()).notifyDataSetChanged();
            }
        } else {
            if(tb != null) {
                // On failure put the text back to retry (if still empty)
                if(tb.getText().toString().isEmpty()) {
                    mTextBox.get().setText(mMessage);
                }
                // Display an error so the user actually knows this happened
                tb.setError("Failed to send message!");
            }
        }
        if(tb != null) tb.setEnabled(true);
    }
}
