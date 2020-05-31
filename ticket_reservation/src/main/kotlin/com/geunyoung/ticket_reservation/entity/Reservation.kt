package com.geunyoung.ticket_reservation.entity

import org.springframework.data.annotation.Id

data class Reservation(
        @Id
        var id: Long? = null,
        var userId: Long,
        var number: Int
)