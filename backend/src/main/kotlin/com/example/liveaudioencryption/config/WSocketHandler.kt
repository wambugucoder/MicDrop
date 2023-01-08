package com.example.liveaudioencryption.config

import org.kurento.client.KurentoClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
class WSocketHandler: WebSocketConfigurer {

    @Bean
    fun webSocketHandler(): WebSocketHandler {
       return MyWebSocketHandler(KurentoClient.create())
    }
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(webSocketHandler(), "/signaling")
    }
}