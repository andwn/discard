package zone.pumpkinhill.discard.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;

import zone.pumpkinhill.discard.ImageHelper;
import zone.pumpkinhill.discard.R;

public abstract class AvatarPickerActivity extends BaseActivity {
    protected final static int CHANGE_AVATAR_CODE = 1;

    protected Bitmap mNewAvatar = null;
    protected ImageView mAvatarPicker = null;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHANGE_AVATAR_CODE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            try {
                mNewAvatar = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                mNewAvatar = ImageHelper.getResizedBitmap(mNewAvatar, 256, 256, true);
                mAvatarPicker.setImageBitmap(mNewAvatar);
            } catch(IOException | NullPointerException e) {
                Toast.makeText(mContext, "Error loading image: " + e, Toast.LENGTH_LONG).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    protected class ChangeAvatarOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT, null);
            i.setType("image/*");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(i, CHANGE_AVATAR_CODE);
        }
    }
}
