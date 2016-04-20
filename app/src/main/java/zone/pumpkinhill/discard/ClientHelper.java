package zone.pumpkinhill.discard;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.HashMap;

import zone.pumpkinhill.discord4droid.api.DiscordClient;
import zone.pumpkinhill.discord4droid.api.EventDispatcher;
import zone.pumpkinhill.discord4droid.handle.obj.User;
import zone.pumpkinhill.discord4droid.util.DiscordException;
import zone.pumpkinhill.discord4droid.util.HTTP429Exception;

public class ClientHelper {
    public static DiscordClient client = null;
    private static EventDispatcher mDispatcher = null;
    private static ArrayList<Object> mSubscribers = new ArrayList<>();
    private static HashMap<String, Bitmap> mImageCache = new HashMap<>();

    /**
     * Attempts to login to discord with given credentials
     * @throws DiscordException if login fails
     */
    public static void login(String email, String password, String server) throws DiscordException {
        logout();
        client = new DiscordClient(email, password, server);
        client.login();
        initDispatcher();
    }
    public static boolean isLoggedIn() {
        return client != null;
    }
    public static User ourUser() {
        return client.getOurUser();
    }

    /**
     * Logout and delete the mClient
     */
    public static void logout() {
        if(client != null) {
            try {
                client.logout();
                abandonClient();
            } catch(DiscordException | HTTP429Exception e) {
                abandonClient();
            }
        }
    }
    public static void abandonClient() {
        if(mDispatcher != null) killDispatcher();
        client = null;
    }

    /**
     * Events
     */
    private static void initDispatcher() {
        mDispatcher = client.getDispatcher();
        for(Object o : mSubscribers) mDispatcher.registerListener(o);
    }
    private static void killDispatcher() {
        for(Object o : mSubscribers) mDispatcher.unregisterListener(o);
        mDispatcher = null;
    }
    public static void subscribe(Object subscriber) {
        if(subscriber == null) return;
        mSubscribers.add(subscriber);
        if(mDispatcher != null) mDispatcher.registerListener(subscriber);
    }
    public static void unsubscribe(Object subscriber) {
        if(subscriber == null) return;
        mSubscribers.remove(subscriber);
        mDispatcher.unregisterListener(subscriber);
    }

    /**
     * Avatars
     */
    public static Bitmap getImageFromCache(String url) {
        return mImageCache.get(url);
    }
    public static void addImageToCache(String url, Bitmap avatar) {
        mImageCache.put(url, avatar);
    }
}
