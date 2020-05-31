package com.geunyoung.ticket_reservation

import com.geunyoung.ticket_reservation.dto.ReserveDto
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.StopWatch
import org.springframework.web.client.RestTemplate

class StressTest {
    @Test
    fun blocking(): Unit {
        val restTemplate = RestTemplate()
        val stopWatch = StopWatch()
        val requestBody = ReserveDto(3)
        val headers = LinkedMultiValueMap<String, String>()
        headers["userId"] = "3"

        stopWatch.start()

        runBlocking {
            repeat(1000) {
                launch {
                    val response = restTemplate.exchange<String>(
                        "http://localhost:8080/reservation/",
                        HttpMethod.POST,
                        HttpEntity(requestBody, headers),
                        String::class.java
                    )
                }
            }
        }

        stopWatch.stop()
        println(stopWatch.totalTimeSeconds)
    }
}