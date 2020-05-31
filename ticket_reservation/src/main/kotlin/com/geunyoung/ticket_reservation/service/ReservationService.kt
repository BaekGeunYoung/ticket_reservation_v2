package com.geunyoung.ticket_reservation.service

import com.geunyoung.ticket_reservation.dto.ReserveDto
import reactor.core.publisher.Mono

interface ReservationService {
    fun reserve(reserveDtoMono: Mono<ReserveDto>, userIdMono: Mono<Long>): Mono<Void>
}