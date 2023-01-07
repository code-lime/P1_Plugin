package p1.web;

import org.lime.system;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.libs.org.apache.commons.io.IOUtils;
import org.bukkit.craftbukkit.libs.org.apache.http.*;
import org.bukkit.craftbukkit.libs.org.apache.http.client.HttpClient;
import org.bukkit.craftbukkit.libs.org.apache.http.client.methods.*;
import org.bukkit.craftbukkit.libs.org.apache.http.entity.ByteArrayEntity;
import org.bukkit.craftbukkit.libs.org.apache.http.entity.StringEntity;
import org.bukkit.craftbukkit.libs.org.apache.http.impl.client.HttpClientBuilder;
import org.bukkit.craftbukkit.libs.org.apache.http.message.BasicStatusLine;
import org.bukkit.craftbukkit.libs.org.apache.http.params.HttpParams;
import org.bukkit.craftbukkit.libs.org.apache.http.util.EntityUtils;
import org.bukkit.scheduler.BukkitScheduler;
import lime;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;

public class WebManager {
    public enum RequestMethod {
        DELETE(HttpDelete::new),
        GET(HttpGet::new),
        PATCH(HttpPatch::new, RequestMethod::setEntry, RequestMethod::setEntry),
        POST(HttpPost::new, RequestMethod::setEntry, RequestMethod::setEntry),
        PUT(HttpPut::new, RequestMethod::setEntry, RequestMethod::setEntry);

        static <T extends HttpEntityEnclosingRequestBase>void setEntry(T base, String str) {
            base.setEntity(new ByteArrayEntity(str.getBytes(StandardCharsets.UTF_8)));
        }
        static <T extends HttpEntityEnclosingRequestBase>void setEntry(T base, byte[] str) {
            base.setEntity(new ByteArrayEntity(str));
        }

        system.Func2<String, String, HttpRequestBase> createBase;
        system.Func2<String, byte[], HttpRequestBase> createBaseBytes;
        <T extends HttpRequestBase>RequestMethod(system.Func1<String, T> createBase, system.Action2<T, String> applyBase, system.Action2<T, byte[]> applyBytes) {
            this.createBase = (url, data) -> {
                T obj = createBase.invoke(url);
                if (applyBase != null) applyBase.invoke(obj, data);
                return obj;
            };
            this.createBaseBytes = (url, data) -> {
                T obj = createBase.invoke(url);
                if (applyBytes != null) applyBytes.invoke(obj, data);
                return obj;
            };
        }
        RequestMethod(system.Func1<String, HttpRequestBase> createBase) {
            this(createBase, null, null);
        }
        public HttpRequestBase Create(String url, String data) {
            return createBase.invoke(url, data);
        }
        public HttpRequestBase Create(String url, byte[] data) {
            return createBaseBytes.invoke(url, data);
        }
    }
    public static void Enqueue(String url, String data, RequestMethod rect, system.Action2<String, Integer> callback)
    {
        Enqueue(url, data, rect, callback, null);
    }
    public static void Enqueue(String url, String data, RequestMethod rect, system.Action2<String, Integer> callback, HashMap<String, String> headers)
    {
        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTaskAsynchronously(lime._plugin, () -> {
            system.Toast2<String, Integer> html = Enqueue(url, data, rect, headers);
            scheduler.scheduleSyncDelayedTask(lime._plugin, () -> callback.invoke(html.val0, html.val1));
        });
    }
    public static void EnqueueBytes(String url, String data, RequestMethod rect, system.Action2<byte[], Integer> callback)
    {
        EnqueueBytes(url, data, rect, callback, null);
    }
    public static void EnqueueBytes(String url, String data, RequestMethod rect, system.Action2<byte[], Integer> callback, HashMap<String, String> headers)
    {
        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTaskAsynchronously(lime._plugin, () -> {
            system.Toast2<byte[], Integer> html = EnqueueBytes(url, data, rect, headers);
            scheduler.scheduleSyncDelayedTask(lime._plugin, () -> callback.invoke(html.val0, html.val1));
        });
    }
    private static String readError(HttpURLConnection connection)
    {
        try
        {
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();
        }
        catch (Exception ignore)
        {
            return null;
        }
    }

    public static system.Toast2<String, Integer> Enqueue(String url, String data, RequestMethod rect) {
        return Enqueue(url, data, rect, (HashMap<String, String>)null);
    }
    public static system.Toast2<String, Integer> Enqueue(String url, String data, RequestMethod rect, HashMap<String, String> headers) {
        HttpClient httpClient = HttpClientBuilder.create().build();
        StatusLine statusLine = new BasicStatusLine(new HttpVersion(0, 0), 0, "EMPTY");
        try {
            HttpRequestBase request = rect.Create(url, data);
            if (headers != null) headers.forEach(request::addHeader);
            HttpResponse response = httpClient.execute(request);
            statusLine =  response.getStatusLine();
            return system.toast(EntityUtils.toString(response.getEntity()), statusLine.getStatusCode());
        } catch (Exception ex) {
            return system.toast(statusLine.getReasonPhrase(), statusLine.getStatusCode());
        }
        /*HttpURLConnection connection = null;
        int code = 0;
        try {
            //Create connection
            URL _url = new URL(url);
            connection = (HttpURLConnection) _url.openConnection();
            connection.setRequestMethod(rect.name());

            connection.setUseCaches(false);
            if (headers != null) headers.forEach(connection::setRequestProperty);
            if (rect != RequestMethod.GET)
            {
                connection.setDoOutput(true);

                //Send request
                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.writeBytes(data);
                wr.close();
            }
            code = connection.getResponseCode();
            InputStream is = connection.getErrorStream();
            if (is == null) is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return new system.Toast2<>(response.toString(), code);
        } catch (Exception e) {
            lime.LogStackTrace(e);
            return new system.Toast2<>(readError(connection), code);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }*/
    }
    public static system.Toast2<byte[], Integer> EnqueueBytes(String url, String data, RequestMethod rect) {
        return EnqueueBytes(url, data, rect, (HashMap<String, String>)null);
    }
    private static system.Toast2<byte[], Integer> EnqueueBytes(String url, String data, RequestMethod rect, HashMap<String, String> headers)
    {
        HttpURLConnection connection = null;
        try {
            //Create connection
            URL _url = new URL(url);
            connection = (HttpURLConnection) _url.openConnection();
            connection.setRequestMethod(rect.name());

            connection.setUseCaches(false);
            if (rect != RequestMethod.GET)
            {
                if (headers != null) headers.forEach(connection::setRequestProperty);

                connection.setDoOutput(true);

                //Send request
                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.writeBytes(data);
                wr.close();
            }
            InputStream is = connection.getErrorStream();
            if (is == null) is = connection.getInputStream();
            return new system.Toast2<>(IOUtils.toByteArray(is), connection.getResponseCode());
        } catch (Exception e) {
            lime.LogStackTrace(e);
            return new system.Toast2<>(null, 0);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
