package com.example.liveaudioencryption.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.kurento.client.IceCandidate
import org.kurento.client.KurentoClient
import org.kurento.client.MediaPipeline
import org.kurento.client.PassThrough
import org.kurento.client.RecorderEndpoint
import org.kurento.client.WebRtcEndpoint
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

class MyWebSocketHandler @Autowired constructor(
    private val kurento: KurentoClient
) : TextWebSocketHandler() {

    private var session: WebSocketSession? = null
    private var pipeline: MediaPipeline? = null
    private var endpoint: WebRtcEndpoint? = null
    private var recorder: RecorderEndpoint? = null
    private var blackholeEndpoint: PassThrough?=null


    override fun afterConnectionEstablished(session: WebSocketSession) {
        this.session = session
        this.pipeline = kurento.createMediaPipeline()
        this.endpoint = WebRtcEndpoint.Builder(pipeline).build()
        this.recorder = RecorderEndpoint.Builder(pipeline,"file:///tmp/recording.webm").build()
        this.blackholeEndpoint=PassThrough.Builder(pipeline).build()

        endpoint?.addIceCandidateFoundListener { event ->
            val payload = """{"type": "candidate", "candidate": "${event.candidate}"}"""
            session.sendMessage(TextMessage(payload))
        }
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val objectMapper = ObjectMapper()
        val json = objectMapper.readValue(message.payload, JsonNode::class.java)
        when (json["type"]?.textValue()) {
            "offer" -> {
                val sdpOffer = json["sdpOffer"]?.textValue() ?: throw IllegalArgumentException("Sdp offer is mandatory")
                endpoint?.processOffer(sdpOffer).let {  sdpAnswer ->
                    val payload = """{"type": "answer", "sdpAnswer": "$sdpAnswer"}"""
                    session.sendMessage(TextMessage(payload))
                    recorder?.record()
                 }
            }

            "answer" -> {
                val sdpAnswer = json["sdpAnswer"]?.textValue()?: throw IllegalArgumentException("Sdp answer is mandatory")
                endpoint?.processAnswer(sdpAnswer)
            }

            "candidate" -> {
                val candidate =
                    json["candidate"]?.textValue()?: throw IllegalArgumentException("Candidate is mandatory")
                endpoint?.addIceCandidate(IceCandidate(candidate,json["sdpMid"].textValue(),json["sdpMLineIndex"].asInt()))

            }

            "stop" -> {
                recorder?.stop()
                recorder?.release()
                pipeline?.release()
            }
            "mute"->{
                endpoint?.connect(blackholeEndpoint)

            }
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, closeStatus: CloseStatus) {
        recorder?.stop()
        recorder?.release()
    }
}