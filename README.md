# 🚀 RedisKafkaLogging 프로젝트

### 📌 프로젝트 개요
Spring Boot 기반으로 **Kafka와 Redis**를 활용한 실시간 이벤트 로깅 시스템을 구축합니다. 클라이언트로부터 이벤트를 받아 Kafka를 통해 비동기 처리하고, 이를 **PostgreSQL(DB)과 Redis(Cache)** 에 저장하여 빠른 조회 및 영구 저장을 지원합니다.


<br/>

## 🛠️ 프로젝트 스펙

### 📂 개발 환경
- **운영체제**: Windows
- **개발 도구**: Eclipse (Spring Tool Suite 4)
- **빌드 시스템**: Maven
- **실행 방식**: Spring Boot 내장 서버 사용 (port: 8080)

### 🏗️ 기술 스택
- **Java 17** (LTS)
- **Spring Boot 3.3.9**
- **Spring Web (REST API)**
- **Spring Data JPA (PostgreSQL 연동)**
- **Spring Kafka (메시지 브로커)**
- **Spring Data Redis (캐싱 서버)**
- **Lombok (코드 간소화)**
- **Jackson (JSON 직렬화/역직렬화)**

### 🏦 데이터베이스
- **PostgreSQL 15**
- **Redis 7.0** (Lettuce 클라이언트 사용)
- **Kafka 3.5.1**

### 🔗 주요 라이브러리 (pom.xml)
- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-data-redis`
- `spring-boot-starter-kafka`
- `postgresql` (JDBC 드라이버)
- `lombok`
- `jackson-datatype-jsr310` (날짜/시간 JSON 변환)

<br/>

---

<br/>

## 📌 주요 기능 및 흐름

### 🔄 **POST /api/events**
1. 클라이언트가 **이벤트 메시지를 전송** (예: `{ "message": "테스트 이벤트" }`)
2. **EventController** → Kafka **Producer** (`KafkaTemplate`)를 사용하여 Kafka **토픽(events)** 에 발행
3. Kafka **Broker** 가 메시지를 저장 후 **Consumer** 에 전달
4. **Kafka Consumer** (`@KafkaListener`)
   - **DB(PostgreSQL)**: `events` 테이블에 이벤트 저장
   - **Redis(Cache)**: 이벤트를 리스트(`event_list`)에 추가하여 실시간 조회 가능
5. **응답 반환** (`200 OK` 및 메시지 정보)

### 🔍 **GET /api/events**
1. 클라이언트가 **이벤트 목록 요청**
2. **EventController**
   - 먼저 **Redis 캐시에서 조회** (`event_list` 리스트)
   - **데이터가 존재하면 바로 반환** (DB 조회 없이 빠른 응답)
   - **Redis에 없으면 DB에서 가져오기 (현재 예제에서는 미구현)**
3. **응답 반환** (이벤트 리스트 JSON)

<br/>

---

<br/>

## 🏃 실행 방법

### 1️⃣ **Kafka 실행**
```bash
cd {root}RedisKafkaStudy\kafka\bin\windows
zookeeper-server-start.bat ..\..\config\zookeeper.properties
```
```bash
kafka-server-start.bat ..\..\config\server.properties
```

### 2️⃣ **Redis 실행**
```bash
cd {root}RedisKafkaStudy\redis
redis-server.exe
```

### 3️⃣ **PostgreSQL 설정**
```sql
CREATE DATABASE rediskafka;
CREATE USER rediskafka WITH PASSWORD '1234';
GRANT ALL PRIVILEGES ON DATABASE rediskafka TO rediskafka;
```

### 4️⃣ **Spring Boot 애플리케이션 실행**
```bash
mvn spring-boot:run
```
또는 Eclipse에서 `RedisKafkaLoggingApplication.java` 실행

<br/>

---

<br/>

## 🛠️ API 테스트 방법

### ▶ **POST /api/events** (이벤트 발행)
```bash
curl -X POST -H "Content-Type: application/json" -d '{"message": "테스트 이벤트"}' http://localhost:8080/api/events
```

**응답 예시**
```json
{
  "id": null,
  "message": "테스트 이벤트",
  "timestamp": "2025-03-10T16:07:04.123456"
}
```

### ▶ **GET /api/events** (이벤트 조회)
```bash
curl http://localhost:8080/api/events
```

**응답 예시**
```json
[
  {
    "id": 1,
    "message": "테스트 이벤트",
    "timestamp": "2025-03-10T16:07:04.123456"
  }
]
```

<br/>

---

<br/>

## ✅ 정리
- **Kafka**: 이벤트 메시지를 처리하는 메시지 브로커
- **Redis**: 빠른 이벤트 조회를 위한 캐시 저장소
- **PostgreSQL**: 영구 저장을 위한 관계형 데이터베이스
- **Spring Boot**: REST API 및 데이터 연동 관리

🔹 **실시간 이벤트 로깅을 위한 효율적인 시스템을 구축하는 예제**로, Kafka를 통한 비동기 메시지 처리와 Redis 기반의 빠른 조회 기능을 포함합니다. 🚀



<br/>
<br/>

