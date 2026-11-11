package com.vova.glyph.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JioSaavnTest {
    private val encrypted =
        "ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyEiudYxlw1gkEb4sbqQ6T/DOF6N6mEuM9SKcWOIFzDJLSechl/5QWkhw7tS9a8Gtq"

    private val json = """
        {
          "total": 3,
          "results": [
            {
              "id": "uPFWTGGp",
              "title": "fukumean",
              "subtitle": "Gunna - a Gift &amp; a Curse",
              "image": "https://c.saavncdn.com/485/a-Gift-a-Curse-English-2023-20230616014652-150x150.jpg",
              "type": "song",
              "more_info": {
                "album": "a Gift &amp; a Curse",
                "duration": "125",
                "encrypted_media_url": "$encrypted",
                "artistMap": {
                  "primary_artists": [{ "id": "873373", "name": "Gunna" }]
                }
              }
            },
            {
              "id": "uPFWTGGp",
              "title": "fukumean",
              "subtitle": "Gunna - a Gift &amp; a Curse",
              "more_info": { "album": "a Gift &amp; a Curse", "duration": "125" }
            },
            {
              "id": "noStream",
              "title": "No Stream",
              "subtitle": "Somebody - Some Album",
              "image": "",
              "more_info": {
                "album": "Some Album",
                "duration": "90",
                "artistMap": { "primary_artists": [{ "name": "Somebody" }] }
              }
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `decrypt returns cdn url`() {
        assertEquals(
            "https://aac.saavncdn.com/485/587c3ec6aa405555cc788cf20b6755b7_96.mp4",
            JioSaavnParser.decrypt(encrypted),
        )
    }

    @Test
    fun `decrypt rejects garbage`() {
        assertNull(JioSaavnParser.decrypt("not-a-base64-payload"))
    }

    @Test
    fun `qualityUrl upgrades 96 kbps to 320`() {
        assertEquals(
            "https://aac.saavncdn.com/485/587c3ec6aa405555cc788cf20b6755b7_320.mp4",
            JioSaavnParser.qualityUrl(
                "https://aac.saavncdn.com/485/587c3ec6aa405555cc788cf20b6755b7_96.mp4"
            ),
        )
        assertEquals(
            "https://aac.saavncdn.com/485/587c3ec6aa405555cc788cf20b6755b7_320.mp4",
            JioSaavnParser.qualityUrl(
                "https://aac.saavncdn.com/485/587c3ec6aa405555cc788cf20b6755b7_320.mp4"
            ),
        )
    }

    @Test
    fun `coverUrl asks for the large image`() {
        assertEquals(
            "https://c.saavncdn.com/485/cover-500x500.jpg",
            JioSaavnParser.coverUrl("https://c.saavncdn.com/485/cover-150x150.jpg"),
        )
        assertNull(JioSaavnParser.coverUrl(""))
        assertNull(JioSaavnParser.coverUrl(null))
    }

    @Test
    fun `decodeEntities unescapes names`() {
        assertEquals(
            "Gunna & Wizkid - \"Views\" <2023>",
            JioSaavnParser.decodeEntities("Gunna &amp; Wizkid - &quot;Views&quot; &lt;2023&gt;"),
        )
        assertEquals("It's", JioSaavnParser.decodeEntities("It&#39;s"))
        assertEquals("A B", JioSaavnParser.decodeEntities("A&#32;B"))
        assertEquals("A\u2019B", JioSaavnParser.decodeEntities("A&#x2019;B"))
        assertEquals("Plain text", JioSaavnParser.decodeEntities("Plain text"))
        assertEquals("&unknown;", JioSaavnParser.decodeEntities("&unknown;"))
    }

    @Test
    fun `parseSongs maps catalog fields`() {
        val tracks = JioSaavnParser.parseSongs(json, 30)

        assertEquals(2, tracks.size)
        val first = tracks.first()
        assertEquals("uPFWTGGp", first.id)
        assertEquals("fukumean", first.title)
        assertEquals("Gunna", first.artist)
        assertEquals("a Gift & a Curse", first.album)
        assertEquals(125_000L, first.durationMs)
        assertEquals(
            "https://c.saavncdn.com/485/a-Gift-a-Curse-English-2023-20230616014652-500x500.jpg",
            first.coverUrl,
        )
        assertTrue(first.streamUrl!!.endsWith("_320.mp4"))
    }

    @Test
    fun `parseSongs keeps tracks without media url`() {
        val track = JioSaavnParser.parseSongs(json, 30).last()

        assertEquals("noStream", track.id)
        assertEquals("Somebody", track.artist)
        assertEquals("Some Album", track.album)
        assertNull(track.streamUrl)
        assertNull(track.coverUrl)
    }

    @Test
    fun `parseSongs ignores duplicates and respects limit`() {
        assertEquals(2, JioSaavnParser.parseSongs(json, 30).size)
        assertEquals(1, JioSaavnParser.parseSongs(json, 1).size)
    }

    @Test
    fun `parseSongs survives broken payloads`() {
        assertTrue(JioSaavnParser.parseSongs("not json").isEmpty())
        assertTrue(JioSaavnParser.parseSongs("{}").isEmpty())
        assertTrue(JioSaavnParser.parseSongs("""{"results":[]}""").isEmpty())
    }

    @Test
    fun `online track becomes a playable track`() {
        val track = JioSaavnParser.parseSongs(json, 30).first().toTrack()

        assertEquals(Track.SOURCE_JIOSAAVN, track.source)
        assertEquals("uPFWTGGp", track.sourceId)
        assertTrue(track.isOnline)
        assertTrue(track.uri.startsWith("https://"))
        assertEquals("Gunna", track.effectiveArtist)
        assertEquals("fukumean", track.effectiveTitle)
    }

    @Test
    fun `searchUrl carries catalog parameters`() {
        val url = JioSaavnParser.searchUrl("gunna fukumean", 15).toString()

        assertTrue(url.startsWith(OnlineEndpoints.JIOSAAVN_API))
        assertTrue(url.contains("__call=search.getResults"))
        assertTrue(url.contains("q=gunna%20fukumean"))
        assertTrue(url.contains("_format=json"))
        assertTrue(url.contains("n=15"))
    }
}
