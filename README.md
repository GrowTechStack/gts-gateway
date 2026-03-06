# GrowTechStack API Gateway

모든 마이크로서비스 요청의 단일 진입점 역할을 하는 API 게이트웨이입니다.

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.3.5, Spring Cloud Gateway |
| Service Discovery | Spring Cloud Netflix Eureka Client |

## 주요 라우팅 규칙

| 경로 | 대상 서비스 | 설명 |
|------|------|------|
| `/api/v1/contents/**` | `gts-collector-service` | 콘텐츠 조회 및 검색 |
| `/api/v1/rss-sources/**` | `gts-collector-service` | RSS 출처 관리 |
| `/api/v1/collector/**` | `gts-collector-service` | 수집 제어 |
| `/api/v1/summarize/**` | `gts-ai-summary-service` | AI 요약 요청 |

## 환경 변수

| 변수 | 설명 |
|------|------|
| `EUREKA_URL` | Eureka 서버 주소 |
| `SERVER_PORT` | 게이트웨이 포트 (기본: 8080) |

## 로컬 개발

```bash
./gradlew bootRun
```
