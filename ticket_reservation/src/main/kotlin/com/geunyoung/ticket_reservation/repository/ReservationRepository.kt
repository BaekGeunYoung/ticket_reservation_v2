package com.geunyoung.ticket_reservation.repository

import com.geunyoung.ticket_reservation.entity.Reservation
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface ReservationRepository : ReactiveCrudRepository<Reservation, String> {

}