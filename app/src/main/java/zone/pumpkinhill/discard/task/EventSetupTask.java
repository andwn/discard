package zone.pumpkinhill.discard.task;

import android.os.AsyncTask;

import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discord4droid.api.DiscordClient;
import zone.pumpkinhill.discord4droid.handle.events.ReadyEvent;
import zone.pumpkinhill.discord4droid.handle.obj.Channel;
import zone.pumpkinhill.discord4droid.util.MessageList;

public class EventSetupTask extends AsyncTask<Void, Void, Boolean> {

    @Override
    protected Boolean doInBackground(Void... params) {
        DiscordClient client = ClientHelper.client;
        for(Channel c : client.getChannels(true)) {
            client.getDispatcher().registerListener(
                    new MessageList.MessageListEventListener(c.getMessages()));
        }
        return true;
    }
}
