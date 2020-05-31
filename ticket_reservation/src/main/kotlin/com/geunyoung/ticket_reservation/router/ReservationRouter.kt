package com.geunyoung.ticket_reservation.router

import com.geunyoung.ticket_reservation.handler.ReservationHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.router

@Component
class ReservationRouter(
        @Autowired private val reservationHandler: ReservationHandler
) {
    @Bean
    fun reservationRoutes(): RouterFunction<*> = router {
        "/reservation".nest {
            POST("/", reservationHandler::reserve)
        }
    }
}