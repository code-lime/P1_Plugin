package org.mineskin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.bukkit.craftbukkit.libs.org.apache.http.client.methods.HttpRequestBase;
import org.bukkit.craftbukkit.libs.org.apache.http.impl.client.HttpClientBuilder;
import org.bukkit.craftbukkit.libs.org.apache.http.util.EntityUtils;
import org.mineskin.data.MineskinException;
import org.mineskin.data.Skin;
import org.mineskin.data.SkinCallback;
import p1.web.DataReader;

import java.io.File;
import java.io.FileInputStream;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkNotNull;

public class MineskinClient {

    private static final String API_BASE = "https://api.mineskin.org";
    private static final String GENERATE_BASE = API_BASE + "/generate";
    private static final String GET_BASE = API_BASE + "/get";

    private static final String ID_FORMAT = "https://api.mineskin.org/get/id/%s";
    private static final String URL_FORMAT = "https://api.mineskin.org/generate/url?url=%s&%s";
    private static final String UPLOAD_FORMAT = "https://api.mineskin.org/generate/upload?%s";
    private static final String USER_FORMAT = "https://api.mineskin.org/generate/user/%s?%s";

    private final Executor requestExecutor;
    private final String userAgent;
    private final String apiKey;

    private final JsonParser jsonParser = new JsonParser();
    private final Gson gson = new Gson();

    private long nextRequest = 0;

    @Deprecated
    public MineskinClient() {
        this.requestExecutor = Executors.newSingleThreadExecutor();
        this.userAgent = "MineSkin-JavaClient";
        this.apiKey = null;
    }

    @Deprecated
    public MineskinClient(Executor requestExecutor) {
        this.requestExecutor = checkNotNull(requestExecutor);
        this.userAgent = "MineSkin-JavaClient";
        this.apiKey = null;
    }

    public MineskinClient(String userAgent) {
        this.requestExecutor = Executors.newSingleThreadExecutor();
        this.userAgent = checkNotNull(userAgent);
        this.apiKey = null;
    }

    public MineskinClient(String userAgent, String apiKey) {
        this.requestExecutor = Executors.newSingleThreadExecutor();
        this.userAgent = checkNotNull(userAgent);
        this.apiKey = apiKey;
    }

    public MineskinClient(Executor requestExecutor, String userAgent, String apiKey) {
        this.requestExecutor = checkNotNull(requestExecutor);
        this.userAgent = checkNotNull(userAgent);
        this.apiKey = apiKey;
    }

    public MineskinClient(Executor requestExecutor, String userAgent) {
        this.requestExecutor = checkNotNull(requestExecutor);
        this.userAgent = checkNotNull(userAgent);
        this.apiKey = null;
    }

    public long getNextRequest() {
        return nextRequest;
    }

    /////

    private HttpRequestBase generateRequest(String endpoint, byte[] data) {
        HttpRequestBase base = WebManager.RequestMethod.POST.Create(GENERATE_BASE + endpoint, data);
        base.addHeader("User-Agent", userAgent);
        if (apiKey != null) base.addHeader("Authorization", "Bearer " + apiKey);
        return base;
    }
    private HttpRequestBase generateRequest(String endpoint, String data) {
        HttpRequestBase base = WebManager.RequestMethod.POST.Create(GENERATE_BASE + endpoint, data);
        base.addHeader("User-Agent", userAgent);
        if (apiKey != null) base.addHeader("Authorization", "Bearer " + apiKey);
        return base;
    }
    private HttpRequestBase getRequest(String endpoint) {
        HttpRequestBase base = WebManager.RequestMethod.GET.Create(GET_BASE + endpoint, "");
        base.addHeader("User-Agent", userAgent);
        return base;
    }


    public CompletableFuture<Skin> getId(long id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequestBase connection = getRequest("/id/" + id);
                return handleResponse(EntityUtils.toString(HttpClientBuilder.create().build().execute(connection).getEntity()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, requestExecutor);
    }

    public CompletableFuture<Skin> getUuid(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequestBase connection = getRequest("/uuid/" + uuid);
                return handleResponse(EntityUtils.toString(HttpClientBuilder.create().build().execute(connection).getEntity()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, requestExecutor);
    }

    public CompletableFuture<Skin> generateUrl(String url) {
        return generateUrl(url, SkinOptions.none());
    }

    /**
     * Generates skin data from an URL
     */
    public CompletableFuture<Skin> generateUrl(String url, SkinOptions options) {
        checkNotNull(url);
        checkNotNull(options);
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (System.currentTimeMillis() < nextRequest) {
                    long delay = (nextRequest - System.currentTimeMillis());
                    Thread.sleep(delay + 1000);
                }

                JsonObject body = options.toJson();
                body.addProperty("url", url);
                HttpRequestBase connection = generateRequest("/url", body.toString());
                connection.addHeader("Content-Type", "application/json");
                return handleResponse(EntityUtils.toString(HttpClientBuilder.create().build().execute(connection).getEntity()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, requestExecutor);
    }

    /*public CompletableFuture<Skin> generateUpload(File file) {
        return generateUpload(file, SkinOptions.none());
    }

    public CompletableFuture<Skin> generateUpload(File file, SkinOptions options) {
        checkNotNull(file);
        checkNotNull(options);
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (System.currentTimeMillis() < nextRequest) {
                    long delay = (nextRequest - System.currentTimeMillis());
                    Thread.sleep(delay + 1000);
                }

                Connection connection = generateRequest("/upload")
                        // It really doesn't like setting a content-type header here for some reason
                        .data("file", file.getName(), new FileInputStream(file));
                options.addAsData(connection);
                return handleResponse(connection.execute().body());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, requestExecutor);
    }*/

    public CompletableFuture<Skin> generateUser(UUID uuid) {
        return generateUser(uuid, SkinOptions.none());
    }

    /**
     * Loads skin data from an existing player
     */
    public CompletableFuture<Skin> generateUser(UUID uuid, SkinOptions options) {
        checkNotNull(uuid);
        checkNotNull(options);
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (System.currentTimeMillis() < nextRequest) {
                    long delay = (nextRequest - System.currentTimeMillis());
                    Thread.sleep(delay + 1000);
                }

                JsonObject body = options.toJson();
                body.addProperty("user", uuid.toString());
                HttpRequestBase connection = generateRequest("/user", body.toString());
                connection.addHeader("Content-Type", "application/json");
                return handleResponse(EntityUtils.toString(HttpClientBuilder.create().build().execute(connection).getEntity()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, requestExecutor);
    }

    Skin handleResponse(String body) throws MineskinException, JsonParseException {
        JsonObject jsonObject = gson.fromJson(body, JsonObject.class);
        if (jsonObject.has("error")) {
            throw new MineskinException(jsonObject.get("error").getAsString());
        }

        Skin skin = gson.fromJson(jsonObject, Skin.class);
        this.nextRequest = System.currentTimeMillis() + ((long) ((skin.nextRequest + 10) * 1000L));
        return skin;
    }

    ///// SkinCallback stuff below


    /*
     * ID
     */

    /**
     * Gets data for an existing Skin
     *
     * @param id       Skin-Id
     * @param callback {@link SkinCallback}
     */
    @Deprecated
    public void getSkin(int id, SkinCallback callback) {
        checkNotNull(callback);
        requestExecutor.execute(() -> {
            try {
                HttpRequestBase connection = WebManager.RequestMethod.GET.Create(String.format(ID_FORMAT, id), "");
                connection.addHeader("User-Agent", userAgent);
                String body = EntityUtils.toString(HttpClientBuilder.create().build().execute(connection).getEntity());
                handleResponse(body, callback);
            } catch (Exception e) {
                callback.exception(e);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        });
    }

    /*
     * URL
     */

    /**
     * Generates skin data from an URL (with default options)
     *
     * @param url      URL
     * @param callback {@link SkinCallback}
     * @see #generateUrl(String, SkinOptions, SkinCallback)
     */
    @Deprecated
    public void generateUrl(String url, SkinCallback callback) {
        generateUrl(url, SkinOptions.none(), callback);
    }

    /**
     * Generates skin data from an URL
     *
     * @param url      URL
     * @param options  {@link SkinOptions}
     * @param callback {@link SkinCallback}
     */
    @Deprecated
    public void generateUrl(String url, SkinOptions options, SkinCallback callback) {
        checkNotNull(url);
        checkNotNull(options);
        checkNotNull(callback);
        requestExecutor.execute(() -> {
            try {
                if (System.currentTimeMillis() < nextRequest) {
                    long delay = (nextRequest - System.currentTimeMillis());
                    callback.waiting(delay);
                    Thread.sleep(delay + 1000);
                }

                callback.uploading();

                HttpRequestBase connection = WebManager.RequestMethod.POST.Create(String.format(URL_FORMAT, url, options.toUrlParam()), "");
                connection.addHeader("User-Agent", userAgent);
                connection.addHeader("Authorization", "Bearer " + apiKey);
                String body = EntityUtils.toString(HttpClientBuilder.create().build().execute(connection).getEntity());
                handleResponse(body, callback);
            } catch (Exception e) {
                callback.exception(e);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        });
    }

    /*
     * Upload
     */

    /*
    @Deprecated
    public void generateUpload(File file, SkinCallback callback) {
        generateUpload(file, SkinOptions.none(), callback);
    }

    @Deprecated
    public void generateUpload(File file, SkinOptions options, SkinCallback callback) {
        checkNotNull(file);
        checkNotNull(options);
        checkNotNull(callback);
        requestExecutor.execute(() -> {
            try {
                if (System.currentTimeMillis() < nextRequest) {
                    long delay = (nextRequest - System.currentTimeMillis());
                    callback.waiting(delay);
                    Thread.sleep(delay + 1000);
                }

                callback.uploading();

                Connection connection = Jsoup
                        .connect(String.format(UPLOAD_FORMAT, options.toUrlParam()))
                        .userAgent(userAgent)
                        .method(Connection.Method.POST)
                        .data("file", file.getName(), new FileInputStream(file))
                        .ignoreContentType(true)
                        .ignoreHttpErrors(true)
                        .timeout(40000);
                if (apiKey != null) {
                    connection.header("Authorization", "Bearer " + apiKey);
                }
                String body = connection.execute().body();
                handleResponse(body, callback);
            } catch (Exception e) {
                callback.exception(e);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        });
    }
*/
    /*
     * User
     */

    /**
     * Loads skin data from an existing player (with default options)
     *
     * @param uuid     {@link UUID} of the player
     * @param callback {@link SkinCallback}
     */
    @Deprecated
    public void generateUser(UUID uuid, SkinCallback callback) {
        generateUser(uuid, SkinOptions.none(), callback);
    }

    /**
     * Loads skin data from an existing player
     *
     * @param uuid     {@link UUID} of the player
     * @param options  {@link SkinOptions}
     * @param callback {@link SkinCallback}
     */
    @Deprecated
    public void generateUser(UUID uuid, SkinOptions options, SkinCallback callback) {
        checkNotNull(uuid);
        checkNotNull(options);
        checkNotNull(callback);
        requestExecutor.execute(() -> {
            try {
                if (System.currentTimeMillis() < nextRequest) {
                    long delay = (nextRequest - System.currentTimeMillis());
                    callback.waiting(delay);
                    Thread.sleep(delay + 1000);
                }

                callback.uploading();

                HttpRequestBase connection = WebManager.RequestMethod.GET.Create(String.format(USER_FORMAT, uuid.toString(), options.toUrlParam()), "");
                connection.addHeader("User-Agent", userAgent);
                connection.addHeader("Authorization", "Bearer " + apiKey);
                String body = EntityUtils.toString(HttpClientBuilder.create().build().execute(connection).getEntity());
                handleResponse(body, callback);
            } catch (Exception e) {
                callback.exception(e);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        });
    }

    @Deprecated
    void handleResponse(String body, SkinCallback callback) {
        try {
            JsonObject jsonObject = jsonParser.parse(body).getAsJsonObject();
            if (jsonObject.has("error")) {
                callback.error(jsonObject.get("error").getAsString());
                return;
            }

            Skin skin = gson.fromJson(jsonObject, Skin.class);
            this.nextRequest = System.currentTimeMillis() + ((long) ((skin.nextRequest + 10) * 1000L));
            callback.done(skin);
        } catch (JsonParseException e) {
            callback.parseException(e, body);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

}
