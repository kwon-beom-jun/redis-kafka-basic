// JacksonConfig.java
package com.example.rediskafkalogging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration  // 이 클래스가 Spring 설정 클래스임을 명시
public class JacksonConfig {

    @Bean  // ObjectMapper Bean을 생성하여 애플리케이션 전역에서 사용
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Java 8 날짜/시간 타입을 처리하기 위한 모듈 등록
        mapper.registerModule(new JavaTimeModule());
        // 기본적으로 날짜를 타임스탬프로 출력하지 않도록 설정
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
