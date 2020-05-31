package com.geunyoung.ticket_reservation.service.impl

import com.geunyoung.ticket_reservation.dto.ReserveDto
import com.geunyoung.ticket_reservation.entity.Reservation
import com.geunyoung.ticket_reservation.repository.ReservationRepository
import com.geunyoung.ticket_reservation.service.RedissonService
import com.geunyoung.ticket_reservation.service.ReservationService
import com.geunyoung.ticket_reservation.util.Constants
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.concurrent.TimeUnit

@Service
class ReservationServiceImpl(
        @Autowired private val reservationRepository: ReservationRepository,
        @Autowired private val redissonService: RedissonService
): ReservationService {
    override fun reserve(reserveDtoMono: Mono<ReserveDto>, userIdMono: Mono<Long>): Mono<Boolean> {
        val lock = redissonService.getLock(Constants.RESERVATION_LOCK_NAME)
        return lock.tryLock(10000, 5000, TimeUnit.MILLISECONDS).flatMap {
            if (it) {
                try {
                    val countMono = redissonService.getAtomicLong("count")
                    return@flatMap countMono.get().flatMap {count ->
                        if (count >= Constants.MAX_RESERVATION_COUNT) Mono.just(false)
                        else countMono.incrementAndGet().flatMap {
                            reserveDtoMono.flatMap { reserveDto ->
                                userIdMono.flatMap { userId ->
                                    reservationRepository.save(Reservation(userId = userId, number = reserveDto.number))
                                }
                            }.map { true }
                        }
                    }
                } finally {
                    lock.unlock()
                }
            }
            else Mono.just(false)
        }
    }
}