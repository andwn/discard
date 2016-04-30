package zone.pumpkinhill.discard.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discard.ImageCache;
import zone.pumpkinhill.discard.R;

public abstract class BaseActivity extends AppCompatActivity {
    protected final static int FILE_SELECT_CODE = 0;

    protected final Context mContext = this;

    protected boolean isAppInBackground;
    protected boolean showMenuProfile = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isAppInBackground = false;
        if(ClientHelper.cache == null) {
            ClientHelper.cache = new ImageCache(mContext);
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        menu.findItem(R.id.menu_preferences).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(mContext, SettingsActivity.class);
                startActivity(i);
                return true;
            }
        });
        if(showMenuProfile) {
            MenuItem profile = menu.findItem(R.id.menu_profile);
            profile.setVisible(true);
            profile.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    Intent i = new Intent(mContext, ProfileActivity.class);
                    i.putExtra("userId", ClientHelper.client.getOurUser().getID());
                    startActivity(i);
                    return true;
                }
            });
        }
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    protected void showProgress(final View progress, final View form, final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            form.setVisibility(show ? View.GONE : View.VISIBLE);
            form.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    form.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            progress.setVisibility(show ? View.VISIBLE : View.GONE);
            progress.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progress.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            progress.setVisibility(show ? View.VISIBLE : View.GONE);
            form.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}
