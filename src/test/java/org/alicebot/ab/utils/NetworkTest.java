package org.alicebot.ab.utils;

import okhttp3.HttpUrl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NetworkTest {

    @Test
    public void pandoraBotUrl() {
        String host = "thisisthehost";
        String botId = "thisisthebotid";
        String custId = "thisisthecustid";
        String input = "fancy input blasé";

        HttpUrl actual = new NetworkUtils().pandoraBotUrl(host, botId, custId, input);
        System.out.println(actual);
        assertEquals(host, actual.host());
        assertEquals(botId, actual.queryParameter("botid"));
        assertEquals(custId, actual.queryParameter("custid"));
        assertEquals(input, actual.queryParameter("input"));
    }

    @Test
    public void noCustId() {
        HttpUrl url = new NetworkUtils().pandoraBotUrl("host", "botId", "0", "input");
        assertNull(url.queryParameter("custid"));
    }
}
