package com.geunyoung.ticket_reservation

import com.geunyoung.ticket_reservation.dto.ReserveDto
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate

class MaxLatencyTest {
    @Test
    fun max_latency_test() {
        val restTemplate = RestTemplate()
        val requestBody = ReserveDto(3)
        val headers = LinkedMultiValueMap<String, String>( )
        headers["userId"] = "3"

        val mutex = Mutex()
        var maxLatency = 0L

        runBlocking {
            repeat(1000) {
                launch {
                    val start = System.currentTimeMillis()
                    val response = restTemplate.exchange<String>(
                            "http://34.120.87.126/reservation",
                            HttpMethod.POST,
                            HttpEntity(requestBody, headers),
                            String::class.java
                    )
                    val end = System.currentTimeMillis()

                    mutex.withLock {
                        if (end - start > maxLatency) maxLatency = end - start
                    }
                }
            }
        }

        println("max latency: $maxLatency ms")
    }
}