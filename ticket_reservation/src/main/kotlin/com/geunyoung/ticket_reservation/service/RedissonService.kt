package com.geunyoung.ticket_reservation.service

import org.redisson.api.RAtomicLongReactive
import org.redisson.api.RLockReactive

interface RedissonService {
    fun getLock(lockName: String): RLockReactive

    fun getAtomicLong(key: String): RAtomicLongReactive
}