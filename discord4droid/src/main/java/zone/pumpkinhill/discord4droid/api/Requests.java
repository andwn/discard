package zone.pumpkinhill.discord4droid.api;

import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import zone.pumpkinhill.discord4droid.Constants;
import zone.pumpkinhill.discord4droid.json.responses.RateLimitResponse;
import zone.pumpkinhill.discord4droid.util.DiscordException;
import zone.pumpkinhill.discord4droid.util.HTTP429Exception;
import zone.pumpkinhill.http.HttpEntity;
import zone.pumpkinhill.http.client.methods.CloseableHttpResponse;
import zone.pumpkinhill.http.client.methods.HttpDelete;
import zone.pumpkinhill.http.client.methods.HttpEntityEnclosingRequestBase;
import zone.pumpkinhill.http.client.methods.HttpGet;
import zone.pumpkinhill.http.client.methods.HttpPatch;
import zone.pumpkinhill.http.client.methods.HttpPost;
import zone.pumpkinhill.http.client.methods.HttpPut;
import zone.pumpkinhill.http.client.methods.HttpUriRequest;
import zone.pumpkinhill.http.impl.client.CloseableHttpClient;
import zone.pumpkinhill.http.impl.client.HttpClients;
import zone.pumpkinhill.http.message.BasicNameValuePair;
import zone.pumpkinhill.http.util.EntityUtils;

/**
 * Represents request types to be sent.
 */
public enum Requests {

    /**
     * Used to send POST Requests
     */
    POST(HttpPost.class),
    /**
     * Used to send GET requests
     */
    GET(HttpGet.class),
    /**
     * Used to send DELETE requests
     */
    DELETE(HttpDelete.class),
    /**
     * Used to send PATCH requests
     */
    PATCH(HttpPatch.class),
    /**
     * Used to send PUT requests
     */
    PUT(HttpPut.class);

    private final static String TAG = Requests.class.getCanonicalName();

    //Same as HttpClients.createDefault() but with the proper user-agent
    static final CloseableHttpClient CLIENT = HttpClients.custom().setUserAgent(Constants.USER_AGENT).build();

    final Class<? extends HttpUriRequest> requestClass;

    Requests(Class<? extends HttpUriRequest> clazz) {
        this.requestClass = clazz;
    }

    /**
     * Gets the HttpREQUEST.class represented by the enum.
     *
     * @return The Http request class.
     */
    public Class<? extends HttpUriRequest> getRequestClass() {
        return requestClass;
    }

    /**
     * Makes a request.
     *
     * @param url The url to make the request to.
     * @param headers The headers to include in the request.
     * @return The result (if any) returned by the request.
     *
     * @throws HTTP429Exception
     * @throws DiscordException
     */
    public String makeRequest(String url, BasicNameValuePair... headers) throws HTTP429Exception, DiscordException {
        try {
            HttpUriRequest request = this.requestClass.getConstructor(String.class).newInstance(url);
            for (BasicNameValuePair header : headers) {
                request.addHeader(header.getName(), header.getValue());
            }

            try {
                CloseableHttpResponse response = CLIENT.execute(request);
                int responseCode = response.getStatusLine().getStatusCode();

                String message = "";
                if (response.getEntity() != null)
                    message = EntityUtils.toString(response.getEntity());

                if (responseCode == 404) {
                    Log.e(TAG, "Received 404 error, please notify the developer and include the URL ("+url+")");
                    return null;
                } else if (responseCode == 403) {
                    Log.e(TAG, "Received 403 forbidden error for url "+url+". If you believe this is a Discord4J error, report this!");
                    return null;
                } else if (responseCode == 204) { //There is a no content response when deleting messages
                    return null;
                } else if (responseCode < 200 || responseCode > 299) {
                    throw new DiscordException("Error on request to "+url+". Received response code "+responseCode+". With response text: "+message);
                }

                JsonParser parser = new JsonParser();
                JsonElement element;
                try {
                    element = parser.parse(message);
                } catch (JsonParseException e) {
                    return null;
                }

                if (responseCode == 429) {
                    throw new HTTP429Exception(DiscordUtils.GSON.fromJson(element, RateLimitResponse.class));
                }

                if (element.isJsonObject() && parser.parse(message).getAsJsonObject().has("message"))
                    throw new DiscordException(element.getAsJsonObject().get("message").getAsString());

                return message;
            } catch (IOException e) {
                Log.e(TAG, "Error making request: " + e);
            }
        } catch(NoSuchMethodException e) {
            Log.e(TAG, "Error making request: " + e);
        } catch(IllegalAccessException e) {
            Log.e(TAG, "Error making request: " + e);
        } catch(InvocationTargetException e) {
            Log.e(TAG, "Error making request: " + e);
        } catch(InstantiationException e) {
            Log.e(TAG, "Error making request: " + e);
        }
        return null;
    }

    /**
     * Makes a request.
     *
     * @param entity Any data to send with the request.
     * @param url The url to make the request to.
     * @param headers The headers to include in the request.
     * @return The result (if any) returned by the request.
     *
     * @throws HTTP429Exception
     * @throws DiscordException
     */
    public String makeRequest(String url, HttpEntity entity, BasicNameValuePair... headers) throws HTTP429Exception, DiscordException {
        try {
            if (HttpEntityEnclosingRequestBase.class.isAssignableFrom(this.requestClass)) {
                HttpEntityEnclosingRequestBase request = (HttpEntityEnclosingRequestBase)
                        this.requestClass.getConstructor(String.class).newInstance(url);
                for (BasicNameValuePair header : headers) {
                    request.addHeader(header.getName(), header.getValue());
                }
                request.setEntity(entity);

                try {
                    CloseableHttpResponse response = CLIENT.execute(request);

                    int responseCode = response.getStatusLine().getStatusCode();

                    String message = "";
                    if (response.getEntity() != null)
                        message = EntityUtils.toString(response.getEntity());

                    if (responseCode == 404) {
                        Log.e(TAG, "Received 404 error, please notify the developer and include the URL ("+url+")");
                        return null;
                    } else if (responseCode == 403) {
                        Log.e(TAG, "Received 403 forbidden error for url ("+url+"). If you believe this is a Discord4J error, report this!");
                        return null;
                    } else if (responseCode == 204) { //There is a no content response when deleting messages
                        return null;
                    } else if (responseCode < 200 || responseCode > 299) {
                        throw new DiscordException("Error on request to "+url+". Received response code "+responseCode+". With response text: "+message);
                    }

                    JsonParser parser = new JsonParser();
                    JsonElement element;
                    try {
                        element = parser.parse(message);
                    } catch (JsonParseException e) {
                        return null;
                    }

                    if (responseCode == 429) {
                        throw new HTTP429Exception(DiscordUtils.GSON.fromJson(element, RateLimitResponse.class));
                    }

                    if (element.isJsonObject() && parser.parse(message).getAsJsonObject().has("message"))
                        throw new DiscordException(element.getAsJsonObject().get("message").getAsString());

                    return message;
                } catch (IOException e) {
                    Log.e(TAG, "Error making request: " + e);
                }
            } else {
                Log.e(TAG, "Tried to attach HTTP entity to invalid type! ("
                                + this.requestClass.getSimpleName()+")");
            }
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "Error making request: " + e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Error making request: " + e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "Error making request: " + e);
        } catch (InstantiationException e) {
            Log.e(TAG, "Error making request: " + e);
        }
        return null;
    }
}
