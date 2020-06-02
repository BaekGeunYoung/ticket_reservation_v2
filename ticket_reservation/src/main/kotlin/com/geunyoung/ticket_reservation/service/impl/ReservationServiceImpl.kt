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
//        val lock = redissonService.getLock(Constants.RESERVATION_LOCK_NAME)

        val countMono = redissonService.getAtomicLong("count")
        return countMono.get().flatMap {count ->
            reserveDtoMono.flatMap { reserveDto ->
                userIdMono.flatMap { userId ->
                    reservationRepository.save(Reservation(userId = userId, number = reserveDto.number)).flatMap {
                        countMono.getAndSet(count + reserveDto.number).flatMap {
                            Mono.just(true)
                        }
                    }
                }
            }
        }

//        return lock.tryLock(10000, 5000, TimeUnit.MILLISECONDS).flatMap {
//            if (it) {
//                val countMono = redissonService.getAtomicLong("count")
//
//                return@flatMap countMono.get().flatMap { count ->
//                    if (count >= Constants.MAX_RESERVATION_COUNT) Mono.just(false)
//                    else reserveDtoMono.flatMap { reserveDto ->
//                            userIdMono.flatMap { userId ->
//                                reservationRepository.save(Reservation(userId = userId, number = reserveDto.number)).flatMap {
//                                    countMono.getAndSet(count + reserveDto.number).flatMap {
//                                        Mono.just(true)
//                                    }
//                                }
//                            }
//                        }
//                }
//            }
//            else Mono.just(false)
//        }
    }
}