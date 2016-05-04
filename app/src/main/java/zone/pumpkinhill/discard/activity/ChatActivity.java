package zone.pumpkinhill.discard.activity;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discard.R;
import zone.pumpkinhill.discard.adapter.*;
import zone.pumpkinhill.discard.task.NetworkTask;
import zone.pumpkinhill.discord4droid.api.EventSubscriber;
import zone.pumpkinhill.discord4droid.handle.events.*;
import zone.pumpkinhill.discord4droid.handle.obj.Channel;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.Message;
import zone.pumpkinhill.discord4droid.handle.obj.PrivateChannel;

public class ChatActivity extends BaseActivity {
    private final static String TAG = ChatActivity.class.getCanonicalName();

    private Context mContext = this;
    private Guild mGuild;
    private List<Channel> mChannelList;
    private NetworkTask mLoadTask = null;
    private String mEditingMessage = null;

    private Channel mChannel;
    private ListView mTextChannelView;
    private ListView mMessageView;
    private ListView mUserListView;
    private DrawerLayout mLayout;
    private EditText mInviteTextBox = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        // Enable MenuItems that should be usable from chat
        showMenuProfile = true;
        // Get the layouts and views
        mLayout = (DrawerLayout) findViewById(R.id.chatActivity);
        ImageView drIcon = (ImageView) findViewById(R.id.guildIcon);
        TextView drName = (TextView) findViewById(R.id.guildName);
        mTextChannelView = (ListView) findViewById(R.id.textChannelList);
        ListView voiceChannels = (ListView) findViewById(R.id.voiceChannelList);
        // Make sure they aren't null
        assert drIcon != null;
        assert drName != null;
        assert mTextChannelView != null;
        assert voiceChannels != null;
        // Subscribe to message events so things can be updated in real time
        ClientHelper.subscribe(this);
        // Grab intent parameters, which guild and channel to focus
        String guildId = getIntent().getExtras().getString("guildId");
        String channelId = getIntent().getExtras().getString("channelId");
        if(guildId == null || guildId.equals("0")) {
            // No guild given -- this is a private chat
            mChannelList = new ArrayList<>();
            for(Channel c : ClientHelper.client.getChannels(true)) {
                if(c == null || !(c instanceof PrivateChannel)) continue;
                if(channelId != null && channelId.equals(c.getID())) mChannel = c;
                mChannelList.add(c);
            }
            if(mChannelList.size() == 0) { // No private channels
                finish();
                return;
            }
            // Setup drawer
            drIcon.setImageResource(R.drawable.ic_menu_camera);
            drName.setText("Direct Messages");
            // Setup adapter for text channel list
            mTextChannelView.setAdapter(new PrivateChannelAdapter(mContext, mChannelList));
            // Disable the user list drawer
            mLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);
            // Clear the "Text Channels" and "Voice Channels" text
            TextView tcLabel = (TextView) findViewById(R.id.textChannelLabel);
            TextView vcLabel = (TextView) findViewById(R.id.voiceChannelLabel);
            assert tcLabel != null;
            assert vcLabel != null;
            tcLabel.setText("");
            vcLabel.setText("");
        } else {
            // Guild
            mGuild = ClientHelper.client.getGuildByID(guildId);
            if (mGuild == null) {
                Log.e(TAG, "Something went wrong passing the guild ID to chat activity.");
                finish();
                return;
            }
            mChannelList = mGuild.getChannels();
            if(mChannelList.size() == 0) { // No channels
                finish();
                return;
            }
            if(channelId != null && !channelId.isEmpty()) {
                mChannel = mGuild.getChannelByID(channelId);
            }
            // Drawer
            drIcon.setImageBitmap(ClientHelper.cache.get(mGuild.getID()));
            drName.setText(mGuild.getName());
            // Setup adapter for text channel list
            mTextChannelView.setAdapter(new TextChannelAdapter(mContext, mGuild.getChannels()));
            // Setup adapter for voice channel list
            voiceChannels.setAdapter(new VoiceChannelAdapter(mContext, mGuild.getVoiceChannels()));
            // Setup user list drawer
            mUserListView = (ListView) findViewById(R.id.onlineUserList);
            assert mUserListView != null;
            mUserListView.setAdapter(new UserListAdapter(mContext, mGuild));
        }
        // Setup onItemClick for text channel list
        mTextChannelView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switchChannel((Channel) parent.getAdapter().getItem(position));
                mLayout.closeDrawers();
            }
        });
        // Fill in message list
        if(mChannel == null) mChannel = mChannelList.get(0);
        mMessageView = (ListView) findViewById(R.id.messageListView);
        assert mMessageView != null;
        registerForContextMenu(mMessageView);
        switchChannel(mChannel);
        // Re-enable notifications for this channel
        NotifiedChannels.remove(mChannel);
        // Setup OnScrollListener to check for scrolling to the top
        mMessageView.setOnScrollListener(new AbsListView.OnScrollListener() {
            private int currentScrollState = SCROLL_STATE_IDLE,
                    currentFirstVisible = 0, currentVisibleCount = 0;
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                currentScrollState = scrollState;
                if(currentVisibleCount == 0 || currentFirstVisible > 0) return;
                if(currentScrollState != SCROLL_STATE_IDLE) return;
                if(mLoadTask == null || mLoadTask.getStatus() == AsyncTask.Status.FINISHED) {
                    mLoadTask = new NetworkTask(mContext);
                    mLoadTask.execute("load-messages", mChannel.getID(), "20",
                            mChannel.getMessages().getEarliest().getID());
                }
            }
            @Override
            public void onScroll(AbsListView view, int firstVisible, int visibleCount, int totalCount) {
                currentFirstVisible = firstVisible;
                currentVisibleCount = visibleCount;
            }
        });
        // Send message button
        ImageButton sendButton = (ImageButton) findViewById(R.id.sendButton);
        if(sendButton != null) {
            sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText msg = (EditText) findViewById(R.id.editMessage);
                    if(msg == null || msg.getText().toString().isEmpty()) return;
                    if(mEditingMessage != null) {
                        new NetworkTask(mContext).execute("edit-message", mChannel.getID(),
                                mEditingMessage, msg.getText().toString());
                        msg.setBackgroundColor(Color.WHITE);
                        mEditingMessage = null;
                    } else {
                        new NetworkTask(mContext).execute("send-message", mChannel.getID(),
                                msg.getText().toString());
                    }
                    msg.setText("");
                }
            });
        }
        // Upload image button
        ImageButton imageButton = (ImageButton) findViewById(R.id.imageButton);
        if(imageButton != null) {
            imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT, null);
                    i.setType("image/*");
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(i, FILE_SELECT_CODE);
                }
            });
        }
    }

    private void switchChannel(Channel newChannel) {
        mChannel = newChannel;
        ClientHelper.setActiveChannel(newChannel);
        mMessageView.setAdapter(new ChatMessageAdapter(mContext, mChannel.getMessages()));
        if(mLoadTask != null && mLoadTask.getStatus() != AsyncTask.Status.FINISHED) {
            mLoadTask.cancel(true);
        }
        mLoadTask = new NetworkTask(mContext);
        mLoadTask.execute("load-messages", mChannel.getID(), "20", null);
        setTitle(mChannel.getName());
    }

    @Override
    public void finish() {
        super.finish();
        ClientHelper.unsubscribe(this);
        if(mMessageView != null && mMessageView.getAdapter() != null) {
            ClientHelper.unsubscribe(mMessageView.getAdapter());
        }
        ClientHelper.setActiveChannel(null);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this,GuildListActivity.class);
        startActivity(intent);
        finish();
    }

    private void showGetInviteDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
        alert.setTitle("Invite");
        alert.setMessage("Send this link to someone to invite them to " + mGuild.getName());
        mInviteTextBox = new EditText(mContext);
        mInviteTextBox.setText("(Getting Invite)");
        alert.setView(mInviteTextBox);
        // Start a background task to get an invite
        NetworkTask task = new NetworkTask(mContext);
        task.execute("get-invite", mGuild.getID());
        // Copy to clipboard or close
        alert.setPositiveButton("Copy", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("invite", mInviteTextBox.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(mContext, "Copied to clipboard", Toast.LENGTH_LONG).show();
            }
        });
        alert.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                mInviteTextBox = null;
            }
        });
        alert.show();
    }

    public void applyInviteCodeToTextBox(final String code) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mInviteTextBox.setText(code);
            }
        });
    }

    private void showGuildConfirmDialog(final boolean delete) {
        AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
        alert.setTitle(delete ? "Delete Guild" : "Leave Guild");
        alert.setMessage("Are you really sure you want to " + (delete ? "delete " : "leave ") +
                mGuild.getName() + "? This action cannot be undone.");
        alert.setPositiveButton(delete ? "Delete" : "Leave", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                NetworkTask task = new NetworkTask(mContext);
                task.execute(delete ? "delete-guild" : "leave-guild", mGuild.getID());
                // Wait for it to finish
                // FIXME: This blocks the UI thread, use callback instead
                boolean result;
                try {
                    result = task.get();
                } catch(ExecutionException | InterruptedException e) {
                    return;
                }
                // If it succeeded close everything
                if(!result) return;
                dialog.dismiss();
                finish();
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alert.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(!super.onCreateOptionsMenu(menu)) return false;
        if(mGuild == null) return true; // Don't add these for private chat
        // Anyone can invite TODO: By default, but it can be restricted via roles
        MenuItem getInvite = menu.findItem(R.id.menu_get_invite);
        getInvite.setVisible(true);
        getInvite.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                showGetInviteDialog();
                return true;
            }
        });
        if(mGuild.getOwnerID().equals(ClientHelper.client.getOurUser().getID())) {
            // Guild owner can edit and delete
            MenuItem editGuild = menu.findItem(R.id.menu_edit_guild);
            editGuild.setVisible(true);
            editGuild.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    Intent i = new Intent(mContext, EditGuildActivity.class);
                    i.putExtra("guildId", mGuild.getID());
                    startActivity(i);
                    return true;
                }
            });
            MenuItem deleteGuild = menu.findItem(R.id.menu_delete_guild);
            deleteGuild.setVisible(true);
            deleteGuild.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    showGuildConfirmDialog(true);
                    return true;
                }
            });
        } else {
            // Non-owner can leave
            MenuItem leaveGuild = menu.findItem(R.id.menu_leave_guild);
            leaveGuild.setVisible(true);
            leaveGuild.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    showGuildConfirmDialog(false);
                    return true;
                }
            });
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            new NetworkTask(mContext).execute("send-file", mChannel.getID(), uri.toString());
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if(v.getId() == R.id.messageListView) {
            ListView lv = (ListView) v;
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            Message msg = (Message) lv.getItemAtPosition(info.position);
            menu.setHeaderTitle(msg.getAuthor().getName());
            if(ClientHelper.client.getOurUser().getID().equals(msg.getAuthor().getID())) {
                menu.add(0, 0, 0, "Edit Message");
                menu.add(0, 1, 1, "Delete Message");
            } else {
                menu.add(0, 2, 2, "Private Chat");
                menu.add(0, 3, 3, "View Profile");
                //menu.add("Kick");
                //menu.add("Ban");
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Message msg = (Message) mMessageView.getAdapter().getItem(info.position);
        switch(item.getItemId()) {
            case 0: // Edit
                EditText editView = (EditText) findViewById(R.id.editMessage);
                assert editView != null;
                editView.setText(msg.getContent());
                editView.setBackgroundColor(Color.YELLOW);
                mEditingMessage = msg.getID();
                return true;
            case 1: // Delete
                new NetworkTask(mContext).execute("delete-message", mChannel.getID(), msg.getID());
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @EventSubscriber
    @SuppressWarnings("unused")
    public void onMessageReceived(final MessageReceivedEvent event) {
        if(!event.getMessage().getChannel().equals(mChannel)) return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mChannel.getMessages().add(event.getMessage());
                ((ChatMessageAdapter) mMessageView.getAdapter()).notifyDataSetChanged();
                if(!isAppInBackground) {
                    new NetworkTask(mContext).execute("ack-message", mChannel.getID(),
                            event.getMessage().getID());
                }
            }
        });
    }

    @EventSubscriber
    @SuppressWarnings("unused")
    public void onMessageSent(final MessageSendEvent event) {
        if(!event.getMessage().getChannel().equals(mChannel)) return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mChannel.getMessages().add(event.getMessage());
                ((ChatMessageAdapter) mMessageView.getAdapter()).notifyDataSetChanged();
            }
        });
    }

    @EventSubscriber
    @SuppressWarnings("unused")
    public void onMessageDelete(final MessageDeleteEvent event) {
        if(!event.getMessage().getChannel().equals(mChannel)) return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mChannel.getMessages().remove(event.getMessage());
                ((ChatMessageAdapter) mMessageView.getAdapter()).notifyDataSetChanged();
            }
        });
    }

    // This is to refresh the channel after resuming from suspend (and reconnecting websocket)
    @EventSubscriber
    @SuppressWarnings("unused")
    public void onReady(final ReadyEvent event) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switchChannel(mChannel);
            }
        });
    }

    // User events to update the user list
    protected void refreshUserList() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((UserListAdapter) mUserListView.getAdapter()).notifyDataSetChanged();
            }
        });
    }

    @EventSubscriber
    @SuppressWarnings("unused")
    public void onGameChange(final GameChangeEvent event) {
        if(event.getGuild().equals(mGuild)) refreshUserList();
    }
    @EventSubscriber
    @SuppressWarnings("unused")
    public void onPresenceUpdate(final PresenceUpdateEvent event) {
        if(event.getGuild().equals(mGuild)) refreshUserList();
    }
    @EventSubscriber
    @SuppressWarnings("unused")
    public void onUserJoin(final UserJoinEvent event) {
        if(event.getGuild().equals(mGuild)) refreshUserList();
    }
    @EventSubscriber
    @SuppressWarnings("unused")
    public void onUserLeave(final UserLeaveEvent event) {
        if(event.getGuild().equals(mGuild)) refreshUserList();
    }
    @EventSubscriber
    @SuppressWarnings("unused")
    public void onUserBan(final UserBanEvent event) {
        if(event.getGuild().equals(mGuild)) refreshUserList();
    }
    @EventSubscriber
    @SuppressWarnings("unused")
    public void onUserUpdate(final UserUpdateEvent event) {
        if(mGuild != null && mGuild.getUserByID(event.getNewUser().getID()) != null)
            refreshUserList();
    }
    @EventSubscriber
    @SuppressWarnings("unused")
    public void onRoleCreate(final RoleCreateEvent event) {
        if(event.getGuild().equals(mGuild)) refreshUserList();
    }
    @EventSubscriber
    @SuppressWarnings("unused")
    public void onRoleDelete(final RoleDeleteEvent event) {
        if(event.getGuild().equals(mGuild)) refreshUserList();
    }
    @EventSubscriber
    @SuppressWarnings("unused")
    public void onRoleUpdate(final RoleUpdateEvent event) {
        if(event.getGuild().equals(mGuild)) refreshUserList();
    }

    // For channels changing
    protected void refreshTextChannels() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextChannelAdapter) mTextChannelView.getAdapter()).notifyDataSetChanged();
            }
        });
    }

    @EventSubscriber
    @SuppressWarnings("unused")
    public void onChannelCreate(final ChannelCreateEvent event) {
        if(event.getChannel().getGuild().equals(mGuild)) refreshTextChannels();
    }
    @EventSubscriber
    @SuppressWarnings("unused")
    public void onChannelDelete(final ChannelDeleteEvent event) {
        if(event.getChannel().getGuild().equals(mGuild)) refreshTextChannels();
    }
    @EventSubscriber
    @SuppressWarnings("unused")
    public void onChannelUpdate(final ChannelUpdateEvent event) {
        if(event.getNewChannel().getGuild().equals(mGuild)) refreshTextChannels();
    }
}
