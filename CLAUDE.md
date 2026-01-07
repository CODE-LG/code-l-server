브래# CODE-L Project Rules & Guidelines

## 프로젝트 개요

CODE-L은 레즈비언 만남 애플리케이션의 백엔드 서버입니다. 사용자 프로필 관리, 시그널 기반 매칭, 실시간 채팅, 추천 시스템 등을 제공하는 Spring Boot 기반의 REST API 서버입니다.

### 기술 스택
- **Language:** Kotlin 1.9.25
- **Framework:** Spring Boot 3.4.3
- **Build Tool:** Gradle (Kotlin DSL)
- **Database:** MySQL 8.0
- **Migration:** Flyway
- **Authentication:** JWT (Bearer Token)
- **Real-time:** WebSocket (STOMP)
- **Cloud Services:** AWS S3, Firebase FCM, Discord Webhook
- **Monitoring:** Spring Actuator, Prometheus, Loki

---

## 🚨 중요한 규칙 (절대 지켜야 함)

### 1. **application.yml 파일 수정 금지**
- `application.yml` 및 `application-dev.yml` 파일은 **절대로 임의로 수정하지 마세요**
- 데이터베이스 연결, JWT 시크릿, AWS 설정 등 민감한 정보가 포함되어 있습니다
- 변경이 필요한 경우 반드시 팀과 상의 후 진행하세요

### 2. **Flyway 마이그레이션 규칙**
- 데이터베이스 스키마 변경은 **반드시 Flyway 마이그레이션**으로만 수행
- JPA DDL Auto는 `validate`로 설정되어 있습니다
- 마이그레이션 파일 위치: `src/main/resources/db/migration`
- 파일명 규칙: `V{숫자}__{설명}.sql` (예: `V12__add_new_column.sql`)
- 이미 적용된 마이그레이션 파일은 **절대 수정하지 마세요**

### 3. **보안 및 인증**
- JWT 토큰 검증은 `JwtAuthFilter`에서 자동으로 처리됩니다
- 컨트롤러에서 `@LoginMember` 어노테이션으로 인증된 사용자 정보를 받습니다
- 공개 엔드포인트는 `JwtAuthFilter`의 `PUBLIC_ENDPOINTS`에 등록해야 합니다
- 절대 비밀번호, 토큰, API 키를 로그에 출력하지 마세요

---

## 프로젝트 구조

### 도메인 기반 모듈 구조 (Domain-Driven Design)

```
src/main/kotlin/codel/
├── member/              # 사용자 관리 및 프로필
├── signal/              # 매칭 시그널 (좋아요/관심 표시)
├── chat/                # 실시간 채팅 및 채팅방
├── notification/        # FCM 푸시 알림 및 Discord 알림
├── recommendation/      # 추천 알고리즘 및 매칭
├── report/              # 신고 기능
├── block/               # 차단 기능
├── question/            # 프로필 질문 뱅크
├── admin/               # 관리자 페이지 및 프로필 심사
├── auth/                # 인증 관련 (JWT Provider 등)
├── config/              # 애플리케이션 설정
└── common/              # 공통 유틸리티 및 Base 엔티티
```

### 각 모듈 내부 구조

```
module/
├── domain/              # 엔티티, Enum, 도메인 인터페이스
├── presentation/        # 컨트롤러 및 DTO
│   ├── request/        # 요청 DTO
│   ├── response/       # 응답 DTO
│   └── swagger/        # Swagger 문서 인터페이스
├── business/            # 비즈니스 로직 (Service)
├── infrastructure/      # Repository, 외부 서비스 어댑터
└── exception/           # 모듈별 예외 클래스
```

---

## 코딩 컨벤션 및 패턴

### 1. 레이어 분리 원칙

**Controller (Presentation Layer)**
- HTTP 요청/응답 처리만 담당
- 비즈니스 로직은 Service로 위임
- `@LoginMember`로 인증된 사용자 정보 주입받기
- Swagger 문서화 필수 (`@Tag`, `@Operation`)

```kotlin
@RestController
@RequestMapping("/v1/members")
@Tag(name = "Member", description = "회원 관리 API")
class MemberController(
    private val memberService: MemberService
) : MemberApi {

    @GetMapping("/me")
    @Operation(summary = "내 프로필 조회")
    fun getMyProfile(@LoginMember member: Member): ProfileResponse {
        return memberService.getMyProfile(member)
    }
}
```

**Service (Business Layer)**
- 비즈니스 로직 구현
- `@Transactional` 적절히 사용
- 도메인 규칙 검증
- 예외는 `CodelException` 계열로 발생

```kotlin
@Service
@Transactional(readOnly = true)
class MemberService(
    private val memberRepository: MemberJpaRepository,
    private val s3Service: S3Service
) : Loggable {

    @Transactional
    fun updateProfile(member: Member, request: ProfileUpdateRequest): ProfileResponse {
        member.validateCanUpdateProfile()
        member.updateProfile(request)
        return ProfileResponse.from(member)
    }
}
```

**Repository (Infrastructure Layer)**
- Spring Data JPA 사용
- 복잡한 쿼리는 `@Query` 사용
- N+1 문제 방지를 위해 `@EntityGraph` 활용

```kotlin
@Repository
interface MemberJpaRepository : JpaRepository<Member, Long> {
    fun findByOauthTypeAndOauthId(oauthType: OauthType, oauthId: String): Member?

    @Query("SELECT m FROM Member m JOIN FETCH m.profile WHERE m.id = :id")
    fun findByIdWithProfile(id: Long): Member?
}
```

### 2. DTO 변환 패턴

**Request DTO → Entity**
```kotlin
data class SignalSendRequest(
    val toMemberId: Long,
    val answer: String?
) {
    fun toEntity(fromMember: Member, toMember: Member): Signal {
        return Signal(
            fromMember = fromMember,
            toMember = toMember,
            answer = answer
        )
    }
}
```

**Entity → Response DTO**
```kotlin
data class ProfileResponse(
    val id: Long,
    val codeName: String,
    val age: Int,
    // ...
) {
    companion object {
        fun from(member: Member): ProfileResponse {
            return ProfileResponse(
                id = member.id!!,
                codeName = member.profile.codeName,
                age = member.profile.calculateAge()
            )
        }
    }
}
```

### 3. 예외 처리 패턴

```kotlin
// 모듈별 예외 정의
class MemberException(
    status: HttpStatus,
    message: String
) : CodelException(status, message)

// 사용 예시
fun getMemberById(id: Long): Member {
    return memberRepository.findById(id)
        ?: throw MemberException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.")
}
```

### 4. 로깅 패턴

```kotlin
interface Loggable {
    val log: KLogger get() = KotlinLogging.logger {}
}

@Service
class MemberService : Loggable {
    fun someMethod() {
        log.info { "Member ${member.id} performed action" }
        log.error { "Error occurred: ${exception.message}" }
    }
}
```

### 5. Kotlin 코드 스타일

- **Data Class**: DTO, Request, Response에 사용
- **Extension Function**: 유틸리티 함수는 확장 함수로 작성
- **Safe Call & Elvis Operator**: `?.`, `?:` 적극 활용
- **Scope Function**: `apply`, `let`, `run` 등 적절히 사용
- **Named Arguments**: 파라미터가 3개 이상이면 named arguments 사용

```kotlin
// Good
val member = Member(
    email = request.email,
    oauthType = OauthType.KAKAO,
    oauthId = request.oauthId
)

// Extension function
fun Member.isProfileComplete(): Boolean {
    return memberStatus == MemberStatus.DONE
}
```

---

## API 엔드포인트 규칙

### 1. URL 패턴
- **버전 관리**: 모든 엔드포인트는 `/v1/` prefix 사용
- **복수형 리소스명**: `/v1/members`, `/v1/signals`, `/v1/chatrooms`
- **계층 구조**: 관계는 URL 계층으로 표현 (`/v1/chatrooms/{id}/chats`)

### 2. HTTP 메서드
- `GET`: 조회 (멱등성)
- `POST`: 생성 또는 액션 수행
- `PUT`: 전체 수정
- `PATCH`: 부분 수정
- `DELETE`: 삭제

### 3. 응답 형식
- **성공**: HTTP 200-201, JSON body 또는 빈 응답
- **에러**: HTTP 4xx/5xx, ErrorResponse 객체
  ```json
  {
    "timestamp": "2025-11-29T10:00:00",
    "status": 404,
    "path": "/v1/members/999",
    "message": "회원을 찾을 수 없습니다.",
    "stackTrace": "..."
  }
  ```

### 4. 페이지네이션
- Query Parameter: `page` (0-based), `size` (기본값: 10)
- Response: `Page<T>` 객체 사용
```kotlin
@GetMapping
fun getMembers(
    @RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "10") size: Int
): Page<MemberResponse>
```

---

## 엔티티 및 도메인 규칙

### 1. BaseTimeEntity 상속
```kotlin
@MappedSuperclass
abstract class BaseTimeEntity {
    @CreatedDate
    var createdAt: LocalDateTime = LocalDateTime.now()

    @LastModifiedDate
    var updatedAt: LocalDateTime = LocalDateTime.now()
}
```

### 2. 연관관계 매핑
- **기본 전략**: LAZY 로딩
- **양방향 관계**: 연관관계 편의 메서드 작성
- **Cascade**: 신중하게 사용 (일반적으로 부모-자식 관계에만)
- **OrphanRemoval**: 컬렉션에서 제거 시 자식도 삭제할 때만 사용

```kotlin
@Entity
class Member : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @OneToOne(mappedBy = "member", cascade = [CascadeType.ALL], orphanRemoval = true)
    var profile: Profile? = null

    @OneToMany(mappedBy = "member", cascade = [CascadeType.ALL])
    var codeImages: MutableList<CodeImage> = mutableListOf()
}
```

### 3. Enum 타입 사용
- 상태 값은 Enum으로 정의
- `@Enumerated(EnumType.STRING)` 사용 (ORDINAL 금지)

```kotlin
enum class MemberStatus {
    SIGNUP,           // 회원가입
    PHONE_VERIFIED,   // 휴대폰 인증 완료
    ESSENTIAL_COMPLETED,  // 필수 프로필 완료
    PERSONALITY_COMPLETED, // 성격 프로필 완료
    HIDDEN_COMPLETED, // 히든 프로필 완료
    PENDING,          // 심사 대기
    REJECT,           // 반려
    DONE,             // 승인 완료
    WITHDRAWN,        // 탈퇴
    ADMIN             // 관리자
}
```

---

## WebSocket 및 실시간 채팅

### 1. WebSocket 엔드포인트
- **연결**: `/ws` (STOMP over SockJS)
- **발행**: `/pub/v1/chatroom/{chatRoomId}/chat`
- **구독**: `/sub/v1/chatroom/{chatRoomId}` (특정 채팅방)
- **구독**: `/sub/v1/chatroom/member/{memberId}` (내 모든 채팅방 알림)

### 2. 메시지 타입
```kotlin
enum class ChatContentType {
    CHAT,    // 일반 채팅 메시지
    SYSTEM   // 시스템 메시지 (입장, 퇴장 등)
}

enum class ChatSenderType {
    SYSTEM,  // 시스템이 보낸 메시지
    MEMBER   // 회원이 보낸 메시지
}
```

### 3. 인증 및 권한
- `JwtConnectInterceptor`: WebSocket 연결 시 JWT 검증
- `ChatRoomSubscriptionInterceptor`: 채팅방 구독 시 권한 검증
- `@LoginMember`로 메시지 송신자 확인

---

## 추천 및 매칭 시스템

### 1. 추천 설정 (application.yml)
```yaml
recommendation:
  daily-code-count: 3           # 일일 매칭 개수
  code-time-count: 2            # 코드타임 참여자 수
  code-time-slots: ["10:00", "22:00"]  # 코드타임 시간대
  daily-refresh-time: "00:00"   # 일일 매칭 갱신 시간
  repeat-avoid-days: 3          # 재추천 방지 기간 (일)
  allow-duplicate: true         # 중복 추천 허용 여부
```

### 2. 추천 종류
- **Daily Code Matching**: 매일 자정에 새로운 3명 추천
- **Code Time**: 특정 시간대 (10시, 22시)에 2명 추천
- **Random**: 랜덤 추천
- **Legacy**: 기존 추천 로직

### 3. 추천 제외 조건
- 차단한 회원
- 최근 N일 내 추천받은 회원 (repeat-avoid-days)
- 시그널을 이미 보낸 회원
- 프로필 심사가 완료되지 않은 회원 (DONE 상태가 아님)

---

## 파일 업로드 (S3)

### 1. 이미지 타입
- **Code Image**: 프로필 대표 이미지 (공개)
- **Face Image**: 얼굴 사진 (히든 프로필, 시그널 승인 후 공개)

### 2. 업로드 플로우
1. 클라이언트가 multipart/form-data로 이미지 전송
2. S3Service가 S3에 업로드
3. S3 URL을 DB에 저장
4. 이전 이미지가 있으면 S3에서 삭제

```kotlin
@PutMapping("/me/profile/code-images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
fun updateCodeImages(
    @LoginMember member: Member,
    @RequestPart codeImages: List<MultipartFile>
): ProfileResponse {
    return memberService.updateCodeImages(member, codeImages)
}
```

### 3. 파일 검증
- 허용된 확장자: jpg, jpeg, png, gif, webp
- 최대 파일 크기: 10MB (설정 가능)

---

## 알림 시스템

### 1. Firebase Cloud Messaging (FCM)
- 회원별 FCM 토큰 저장 (`Member.fcmToken`)
- 시그널 수신, 채팅 메시지, 코드 공개 요청 등에 푸시 알림
- 비동기 처리 (`@Async`)

```kotlin
@Service
class NotificationService(
    private val firebaseMessaging: FirebaseMessaging
) {
    @Async
    fun sendSignalNotification(member: Member, fromMember: Member) {
        val message = Message.builder()
            .setToken(member.fcmToken)
            .setNotification(...)
            .build()
        firebaseMessaging.send(message)
    }
}
```

### 2. Discord Webhook
- 관리자 알림용 (회원 탈퇴, 프로필 반려 등)
- 비동기 처리

---

## 테스트 작성 가이드

### 1. 테스트 구조
- 단위 테스트: 비즈니스 로직, 유틸리티 함수
- 통합 테스트: API 엔드포인트, 데이터베이스 연동
- `@SpringBootTest`로 통합 테스트 작성
- H2 in-memory DB 사용

### 2. 데이터 정리
- `DataCleanerExtension`으로 각 테스트 후 DB 클린업
- 트랜잭션 롤백 활용

### 3. Rest-Assured 사용
```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberControllerTest {

    @LocalServerPort
    var port: Int = 0

    @Test
    fun `내 프로필 조회 성공`() {
        given()
            .port(port)
            .header("Authorization", "Bearer $token")
        .`when`()
            .get("/v1/member/me")
        .then()
            .statusCode(200)
            .body("codeName", equalTo("테스트"))
    }
}
```

---

## 성능 및 모니터링

### 1. 데이터베이스 최적화
- N+1 문제 방지: `@EntityGraph`, JOIN FETCH 사용
- 인덱스 활용: Flyway 마이그레이션으로 인덱스 추가
- 쿼리 로그 확인: `spring.jpa.show-sql=true` (개발 환경)

### 2. 비동기 처리
- 알림 발송, 외부 API 호출 등은 `@Async` 사용
- Executor 설정으로 스레드 풀 관리

```kotlin
@Configuration
@EnableAsync
class AsyncConfig : AsyncConfigurer {
    override fun getAsyncExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 5
        executor.maxPoolSize = 10
        executor.queueCapacity = 100
        executor.initialize()
        return executor
    }
}
```

### 3. 모니터링
- **Actuator Health Check**: `/actuator/health`
- **Prometheus Metrics**: `/actuator/prometheus`
- **로그 수집**: Loki Logback Appender

---

## 보안 체크리스트

- [ ] JWT 토큰 검증 로직 확인
- [ ] 민감한 정보 로그 출력 금지 (비밀번호, 토큰, API 키)
- [ ] SQL Injection 방지 (Parameterized Query 사용)
- [ ] XSS 방지 (입력 값 검증 및 이스케이프)
- [ ] CSRF 방지 (필요 시 CSRF 토큰 사용)
- [ ] CORS 설정 확인
- [ ] 파일 업로드 검증 (확장자, 크기, MIME 타입)
- [ ] 권한 검증 (본인 데이터만 수정 가능한지 확인)

---

## 배포 및 운영

### 1. 환경 분리
- **개발 환경**: `application-dev.yml`
- **운영 환경**: `application.yml`
- 프로필 활성화: `spring.profiles.active=dev` (개발 시)

### 2. 빌드 및 실행
```bash
# 빌드
./gradlew clean build

# 실행
java -jar build/libs/codel-0.0.1-SNAPSHOT.jar

# 개발 환경으로 실행
java -jar -Dspring.profiles.active=dev build/libs/codel-0.0.1-SNAPSHOT.jar
```

### 3. Health Check
```bash
curl http://localhost:8080/actuator/health
```

---

## 문의 및 지원

프로젝트 관련 문의사항이 있거나 규칙 변경이 필요한 경우 팀 리더에게 문의하세요.

**마지막 업데이트**: 2025-11-29