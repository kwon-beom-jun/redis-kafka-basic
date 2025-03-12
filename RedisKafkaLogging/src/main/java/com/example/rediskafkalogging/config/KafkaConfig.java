package com.example.rediskafkalogging.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
/**
 * 순서 *

	1. POST /api/events 요청 흐름
		[1] 클라이언트 → REST 컨트롤러 (EventController.java)
				클라이언트 요청:
					클라이언트가 POST /api/events API를 호출하면서 JSON 형태로 이벤트 메시지 (예: {"message": "테스트 이벤트 발생"})를 보냄
			
			EventController.postEvent():
				역할:
					HTTP 요청 바디에 있는 JSON 데이터를 LogEvent 객체로 매핑받음
				주요 작업:
					클라이언트로부터 받은 message 값을 이용해 새 LogEvent 객체를 생성하고, 현재 서버의 시간을 timestamp로 설정함
					생성된 이벤트 객체를 Kafka 프로듀서(예: EventProducer.sendEvent(event))로 전달함
					클라이언트에게 생성된 이벤트 객체를 즉시 응답으로 반환함
			
			이유:
				REST 컨트롤러는 클라이언트와 서버 간 데이터 전달의 입구 역할을 하며, 
				클라이언트가 이벤트를 발생시키면 이를 Kafka로 비동기 전송해 이후의 처리(실시간 로깅, DB 저장, 캐싱 등)를 담당하게 하기 위함
			
		[2] EventProducer.java (Kafka 프로듀서)
			EventProducer.sendEvent(LogEvent event):
				역할: 
					이벤트를 Kafka 토픽(예, "events")으로 발행함
				주요 작업:
					Spring Boot가 자동 구성한 KafkaTemplate<String, LogEvent> 빈을 사용하여 send() 메서드 호출.
					이벤트 객체를 Kafka에 발행하면, 내부적으로 JSON으로 직렬화되어 Kafka 브로커로 전송됨
			
			이유:
				Kafka를 사용함으로써 이벤트를 비동기적으로 처리할 수 있음. 이렇게 하면 이벤트 발생 시 즉시 클라이언트 응답을 반환하고, 
				백그라운드에서 이벤트 소비(Consumer)가 데이터를 처리할 수 있게 됨.
				또한, Kafka는 높은 처리량과 내구성, 분산 환경에서 메시지를 안정적으로 전달하기 때문에 실시간 이벤트 로깅에 적합함
			
			
		[3] Kafka Broker 및 Kafka Consumer
			Kafka Broker:
				이벤트가 발행되면, Kafka Broker가 이를 지정된 토픽("events")에 저장함
				특징: 
					이벤트는 디스크에 로그 형식으로 기록되어 내구성을 보장하며, 여러 Consumer가 같은 토픽을 읽을 수 있음
					
			Kafka Consumer (EventConsumer.java):
				역할: 
					Kafka 토픽 "events"에 새 메시지가 도착하면 이를 자동으로 수신함
				주요 작업:
					Kafka가 수신한 JSON 메시지를 자동으로 LogEvent 객체로 역직렬화함
					DB 저장: LogEventRepository.save(event)를 호출하여 PostgreSQL 데이터베이스의 events 테이블에 이벤트를 저장함
				Redis 캐싱: 
					RedisTemplate을 사용해, 이벤트 객체를 JSON 문자열로 변환한 후 Redis의 리스트(예: "event_list")에 추가함
			
			이유:
				Kafka Consumer는 이벤트의 후처리를 담당함
					PostgreSQL: 
						이벤트를 영구 저장하여, 나중에 상세 분석이나 로그 보관이 가능하도록 함
					Redis: 
						실시간 조회 및 캐싱 용도로 사용하여, 빠른 응답을 제공할 수 있도록 함 
						Redis는 메모리 기반이므로 DB보다 훨씬 빠른 읽기/조회가 가능함
			
			
			
	2. GET /api/events 요청 흐름
		[1] 클라이언트 → REST 컨트롤러 (EventController.java)
			클라이언트 요청:
				클라이언트가 GET /api/events API를 호출하여 저장된 이벤트 로그를 조회함
			
			EventController.getEvents():
				역할: 
					Redis에서 이벤트 로그 데이터를 조회하고, 클라이언트에게 반환함
				주요 작업:
					RedisTemplate.opsForList().range("event_list", 0, -1)를 호출하여 Redis에 저장된 "event_list" 키의 전체 리스트를 가져옴
					각 리스트 항목은 JSON 문자열. 이를 ObjectMapper를 사용해 LogEvent 객체로 변환함
					변환된 이벤트 객체들을 리스트로 만들어 클라이언트에게 응답함
			
			이유:
				실시간 조회 시, Redis는 메모리 캐시로 동작하여 빠른 응답을 제공할 음니다.
				Redis 우선 조회:
					이벤트 로그를 Redis에 저장함으로써, 클라이언트가 요청할 때 빠르게 조회할 수 있음
				DB와의 관계:
					이 예제에서는 GET 요청 시 우선 Redis에서 데이터를 가져옴 
					(실제 서비스에서는 캐시 미스(Cache Miss) 시 DB에서 데이터를 다시 가져오거나, 캐시를 갱신하는 전략을 추가할 수 있음)
					여기서는 간단히 Redis에 저장된 데이터를 조회만 하므로, DB에 추가적인 영향을 주지 않음
			
		[2] Redis의 역할 및 DB와의 관계 (GET 요청 시)
			Redis:
				역할:
					캐시: Kafka Consumer가 이벤트를 받아 Redis에 JSON 형태로 저장함
					빠른 조회: 클라이언트가 GET 요청 시, Redis에 저장된 이벤트 리스트를 빠르게 조회할 수 있음
				
				저장 방식:
					Redis의 List 자료구조("event_list")에 이벤트 JSON 문자열이 rightPush()로 누적됨
					조회는 LRANGE 명령처럼 리스트 전체를 가져오는 방식으로 이루어집니다.
			
			DB (PostgreSQL):
				역할:
					영구 저장: Kafka Consumer가 이벤트를 받아 DB에 저장하여, 데이터 유실 없이 이벤트 로그를 보관함
				영향:
					GET 요청 시에는 DB에 접근하지 않고, 빠른 응답을 위해 Redis 캐시를 사용함
					만약 Redis 캐시에서 원하는 이벤트가 없는 경우(캐시 미스) 추가 로직(예, DB에서 데이터를 가져와 캐시 갱신)을 구현할 수 있지만, 
					이 예제에서는 단순 조회만 처리함
				결과:
					클라이언트가 GET 요청 시 Redis에 있는 데이터(즉, 최근에 처리된 이벤트 로그)를 JSON 배열로 받아볼 수 있고, 
					이 데이터는 Kafka Consumer에서 DB에도 저장한 데이터와 일치하게 됨


		---------------------------------------------------------
		
		
		전체 흐름 요약
			POST /api/events 요청
				1. 클라이언트가 JSON 데이터({"message": "테스트 이벤트 발생"})를 POST 요청으로 보냄.
				2. EventController.postEvent(): 
					요청 데이터를 받아 LogEvent 객체 생성 후, *EventProducer.sendEvent()*로 전달.
				3. EventProducer: 
					KafkaTemplate을 사용해 이벤트 객체를 Kafka 토픽("events")에 발행.
				4. Kafka Broker: 
					이벤트 메시지를 토픽에 기록.
				5. EventConsumer: Kafka 토픽을 구독하여 메시지를 받아,
					PostgreSQL DB: LogEventRepository.save(event)로 이벤트를 영구 저장.
					Redis: 이벤트 객체를 JSON 문자열로 변환 후 Redis 리스트("event_list")에 추가.
				6. 클라이언트는 즉시 POST 요청에 의해 생성된 이벤트 객체(예: 타임스탬프 포함)를 응답으로 받음.
			GET /api/events 요청
				1. 클라이언트가 GET 요청을 보내 이벤트 로그 목록을 요청.
				2. EventController.getEvents(): 
					RedisTemplate을 사용해 Redis의 "event_list" 키에 저장된 전체 이벤트 JSON 리스트를 조회.
				3. 각 JSON 문자열을 ObjectMapper를 사용해 LogEvent 객체로 변환.
				4. 변환된 이벤트 객체 리스트를 HTTP 응답으로 반환.
				5. 클라이언트는 Redis에서 빠르게 조회한 이벤트 로그를 받아볼 수 있음.

 */
/**
 * Kafka 토픽 설정
 * 
 * @EnableKafka
 * 		@KafkaListener 등을 사용해 컨슈머를 만들기 위해 필요
 * 		이 애노테이션을 추가하면 Spring이 Kafka 리스너를 검색하고 실행
 * NewTopic 빈
 * 		토픽 이름 "events"로 1개 파티션과 1개 복제본을 가진 토픽을 생성
 *		애플리케이션 시작 시 Spring Kafka의 AdminClient를 통해 Kafka에 토픽을 만들게 함 (이미 존재하면 무시됨)
 */
@Configuration                // 이 클래스가 설정(빈 정의) 클래스임을 명시
@EnableKafka                  // Kafka 리스너(@KafkaListener) 기능을 활성화
public class KafkaConfig {

    private static final String TOPIC_NAME = "events";  // Kafka 토픽 이름 정의

    @Bean
    public NewTopic createTopic() {
        // Kafka 토픽이 존재하지 않을 경우 생성을 위한 빈(Bean) 정의
        return TopicBuilder.name(TOPIC_NAME)  // 토픽 이름 설정
                            .partitions(1)    // 파티션 수 (예제에서는 1개)
                            .replicas(1)      // 복제본 수 (로컬이므로 1개로 설정)
                            .build();
    }
}
