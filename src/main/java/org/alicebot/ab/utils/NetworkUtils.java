package org.alicebot.ab.utils;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okio.BufferedSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public final class NetworkUtils {

    private static final Logger logger = LoggerFactory.getLogger(NetworkUtils.class);

    private final OkHttpClient okHttpClient = new OkHttpClient();

    public String localIPAddress() {
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface intf = en.nextElement();
                Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
                while (enumIpAddr.hasMoreElements()) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        String ipAddress = inetAddress.getHostAddress();
                        int p = ipAddress.indexOf('%');
                        if (p > 0) { ipAddress = ipAddress.substring(0, p); }
                        return ipAddress;
                    }
                }
            }
        } catch (SocketException ex) {
            logger.error("localIPAddress error", ex);
        }
        return "127.0.0.1";
    }

    public String responseContent(HttpUrl url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        try (BufferedSource body = okHttpClient.newCall(request).execute().body().source()) {
            return body.readUtf8();
        }
    }

    public HttpUrl pandoraBotUrl(String host, String botid, String custid, String input) {
        HttpUrl.Builder builder = new HttpUrl.Builder()
            .scheme("http")
            .host(host)
            .addPathSegment("pandora").addPathSegment("talk-xml")
            .addQueryParameter("botid", botid)
            .addQueryParameter("input", input);
        if (!"0".equals(custid)) {
            builder.addQueryParameter("custid", custid);
        }
        return builder.build();
    }

}
