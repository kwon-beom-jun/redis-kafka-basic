// LogEvent.java
package com.example.rediskafkalogging.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity                     // JPA 엔티티 선언
@Table(name = "events")     // 매핑될 테이블명 지정 (events 테이블)
public class LogEvent implements java.io.Serializable {
    // 직렬화를 위해 Serializable 구현 (Kafka 전송 등에서 필요할 수 있음)

    @Id  // 기본 키
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 이벤트 ID (DB에서 자동 생성됨)
    
    private String message;       // 이벤트 메시지 내용
    private LocalDateTime timestamp; // 이벤트 발생 시각
    
    // 생성자: JPA 및 사용 편의를 위한 기본 생성자와 필드 생성자 정의
    public LogEvent() { 
        // 기본 생성자 (JPA 요구사항)
    }
    
    public LogEvent(String message, LocalDateTime timestamp) {
        this.message = message;
        this.timestamp = timestamp;
    }
    
    // Getter 및 Setter 메서드들 (각 필드별로) -- Lombok의 @Data 등을 사용하면 자동 생성 가능
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "LogEvent{id=" + id + 
               ", message='" + message + '\'' + 
               ", timestamp=" + timestamp + '}';
    }
}
