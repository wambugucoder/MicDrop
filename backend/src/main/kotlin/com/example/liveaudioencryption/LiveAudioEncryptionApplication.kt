package com.example.liveaudioencryption

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.socket.config.annotation.EnableWebSocket

@SpringBootApplication
@EnableWebSocket
class LiveAudioEncryptionApplication

fun main(args: Array<String>) {
    runApplication<LiveAudioEncryptionApplication>(*args)
}