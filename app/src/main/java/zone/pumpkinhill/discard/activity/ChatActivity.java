package zone.pumpkinhill.discard.activity;

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
import zone.pumpkinhill.discard.task.SendMessageTask;
import zone.pumpkinhill.discord4droid.api.DiscordClient;
import zone.pumpkinhill.discord4droid.api.EventSubscriber;
import zone.pumpkinhill.discord4droid.handle.events.DiscordDisconnectedEvent;
import zone.pumpkinhill.discord4droid.handle.events.MessageReceivedEvent;
import zone.pumpkinhill.discord4droid.handle.obj.Channel;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.Message;
import zone.pumpkinhill.discord4droid.handle.obj.PrivateChannel;

public class ChatActivity extends AppCompatActivity {
    private final static String TAG = ChatActivity.class.getCanonicalName();

    private Guild mGuild;
    private Channel mChannel;
    private ListView mMessageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ClientHelper.subscribe(this);
        DiscordClient client = ClientHelper.client;
        String guildId = getIntent().getExtras().getString("guildId");
        if(guildId == null || guildId.equals("0")) {
            // Private chat
            ArrayList<PrivateChannel> channelList = new ArrayList<>();
            for(Channel c : client.getChannels(true)) {
                if(!(c instanceof PrivateChannel)) continue;
                if(mChannel == null) mChannel = c;
                channelList.add((PrivateChannel) c);
            }
            if(mChannel == null) {
                // No private channels
                finish();
                return;
            }
            setTitle(mChannel.getName());
            // Setup drawer
            ImageView drIcon = (ImageView) findViewById(R.id.guildIcon);
            drIcon.setImageResource(R.drawable.ic_menu_camera);
            TextView drName = (TextView) findViewById(R.id.guildName);
            drName.setText("Private Messages");
            TextView drNotify = (TextView) findViewById(R.id.notifyCount);
            drNotify.setText("");
            // Setup adapter for text channel list
            ListView textChannels = (ListView) findViewById(R.id.textChannelList);
            textChannels.setAdapter(new PrivateChannelAdapter(getApplicationContext(), channelList));
            // Setup onItemClick for text channel list
            textChannels.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    ListView messages = (ListView) findViewById(R.id.messageListView);
                    mChannel = (Channel) parent.getAdapter().getItem(position);
                    messages.setAdapter(new ChatMessageAdapter(getApplicationContext(), mChannel.getMessages()));
                    setTitle(mChannel.getName());
                }
            });
        } else {
            // Guild
            mGuild = client.getGuildByID(guildId);
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
            textChannels.setAdapter(new TextChannelAdapter(getApplicationContext(), mGuild.getChannels()));
            // Setup onItemClick for text channel list
            textChannels.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    ListView messages = (ListView) findViewById(R.id.messageListView);
                    mChannel = (Channel) parent.getAdapter().getItem(position);
                    messages.setAdapter(new ChatMessageAdapter(getApplicationContext(), mChannel.getMessages()));
                    setTitle(mChannel.getName());
                }
            });
            // Setup adapter for text channel list
            ListView voiceChannels = (ListView) findViewById(R.id.voiceChannelList);
            voiceChannels.setAdapter(new VoiceChannelAdapter(getApplicationContext(), mGuild.getVoiceChannels()));
        }
        // Fill in message list
        mMessageList = (ListView) findViewById(R.id.messageListView);
        if(mMessageList != null) {
            mMessageList.setAdapter(new ChatMessageAdapter(getApplicationContext(), mChannel.getMessages()));
        }
        Button sendButton = (Button) findViewById(R.id.sendButton);
        if(sendButton != null) {
            sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText msg = (EditText) findViewById(R.id.editMessage);
                    if(msg != null && !msg.getText().toString().isEmpty()) {
                        new SendMessageTask(msg, mMessageList).execute(mChannel.getID());
                    }
                }
            });
        }
    }
/*
    View.OnClickListener openGuildsDrawer = new View.OnClickListener() {
        public void onClick(View v) {
            Log.d("openGuildsDrawer", "I have been called.");
            mGuildsDrawer.openDrawer(Gravity.LEFT);
            //Intent i = new Intent(getApplicationContext(), GuildsDrawerActivity.class);
            //startActivity(i);
        }
    };
*/
    /*
    @EventSubscriber
    public void onReady(ReadyEvent event) {
        DiscordClient client = event.getClient();
        User ourUser = client.getOurUser();
        Guild guild = client.getGuilds().get(0);
        //System.out.println("Logged in as " + ourUser.getName());
        if(guild == null) {
            System.out.println("Not a member of any guilds.");
            return;
        }
        System.out.println("Current guild: " + guild.getName());
        Channel channel = guild.getChannels().get(0);
        if(channel == null) {
            System.out.println("This guild has no channels.");
            return;
        }
        System.out.println("Current channel: " + channel.getName() + " (" + channel.getID() + ")");
        this.mChannel = channel;
        // Toast makes the app crash...
        //Toast.makeText(getBaseContext(), "Logged in as " + ourUser.getName(), Toast.LENGTH_SHORT).show();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Fill in action bar
                    ActionBar bar = getSupportActionBar();
                    bar.setDisplayShowHomeEnabled(false);
                    View barView = getLayoutInflater().inflate(R.layout.action_bar, null);
                    barView.findViewById(R.id.guildsButton).setOnClickListener(openGuildsDrawer);
                    ((TextView)barView.findViewById(R.id.titleText)).setText(mChannel.getName());
                    bar.setCustomView(barView);
                    bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
                    // Fill in message list
                    ListView msgList = (ListView) findViewById(R.id.messageListView);
                    if(msgList == null) return;
                    msgList.setAdapter(new ChatMessageAdapter(getApplicationContext(),
                            mChannel.getID(), mChannel.getMessages()));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        });
        //ClientHelper.fillChatMessages(channel.getMessages());
    }
*/
    @EventSubscriber
    public void onDisconnect(DiscordDisconnectedEvent event) {
        System.out.println("Disconnected event");
        ClientHelper.abandonClient();
        finish();
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
        //ClientHelper.newChatMessage(message.getChannel().getID(), message);
        //ListView msgList = (ListView) findViewById(R.id.chatMessages);

    }
}
