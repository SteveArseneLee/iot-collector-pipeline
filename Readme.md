# 실시간 IoT 데이터 파이프라인 아키텍처

## 개요
이 프로젝트는 IoT의 실시간 데이터 수집, 처리, 저장을 위한 Modern 데이터 파이프라인 아키텍처입니다. Apache Kafka를 중심으로 한 스트리밍 데이터 처리와 Elasticsearch를 활용한 검색 및 분석 기능을 제공합니다.

## 아키텍처 구성요소

### 1. 데이터 원천 (Data Sources)
- **다양한 데이터 소스**: 여러 애플리케이션과 시스템에서 생성되는 로그 및 이벤트 데이터
- **MySQL**: 관계형 데이터베이스에서 발생하는 트랜잭션 데이터

### 2. 데이터 수집 (Data Collection)
- **Logstash**: 다양한 소스에서 로그 데이터를 수집하고 변환
- **Vector by Datadog**: 고성능 데이터 수집 및 라우팅
- **Flink CDC**: MySQL 데이터베이스의 변경 사항을 실시간으로 캡처

### 3. 데이터 처리 (Data Processing)
- **Apache Kafka**: 실시간 스트리밍 데이터의 중앙 허브 역할
- **Apache Flink**: 실시간 스트림 처리 및 복잡한 이벤트 처리

### 4. 데이터 저장 (Data Storage)
- **Elasticsearch**: 실시간 검색 및 분석을 위한 검색 엔진
- **RDBMS**: 구조화된 데이터 저장 (TBD)
  - MySQL
  - PostgreSQL
  - Oracle
  - MariaDB

## 주요 기능

### 실시간 데이터 처리
- 스트리밍 데이터의 실시간 수집 및 처리
- 복잡한 이벤트 처리 및 데이터 변환
- 높은 처리량과 낮은 지연시간 보장

### 확장성
- 수평적 확장 가능한 아키텍처
- 마이크로서비스 기반 구성
- 클라우드 네이티브 설계

### 데이터 통합
- 다양한 데이터 소스의 통합
- 실시간 CDC(Change Data Capture)
- 배치 및 스트림 처리 지원

## 기술 스택

| 카테고리 | 기술 |
|---------|------|
| 메시지 큐 | Apache Kafka |
| 스트림 처리 | Apache Flink |
| 데이터 수집 | Logstash, Vector |
| 검색 엔진 | Elasticsearch |
| 데이터베이스 | MySQL, PostgreSQL |
| CDC | Flink CDC |

## 설치 및 실행

### 사전 요구사항
- K8s
- Java 11+
- Apache Kafka
- Elasticsearch


### 주요 구성 요소
![alt text](architecture.png)