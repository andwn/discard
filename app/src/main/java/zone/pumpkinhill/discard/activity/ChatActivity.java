package zone.pumpkinhill.discard.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import zone.pumpkinhill.discard.adapter.ChatMessageAdapter;
import zone.pumpkinhill.discard.adapter.PrivateChannelAdapter;
import zone.pumpkinhill.discard.adapter.TextChannelAdapter;
import zone.pumpkinhill.discard.adapter.UserListAdapter;
import zone.pumpkinhill.discard.adapter.VoiceChannelAdapter;
import zone.pumpkinhill.discard.task.LoadMessagesTask;
import zone.pumpkinhill.discard.task.NetworkTask;
import zone.pumpkinhill.discord4droid.api.Event;
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

    private static final int FILE_SELECT_CODE = 0;

    private Context mContext = this;
    private Guild mGuild;
    private List<Channel> mChannelList;
    private Channel mChannel;
    private ListView mMessageView;
    private boolean mIsPrivate;
    private DrawerLayout mLayout;

    private Event mEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        mLayout = (DrawerLayout) findViewById(R.id.chatActivity);
        ClientHelper.subscribe(this);
        String guildId = getIntent().getExtras().getString("guildId");
        String channelId = getIntent().getExtras().getString("channelId");
        if(guildId == null || guildId.equals("0")) {
            // Private chat
            mIsPrivate = true;
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
            ImageView drIcon = (ImageView) findViewById(R.id.guildIcon);
            drIcon.setImageResource(R.drawable.ic_menu_camera);
            TextView drName = (TextView) findViewById(R.id.guildName);
            drName.setText("Direct Messages");
            // Setup adapter for text channel list
            ListView textChannels = (ListView) findViewById(R.id.textChannelList);
            textChannels.setAdapter(new PrivateChannelAdapter(mContext, mChannelList));
            // Setup onItemClick for text channel list
            textChannels.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    switchChannel((Channel) parent.getAdapter().getItem(position));
                    mLayout.closeDrawers();
                }
            });
            // Disable the user list drawer
            ((DrawerLayout) findViewById(R.id.chatActivity))
                    .setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);
        } else {
            // Guild
            mIsPrivate = false;
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
            ImageView drIcon = (ImageView) findViewById(R.id.guildIcon);
            drIcon.setImageBitmap(ClientHelper.getImageFromCache(mGuild.getIconURL()));
            TextView drName = (TextView) findViewById(R.id.guildName);
            drName.setText(mGuild.getName());
            // Setup adapter for text channel list
            ListView textChannels = (ListView) findViewById(R.id.textChannelList);
            textChannels.setAdapter(new TextChannelAdapter(mContext, mGuild.getChannels()));
            // Setup onItemClick for text channel list
            textChannels.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    switchChannel((Channel) parent.getAdapter().getItem(position));
                    mLayout.closeDrawers();
                }
            });
            // Setup adapter for voice channel list
            ListView voiceChannels = (ListView) findViewById(R.id.voiceChannelList);
            voiceChannels.setAdapter(new VoiceChannelAdapter(mContext, mGuild.getVoiceChannels()));
            // Setup user list drawer
            ListView online = (ListView) findViewById(R.id.onlineUserList);
            ListView offline = (ListView) findViewById(R.id.offlineUserList);
            online.setAdapter(new UserListAdapter(mContext, mGuild, true));
            offline.setAdapter(new UserListAdapter(mContext, mGuild, false));
        }
        // Fill in message list
        if(mChannel == null) mChannel = mChannelList.get(0);
        mMessageView = (ListView) findViewById(R.id.messageListView);
        if(mMessageView != null) switchChannel(mChannel);
        ImageButton sendButton = (ImageButton) findViewById(R.id.sendButton);
        if(sendButton != null) {
            sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText msg = (EditText) findViewById(R.id.editMessage);
                    if(msg == null || msg.getText().toString().isEmpty()) return;
                    new NetworkTask(mContext).execute("send-message", mChannel.getID(), msg.getText().toString());
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
        new LoadMessagesTask(mChannel.getMessages(), mMessageView).execute(mChannel.getID());
        setTitle(mChannel.getName());
    }

    @Override
    public void finish() {
        super.finish();
        ClientHelper.unsubscribe(this);
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
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    new NetworkTask(mContext).execute("send-file", mChannel.getID(), uri.toString());
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @EventSubscriber
    public void onMessageReceived(MessageReceivedEvent event) {
        if(!event.getMessage().getChannel().equals(mChannel)) return;
        mEvent = event;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mChannel.getMessages().add(((MessageReceivedEvent)mEvent).getMessage());
                ((ChatMessageAdapter) mMessageView.getAdapter()).notifyDataSetChanged();
            }
        });
    }

    @EventSubscriber
    public void onMessageSent(MessageSendEvent event) {
        if(!event.getMessage().getChannel().equals(mChannel)) return;
        mEvent = event;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mChannel.getMessages().add(((MessageSendEvent)mEvent).getMessage());
                ((ChatMessageAdapter) mMessageView.getAdapter()).notifyDataSetChanged();
            }
        });
    }

    @EventSubscriber
    public void onMessageDelete(MessageDeleteEvent event) {
        if(!event.getMessage().getChannel().equals(mChannel)) return;
        mEvent = event;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mChannel.getMessages().remove(((MessageDeleteEvent)mEvent).getMessage());
                ((ChatMessageAdapter) mMessageView.getAdapter()).notifyDataSetChanged();
            }
        });
    }

    // This is to refresh the channel after resuming from suspend (and reconnecting websocket)
    @EventSubscriber
    public void onReady(ReadyEvent event) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switchChannel(mChannel);
            }
        });
    }
}
