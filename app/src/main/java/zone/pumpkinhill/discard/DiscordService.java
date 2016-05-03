package zone.pumpkinhill.discard;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import zone.pumpkinhill.discard.task.NetworkTask;
import zone.pumpkinhill.discord4droid.util.DiscordException;

public class DiscordService extends Service {
    private final static String TAG = DiscordService.class.getCanonicalName();
    private volatile static boolean mScreenOn;
    private Timer mNotifyTimer;
    private Context mContext = this;

    public DiscordService() {}

    @Override
    public void onCreate() {
        super.onCreate();
        // register receiver that handles screen on and screen off logic
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        BroadcastReceiver mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);
        // Setup notification timer
        mScreenOn = true;
        mNotifyTimer = new Timer();
        mNotifyTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(mScreenOn) return;
                if(!ClientHelper.isNetworkConnected(mContext)) return;
                if(!ClientHelper.isReady()) return;
                if(!ClientHelper.client.isSuspended()) return;
                try {
                    ClientHelper.client.resume();
                    Thread.sleep(5000, 0);
                    if(!mScreenOn) ClientHelper.client.suspend();
                } catch(DiscordException | InterruptedException e) {
                    Log.d(TAG, e.toString());
                }
            }
        }, 0, 1000 * 60 * 5);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        if(ClientHelper.client == null || ClientHelper.client.getOurUser() == null) return;
        try {
            mScreenOn = intent.getBooleanExtra("screen_state", false);
            new NetworkTask(getApplicationContext()).execute(mScreenOn ? "resume" : "suspend");
        } catch(NullPointerException e) {
            Log.d(TAG, e.toString());
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return new Binder(); }

    private class ScreenReceiver extends BroadcastReceiver {
        private boolean screenOn;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                screenOn = false;
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                screenOn = true;
            }
            Intent i = new Intent(context, DiscordService.class);
            i.putExtra("screen_state", screenOn);
            context.startService(i);
        }
    }
}
