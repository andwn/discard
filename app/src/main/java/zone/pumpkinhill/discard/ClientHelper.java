package zone.pumpkinhill.discard;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.HashMap;

import zone.pumpkinhill.discord4droid.api.DiscordClient;
import zone.pumpkinhill.discord4droid.api.EventDispatcher;
import zone.pumpkinhill.discord4droid.handle.obj.Channel;
import zone.pumpkinhill.discord4droid.handle.obj.User;
import zone.pumpkinhill.discord4droid.util.DiscordException;
import zone.pumpkinhill.discord4droid.util.HTTP429Exception;

public class ClientHelper {
    public static DiscordClient client = null;
    private static EventDispatcher mDispatcher = null;
    private static ArrayList<Object> mSubscribers = new ArrayList<>();
    private static HashMap<String, Bitmap> mAvatarCache = new HashMap<>();
    private static HashMap<String, Bitmap> mImageCache = new HashMap<>();
    private static Channel mActiveChannel = null;

    // Login/Logout
    public static void login(String email, String password, String server) throws DiscordException {
        logout();
        client = new DiscordClient(email, password, server);
        client.login();
        initDispatcher();
    }
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
    public static boolean isReady() {
        return client != null && client.isReady();
    }
    public static void abandonClient() {
        if(mDispatcher != null) killDispatcher();
        client = null;
        mActiveChannel = null;
    }

    // Events
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
        if(mDispatcher != null) mDispatcher.unregisterListener(subscriber);
    }

    // Avatars
    public static Bitmap getAvatarFromCache(String url) {
        return mAvatarCache.get(url);
    }
    public static void addAvatarToCache(String url, Bitmap avatar) {
        mAvatarCache.put(url, avatar);
    }
    // Images
    public static Bitmap getImageFromCache(String url) {
        return mImageCache.get(url);
    }
    public static void addImageToCache(String url, Bitmap avatar) {
        mImageCache.put(url, avatar);
    }
    public static void purgeImageCache() {
        mImageCache.clear();
    }

    // User/Channel
    public static User ourUser() {
        return client.getOurUser();
    }
    public static Channel getActiveChannel() {
        return mActiveChannel;
    }
    public static void setActiveChannel(Channel channel) {
        mActiveChannel = channel;
    }
}
