package zone.pumpkinhill.discard.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discard.R;
import zone.pumpkinhill.discard.task.ImageDownloaderTask;
import zone.pumpkinhill.discard.task.NetworkTask;
import zone.pumpkinhill.discord4droid.handle.obj.User;
import zone.pumpkinhill.discord4droid.util.ImageHelper;

public class ProfileActivity extends BaseActivity {
    private final static String TAG = ProfileActivity.class.getCanonicalName();

    private NetworkTask mSaveTask = null;
    private User mUser;
    private Bitmap mNewAvatar;
    private View mProgressView, mFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        mProgressView = findViewById(R.id.save_progress);
        mFormView = findViewById(R.id.optionContainer);
        String userId = getIntent().getExtras().getString("userId");
        if(userId == null || userId.isEmpty()) {
            Toast.makeText(this, "No userId for ProfileActivity", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        mUser = findUser(userId);
        if(mUser == null) {
            Toast.makeText(this, "Couldn't find user", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // Name
        setTitle(mUser.getName() + " #" + mUser.getDiscriminator());
        // Avatar
        ImageView avatarView = (ImageView) findViewById(R.id.profAvatar);
        Bitmap avatar = ClientHelper.getAvatarFromCache(mUser.getAvatarURL());
        if(avatar == null) {
            new ImageDownloaderTask(avatarView, true).execute(mUser.getAvatarURL());
        } else {
            avatarView.setImageBitmap(avatar);
        }
        if(ClientHelper.client.getOurUser().getID().equals(userId)) {
            // Looking at our own profile, allow editing
            getLayoutInflater().inflate(R.layout.profile_own,
                    (ViewGroup) findViewById(R.id.optionContainer));
            // Set hints
            final EditText nameBox = (EditText) findViewById(R.id.changeName);
            final EditText emailBox = (EditText) findViewById(R.id.changeEmail);
            final EditText passwordBox = (EditText) findViewById(R.id.changePassword);
            final EditText confirmBox = (EditText) findViewById(R.id.confirmPassword);
            nameBox.setHint(mUser.getName());
            emailBox.setHint(ClientHelper.client.getEmail());
            // TODO: Set onClick for avatar (pick new avatar image)

            // Set onClick for save button
            Button saveButton = (Button) findViewById(R.id.saveButton);
            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSaveTask != null) return;
                    String name = nameBox.getText().toString(),
                            email = emailBox.getText().toString(),
                            password = passwordBox.getText().toString(),
                            confirm = confirmBox.getText().toString();
                    if(!password.isEmpty() && !password.equals(confirm)) {
                        Toast.makeText(mContext, "Passwords don't match", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String avatar = mNewAvatar == null ? null : ImageHelper.getBase64JPEG(mNewAvatar);
                    showProgress(mProgressView, mFormView, true);
                    mSaveTask = new NetworkTask(mContext);
                    mSaveTask.execute("change-profile",
                            name.isEmpty() ? null : name, email.isEmpty() ? null : email,
                            password.isEmpty() ? null : password, avatar);
                }
            });
        } else {
            // Looking at another user's profile
            getLayoutInflater().inflate(R.layout.profile_others,
                    (ViewGroup) findViewById(R.id.optionContainer));
        }
    }

    private User findUser(String id) {
        if(ClientHelper.client.getOurUser().getID().equals(id)) {
            return ClientHelper.client.getOurUser();
        }
        return ClientHelper.client.getUserByID("userId");
    }

    public void taskFinished(boolean result) {
        mSaveTask = null;
        showProgress(mProgressView, mFormView, false);
    }
}
