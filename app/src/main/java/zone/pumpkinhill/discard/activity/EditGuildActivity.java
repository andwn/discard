package zone.pumpkinhill.discard.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discard.ImageHelper;
import zone.pumpkinhill.discard.R;
import zone.pumpkinhill.discard.adapter.RegionAdapter;
import zone.pumpkinhill.discard.task.NetworkTask;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.Region;
import zone.pumpkinhill.discord4droid.handle.obj.User;

public class EditGuildActivity extends AvatarPickerActivity {
    private NetworkTask mSaveTask = null;
    private EditText mNameBox;
    private Spinner mRegionPicker;
    private View mProgressView, mFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_guild);
        String guildId = getIntent().getStringExtra("guildId");
        // Get widgets
        mAvatarPicker = (ImageView) findViewById(R.id.guildIcon);
        mNameBox = (EditText) findViewById(R.id.guildName);
        mRegionPicker = (Spinner) findViewById(R.id.guildRegion);
        Button vButton = (Button) findViewById(R.id.saveButton);
        assert mAvatarPicker != null;
        assert mNameBox != null;
        assert mRegionPicker != null;
        assert vButton != null;
        mRegionPicker.setAdapter(new RegionAdapter(mContext));
        if(guildId == null || guildId.isEmpty()) {
            // New guild
            setTitle("Create Guild");
            vButton.setText("Create");
            mAvatarPicker.setOnClickListener(new ChangeAvatarOnClickListener());
            vButton.setOnClickListener(new CreateButtonOnClickListener());
        } else {
            // Edit existing guild
            Guild guild = ClientHelper.client.getGuildByID(guildId);
            if(guild == null) {
                Toast.makeText(mContext, "Invalid guild ID", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            setTitle(guild.getName());
            mAvatarPicker.setImageBitmap(ClientHelper.cache.get(guildId));
            if(guild.getOwnerID().equals(ClientHelper.client.getOurUser().getID())) {
                // We are the guild owner
                mAvatarPicker.setOnClickListener(new ChangeAvatarOnClickListener());
                vButton.setOnClickListener(new SaveButtonOnClickListener());
            } else {
                // We are not the guild owner
                mNameBox.setVisibility(View.INVISIBLE);
                mRegionPicker.setEnabled(false);
                vButton.setVisibility(View.INVISIBLE);
            }
            populateOwnerView(guild.getOwner());
        }
    }

    private void populateOwnerView(User owner) {
        FrameLayout vOwnerParent = (FrameLayout) findViewById(R.id.ownerLayout);
        getLayoutInflater().inflate(R.layout.list_item_guild, vOwnerParent);
        // Get all the widgets
        ImageView vAvatar = (ImageView) findViewById(R.id.avatarImageView);
        TextView vName = (TextView) findViewById(R.id.nameTextView);
        TextView vDiscriminator = (TextView) findViewById(R.id.discriminatorTextView);
        TextView vContent = (TextView) findViewById(R.id.nowPlaying);
        TextView vStatus = (TextView) findViewById(R.id.statusText);
        assert vAvatar != null;
        assert vName != null;
        assert vDiscriminator != null;
        assert vContent != null;
        assert vStatus != null;
        vAvatar.setImageBitmap(ClientHelper.cache.get(owner.getID()));
        vName.setText(owner.getName());
        vDiscriminator.setText(owner.getDiscriminator());
        vContent.setText("Owner");
        switch(owner.getPresence()) {
            case ONLINE: vStatus.setBackgroundResource(R.color.colorStatusOnline); break;
            case IDLE: vStatus.setBackgroundResource(R.color.colorStatusIdle); break;
            case OFFLINE: vStatus.setBackgroundResource(R.color.colorStatusOffline); break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    // Save Button OnClick(s)
    private class CreateButtonOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (mSaveTask != null) return;
            String name = mNameBox.getText().toString(),
                    region = ((Region) mRegionPicker.getSelectedItem()).getID();
            if(name.isEmpty()) {
                Toast.makeText(mContext, "Give your guild a name!", Toast.LENGTH_LONG).show();
                return;
            }
            String avatar = mNewAvatar == null ? null : ImageHelper.getBase64JPEG(mNewAvatar);
            //showProgress(mProgressView, mFormView, true);
            mSaveTask = new NetworkTask(mContext);
            mSaveTask.execute("create-guild", name, region, avatar);
        }
    }
    private class SaveButtonOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {

        }
    }

    public void taskFinished(boolean result) {
        mSaveTask = null;
        if(!result) return;
        Toast.makeText(mContext, "New guild " + mNameBox.getText().toString() + " created",
                Toast.LENGTH_LONG).show();
        finish();
    }
}
