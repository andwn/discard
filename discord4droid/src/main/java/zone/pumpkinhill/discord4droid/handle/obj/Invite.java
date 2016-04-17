package zone.pumpkinhill.discord4droid.handle.obj;

import com.google.gson.Gson;

import zone.pumpkinhill.discord4droid.api.DiscordClient;
import zone.pumpkinhill.discord4droid.api.Endpoints;
import zone.pumpkinhill.discord4droid.api.Requests;
import zone.pumpkinhill.discord4droid.json.responses.InviteJSONResponse;
import zone.pumpkinhill.discord4droid.util.DiscordException;
import zone.pumpkinhill.discord4droid.util.HTTP429Exception;
import zone.pumpkinhill.http.message.BasicNameValuePair;

/**
 * Represents an invite into a channel.
 */
public class Invite {
    /**
     * An invite code, AKA an invite URL minus the https://discord.gg/
     */
    protected final String inviteCode;

    /**
     * The human-readable version of the invite code, if available.
     */
    protected final String xkcdPass;

    /**
     * The client that created this object.
     */
    protected final DiscordClient client;

    public Invite(DiscordClient client, String inviteCode, String xkcdPass) {
        this.client = client;
        this.inviteCode = inviteCode;
        this.xkcdPass = xkcdPass;
    }

    /**
     * @return The invite code
     */
    public String getInviteCode() {
        return inviteCode;
    }

    /**
     * @return The xkcd pass, this is null if it doesn't exist!
     */
    public String getXkcdPass() {
        return xkcdPass;
    }

    /**
     * Accepts the invite and returns relevant information,
     * such as the Guild ID and name, and the channel the invite
     * was created from.
     *
     * @return Information about the invite.
     *
     * @throws DiscordException
     * @throws HTTP429Exception
     */
    public InviteResponse accept() throws DiscordException, HTTP429Exception {
        if (client.isReady()) {
            String response = Requests.POST.makeRequest(client.getURL() + Endpoints.INVITE+inviteCode,
                    new BasicNameValuePair("authorization", client.getToken()));

            InviteJSONResponse inviteResponse = new Gson().fromJson(response, InviteJSONResponse.class);

            return new InviteResponse(inviteResponse.guild.id, inviteResponse.guild.name,
                    inviteResponse.channel.id, inviteResponse.channel.name);
        } else {
            System.out.println("Bot has not signed in yet!");
            return null;
        }
    }

    /**
     * Gains the same information as accepting,
     * but doesn't actually accept the invite.
     *
     * @return an InviteResponse containing the invite's details.
     *
     * @throws DiscordException
     * @throws HTTP429Exception
     */
    public InviteResponse details() throws DiscordException, HTTP429Exception {
        if (client.isReady()) {
            String response = Requests.GET.makeRequest(client.getURL() + Endpoints.INVITE+inviteCode,
                    new BasicNameValuePair("authorization", client.getToken()));

            InviteJSONResponse inviteResponse = new Gson().fromJson(response, InviteJSONResponse.class);

            return new InviteResponse(inviteResponse.guild.id, inviteResponse.guild.name,
                    inviteResponse.channel.id, inviteResponse.channel.name);
        } else {
            System.out.println("Bot has not signed in yet!");
            return null;
        }
    }

    /**
     * Attempts to delete the invite this object represents.
     *
     * @throws HTTP429Exception
     * @throws DiscordException
     */
    public void delete() throws HTTP429Exception, DiscordException {
        Requests.DELETE.makeRequest(client.getURL() + Endpoints.INVITE+inviteCode,
                new BasicNameValuePair("authorization", client.getToken()));
    }

    /**
     * This gets the client that this object is tied to.
     *
     * @return The client.
     */
    public DiscordClient getClient() {
        return client;
    }

    @Override
    public boolean equals(Object other) {
        return this.getClass().isAssignableFrom(other.getClass()) && ((Invite) other).getInviteCode().equals(getInviteCode());
    }

    @Override
    public String toString() {
        return inviteCode;
    }
}
