package com.wire.kalium

import java.util.UUID
import com.waz.model.Messages.GenericMessage
import com.waz.model.Messages.Asset.ImageMetaData
import com.waz.model.Messages.Asset.Original
import com.waz.model.Messages.Asset.RemoteData
import com.google.protobuf.ByteString
import com.waz.model.Messages.Asset.AudioMetaData
import com.wire.kalium.models.MessageBase
import com.wire.kalium.models.LinkPreviewMessage
import com.wire.kalium.models.AudioPreviewMessage
import com.wire.kalium.backend.GenericMessageProcessor
import com.waz.model.Messages
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.Date
import java.util.Random

class GenericMessageProcessorTest {
    @Test
    fun testLinkPreview() {
        val handler = MessageHandler()
        val processor = GenericMessageProcessor(null, handler)
        val eventId = UUID.randomUUID()
        val messageId = UUID.randomUUID()
        val from = UUID.randomUUID()
        val convId = UUID.randomUUID()
        val sender = UUID.randomUUID().toString()
        val time = Date().toString()
        val image = ImageMetaData.newBuilder()
            .setHeight(HEIGHT)
            .setWidth(WIDTH)
        val original = Original.newBuilder()
            .setSize(SIZE.toLong())
            .setMimeType(MIME_TYPE)
            .setImage(image)
        val uploaded = RemoteData.newBuilder()
            .setAssetId(ASSET_KEY)
            .setAssetToken(ASSET_TOKEN)
            .setOtrKey(ByteString.EMPTY)
            .setSha256(ByteString.EMPTY)
        val asset = Messages.Asset.newBuilder()
            .setOriginal(original)
            .setUploaded(uploaded)
        val linkPreview = Messages.LinkPreview.newBuilder()
            .setTitle(TITLE)
            .setSummary(SUMMARY)
            .setUrl(URL)
            .setUrlOffset(URL_OFFSET)
            .setImage(asset)
        val text = Messages.Text.newBuilder()
            .setContent(CONTENT)
            .addLinkPreview(linkPreview)
        val builder = GenericMessage.newBuilder()
            .setMessageId(messageId.toString())
            .setText(text)
        val msgBase = MessageBase(eventId, messageId, convId, sender, from, time)
        processor.process(msgBase, builder.build())
    }

    @Test
    fun testAudioOrigin() {
        val handler = MessageHandler()
        val processor = GenericMessageProcessor(null, handler)
        val eventId = UUID.randomUUID()
        val messageId = UUID.randomUUID()
        val from = UUID.randomUUID()
        val convId = UUID.randomUUID()
        val sender = UUID.randomUUID().toString()
        val time = Date().toString()
        val levels = ByteArray(100)
        Random().nextBytes(levels)
        val audioMeta = AudioMetaData.newBuilder()
            .setDurationInMillis(DURATION.toLong())
            .setNormalizedLoudness(ByteString.copyFrom(levels))
        val original = Original.newBuilder()
            .setSize(SIZE.toLong())
            .setName(NAME)
            .setMimeType(AUDIO_MIME_TYPE)
            .setAudio(audioMeta)
        val asset = Messages.Asset.newBuilder()
            .setOriginal(original)
        val builder = GenericMessage.newBuilder()
            .setMessageId(messageId.toString())
            .setAsset(asset)
        val msgBase = MessageBase(eventId, messageId, convId, sender, from, time)
        processor.process(msgBase, builder.build())
    }

    @Test
    fun testAudioUploaded() {
        val handler = MessageHandler()
        val processor = GenericMessageProcessor(null, handler)
        val eventId = UUID.randomUUID()
        val messageId = UUID.randomUUID()
        val from = UUID.randomUUID()
        val convId = UUID.randomUUID()
        val sender = UUID.randomUUID().toString()
        val time = Date().toString()
        val levels = ByteArray(100)
        Random().nextBytes(levels)
        val uploaded = RemoteData.newBuilder()
            .setAssetId(ASSET_KEY)
            .setAssetToken(ASSET_TOKEN)
            .setOtrKey(ByteString.EMPTY)
            .setSha256(ByteString.EMPTY)
        val asset = Messages.Asset.newBuilder()
            .setUploaded(uploaded)
        val builder = GenericMessage.newBuilder()
            .setMessageId(messageId.toString())
            .setAsset(asset)
        val msgBase = MessageBase(eventId, messageId, convId, sender, from, time)
        processor.process(msgBase, builder.build())
    }

    private class MessageHandler : MessageHandlerBase() {
        override fun onLinkPreview(client: WireClient?, msg: LinkPreviewMessage?) {
            Assertions.assertEquals(TITLE, msg.getTitle())
            Assertions.assertEquals(SUMMARY, msg.getSummary())
            Assertions.assertEquals(URL, msg.getUrl())
            Assertions.assertEquals(URL_OFFSET, msg.getUrlOffset())
            Assertions.assertEquals(CONTENT, msg.getText())
            Assertions.assertEquals(WIDTH, msg.getWidth())
            Assertions.assertEquals(HEIGHT, msg.getHeight())
            Assertions.assertEquals(SIZE.toLong(), msg.getSize())
            Assertions.assertEquals(MIME_TYPE, msg.getMimeType())
            Assertions.assertEquals(ASSET_TOKEN, msg.getAssetToken())
        }

        override fun onAudioPreview(client: WireClient?, msg: AudioPreviewMessage?) {
            Assertions.assertEquals(AUDIO_MIME_TYPE, msg.getMimeType())
        }
    }

    companion object {
        val AUDIO_MIME_TYPE: String? = "audio/x-m4a"
        val NAME: String? = "audio.m4a"
        const val DURATION = 27000
        private val TITLE: String? = "title"
        private val SUMMARY: String? = "summary"
        private val URL: String? = "https://wire.com"
        private val CONTENT: String? = "This is https://wire.com"
        private const val URL_OFFSET = 8
        private val ASSET_KEY: String? = "key"
        private val ASSET_TOKEN: String? = "token"
        private const val HEIGHT = 43
        private const val WIDTH = 84
        private const val SIZE = 123
        private val MIME_TYPE: String? = "image/png"
    }
}