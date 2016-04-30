package zone.pumpkinhill.discard.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discard.R;
import zone.pumpkinhill.discard.adapter.*;
import zone.pumpkinhill.discard.task.NetworkTask;
import zone.pumpkinhill.discord4droid.api.EventSubscriber;
import zone.pumpkinhill.discord4droid.handle.events.MessageDeleteEvent;
import zone.pumpkinhill.discord4droid.handle.events.MessageReceivedEvent;
import zone.pumpkinhill.discord4droid.handle.events.MessageSendEvent;
import zone.pumpkinhill.discord4droid.handle.events.ReadyEvent;
import zone.pumpkinhill.discord4droid.handle.obj.Channel;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.PrivateChannel;

public class ChatActivity extends BaseActivity {
    private final static String TAG = ChatActivity.class.getCanonicalName();

    private Context mContext = this;
    private Guild mGuild;
    private List<Channel> mChannelList;
    private Channel mChannel;
    private ListView mMessageView;
    private DrawerLayout mLayout;
    private NetworkTask mLoadTask = null;

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
        ListView textChannels = (ListView) findViewById(R.id.textChannelList);
        ListView voiceChannels = (ListView) findViewById(R.id.voiceChannelList);
        // Make sure they aren't null
        assert drIcon != null;
        assert drName != null;
        assert textChannels != null;
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
            textChannels.setAdapter(new PrivateChannelAdapter(mContext, mChannelList));
            // Disable the user list drawer
            mLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);
        } else {
            // Guild
            mGuild = ClientHelper.client.getGuildByID(guildId);
            if (mGuild == null) {
                Log.e(TAG, "Something went wrong passing the guild ID to chat activity.");
                finish();
                return;
            }
            mChannelList = mGuild.getChannels();
            if(channelId != null && !channelId.isEmpty()) {
                mChannel = mGuild.getChannelByID(channelId);
            }
            // Drawer
            drIcon.setImageBitmap(ClientHelper.getAvatarFromCache(mGuild.getIconURL()));
            drName.setText(mGuild.getName());
            // Setup adapter for text channel list
            textChannels.setAdapter(new TextChannelAdapter(mContext, mGuild.getChannels()));
            // Setup adapter for voice channel list
            voiceChannels.setAdapter(new VoiceChannelAdapter(mContext, mGuild.getVoiceChannels()));
            // Setup user list drawer
            ListView online = (ListView) findViewById(R.id.onlineUserList);
            ListView offline = (ListView) findViewById(R.id.offlineUserList);
            assert online != null;
            assert offline != null;
            online.setAdapter(new UserListAdapter(mContext, mGuild, true));
            offline.setAdapter(new UserListAdapter(mContext, mGuild, false));
        }
        // Setup onItemClick for text channel list
        textChannels.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switchChannel((Channel) parent.getAdapter().getItem(position));
                mLayout.closeDrawers();
            }
        });
        // Fill in message list
        if(mChannel == null) mChannel = mChannelList.get(0);
        mMessageView = (ListView) findViewById(R.id.messageListView);
        if(mMessageView != null) switchChannel(mChannel);
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
                    new NetworkTask(mContext).execute("send-message",
                            mChannel.getID(), msg.getText().toString());
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
        ClientHelper.unsubscribe(mMessageView.getAdapter());
        ClientHelper.setActiveChannel(null);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this,GuildListActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            new NetworkTask(mContext).execute("send-file", mChannel.getID(), uri.toString());
        }
        super.onActivityResult(requestCode, resultCode, data);
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
    public void onReady(ReadyEvent event) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switchChannel(mChannel);
            }
        });
    }
}
