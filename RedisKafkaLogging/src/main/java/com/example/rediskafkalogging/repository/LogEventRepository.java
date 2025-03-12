// LogEventRepository.java
package com.example.rediskafkalogging.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.rediskafkalogging.model.LogEvent;

@Repository  // Spring Data JPA Repository로 표시
public interface LogEventRepository extends JpaRepository<LogEvent, Long> {
    // 기본적인 CRUD 메서드가 JpaRepository 통해 제공됨 (별도 구현 불필요)
    
    // 필요에 따라 커스텀 쿼리 메서드를 정의할 수 있음. (예: findByMessageContaining 등)
}
