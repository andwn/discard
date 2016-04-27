package zone.pumpkinhill.discard.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {
    protected boolean isAppInBackground;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isAppInBackground = false;
    }
    @Override
    protected void onPause() {
        super.onPause();
        isAppInBackground = false;
    }
    @Override
    protected void onStop() {
        super.onStop();
        isAppInBackground = true;
    }
}
