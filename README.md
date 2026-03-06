# GrowTechStack API Gateway

모든 클라이언트 요청의 단일 진입점 역할을 하는 API 게이트웨이입니다.
Eureka 기반 로드밸런싱(`lb://`)으로 각 마이크로서비스로 요청을 라우팅합니다.

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.3.5, Spring Cloud Gateway |
| Service Discovery | Spring Cloud Netflix Eureka Client |

## 라우팅 규칙

| 경로 | 대상 서비스 | 설명 |
|------|-------------|------|
| `/api/v1/contents/**` | `gts-collector-service` | 콘텐츠 조회 및 검색 |
| `/api/v1/rss-sources/**` | `gts-collector-service` | RSS 출처 관리 |
| `/api/v1/collector/**` | `gts-collector-service` | 수집 제어 및 로그 |
| `/api/v1/summarize/**` | `gts-ai-summary-service` | AI 요약 요청 |

## 환경 변수

| 변수 | 설명 |
|------|------|
| `EUREKA_URL` | Eureka 서버 주소 (기본: `http://localhost:8761/eureka/`) |

## 배포

`main` 브랜치 push → GitHub Actions → ECR push → EC2 자동 배포
