package com.podbelly.core.network.opml

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlSerializer
import java.io.StringReader
import java.io.StringWriter

data class OpmlFeed(
    val title: String,
    val feedUrl: String
)

object OpmlHandler {

    /**
     * Parses an OPML XML document and extracts podcast feed information
     * from <outline> elements that have type="rss" or an xmlUrl attribute.
     *
     * Handles both flat and nested outline structures.
     */
    fun parseOpml(xmlContent: String): List<OpmlFeed> {
        val feeds = mutableListOf<OpmlFeed>()

        val factory = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = false
        }
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xmlContent))

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name.equals("outline", ignoreCase = true)) {
                val xmlUrl = parser.getAttributeValue(null, "xmlUrl")
                    ?: parser.getAttributeValue(null, "xmlurl")
                val type = parser.getAttributeValue(null, "type")

                // An outline is a feed if it has an xmlUrl, or if type is "rss"
                if (!xmlUrl.isNullOrBlank()) {
                    val title = parser.getAttributeValue(null, "title")
                        ?: parser.getAttributeValue(null, "text")
                        ?: ""
                    feeds.add(
                        OpmlFeed(
                            title = title.trim(),
                            feedUrl = xmlUrl.trim()
                        )
                    )
                } else if (type != null && type.equals("rss", ignoreCase = true)) {
                    // type="rss" but missing xmlUrl -- try htmlUrl or url as fallback
                    val fallbackUrl = parser.getAttributeValue(null, "htmlUrl")
                        ?: parser.getAttributeValue(null, "url")
                    if (!fallbackUrl.isNullOrBlank()) {
                        val title = parser.getAttributeValue(null, "title")
                            ?: parser.getAttributeValue(null, "text")
                            ?: ""
                        feeds.add(
                            OpmlFeed(
                                title = title.trim(),
                                feedUrl = fallbackUrl.trim()
                            )
                        )
                    }
                }
            }
            eventType = parser.next()
        }

        return feeds
    }

    /**
     * Generates a valid OPML 2.0 XML document from a list of [OpmlFeed] entries.
     */
    fun generateOpml(feeds: List<OpmlFeed>): String {
        val writer = StringWriter()

        val factory = XmlPullParserFactory.newInstance()
        val serializer: XmlSerializer = factory.newSerializer()
        serializer.setOutput(writer)

        // XML declaration
        serializer.startDocument("UTF-8", true)
        serializer.text("\n")

        // <opml version="2.0">
        serializer.startTag(null, "opml")
        serializer.attribute(null, "version", "2.0")
        serializer.text("\n  ")

        // <head>
        serializer.startTag(null, "head")
        serializer.text("\n    ")
        serializer.startTag(null, "title")
        serializer.text("Podbelly Podcast Subscriptions")
        serializer.endTag(null, "title")
        serializer.text("\n  ")
        serializer.endTag(null, "head")
        serializer.text("\n  ")

        // <body>
        serializer.startTag(null, "body")

        for (feed in feeds) {
            serializer.text("\n    ")
            serializer.startTag(null, "outline")
            serializer.attribute(null, "type", "rss")
            serializer.attribute(null, "text", feed.title)
            serializer.attribute(null, "title", feed.title)
            serializer.attribute(null, "xmlUrl", feed.feedUrl)
            serializer.endTag(null, "outline")
        }

        serializer.text("\n  ")
        serializer.endTag(null, "body")
        serializer.text("\n")

        serializer.endTag(null, "opml")
        serializer.text("\n")
        serializer.endDocument()

        return writer.toString()
    }
}
