# CLOG 백엔드 프로젝트 컨텍스트

> **목적**: 팀원 간 공유 및 다음 작업 참고용. 현재 구현 완료된 내용과 앞으로 해야 할 작업을 한 문서에 정리한다.

---

## 프로젝트 개요

| 항목 | 내용 |
|------|------|
| 프레임워크 | Spring Boot 3.5.7 / Java 21 |
| 빌드 | Gradle |
| 메인 DB | MySQL (AWS RDS `clog-db.cfmkssq6u73l.ap-northeast-2.rds.amazonaws.com`) |
| 채팅/프로젝트 DB | MongoDB Atlas (기본 DB: `clog`) |
| LLM 연동 | AWS Lambda (`clog-llm-generator`, 서울 리전) |
| 인증 | GitHub OAuth → JWT (HMAC, 1시간 만료) |
| 포트 | 8080 |
| 레포 | https://github.com/KONKUK-CLOG/backend-repo |

---

## 기술 스택 (build.gradle 기준)

| 분류 | 라이브러리 |
|------|-----------|
| Web | spring-boot-starter-web |
| JPA | spring-boot-starter-data-jpa, mysql-connector-j |
| MongoDB | spring-boot-starter-data-mongodb |
| Security | spring-boot-starter-security |
| JWT | jjwt-api/impl/jackson 0.12.6 |
| AWS SDK | aws-java-sdk-bom 2.31.1, lambda |
| 유효성 검사 | spring-boot-starter-validation |
| Rate Limit | bucket4j-core 8.10.1 |
| 기타 | Lombok, H2(test) |

---

## 데이터베이스 구조

### MySQL (JPA @Entity)

```
users
  ├── id, name, nickname, email, socialId
  ├── passwordHash, githubAccessTokenEncrypted, githubTokenExpiresAt
  └── createdAt, updatedAt (BaseTimeEntity)

blogs
  ├── id, title, content, status(DRAFT/PUBLISHED/DELETED), visibility(PUBLIC/PRIVATE)
  ├── viewCount, ogTitle, ogBlogUrl, publishedAt, deletedAt
  ├── codeDiff, codeContext, prompt, chatSessionId  ← Extension 발행 시 사용
  ├── author(FK→users), comments, bookmarks
  └── createdAt, updatedAt

comments
  ├── id, content, authorType(USER/GUEST), guestNickname
  ├── author(FK→users, nullable), blog(FK→blogs)
  └── createdAt, updatedAt

bookmarks
  ├── id, user(FK→users), blog(FK→blogs)
  └── createdAt, updatedAt

quizzes
  ├── id, question, answer
  └── createdAt, updatedAt
```

### MongoDB (Mongo @Document)

```
chat_sessions
  ├── _id (ObjectId)
  ├── userId (Long, MySQL users.id 참조)
  ├── projectId (String, nullable — null이면 프로젝트 미연동 세션)
  ├── status (ACTIVE — ARCHIVED 제거됨, 초과 시 삭제 후 새 세션 생성)
  ├── systemMessage (이전 대화 요약, 첫 세션은 null)
  ├── totalTokenCount (토큰 누적 추정치)
  └── createdAt, updatedAt

chat_messages
  ├── _id (ObjectId)
  ├── sessionId (chat_sessions._id 참조)
  ├── role (USER/ASSISTANT/SYSTEM)
  ├── content, reasoning, markdown
  ├── codeSnippets [{fileName, language, startLine, endLine, code}]
  ├── blogId (nullable, 블로그 연동용)
  ├── estimatedTokens
  └── createdAt

projects
  ├── _id (ObjectId)
  ├── userId (Long, MySQL users.id 참조)
  ├── name, description
  └── createdAt, updatedAt

project_files
  ├── _id (ObjectId)
  ├── projectId (projects._id 참조)
  ├── filePath (예: src/main.py)
  ├── language
  ├── content (파일 전체 텍스트)
  └── createdAt, updatedAt
```

---

## 전체 API 목록

### 인증 규칙 요약

- 공개(`permitAll`): `OPTIONS /**`, `/api/auth/**`, `GET /api/blogs/published`, `GET /api/blogs/users/**`, `GET /api/blogs/{id}`, `POST /api/blogs/*/view`, `GET /api/comments/blog/**`, `POST /api/comments`, `GET /api/quizzes/**`
- 인증 필요(`authenticated`): 그 외 모두

---

### `/api/auth` — GitHub OAuth

| Method | Path | 설명 |
|--------|------|------|
| GET | `/github/callback?code=` | GitHub 인증 코드 → JWT 발급 |

---

### `/api/users` — 사용자

| Method | Path | 설명 |
|--------|------|------|
| GET | `/{userId}` | 회원 조회 (본인만) |
| PATCH | `/{userId}/github-token` | GitHub 토큰 등록·갱신 (본인만) |
| DELETE | `/{userId}/github-token` | GitHub 토큰 해제 (본인만) |
| DELETE | `/{userId}` | 회원 탈퇴 (본인만) |

---

### `/api/blogs` — 블로그

| Method | Path | 설명 |
|--------|------|------|
| POST | `/` | 블로그 생성 |
| POST | `/extension/publish` | VS Code Extension 발행 (즉시 PUBLISHED) |
| PUT | `/{blogId}` | 수정 |
| DELETE | `/{blogId}` | 삭제(소프트) |
| POST | `/{blogId}/publish` | 발행 처리 |
| POST | `/{blogId}/view` | 조회수 증가 (공개) |
| GET | `/{blogId}` | 단건 조회 (공개) |
| GET | `/users/{userId}` | 사용자별 블로그 목록 (공개) |
| GET | `/published` | 전체 공개 블로그 피드 (공개) |
| POST | `/generate` (SSE) | LLM으로 블로그 초안 스트리밍 생성 |

---

### `/api/comments` — 댓글

| Method | Path | 설명 |
|--------|------|------|
| POST | `/` | 댓글 작성 (게스트/회원 모두 — 공개) |
| PUT | `/{commentId}` | 수정 |
| DELETE | `/{commentId}` | 삭제 |
| GET | `/blog/{blogId}` | 블로그별 댓글 목록 (공개) |

---

### `/api/bookmarks` — 북마크

| Method | Path | 설명 |
|--------|------|------|
| POST | `/` | 북마크 추가 |
| DELETE | `/{bookmarkId}` | 북마크 제거 |
| GET | `/` | 내 북마크 목록 |

---

### `/api/quizzes` — 퀴즈

| Method | Path | 설명 |
|--------|------|------|
| POST | `/` | 생성 |
| PUT | `/{quizId}` | 수정 |
| DELETE | `/{quizId}` | 삭제 |
| GET | `/{quizId}` | 단건 조회 (공개) |
| GET | `/` | 전체 목록 (공개) |

---

### `/api/chat` — 채팅 (Lambda SSE)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/history?projectId=` | 활성 세션 메시지 이력 (projectId 없으면 레거시 세션) |
| POST | `/send` (SSE) | 메시지 전송 → Lambda GENERATE → `reasoning`/`markdown`/`done` 이벤트 스트리밍 |

**요청 body (`ChatSendRequest`)**:
```json
{
  "chatSessionId": "optional",
  "projectId": "optional",
  "message": "질문 내용",
  "codeSnippets": []
}
```

---

### `/api/projects` — 프로젝트 코드베이스

| Method | Path | 설명 |
|--------|------|------|
| POST | `/` | 프로젝트 생성 |
| GET | `/` | 내 프로젝트 목록 |
| DELETE | `/{projectId}` | 삭제 (연관 파일·채팅 세션·메시지 정리) |
| POST | `/{projectId}/files` | 파일 추가 (최대 20개, 파일당 50KB) |
| GET | `/{projectId}/files` | 파일 목록 |
| PUT | `/{projectId}/files/{fileId}` | 파일 수정 |
| DELETE | `/{projectId}/files/{fileId}` | 파일 삭제 |

---

## Lambda 연동 구조

### 호출 방식
- AWS SDK v2 `LambdaClient.invoke` (동기 호출)
- 응답을 받은 후 SSE 이벤트로 분할 전송 (실시간 토큰 스트리밍 아님)

### 입력 (`LambdaPayload`)
```json
{
  "action": "GENERATE | SUMMARIZE",
  "userId": 1,
  "chatHistory": [{ "role": "user|assistant|system", "content": "...", "codeSnippets": [] }],
  "codeSnippets": [],
  "prompt": "...",
  "projectFiles": [{ "filePath": "src/main.py", "language": "python", "content": "..." }]
}
```
- `projectFiles`: 세션에 `projectId`가 있을 때만 백엔드가 채워서 전송
- Lambda 측에서 `projectFiles`를 아직 처리하지 않아도 무시 가능 (`@JsonIgnoreProperties`)

### 출력 (`LambdaResult`)
```json
{
  "reasoning": "사고 과정 텍스트",
  "markdown": "# 블로그 본문",
  "summary": "대화 요약 (SUMMARIZE 액션 시)"
}
```

### SSE 이벤트 순서 (GENERATE)
```
event: reasoning → { "content": "..." }
event: markdown  → { "content": "..." }
event: done      → { "sessionId": "...", "assistantMessageId": "..." }
event: error     → { "message": "..." }  (오류 시)
```

---

## 채팅 세션 Rotation 정책

- 토큰 한도: **100,000** (환경변수 `APP_CHAT_MAX_CONTEXT_TOKENS`)
- 초과 감지 시:
  1. Lambda `SUMMARIZE` 호출 → 요약문 수신
  2. 요약문을 `systemMessage`로 가진 **새 ACTIVE 세션** 저장 (`projectId` 유지)
  3. 기존 메시지 전체 삭제 (`deleteBySessionId`)
  4. 기존 세션 삭제 (`deleteById`)
  5. 이어서 Lambda `GENERATE` 호출 (동일 요청에서)
- 한도 미달 시: `GENERATE` 1회만 호출

---

## 보안 설정 요약

| 항목 | 값 |
|------|-----|
| JWT 최소 키 길이 | 32 bytes (256 bit) |
| JWT 만료 | 1시간 (환경변수 `JWT_EXPIRATION_MS`) |
| CORS | `SecurityProperties.corsAllowedOrigins` 화이트리스트만 허용, 와일드카드 금지 |
| Rate Limit (dev) | 일반 120/분, Auth 30/분, LLM 12/분 |
| Rate Limit (prod) | 일반 60/분, Auth 20/분, LLM 8/분 |
| Security Headers | `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff` |

---

## 환경변수 목록 (필수 / 선택)

| 변수명 | 필수 | 설명 | 기본값(dev) |
|--------|------|------|-------------|
| `DB_HOST` | 필수 | MySQL 호스트 | `localhost` |
| `DB_PORT` | 선택 | MySQL 포트 | `3306` |
| `DB_NAME` | 선택 | DB명 | `clog` |
| `DB_USERNAME` | 필수 | DB 사용자 | `root` |
| `DB_PASSWORD` | 필수 | DB 비밀번호 | — |
| `JWT_SECRET` | 필수 | JWT HMAC 키 (32자 이상) | — |
| `JWT_EXPIRATION_MS` | 선택 | JWT 만료(ms) | `3600000` |
| `MONGODB_URI` | 필수 | MongoDB 연결 문자열 | `mongodb://localhost:27017/clog` |
| `MONGODB_DATABASE` | 선택 | MongoDB DB명 | `clog` |
| `AWS_LAMBDA_FUNCTION_NAME` | 필수 | Lambda 함수명 | `clog-llm-generator` |
| `AWS_REGION` | 선택 | AWS 리전 | `ap-northeast-2` |
| `GITHUB_CLIENT_ID` | 필수 | GitHub OAuth App ID | — |
| `GITHUB_CLIENT_SECRET` | 필수 | GitHub OAuth Secret | — |
| `GITHUB_REDIRECT_URI` | 필수 | OAuth 콜백 URL | `http://localhost:8080/api/auth/github/callback` |
| `APP_BLOG_PUBLIC_BASE_URL` | 선택 | 블로그 공개 URL 접두사 | `http://localhost:3000/blog` |
| `APP_CORS_ALLOWED_ORIGINS` | 필수 | CORS 허용 오리진 (쉼표 구분) | `http://localhost:3000,http://localhost:5173,...` |
| `APP_CHAT_MAX_CONTEXT_TOKENS` | 선택 | 채팅 토큰 한도 | `100000` |
| `APP_PROJECT_MAX_FILES` | 선택 | 프로젝트당 파일 최대 개수 | `20` |
| `APP_PROJECT_MAX_FILE_BYTES` | 선택 | 파일 최대 크기 (bytes) | `51200` (50KB) |
| `APP_CRYPTO_SECRET` | 필수 | GitHub 토큰 암호화 키 (32자 이상) | — |

---

## 현재 구현 완료된 내용

### 인증/인가
- [x] GitHub OAuth 로그인 (code → access token → 사용자 정보 → JWT 발급)
- [x] JWT 기반 stateless 인증 필터
- [x] `SecurityUtils.requireCurrentUserId()` / `assertSelf()` 본인 검증

### 사용자
- [x] 회원 조회, GitHub 토큰 등록/해제, 회원 탈퇴

### 블로그
- [x] CRUD (생성, 수정, 삭제, 조회)
- [x] 발행 처리, 조회수 증가
- [x] VS Code Extension 직접 발행 (`/extension/publish`)
- [x] 공개 피드, 사용자별 목록

### 댓글
- [x] 게스트/회원 댓글 생성
- [x] 수정, 삭제, 블로그별 목록

### 북마크
- [x] 추가, 제거, 목록

### 퀴즈
- [x] CRUD + 공개 조회

### 채팅 (LLM 연동)
- [x] 활성 채팅 세션 관리 (유저별 ACTIVE 세션 1개)
- [x] Lambda GENERATE 호출 → SSE 스트리밍 (`reasoning` → `markdown` → `done`)
- [x] 토큰 한도 초과 시 SUMMARIZE → 세션 교체 (이전 세션/메시지 삭제)
- [x] 채팅 이력 조회 (`GET /api/chat/history?projectId=`)

### 프로젝트 코드베이스
- [x] 프로젝트 생성/목록/삭제
- [x] 파일 추가/수정/삭제/목록 (50KB, 20개 제한)
- [x] `ChatSession.projectId` 연결
- [x] GENERATE 시 `projectFiles`를 Lambda payload에 주입
- [x] 프로젝트 삭제 시 연관 채팅 세션·메시지 정리

### 인프라/설정
- [x] `docker-compose.yml` (API + MongoDB, MySQL은 외부)
- [x] `application-dev.properties` / `application-prod.properties` 프로파일 분리
- [x] Rate limiting (`bucket4j`, IP 기반, 인메모리)
- [x] Security 헤더 설정
- [x] CORS 화이트리스트

### 테스트
- [x] `LlmServiceTest` — Lambda 호출·파싱·에러 7개 테스트 (mock)
- [x] `ChatServiceTest` — 세션 생성·재사용·실패·rotation·이력 6개 테스트 (mock)

---

## 앞으로 해야 할 작업 체크리스트

### MongoDB 연동 검증

- [ ] Atlas 클러스터 Resume 후 IP Access List에 EC2(서버)·개발 PC IP 추가
- [ ] Atlas DB User 생성 (`chatReadWrite` 역할 이상)
- [ ] `MONGODB_URI` 환경변수에 Atlas 연결 문자열 주입 후 Spring 기동 확인
- [ ] 채팅 한 번 보내 `chat_sessions`, `chat_messages` 컬렉션 자동 생성 확인
- [ ] 프로젝트 파일 추가 후 `projects`, `project_files` 컬렉션 확인

### Lambda 검증

- [ ] Lambda 함수 AWS 콘솔 또는 AWS CLI로 **단독 테스트 이벤트** 실행
  - GENERATE 페이로드 예시로 `reasoning`, `markdown` 필드가 응답에 있는지 확인
  - SUMMARIZE 페이로드 예시로 `summary` 필드 확인
- [ ] Lambda 함수가 `projectFiles` 배열을 받아 프롬프트에 주입하는 로직 구현 (Lambda 팀)
- [ ] Lambda IAM 역할 확인 (백엔드 EC2 인스턴스 역할에 `lambda:InvokeFunction` 권한)
- [ ] Lambda 응답 타임아웃 설정 (SSE timeout 900초와 충돌하지 않도록)

### 프론트엔드 연동

- [ ] **SSE 클라이언트 구현**: `POST /api/chat/send`는 `EventSource`가 아닌 `fetch` + ReadableStream으로 처리 (POST + Bearer header 필요)
- [ ] `Authorization: Bearer <JWT>` 헤더 전송 확인
- [ ] `POST /api/auth/github/callback?code=` 응답에서 `data.token` 추출 후 저장
- [ ] `projectId`를 채팅·블로그 생성 요청에 포함하는 UX 흐름 설계
- [ ] CORS 오리진을 `APP_CORS_ALLOWED_ORIGINS`에 추가 (프론트 배포 URL 확정 후)

### 통합 테스트 (3자 연동: 프론트 + 백엔드 + Lambda)

- [ ] GitHub OAuth 전체 흐름 테스트 (실제 리디렉션 → JWT 수신)
- [ ] 채팅 전송 → Lambda GENERATE → SSE 수신 → 프론트 렌더링 확인
- [ ] Extension 발행 (`POST /api/blogs/extension/publish`) → 공개 URL 반환 확인
- [ ] 토큰 한도 초과 시 세션 rotation 실제 동작 확인 (대용량 대화)
- [ ] 프로젝트 파일 업로드 → 채팅 → Lambda payload에 `projectFiles` 포함 여부 로그 확인

### 배포

- [ ] EC2 인스턴스에 IAM 역할 부여 (`lambda:InvokeFunction`, RDS 접근 등)
- [ ] `SPRING_PROFILES_ACTIVE=prod` + 모든 필수 환경변수 주입
- [ ] `application-prod.properties` 기준 `ddl-auto=none` 확인 (스키마 직접 관리)
- [ ] RDS MySQL 보안 그룹 — EC2에서 3306 인바운드 허용
- [ ] MongoDB Atlas Network Access — EC2 IP 추가
- [ ] HTTPS 설정 (ALB SSL 인증서 또는 nginx + Let's Encrypt)

### 기능 보강 (선택)

- [ ] 블로그 `GET /{blogId}` 에서 `visibility=PRIVATE`일 때 본인만 조회 가능하도록 서비스 레이어에 검증 추가 (현재 URL상 공개이나 서비스 내부 제한 여부 확인 필요)
- [ ] 퀴즈 생성·수정·삭제 권한 검토 (현재 `authenticated`이나 관리자 역할 구분 없음)
- [ ] Extension `chatSessionId` → `projectId` 연동 흐름 정의 (Extension이 어느 시점에 프로젝트를 선택·생성하는지)
- [ ] 채팅 히스토리 페이지네이션 (현재 전체 조회)
- [ ] 프로젝트 파일 일괄 업로드 API (현재 한 번에 1개)

---

## 로컬 실행 방법

### 1. MongoDB + API 컨테이너

```bash
# .env 또는 환경변수 설정 후
docker compose up
```

필수 환경변수 (호스트에서):
```
AWS_LAMBDA_FUNCTION_NAME=clog-llm-generator
AWS_REGION=ap-northeast-2
DB_HOST=host.docker.internal   # Docker Desktop (Mac/Windows)
DB_USERNAME=...
DB_PASSWORD=...
JWT_SECRET=...
GITHUB_CLIENT_ID=...
GITHUB_CLIENT_SECRET=...
APP_CRYPTO_SECRET=...
```

### 2. 로컬 Gradle 실행 (MongoDB Atlas 연결)

```bash
export SPRING_PROFILES_ACTIVE=dev
export MONGODB_URI="mongodb+srv://user:pass@cluster.mongodb.net/clog?retryWrites=true&w=majority"
# + 위 환경변수들
./gradlew bootRun
```

### 3. 테스트 실행 (Windows — 한글 경로 우회)

```powershell
cmd /c mklink /J "C:\clog-backend" "<실제 경로>\backend-repo"
cd C:\clog-backend
.\gradlew.bat test -PenableTests
```

---

## 주요 패키지 구조

```
konkuk.clog
├── domain
│   ├── blog        — BlogController, BlogService, Blog(JPA)
│   ├── bookmark    — BookmarkController, BookmarkService, Bookmark(JPA)
│   ├── chat        — ChatController, ChatService
│   │                 document: ChatSession, ChatMessage (Mongo)
│   │                 dto: ChatSendRequest, ChatHistoryResponse, ChatMessageView
│   ├── comment     — CommentController, CommentService, Comment(JPA)
│   ├── llm         — LlmController, LlmService
│   │                 config: AwsLambdaConfig
│   │                 dto: LambdaPayload, LambdaResult, LambdaChatTurn, ProjectFileContext, BlogGenerateRequest
│   ├── project     — ProjectController, ProjectService
│   │                 document: Project, ProjectFile (Mongo)
│   │                 dto: ProjectCreateRequest, ProjectResponse, ProjectFileCreateRequest,
│   │                      ProjectFileUpdateRequest, ProjectFileResponse
│   ├── quiz        — QuizController, QuizService, Quiz(JPA)
│   └── user        — UserController, AuthController
│                     service: UserService, AuthService, GithubOAuthService
│                     domain: User(JPA)
└── global
    ├── config      — SecurityConfig, SecurityProperties, ApplicationSecurityStartupValidator
    ├── dto         — ApiResponse<T>
    ├── exception   — BusinessException, ErrorCode, GlobalExceptionHandler
    ├── jpa         — BaseTimeEntity
    └── security    — JwtTokenProvider, JwtAuthenticationFilter, SecurityUtils, ApiRateLimitFilter
```

---

## 관련 문서

- [프로젝트_코드베이스_컨텍스트_설계.md](./프로젝트_코드베이스_컨텍스트_설계.md) — Project 도메인 설계 상세
