package zone.pumpkinhill.discard.task;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import zone.pumpkinhill.discard.ClientHelper;

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
                case "message-send":
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
        if(result) return;
        Toast.makeText(mContext, mErrorMsg, Toast.LENGTH_LONG).show();
    }
}
