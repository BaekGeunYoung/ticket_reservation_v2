package com.geunyoung.ticket_reservation.handler

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.status
import reactor.core.publisher.Mono

@Component
class HealthCheckHandler {
    fun healthCheck(serverRequest: ServerRequest): Mono<ServerResponse> = status(HttpStatus.OK).build()
}