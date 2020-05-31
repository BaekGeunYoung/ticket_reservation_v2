# ticket_reservation_v2

[멀티스레드 티켓 예매 서버 구현](https://github.com/BaekGeunYoung/stress-ticket-reservation-worker) 프로젝트를 진행하면서 아쉬웠던 점들을 개선하여 low latency 서버를 다시 한 번 만들어보고자 한다.

## 개선할 점

### spring webflux를 활용한 Web Application Server의 동시성 향상

기존에 구현했던 WAS는 spring MVC를 이용해 만든 것으로, 순간적으로 많은 수의 요청이 들어왔을 때 비효율적으로 많은 스레드를 사용하게 되고, blocking I/O를 기반으로 작동하기 때문에 latency도 높아지게 된다. 따라서 적은 수의 자원으로 높은 동시성을 갖출 수 있는 spring webflux를 사용해 WAS를 구현한다.

### redis distributed lock을 이용한 공유자원 관리

기존에는 queue consumer를 한 대만 두어 하나의 프로세스 안에서만 공유 자원 관리를 했었다. 하지만 성능을 더욱 높이고 latency를 줄이기 위해서는 consumer를 동시에 여러 개 돌려야 할텐데, 그렇게 했을 때에는 여러 프로세스에 걸친 공유 자원 관리 전략이 필요했다. redis에 구현되어 있는 distributed lock을 이용해 데이터의 결함 없이 공유 자원을 관리한다.

### k8s를 이용한 container orchestration

low latency를 위해서는 들어오는 트래픽에 따라 WAS 및 worker의 replica 수를 동적으로 조절하는 container orchestration 전략이 꼭 필요하다. k8s를 이용해 어플리케이션을 배포하여 변화하는 트래픽에 유연하게 대응할 수 있는 클러스터를 구축해본다.