package zone.pumpkinhill.discard.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discard.R;
import zone.pumpkinhill.discard.adapter.ChatMessageAdapter;
import zone.pumpkinhill.discard.adapter.PrivateChannelAdapter;
import zone.pumpkinhill.discard.adapter.TextChannelAdapter;
import zone.pumpkinhill.discard.adapter.VoiceChannelAdapter;
import zone.pumpkinhill.discard.task.LoadMessagesTask;
import zone.pumpkinhill.discard.task.SendMessageTask;
import zone.pumpkinhill.discord4droid.api.DiscordClient;
import zone.pumpkinhill.discord4droid.api.EventSubscriber;
import zone.pumpkinhill.discord4droid.handle.events.DiscordDisconnectedEvent;
import zone.pumpkinhill.discord4droid.handle.events.MessageReceivedEvent;
import zone.pumpkinhill.discord4droid.handle.obj.Channel;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.Message;
import zone.pumpkinhill.discord4droid.handle.obj.PrivateChannel;
import zone.pumpkinhill.discord4droid.util.MessageList;

public class ChatActivity extends AppCompatActivity {
    private final static String TAG = ChatActivity.class.getCanonicalName();

    private Context mContext = this;
    private Guild mGuild;
    private Channel mChannel;
    private ListView mMessageView;
    private MessageList.MessageListEventListener mMessageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ClientHelper.subscribe(this);
        String guildId = getIntent().getExtras().getString("guildId");
        if(guildId == null || guildId.equals("0")) {
            // Private chat
            ArrayList<PrivateChannel> channelList = new ArrayList<>();
            for(Channel c : ClientHelper.client.getChannels(true)) {
                if(!(c instanceof PrivateChannel)) continue;
                if(mChannel == null) mChannel = c;
                channelList.add((PrivateChannel) c);
            }
            if(mChannel == null) {
                // No private channels
                finish();
                return;
            }
            // Setup drawer
            ImageView drIcon = (ImageView) findViewById(R.id.guildIcon);
            drIcon.setImageResource(R.drawable.ic_menu_camera);
            TextView drName = (TextView) findViewById(R.id.guildName);
            drName.setText("Direct Messages");
            TextView drNotify = (TextView) findViewById(R.id.notifyCount);
            drNotify.setText("");
            // Setup adapter for text channel list
            ListView textChannels = (ListView) findViewById(R.id.textChannelList);
            textChannels.setAdapter(new PrivateChannelAdapter(mContext, channelList));
            // Setup onItemClick for text channel list
            textChannels.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    switchChannel((Channel) parent.getAdapter().getItem(position));
                }
            });
        } else {
            // Guild
            mGuild = ClientHelper.client.getGuildByID(guildId);
            if (mGuild == null) {
                Log.e(TAG, "Something went wrong passing the guild ID to chat activity.");
                finish();
                return;
            }
            setTitle(mGuild.getName());
            mChannel = mGuild.getChannels().get(0);
            // Drawer
            ImageView drIcon = (ImageView) findViewById(R.id.guildIcon);
            drIcon.setImageBitmap(ClientHelper.getImageFromCache(mGuild.getIconURL()));
            TextView drName = (TextView) findViewById(R.id.guildName);
            drName.setText(mGuild.getName());
            TextView drNotify = (TextView) findViewById(R.id.notifyCount);
            drNotify.setText("");
            // Setup adapter for text channel list
            ListView textChannels = (ListView) findViewById(R.id.textChannelList);
            textChannels.setAdapter(new TextChannelAdapter(mContext, mGuild.getChannels()));
            // Setup onItemClick for text channel list
            textChannels.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    switchChannel((Channel) parent.getAdapter().getItem(position));
                }
            });
            // Setup adapter for voice channel list
            ListView voiceChannels = (ListView) findViewById(R.id.voiceChannelList);
            voiceChannels.setAdapter(new VoiceChannelAdapter(mContext, mGuild.getVoiceChannels()));
        }
        // Fill in message list
        mMessageView = (ListView) findViewById(R.id.messageListView);
        if(mMessageView != null) switchChannel(mChannel);
        Button sendButton = (Button) findViewById(R.id.sendButton);
        if(sendButton != null) {
            sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText msg = (EditText) findViewById(R.id.editMessage);
                    if(msg != null && !msg.getText().toString().isEmpty()) {
                        new SendMessageTask(msg, mMessageView).execute(mChannel.getID());
                    }
                }
            });
        }
    }

    private void switchChannel(Channel newChannel) {
        mChannel = newChannel;
        mMessageView.setAdapter(new ChatMessageAdapter(mContext, mChannel.getMessages()));
        new LoadMessagesTask(mChannel.getMessages(), mMessageView).execute();
        ClientHelper.unsubscribe(mMessageListener);
        mMessageListener = new MessageList.MessageListEventListener(mChannel.getMessages());
        ClientHelper.subscribe(mMessageListener);
        setTitle(mChannel.getName());
    }

    @Override
    public void finish() {
        super.finish();
        ClientHelper.unsubscribe(this);
        ClientHelper.unsubscribe(mMessageListener);
    }

    @EventSubscriber
    public void onMessage(MessageReceivedEvent event) {
        Message message = event.getMessage();
        if(message.getChannel().getID().equals(mChannel.getID())) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ListView msgList = (ListView) findViewById(R.id.messageListView);
                    Log.v(TAG, "Refreshing list adapter.");
                    ((ChatMessageAdapter) msgList.getAdapter()).notifyDataSetChanged();
                }
            });
        }
    }
}
