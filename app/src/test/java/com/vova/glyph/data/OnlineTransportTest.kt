package com.vova.glyph.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class OnlineTransportTest {
    @Test
    fun `envelope wraps the target url`() {
        val target = "${OnlineEndpoints.JIOSAAVN_API}?__call=search.getResults&q=gunna"
        val envelope = JSONObject(OnlineProxy.envelope(target))

        assertEquals(target, envelope.getString("url"))
        assertEquals("GET", envelope.getString("method"))
        assertEquals(
            OnlineEndpoints.USER_AGENT,
            envelope.getJSONObject("headers").getString("User-Agent"),
        )
    }

    @Test
    fun `only catalog targets are allowed`() {
        assertTrue(OnlineProxy.isAllowed("${OnlineEndpoints.JIOSAAVN_API}?q=gunna"))
        assertFalse(OnlineProxy.isAllowed("https://example.com/api.php"))
        assertFalse(OnlineProxy.isAllowed("http://www.jiosaavn.com/api.php"))
    }

    @Test
    fun `unwrap returns the proxied body`() {
        val body = """{"total":1296}"""
        val payload = JSONObject()
            .put("status", 200)
            .put("body", body)
            .toString()

        assertEquals(body, OnlineProxy.unwrap(payload))
    }

    @Test(expected = IOException::class)
    fun `unwrap fails on non 200 status`() {
        OnlineProxy.unwrap(JSONObject().put("status", 503).put("body", "").toString())
    }

    @Test(expected = IOException::class)
    fun `unwrap fails on empty body`() {
        OnlineProxy.unwrap(JSONObject().put("status", 200).put("body", "").toString())
    }

    @Test(expected = IOException::class)
    fun `unwrap fails on malformed payload`() {
        OnlineProxy.unwrap("<html>Bad gateway</html>")
    }
}
