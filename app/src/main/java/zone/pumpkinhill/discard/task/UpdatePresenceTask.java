package zone.pumpkinhill.discard.task;

import android.os.AsyncTask;
import android.util.Log;

import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discord4droid.api.DiscordClient;
import zone.pumpkinhill.discord4droid.handle.obj.Presences;
import zone.pumpkinhill.discord4droid.handle.obj.User;
import zone.pumpkinhill.discord4droid.util.DiscordException;

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
        if(mPresence != null && !mPresence) {
            try {
                client.resume();
            } catch(DiscordException e) {
                Log.e("DiscordService", "Error resuming websocket: " + e);
            }
        }
        client.updatePresence(mPresence == null ? us.getPresence() == Presences.IDLE : mPresence,
                mGame == null ? us.getGame() : mGame);
        if(mPresence != null && mPresence) client.suspend();
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
