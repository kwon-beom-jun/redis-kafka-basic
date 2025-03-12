// EventProducer.java
package com.example.rediskafkalogging.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.example.rediskafkalogging.model.LogEvent;

/**
 * Kafka 프로듀서는 애플리케이션 내에서 이벤트를 Kafka로 보내는 역할
 * Spring Kafka에서는 KafkaTemplate을 사용하여 메시지를 보냄
 * 우리는 이벤트 데이터를 LogEvent 객체로 만들고 JSON으로 직렬화하여 Kafka에 보낼 것이므로, KafkaTemplate<String, LogEvent>를 사용
 */
@Service  // Spring 서비스 컴포넌트로 등록
public class EventProducer {

    private static final String TOPIC_NAME = "events"; // Kafka 토픽 이름 (KafkaConfig와 동일하게 설정)
    
    // KafkaTemplate<String, LogEvent>: 제네릭으로 <키, 값> 타입을 설정
    // 키는 특별히 사용하지 않으므로 String (null로 보냄)으로, 값은 우리가 보낼 이벤트 객체 LogEvent로 타입을 지정
    // Spring Boot의 자동설정은 application.properties의 serializer 설정에 따라 이 KafkaTemplate을 적절히 구성
    private final KafkaTemplate<String, LogEvent> kafkaTemplate;
    
    // Spring Boot의 Auto-Configuration 기능 덕분에 spring-boot-starter-kafka 의존성을 추가하면 KafkaTemplate 빈이 자동으로 생성
    // 생성자를 통한 KafkaTemplate 주입 (Spring Boot가 자동 구성한 KafkaTemplate 사용)
    // 생성자: 단 하나의 생성자이므로 @Autowired 생략 가능 (Spring이 자동으로 주입)
    public EventProducer(KafkaTemplate<String, LogEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    /**
     * 주어진 이벤트를 Kafka 'events' 토픽으로 발행한다.
     * @param event Kafka로 보낼 이벤트 객체
     * KafkaTemplate.send(String topic, V data)를 호출하여 지정한 토픽으로 데이터를 발행
     * Spring Kafka는 LogEvent 객체를 JsonSerializer를 통해 JSON 문자열로 변환하여 Kafka에 보냄
     */
    public void sendEvent(LogEvent event) {
        // Kafka로 이벤트 전송 (키는 null, 값은 event 객체)
        kafkaTemplate.send(TOPIC_NAME, event);
        // 참고: send 메서드는 내부적으로 비동기로 동작하며, Future를 반환할 수 있다.
        // 여기서는 예제 단순화를 위해 반환값을 확인하지 않고 전송만 수행.
    }
}
