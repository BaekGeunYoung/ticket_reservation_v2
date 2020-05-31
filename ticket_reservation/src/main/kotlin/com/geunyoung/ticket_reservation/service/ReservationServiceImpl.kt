package com.geunyoung.ticket_reservation.service

import com.geunyoung.ticket_reservation.dto.ReserveDto
import com.geunyoung.ticket_reservation.entity.Reservation
import com.geunyoung.ticket_reservation.repository.ReservationRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class ReservationServiceImpl(
        @Autowired private val reservationRepository: ReservationRepository
): ReservationService {
    override fun reserve(reserveDtoMono: Mono<ReserveDto>, userIdMono: Mono<Long>): Mono<Void> {
        return reserveDtoMono.flatMap {reserveDto ->
            userIdMono.flatMap {userId ->
                reservationRepository.save(Reservation(userId = userId, number = reserveDto.number))
            }
        }.then()
    }
}