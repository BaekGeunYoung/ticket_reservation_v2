package com.geunyoung.ticket_reservation.router

import com.geunyoung.ticket_reservation.handler.HealthCheckHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.router

@Component
class HealthCheckRouter (
    @Autowired private val healthCheckHandler: HealthCheckHandler
) {
    @Bean
    fun healthCheckRoutes(): RouterFunction<*> = router {
        "/".nest {
            GET("", healthCheckHandler::healthCheck)
        }
    }
}