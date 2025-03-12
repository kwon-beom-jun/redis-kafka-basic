// EventConsumer.java
package com.example.rediskafkalogging.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.rediskafkalogging.model.LogEvent;
import com.example.rediskafkalogging.repository.LogEventRepository;
import org.springframework.data.redis.core.RedisTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Kafka 컨슈머는 @KafkaListener를 사용해 구현
 * 토픽 "events"를 구독하여 새로운 메시지가 들어오면 자동으로 호출되며, 
 * 메시지 내용을 가져와 Redis와 PostgreSQL에 저장
 * 메시지 변환은 Spring Kafka가 자동으로 JSON을 LogEvent 자바 객체로 역직렬화해주므로, 
 * 리스너 메서드의 파라미터로 LogEvent 타입을 직접 받을 수 있음
 */
@Component  // Kafka 컨슈머도 Spring Bean으로 등록
public class EventConsumer {

    @Autowired
    private LogEventRepository logEventRepository;  // JPA 리포지토리 (PostgreSQL 저장용)

    @Autowired
    private RedisTemplate<String, String> redisTemplate;  // RedisTemplate (문자열 저장용)

    // Jackson ObjectMapper: LogEvent 객체를 JSON 문자열로 변환하기 위해 사용
//    private ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Kafka 'events' 토픽의 메시지를 수신하는 Kafka 리스너.
     * 토픽에 새로운 이벤트가 게시되면 이 메서드가 호출된다.
     */
    // @KafkaListener(topics = "events", groupId = "event-group"): Kafka 토픽 "events"를 구독하는 리스너를 정의
    // 그룹 ID는 설정 파일에서 정의한 "event-group"으로 지정
    // 명시적으로 지정하지 않아도 application.properties의 설정을 따라감
    @KafkaListener(topics = "events", groupId = "event-group")
    @Transactional  // 트랜잭션 처리 (DB 저장을 트랜잭션으로 묶기 위해 사용, 필요에 따라 선택)
    // Kafka에서 전달된 이벤트 객체를 파라미터로 받음
    // Spring Kafka가 JsonDeserializer를 사용해 JSON 메시지를 LogEvent 클래스의 객체로 변환해주기 때문에 가능하며, 이를 위해 앞서 trusted.packages 설정
    public void receiveEvent(LogEvent event) {
        try {
            // 1. 수신한 이벤트를 PostgreSQL DB에 저장 (JPA Repository 사용)
        	// logEventRepository.save(event)를 통해 이벤트를 PostgreSQL에 저장
            logEventRepository.save(event);
            // save() 호출 후, event 객체는 DB에서 생성된 ID가 설정된 상태가 됩니다.
            
            // 2. 동일한 이벤트 데이터를 Redis에 저장
            //    Redis에는 리스트(List) 구조로 저장하여 이벤트 로그를 순서대로 보관합니다.
            // 	  객체를 JSON 문자열로 변환
            String eventJson = objectMapper.writeValueAsString(event); 
            
            // RedisTemplate을 이용하여 Redis에 이벤트를 리스트로 저장
            // "event_list" 키를 가진 리스트의 맨 끝에 이벤트를 추가
            // event 객체를 ObjectMapper로 JSON 문자열로 변환하여 저장으로써, 추후 Redis에서 조회 시 JSON을 파싱하거나 그대로 응답으로 사용할 수 있음
            // 실제 운영환경에서는 Redis에 객체를 저장할 때 직렬화 방식을 정하고, 가능하면 RedisTemplate에 JSON 직렬화기를 설정하는 것이 좋음
            // 여기서는 간단히 ObjectMapper를 사용
            redisTemplate.opsForList().rightPush("event_list", eventJson);
            // "event_list" 라는 키의 리스트에 이벤트 JSON을 추가 (맨 뒤에 추가하여 시간순 저장)
            
            // (추가) 필요에 따라 리스트 크기를 제한하거나 expire를 설정할 수 있음.
        } catch (JsonProcessingException e) {
            // JSON 직렬화 오류가 발생한 경우 예외 처리
            e.printStackTrace();
        }
    }
}
