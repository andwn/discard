package zone.pumpkinhill.discard.task;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.EditText;
import android.widget.ListView;

import java.lang.ref.WeakReference;

import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discard.adapter.ChatMessageAdapter;
import zone.pumpkinhill.discord4droid.api.DiscordClient;
import zone.pumpkinhill.discord4droid.handle.obj.Channel;
import zone.pumpkinhill.discord4droid.handle.obj.Presences;
import zone.pumpkinhill.discord4droid.handle.obj.User;
import zone.pumpkinhill.discord4droid.util.DiscordException;
import zone.pumpkinhill.discord4droid.util.HTTP429Exception;
import zone.pumpkinhill.discord4droid.util.MissingPermissionsException;

public class UpdatePresenceTask extends AsyncTask<String, Void, Boolean> {
    private final static String TAG = UpdatePresenceTask.class.getCanonicalName();

    private final Boolean mPresence;
    private final String mGame;

    public UpdatePresenceTask(Boolean presence, String game) {
        mPresence = presence;
        mGame = game;
    }

    protected Boolean doInBackground(String... params) {
        DiscordClient client = ClientHelper.client;
        User us = client.getOurUser();
        if(us == null) return false;
        client.updatePresence(mPresence == null ? us.getPresence() == Presences.IDLE : mPresence,
                mGame == null ? us.getGame() : mGame);
        return true;
    }

    protected void onPostExecute(Boolean result) {
        if(result) {
            if(mPresence != null) Log.i(TAG, "Presence changed to " + (mPresence ? "IDLE" : "ONLINE"));
            if(mGame != null) Log.i(TAG, "Game changed to " + mGame);
        } else {
            Log.w(TAG, "Failed to update presence.");
        }
    }
}
