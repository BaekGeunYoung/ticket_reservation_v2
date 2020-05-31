package com.geunyoung.ticket_reservation.service.impl

import com.geunyoung.ticket_reservation.service.RedissonService
import org.redisson.Redisson
import org.redisson.api.RAtomicLongReactive
import org.redisson.api.RLockReactive
import org.redisson.api.RedissonReactiveClient
import org.redisson.config.Config
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@Service
class RedissonServiceImpl : RedissonService {
//    @Value("\${redis.uri:redis://localhost:6379}")
    private val redisUri: String = "redis://localhost:6379"
    private val config = Config()
    private lateinit var redissonClient: RedissonReactiveClient

    init {
        config.useSingleServer().address = redisUri
        redissonClient = Redisson.createReactive(config)
    }

    override fun getLock(lockName: String): RLockReactive = redissonClient.getLock(lockName)

    override fun getAtomicLong(key: String): RAtomicLongReactive = redissonClient.getAtomicLong(key)
}