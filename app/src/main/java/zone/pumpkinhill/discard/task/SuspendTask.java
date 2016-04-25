package zone.pumpkinhill.discard.task;

import android.os.AsyncTask;
import android.util.Log;

import zone.pumpkinhill.discard.ClientHelper;

public class SuspendTask extends AsyncTask<String, Void, Boolean> {
    public SuspendTask() {}
    protected Boolean doInBackground(String... params) {
        try {
            switch(params[0]) {
                case "suspend": ClientHelper.client.suspend(); break;
                case "resume": ClientHelper.client.resume(); break;
                case "logout": ClientHelper.logout(); break;
                default: return false;
            }
            return true;
        } catch(Exception e) {
            Log.d("SuspendTask", e.toString());
            return false;
        }
    }
    protected void onPostExecute(Boolean result) {}
}
