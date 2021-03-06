package zone.pumpkinhill.discard.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.securepreferences.SecurePreferences;

import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discard.DiscordService;
import zone.pumpkinhill.discard.R;
import zone.pumpkinhill.discord4droid.util.DiscordException;

public class LoginActivity extends BaseActivity {
    private final static String TAG = LoginActivity.class.getCanonicalName();

    private UserLoginTask mAuthTask = null;
    private EditText mServerView;
    private EditText mEmailView;
    private EditText mPasswordView;
    private CheckBox mRememberView;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!isTaskRoot()) {
            // Activity was brought to front and not created,
            // Thus finishing this will get us to the last viewed activity
            finish();
            return;
        }
        NotifiedChannels.clear();
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mServerView = (EditText) findViewById(R.id.server);
        mEmailView = (EditText) findViewById(R.id.email);
        mPasswordView = (EditText) findViewById(R.id.password);
        mRememberView = (CheckBox) findViewById(R.id.remember);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        populateAutoComplete();

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        assert mEmailSignInButton != null;
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        Intent i = new Intent(getApplicationContext(), DiscordService.class);
        getApplicationContext().startService(i);
    }

    private void saveCredentials() {
        SharedPreferences prefs = new SecurePreferences(this);
        String server = mServerView.getText().toString();
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        prefs.edit().putString("server", server)
                .putString("email", email)
                .putString("password", password)
                .putBoolean("remember", true)
                .apply();
    }

    private void populateAutoComplete() {
        SharedPreferences prefs = new SecurePreferences(this);
        if(!prefs.getBoolean("remember", false)) return;
        String server = prefs.getString("server", mServerView.getText().toString());
        String email = prefs.getString("email", "");
        String password = prefs.getString("password", "");
        mServerView.setText(server);
        mEmailView.setText(email);
        mPasswordView.setText(password);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }
        // Reset errors.
        mServerView.setError(null);
        mEmailView.setError(null);
        mPasswordView.setError(null);
        // Store values at the time of the login attempt.
        String server = mServerView.getText().toString();
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;
        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }
        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }
        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(mProgressView, mLoginFormView, true);
            mAuthTask = new UserLoginTask(server, email, password);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isEmailValid(String email) {
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
        private final String mServer;
        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String server, String email, String password) {
            mServer = server;
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                ClientHelper.login(mEmail, mPassword, mServer);
                return true;
            } catch(DiscordException e) {
                Log.e(TAG, e.getErrorMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(mProgressView, mLoginFormView, false);

            if (success) {
                if(mRememberView.isChecked()) saveCredentials();
                Intent i = new Intent(mContext, GuildListActivity.class);
                startActivity(i);
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(mProgressView, mLoginFormView, false);
        }
    }
}

