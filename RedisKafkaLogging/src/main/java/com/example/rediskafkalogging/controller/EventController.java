// EventController.java
package com.example.rediskafkalogging.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.rediskafkalogging.kafka.EventProducer;
import com.example.rediskafkalogging.model.LogEvent;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

@RestController
@RequestMapping("/api/events")  // 기본 경로 설정
public class EventController {

	// Kafka 프로듀서 서비스
    @Autowired
    private EventProducer eventProducer; 
    
    // RedisTemplate (문자열용)
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // Redis에 저장된 JSON을 파싱할 ObjectMapper
//    private ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 새로운 이벤트를 수신하여 Kafka에 발행하는 엔드포인트.
     * @param request 요청 바디로 들어온 이벤트 정보(JSON) - message 필드만 받음
     * @return 발행된 이벤트의 내용과 상태 응답
     */
    // 여기서는 단순히 message 필드만 있는 JSON을 기대
    // 예: {"message": "Example log message"}
    @PostMapping
    public ResponseEntity<LogEvent> postEvent(@RequestBody LogEvent request) {
        // 요청으로 받은 message와 현재 시각을 이용해 LogEvent 객체 생성
    	// 새로운 LogEvent 객체를 생성하면서 timestamp를 현재 시간으로 설정
        LogEvent event = new LogEvent(request.getMessage(), LocalDateTime.now());
        // 이벤트를 Kafka에 발행 (비동기 처리)
        // eventProducer.sendEvent(event)를 호출해 Kafka에 이벤트를 발행
        // 이 호출이 완료되기 전에 Kafka 전송은 비동기로 진행되지만, 우리는 즉시 응답을 줄 수 있음
        eventProducer.sendEvent(event);
        // 클라이언트에게는 발행한 이벤트 객체를 바로 반환 (ID는 아직 null일 수 있음)
        return ResponseEntity.ok(event);
    }

    /**
     * Redis에 누적된 이벤트 로그 목록을 반환하는 엔드포인트.
     * @return 이벤트 로그의 리스트 (JSON 배열)
     */
    @GetMapping
    public ResponseEntity<List<LogEvent>> getEvents() {
        List<LogEvent> events = new ArrayList<>();
        // Redis의 "event_list" 키에 저장된 모든 이벤트 JSON을 리스트로 가져옴
        // opsForList().range("event_list", 0, -1)를 통해 리스트 전체를 조회
        List<String> eventJsonList = redisTemplate.opsForList().range("event_list", 0, -1);
        if (eventJsonList != null) {
            for (String eventJson : eventJsonList) {
                try {
                    // JSON 문자열을 LogEvent 객체로 변환하여 리스트에 추가
                	// 가져온 각 JSON 문자열을 ObjectMapper.readValue(..., LogEvent.class)로 LogEvent 객체로 변환하고 리스트 events에 추가
                    LogEvent event = objectMapper.readValue(eventJson, LogEvent.class);
                    events.add(event);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
        // 이벤트 목록 반환
        // 모두 수집한 후 events 리스트를 HTTP 응답으로 반환
        // 클라이언트는 현재까지 수신된 모든 이벤트 로그를 확인할 수 있음
        return ResponseEntity.ok(events);
    }
}
