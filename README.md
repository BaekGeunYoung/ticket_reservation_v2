# ticket_reservation_v2

[멀티스레드 티켓 예매 서버 구현](https://github.com/BaekGeunYoung/stress-ticket-reservation-worker) 프로젝트를 진행하면서 아쉬웠던 점들을 개선하여 low latency 서버를 다시 한 번 만들어보고자 한다.

## 개선할 점

### spring webflux를 활용한 Web Application Server의 동시성 향상

기존에 구현했던 WAS는 spring MVC를 이용해 만든 것으로, 순간적으로 많은 수의 요청이 들어왔을 때 비효율적으로 많은 스레드를 사용하게 되고, blocking I/O를 기반으로 작동하기 때문에 latency도 높아지게 된다. 따라서 적은 수의 자원으로 높은 동시성을 갖출 수 있는 spring webflux를 사용해 WAS를 구현한다.

### redis distributed lock을 이용한 공유자원 관리

기존에는 queue consumer를 한 대만 두어 하나의 프로세스 안에서만 공유 자원 관리를 했었다. 하지만 성능을 더욱 높이고 latency를 줄이기 위해서는 consumer를 동시에 여러 개 돌려야 할텐데, 그렇게 했을 때에는 여러 프로세스에 걸친 공유 자원 관리 전략이 필요했다. redis에 구현되어 있는 distributed lock을 이용해 데이터의 결함 없이 공유 자원을 관리한다.

### k8s를 이용한 container orchestration

low latency를 위해서는 들어오는 트래픽에 따라 WAS 및 worker의 replica 수를 동적으로 조절하는 container orchestration 전략이 꼭 필요하다. k8s를 이용해 어플리케이션을 배포하여 변화하는 트래픽에 유연하게 대응할 수 있는 클러스터를 구축해본다.

## 구현

### spring webflux + redisson

redisson은 여러 종류의 redis client를 제공하는데, 그 중 reactive programming 방식을 따르는 reactive client도 존재한다. 이 client를 내부적으로 사용하는 redissonService를 만들고, 이를 이용해 작성한 티켓 예매 관련 핵심 로직은 아래와 같다.

```kotlin
    val countMono = redissonService.getAtomicLong("count")
    
    return countMono.get().flatMap { count ->
        if (count >= Constants.MAX_RESERVATION_COUNT) Mono.just(false)
        else reserveDtoMono.flatMap { reserveDto ->
            userIdMono.flatMap { userId ->
                reservationRepository.save(Reservation(userId = userId, number = reserveDto.number)).flatMap {
                    countMono.getAndSet(count + reserveDto.number).flatMap {
                        Mono.just(true)
                    }
                }
            }
        }
    }
```

redissonClient에서 제공하는 연산들은 atomicity를 보장하기 때문에 멀티 프로세스 환경에서도 데이터의 결함이 없음을 보장받을 수 있다. 

### k8s cluster 구축

k8s cluster를 구성하는 pod의 종류는 아래와 같다.

- api server (embedded netty server)
- redis
- mysql

이 중 redis와 mysql은 type이 clusterIP인 service로 노출시켰고, api server는 service의 타입을 nodePort로 지정해 ingress를 통해 클러스터 외부로 노출시켰다. 

```
~/ticket_reservation_v2 # kubectl get svc
NAME                       TYPE        CLUSTER-IP     EXTERNAL-IP   PORT(S)        AGE
kubernetes                 ClusterIP   10.27.240.1    <none>        443/TCP        2d5h
mysql                      ClusterIP   None           <none>        3306/TCP       2d4h
ticket-reservation-api     NodePort    10.27.247.42   <none>        80:31528/TCP   6h1m
ticket-reservation-redis   ClusterIP   None           <none>        6379/TCP       7h14m
```

### 테스트

#### 부하 테스트

동시에 1000개의 요청을 보내 얼마만큼의 시간이 걸리는지 테스트해보았다.

```kotlin
@Test
    fun stress_test() {
        val restTemplate = RestTemplate()
        val stopWatch = StopWatch()
        val requestBody = ReserveDto(3)
        val headers = LinkedMultiValueMap<String, String>()
        headers["userId"] = "3"

        stopWatch.start()

        runBlocking {
            repeat(1000) {
                launch {
                    val response = restTemplate.exchange<String>(
                        "http://34.120.87.126/reservation",
                        HttpMethod.POST,
                        HttpEntity(requestBody, headers),
                        String::class.java
                    )
                }
            }
        }

        stopWatch.stop()
        println(stopWatch.totalTimeSeconds)
    }
```

테스트 결과:

api pod의 replica 수가 1개일 때
```
> Task :compileKotlin
> Task :compileJava NO-SOURCE
> Task :processResources UP-TO-DATE
> Task :classes UP-TO-DATE
> Task :compileTestKotlin
> Task :compileTestJava NO-SOURCE
> Task :processTestResources UP-TO-DATE
> Task :testClasses UP-TO-DATE
> Task :test
79.035465377
BUILD SUCCESSFUL in 1m 22s
```

api pod의 replica 수가 5개일 때

```
> Task :compileKotlin
> Task :compileJava NO-SOURCE
> Task :processResources UP-TO-DATE
> Task :classes UP-TO-DATE
> Task :compileTestKotlin
> Task :compileTestJava NO-SOURCE
> Task :processTestResources UP-TO-DATE
> Task :testClasses UP-TO-DATE
> Task :test
89.596828294
BUILD SUCCESSFUL in 1m 31s
```

다수의 request를 모두 처리하는 데 걸리는 총 시간은 replica 갯수가 늘어남에 따라 크게 이득을 보는 것 같지는 않다.

#### max latency 테스트

사실 1000개의 요청을 모두 처리하는 데 얼마만큼의 시간이 걸리는 지는 서버의 성능을 평가하는 아주 직접적인 지표는 아니라고 생각한다. 그보다는 각각의 요청을 얼만큼 빨리 처리해내느냐가 더욱 확실한 지표일텐데, 그런 의미에서 1000개의 요청 중 latency가 가장 긴 요청의 latency는 얼마나 되는 지를 측정하는 테스트 코드를 작성했다.

```kotlin
@Test
    fun max_latency_test() {
        val restTemplate = RestTemplate()
        val requestBody = ReserveDto(3)
        val headers = LinkedMultiValueMap<String, String>( )
        headers["userId"] = "3"

        val mutex = Mutex()
        var maxLatency = 0L

        runBlocking {
            repeat(1000) {
                launch {
                    val start = System.currentTimeMillis()
                    val response = restTemplate.exchange<String>(
                            "http://34.120.87.126/reservation",
                            HttpMethod.POST,
                            HttpEntity(requestBody, headers),
                            String::class.java
                    )
                    val end = System.currentTimeMillis()

                    mutex.withLock {
                        if (end - start > maxLatency) maxLatency = end - start
                    }
                }
            }
        }

        println("max latency: $maxLatency ms")
    }
```

coroutine을 이용해 테스트 코트를 작성했기 때문에 모든 coroutine이 공유하는 maxLatency 변수는 mutex 라이브러리를 이용해 상호 배제적으로 접근이 가능하도록 코드를 작성했다.

테스트 결과 :

replica 수가 1개일 때
```
> Task :compileKotlin
> Task :compileJava NO-SOURCE
> Task :processResources UP-TO-DATE
> Task :classes UP-TO-DATE
> Task :compileTestKotlin
> Task :compileTestJava NO-SOURCE
> Task :processTestResources UP-TO-DATE
> Task :testClasses UP-TO-DATE
> Task :test
max latency: 3058 ms
BUILD SUCCESSFUL in 1m 20s
```

replica 수가 5개일 때

```
> Task :compileKotlin
> Task :compileJava NO-SOURCE
> Task :processResources UP-TO-DATE
> Task :classes UP-TO-DATE
> Task :compileTestKotlin
> Task :compileTestJava NO-SOURCE
> Task :processTestResources UP-TO-DATE
> Task :testClasses UP-TO-DATE
> Task :test
max latency: 277 ms
BUILD SUCCESSFUL in 1m 31s
```

replica 갯수를 늘림에 따라 max latency 값이 현저하게 줄어든 것을 확인할 수 있었다. 이를 통해 redis의 분산락을 이용해 데이터의 결함 없이 공유 자원을 관리하는 동시에, 멀티 프로세스 환경으로 latency를 확실히 낮출 수 있음을 확인했다.

## 결론

spring webflux를 이용해 높은 동시성을 갖춘 서버를 구축할 수 있었고, latency를 낮추기 위해 다수의 프로세스를 작동시키는 환경에서 redis를 이용해 공유 자원을 효과적으로 관리하는 방법을 학습해보았다. spring webflux가 spring MVC에 비해서 가지는 장점을 체감해볼 수 있었고, redisson이 제공하는 atomic한 연산들의 도움을 받아 손쉽게 코드를 작성할 수 있었다. 전체적으로 서버의 latency를 낮추기 위해서는 어느 부분에 신경을 써야하는 지에 대해 많은 고민을 해볼 수 있는 실습이었다고 생각한다.