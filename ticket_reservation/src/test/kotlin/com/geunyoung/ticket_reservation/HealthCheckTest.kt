package com.geunyoung.ticket_reservation

import com.geunyoung.ticket_reservation.dto.ReserveDto
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.StopWatch
import org.springframework.web.client.RestTemplate

class HealthCheckTest {
    @Test
    fun basicHealthCheckTest() {
        val restTemplate = RestTemplate()
        val response = restTemplate.exchange<String>(
                "http://localhost:8080/",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String::class.java)

        assert(response.statusCode == HttpStatus.OK)
    }
}