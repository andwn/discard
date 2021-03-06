package zone.pumpkinhill.discard.task;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discard.R;
import zone.pumpkinhill.discard.activity.ChatActivity;
import zone.pumpkinhill.discard.activity.EditGuildActivity;
import zone.pumpkinhill.discard.activity.ProfileActivity;
import zone.pumpkinhill.discard.adapter.ChatMessageAdapter;
import zone.pumpkinhill.discord4droid.handle.obj.Channel;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.Invite;
import zone.pumpkinhill.discord4droid.handle.obj.InviteResponse;
import zone.pumpkinhill.discord4droid.handle.obj.Message;
import zone.pumpkinhill.discord4droid.util.DiscordException;
import zone.pumpkinhill.discord4droid.util.HTTP429Exception;
import zone.pumpkinhill.discord4droid.util.MessageList;
import zone.pumpkinhill.discord4droid.util.MissingPermissionsException;
import zone.pumpkinhill.http.entity.ContentType;

public class NetworkTask extends AsyncTask<String, Void, Boolean> {
    private final static String TAG = NetworkTask.class.getCanonicalName();

    private final Context mContext;
    private String mErrorMsg;
    private String[] mParams;
    private Handler.Callback mCallback = null;

    private MessageList mTempMsgList = null;

    public NetworkTask(Context context) {
        mContext = context;
    }

    public void callWhenFinished(Handler.Callback callback) {
        mCallback = callback;
    }

    protected Boolean doInBackground(String... params) {
        try {
            mParams = params;
            switch(params[0]) {
                case "login": ClientHelper.login(params[1], params[2], params[3]); break;
                case "logout": ClientHelper.logout(); break;
                case "suspend": ClientHelper.client.suspend(); break;
                case "resume": ClientHelper.client.resume(); break;
                case "load-messages": return doLoadMessages(params[1], params[2], params[3]);
                case "ack-message" : ClientHelper.client.ackMessage(params[1], params[2]); break;
                case "send-message": return doSendMessage(params[1], params[2]);
                case "edit-message": return doEditMessage(params[1], params[2], params[3]);
                case "delete-message": return doDeleteMessage(params[1], params[2]);
                case "send-file": return doSendFile(params[1], params[2]);
                case "get-invite": return doGetInvite(params[1]);
                case "join-guild": return doJoinGuild(params[1]);
                case "create-guild": return doCreateGuild(params[1], params[2], params[3]);
                case "delete-guild": ClientHelper.client.getGuildByID(params[1]).deleteGuild(); break;
                case "leave-guild": ClientHelper.client.getGuildByID(params[1]).leaveGuild(); break;
                case "change-profile":
                    ClientHelper.client.changeAccountInfo(params[1], params[2], params[3], params[4]);
                    break;
                case "get-regions": ClientHelper.client.getRegions(); break;
                default: mErrorMsg = "Unknown command: " + params[0]; return false;
            }
            return true;
        } catch(Exception e) {
            Log.d(TAG, "Exception: " + e);
            mErrorMsg = e.getLocalizedMessage();
            return false;
        }
    }

    protected boolean doLoadMessages(String channelId, String count, String before) {
        try {
            Channel channel = ClientHelper.client.getChannelByID(channelId);
            mTempMsgList = new MessageList(ClientHelper.client, channel);
            mTempMsgList.load(Integer.parseInt(count), before);
            return true;
        } catch(HTTP429Exception | DiscordException | MissingPermissionsException e) {
            mErrorMsg = "Failed to load messages: " + e;
            return false;
        }
    }

    protected boolean doSendMessage(String channelId, String content) {
        Channel channel = ClientHelper.client.getChannelByID(channelId);
        if(channel == null) {
            mErrorMsg = "Unable to find channel with ID " + channelId;
            return false;
        }
        try {
            channel.sendMessage(content);
            return true;
        } catch(MissingPermissionsException e) {
            mErrorMsg = "Don't have permission to send messages in this channel";
        } catch(HTTP429Exception e) {
            mErrorMsg = "Unable to send message - traffic is being rate limited";
        } catch(DiscordException e) {
            mErrorMsg = "Error sending message: " + e;
        }
        return false;
    }

    protected boolean doEditMessage(String channelId, String messageId, String content) {
        Channel channel = ClientHelper.client.getChannelByID(channelId);
        if(channel == null) {
            mErrorMsg = "Unable to find channel with ID " + channelId;
            return false;
        }
        Message message = channel.getMessageByID(messageId);
        if(message == null) {
            mErrorMsg = "Unable to find message with ID " + messageId;
            return false;
        }
        try {
            message.edit(content);
            return true;
        } catch(MissingPermissionsException e) {
            mErrorMsg = "Don't have permission to edit messages in this channel";
        } catch(HTTP429Exception e) {
            mErrorMsg = "Unable to edit message - traffic is being rate limited";
        } catch(DiscordException e) {
            mErrorMsg = "Error editing message: " + e;
        }
        return false;
    }

    protected boolean doDeleteMessage(String channelId, String messageId) {
        Channel channel = ClientHelper.client.getChannelByID(channelId);
        if(channel == null) {
            mErrorMsg = "Unable to find channel with ID " + channelId;
            return false;
        }
        Message message = channel.getMessageByID(messageId);
        if(message == null) {
            mErrorMsg = "Unable to find message with ID " + messageId;
            return false;
        }
        try {
            message.delete();
            return true;
        } catch(MissingPermissionsException e) {
            mErrorMsg = "Don't have permission to delete messages in this channel";
        } catch(HTTP429Exception e) {
            mErrorMsg = "Unable to delete message - traffic is being rate limited";
        } catch(DiscordException e) {
            mErrorMsg = "Error deleting message: " + e;
        }
        return false;
    }

    protected boolean doSendFile(String channelId, String uri) {
        Channel channel = ClientHelper.client.getChannelByID(channelId);
        if(channel == null) {
            mErrorMsg = "Unable to find channel with ID " + channelId;
            return false;
        }
        try {
            // This is the real Rube Goldberg shit right here
            ContentResolver cr = mContext.getContentResolver();
            MimeTypeMap mime = MimeTypeMap.getSingleton();

            InputStream stream = cr.openInputStream(Uri.parse(uri));
            if(stream == null) {
                Log.e(TAG, "Failed to open stream for " + uri);
                return false;
            }

            String typeStr = cr.getType(Uri.parse(uri));
            ContentType type = typeStr == null ? ContentType.DEFAULT_BINARY : ContentType.parse(typeStr);

            String ext = mime.getExtensionFromMimeType(typeStr);
            String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.ENGLISH)
                    .format(new Date()) + "." + ext;

            channel.sendFile(stream, type, name, null);
            stream.close();
            return true;
        } catch(MissingPermissionsException e) {
            mErrorMsg = "Don't have permission to send files in this channel";
        } catch(HTTP429Exception e) {
            mErrorMsg = "Unable to send file - traffic is being rate limited";
        } catch(DiscordException e) {
            mErrorMsg = "Error sending file: " + e;
        } catch(IOException e) {
            mErrorMsg = "Error opening file to send: " + e;
        }
        return false;
    }

    protected boolean doGetInvite(String guildId) {
        try {
            String ourInviteCode;
            Guild guild = ClientHelper.client.getGuildByID(guildId);
            List<Invite> invites = guild.getInvites();
            if(invites.size() > 0) {
                ourInviteCode = invites.get(0).getInviteCode();
            } else {
                ourInviteCode = guild.getChannels().get(0)
                        .createInvite(0, 0, false, false).getInviteCode();
            }
            ((ChatActivity) mContext).applyInviteCodeToTextBox(ourInviteCode);
            return true;
        } catch(HTTP429Exception | DiscordException | MissingPermissionsException e) {
            mErrorMsg = "Failed to join guild: " + e;
            return false;
        }

    }

    protected boolean doJoinGuild(String inviteCode) {
        try {
            Invite invite = ClientHelper.client.getInviteForCode(inviteCode);
            if(invite == null) {
                mErrorMsg = "Unable to get invite from \"" + inviteCode + "\"";
                return false;
            }
            InviteResponse response = invite.accept();
            if(response.getGuildID() == null) {
                mErrorMsg = "Not a guild invite!";
                return false;
            }
            return true;
        } catch(HTTP429Exception | DiscordException e) {
            mErrorMsg = "Failed to join guild: " + e;
            return false;
        }
    }

    protected boolean doCreateGuild(String name, String region, String icon) {
        try {
            Guild guild = ClientHelper.client.createGuild(name, region, icon);
            if(guild == null) {
                mErrorMsg = "Failed to create guild";
                return false;
            }
            // For some reason, the response discord gives us has no channels, so we manually
            // ask for the full guild object here
            ClientHelper.client.refreshGuild(guild.getID());
            return true;
        } catch(HTTP429Exception | DiscordException e) {
            mErrorMsg = "Failed to create guild: " + e;
            return false;
        }
    }

    protected void onPostExecute(Boolean result) {
        try {
            switch (mParams[0]) {
                case "load-messages": postLoadMessages(result); break;
                case "change-profile": ((ProfileActivity) mContext).taskFinished(result); break;
                case "create-guild": ((EditGuildActivity) mContext).taskFinished(result); break;
            }
        } catch(Exception e) {
            mErrorMsg += " :: " + e;
        }
        if(!result) Toast.makeText(mContext, mErrorMsg, Toast.LENGTH_LONG).show();
        if(mCallback != null) mCallback.handleMessage(new android.os.Message());
    }

    private boolean postLoadMessages(Boolean result) {
        if(!result) return false;
        Channel c = ClientHelper.client.getChannelByID(mParams[1]);
        if(c == null) return true;
        MessageList m = c.getMessages();
        m.addAll(mTempMsgList.reverse());
        ((ChatMessageAdapter) ((ListView) ((ChatActivity) mContext)
                .findViewById(R.id.messageListView)).getAdapter()).notifyDataSetChanged();
        if(mParams[3] == null) {
            new NetworkTask(mContext).execute("ack-message", c.getID(), m.getLatest().getID());
        }
        return true;
    }
}
