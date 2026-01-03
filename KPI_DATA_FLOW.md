# KPI 데이터 수집 흐름 설명서

## 📋 목차
1. [전체 구조](#전체-구조)
2. [데이터 수집 시점](#데이터-수집-시점)
3. [수집 흐름 (Step by Step)](#수집-흐름)
4. [각 KPI별 상세 설명](#각-kpi별-상세-설명)

---

## 🏗️ 전체 구조

```
┌─────────────────────────────────────────────────────────┐
│                    KpiScheduler                         │
│  - 매일 새벽 1시 자동 실행                                │
│  - 앱 시작 시 최근 7일 집계                               │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│              KpiBatchService                            │
│  aggregateDailyKpi(kstDate: LocalDate)                 │
│  - 한국 시간 기준 날짜를 받아서 하루치 KPI 집계           │
└────────────────┬────────────────────────────────────────┘
                 │
                 ├─── 1. 시그널 KPI 집계
                 ├─── 2. 채팅 KPI 집계
                 ├─── 3. 질문 KPI 집계
                 ├─── 4. 코드해제 KPI 집계
                 └─── 5. 종료된 채팅방 KPI 집계

                 ▼
┌─────────────────────────────────────────────────────────┐
│                   DailyKpi 엔티티                       │
│  - DB에 날짜별로 저장됨 (daily_kpi 테이블)               │
└─────────────────────────────────────────────────────────┘
```

---

## ⏰ 데이터 수집 시점

### 1. 자동 수집
```kotlin
@Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
fun runDailyKpiAggregation() {
    val yesterdayKst = DateTimeFormatter.getToday("ko").minusDays(1)
    kpiBatchService.aggregateDailyKpi(yesterdayKst)
}
```
**시점**: 매일 새벽 1시 (한국 시간)
**대상**: 어제 날짜 (예: 1월 2일 새벽 1시 → 1월 1일 KPI 집계)

### 2. 앱 시작 시 수집
```kotlin
@PostConstruct
fun aggregateRecentKpisOnStartup() {
    val today = DateTimeFormatter.getToday("ko")
    for (i in 1..7) {
        val targetDate = today.minusDays(i.toLong())
        kpiBatchService.aggregateDailyKpi(targetDate)
    }
}
```
**시점**: 애플리케이션 시작 시
**대상**: 최근 7일치 (테스트/백필용)

### 3. 수동 수집
```kotlin
POST /v1/admin/kpi/aggregate?date=2025-01-01
```
**시점**: 관리자가 원할 때
**대상**: 지정한 날짜

---

## 🔄 수집 흐름 (Step by Step)

### Step 0: 시간 변환
```kotlin
fun aggregateDailyKpi(kstDate: LocalDate) {
    // 한국 날짜를 UTC 시간 범위로 변환
    val (utcStart, utcEnd) = DateTimeFormatter.getUtcRangeForKstDate(kstDate)
}
```

**예시**:
- 입력: `2025-01-01` (KST)
- 변환: `2024-12-31 15:00:00` ~ `2025-01-01 14:59:59` (UTC)

**이유**: DB에 UTC로 저장되어 있어서, 한국 날짜 기준으로 조회하려면 UTC 시간 범위로 변환 필요

---

### Step 1: 시그널 KPI 집계

```kotlin
private fun aggregateSignalKpi(
    dailyKpi: DailyKpi,
    utcStart: LocalDateTime,
    utcEnd: LocalDateTime
) {
    // 1. 시그널 보낸 수
    dailyKpi.signalSentCount = kpiSignalRepository.countByCreatedAtBetween(utcStart, utcEnd)

    // 2. 시그널 수락 수
    dailyKpi.signalAcceptedCount = kpiSignalRepository.countApprovedByUpdatedAtBetween(utcStart, utcEnd)
}
```

#### 📊 수집 데이터

| KPI | 데이터 소스 | 조건 | 의미 |
|-----|-----------|------|------|
| **시그널 보낸 수** | `Signal.createdAt` | `createdAt BETWEEN utcStart AND utcEnd` | 해당 날짜에 보낸 시그널 총 개수 |
| **시그널 수락 수** | `Signal.updatedAt` + `Signal.status` | `updatedAt BETWEEN ... AND status = APPROVED` | 해당 날짜에 수락된 시그널 총 개수 |
| **시그널 수락률** | 계산 | `(수락 수 / 보낸 수) * 100` | 자동 계산 (DailyKpi.getSignalAcceptanceRate()) |

**쿼리 예시**:
```sql
-- 시그널 보낸 수
SELECT COUNT(*) FROM signal WHERE created_at BETWEEN '2024-12-31 15:00' AND '2025-01-01 15:00';

-- 시그널 수락 수
SELECT COUNT(*) FROM signal
WHERE updated_at BETWEEN '2024-12-31 15:00' AND '2025-01-01 15:00'
AND status = 'APPROVED';
```

---

### Step 2: 채팅 KPI 집계 (가장 복잡)

```kotlin
private fun aggregateChatKpi(
    dailyKpi: DailyKpi,
    kstDate: LocalDate,
    utcStart: LocalDateTime,
    utcEnd: LocalDateTime
) {
    // 1. 해당 날짜에 생성된 채팅방 수
    dailyKpi.openChatroomsCount = kpiChatRepository.countByCreatedAtBetween(utcStart, utcEnd)

    // 2. 활성 채팅방 수 (최근 7일 내 활동)
    val utcAsOfDate = DateTimeFormatter.convertKstToUtc(kstDate.atTime(LocalTime.MAX))
    val utcSevenDaysAgo = DateTimeFormatter.convertKstToUtc(kstDate.minusDays(7).atStartOfDay())

    dailyKpi.activeChatroomsCount = kpiChatRepository.countActiveChatroomsAsOfDate(
        utcAsOfDate,
        utcSevenDaysAgo
    )

    // 3. 해당 날짜에 생성된 채팅방 조회
    val createdChatRooms = kpiChatRepository.findByCreatedAtBetween(utcStart, utcEnd)

    // 4. 각 채팅방의 메시지를 분석하여 메트릭 계산
    createdChatRooms.forEach { chatRoom ->
        val messages = kpiChatMessageRepository.findByChatRoomOrderBySentAtAsc(chatRoom)

        if (messages.size > 6) {  // 템플릿 6개 제외
            firstMessageCount++
            totalMessageCount += messages.size

            if (hasThreeTurnOrMore(messages)) threeTurnCount++
            if (hasReturnWithin24Hours(messages)) returnWithin24hCount++
        }
    }

    // 5. 비율 계산
    dailyKpi.firstMessageRate = calculateRate(firstMessageCount, totalChatRooms)
    dailyKpi.threeTurnRate = calculateRate(threeTurnCount, totalChatRooms)
    dailyKpi.chatReturnRate = calculateRate(returnWithin24hCount, totalChatRooms)
    dailyKpi.avgMessageCount = totalMessageCount / totalChatRooms
}
```

#### 📊 수집 데이터

| KPI | 데이터 소스 | 계산 방법 | 의미 |
|-----|-----------|----------|------|
| **열린 채팅방 수** | `ChatRoom.createdAt` | 해당 날짜에 생성된 채팅방 | 새로 매칭되어 채팅이 시작된 수 |
| **활성 채팅방 수** | `ChatRoom` + `Chat` | 종료되지 않고 + 최근 7일 내 메시지 있음 | 실제로 대화가 오가는 채팅방 |
| **FMR (첫메시지율)** | `Chat` | 템플릿 6개 이후 메시지가 있는 채팅방 비율 | (첫 메시지 보낸 채팅방 / 전체 채팅방) * 100 |
| **3턴 이상 대화 비율** | `Chat` | 두 멤버가 각각 3개 이상 메시지 교환한 비율 | 의미 있는 대화가 이루어진 비율 |
| **CRR (24h 재방문)** | `Chat` | 첫 메시지 후 24시간 내 다시 대화한 비율 | 채팅방에 재접속한 비율 |
| **평균 메시지 수** | `Chat` | 채팅방당 평균 메시지 개수 | 총 메시지 수 / 채팅방 수 |

#### 🔍 상세 로직

**1) 활성 채팅방 판단**:
```sql
SELECT COUNT(*) FROM chat_room cr
WHERE cr.status != 'CLOSED'  -- 종료되지 않음
AND cr.created_at <= '2025-01-01 15:00'  -- 해당 날짜까지 생성됨
AND EXISTS (
    SELECT 1 FROM chat c
    WHERE c.chat_room_id = cr.id
    AND c.sent_at >= '2024-12-25 15:00'  -- 최근 7일 내 메시지
    AND c.sent_at <= '2025-01-01 15:00'
)
```

**2) 3턴 이상 대화 판단**:
```kotlin
private fun hasThreeTurnOrMore(messages: List<Chat>): Boolean {
    val realMessages = messages
        .drop(6)  // 템플릿 6개 제외
        .filter { it.senderType == ChatSenderType.USER && it.chatContentType == ChatContentType.TEXT }

    if (realMessages.size < 6) return false

    val messagesByMember = realMessages.groupBy { it.fromChatRoomMember?.member?.id }

    return messagesByMember.size >= 2 &&  // 두 명이 대화
           messagesByMember.all { it.value.size >= 3 }  // 각자 최소 3개 메시지
}
```

**3) 24시간 내 재방문 판단**:
```kotlin
private fun hasReturnWithin24Hours(messages: List<Chat>): Boolean {
    val realMessages = messages.drop(6).filter { ... }

    if (realMessages.size < 2) return false

    val firstMessageTime = realMessages.first().getSentAtOrThrow()
    val secondMessageTime = realMessages[1].getSentAtOrThrow()

    val hoursDiff = Duration.between(firstMessageTime, secondMessageTime).toHours()

    return hoursDiff <= 24
}
```

---

### Step 3: 질문 KPI 집계

```kotlin
private fun aggregateQuestionKpi(
    dailyKpi: DailyKpi,
    utcStart: LocalDateTime,
    utcEnd: LocalDateTime
) {
    // 1. 질문 클릭 수 (초기 질문 제외)
    dailyKpi.questionClickCount = kpiQuestionRepository
        .countQuestionClicksByCreatedAtBetweenExcludingInitial(utcStart, utcEnd)

    // 2. 질문 사용 채팅방 수 (초기 질문 제외)
    dailyKpi.questionUsedChatroomsCount = kpiQuestionRepository
        .countDistinctChatRoomsByCreatedAtBetweenExcludingInitial(utcStart, utcEnd)

    // 3. 해당 날짜에 생성된 채팅방 조회
    val createdChatRooms = kpiChatRepository.findByCreatedAtBetween(utcStart, utcEnd)
    val createdChatRoomIds = createdChatRooms.mapNotNull { it.id }

    // 4. 이 채팅방들이 (언제든) 질문을 사용했는지 확인
    val questionUsedChatRoomIds = kpiQuestionRepository
        .findChatRoomIdsWithQuestionsFromList(createdChatRoomIds)
        .toSet()

    // 5. 질문 사용/미사용 채팅방 분리
    val (questionUsedRooms, questionNotUsedRooms) = createdChatRooms.partition {
        it.id in questionUsedChatRoomIds
    }

    // 6. 각 그룹별 메트릭 계산
    val usedMetrics = calculateChatMetrics(questionUsedRooms)
    dailyKpi.questionUsedAvgMessageCount = usedMetrics.avgMessageCount
    dailyKpi.questionUsedThreeTurnRate = usedMetrics.threeTurnRate
    dailyKpi.questionUsedChatReturnRate = usedMetrics.chatReturnRate

    val notUsedMetrics = calculateChatMetrics(questionNotUsedRooms)
    dailyKpi.questionNotUsedAvgMessageCount = notUsedMetrics.avgMessageCount
    dailyKpi.questionNotUsedThreeTurnRate = notUsedMetrics.threeTurnRate
    dailyKpi.questionNotUsedChatReturnRate = notUsedMetrics.chatReturnRate
}
```

#### 📊 수집 데이터

| KPI | 데이터 소스 | 조건 | 의미 |
|-----|-----------|------|------|
| **질문 클릭 수** | `ChatRoomQuestion.createdAt` | `isInitial = false` | 해당 날짜에 질문하기 버튼을 클릭한 총 횟수 |
| **질문 사용 채팅방 수** | `ChatRoomQuestion` | `isInitial = false` + DISTINCT chatRoom | 질문 기능을 사용한 채팅방 수 |
| **질문 사용 - 평균 메시지** | `Chat` | 질문 사용한 채팅방만 | 질문을 사용한 채팅방의 평균 메시지 수 |
| **질문 사용 - 3턴 비율** | `Chat` | 질문 사용한 채팅방만 | 질문을 사용한 채팅방의 3턴 이상 대화 비율 |
| **질문 사용 - CRR** | `Chat` | 질문 사용한 채팅방만 | 질문을 사용한 채팅방의 재방문률 |
| **질문 미사용 - 평균 메시지** | `Chat` | 질문 미사용 채팅방만 | 질문을 사용하지 않은 채팅방의 평균 메시지 수 |
| **질문 미사용 - 3턴 비율** | `Chat` | 질문 미사용 채팅방만 | 질문을 사용하지 않은 채팅방의 3턴 이상 대화 비율 |
| **질문 미사용 - CRR** | `Chat` | 질문 미사용 채팅방만 | 질문을 사용하지 않은 채팅방의 재방문률 |

#### 🔍 핵심 로직: 질문 사용 여부 판단

**중요**: 채팅방 생성일 기준, 이후 언제든 질문을 사용했는지 확인

```sql
-- 잘못된 방법 (수정 전)
SELECT DISTINCT chat_room_id FROM chat_room_question
WHERE is_initial = false
AND created_at BETWEEN '2025-01-01 00:00' AND '2025-01-01 23:59'
-- 문제: 1월 1일에 생성된 채팅방에 1월 2일에 질문을 추가하면 누락됨

-- 올바른 방법 (수정 후)
SELECT DISTINCT chat_room_id FROM chat_room_question
WHERE is_initial = false
AND chat_room_id IN (
    -- 1월 1일에 생성된 채팅방 ID 목록
    SELECT id FROM chat_room
    WHERE created_at BETWEEN '2025-01-01 00:00' AND '2025-01-01 23:59'
)
-- 해결: 1월 1일에 생성된 채팅방이 언제든 질문을 사용했는지 확인
```

**초기 질문 제외 이유**:
- 채팅방 생성 시 자동으로 추가되는 질문 6개는 제외
- 사용자가 실제로 "질문하기" 버튼을 클릭한 것만 집계

---

### Step 4: 코드해제 KPI 집계

```kotlin
private fun aggregateCodeUnlockKpi(
    dailyKpi: DailyKpi,
    utcStart: LocalDateTime,
    utcEnd: LocalDateTime
) {
    // 1. 코드해제 요청 수
    dailyKpi.codeUnlockRequestCount = kpiCodeUnlockRepository
        .countByCreatedAtBetween(utcStart, utcEnd)

    // 2. 코드해제 승인 수
    dailyKpi.codeUnlockApprovedCount = kpiCodeUnlockRepository
        .countApprovedByUpdatedAtBetween(utcStart, utcEnd)
}
```

#### 📊 수집 데이터

| KPI | 데이터 소스 | 조건 | 의미 |
|-----|-----------|------|------|
| **코드해제 요청 수** | `CodeUnlockRequest.createdAt` | `createdAt BETWEEN ...` | 해당 날짜에 요청된 코드해제 총 개수 |
| **코드해제 승인 수** | `CodeUnlockRequest.updatedAt` | `updatedAt BETWEEN ... AND status = APPROVED` | 해당 날짜에 승인된 코드해제 총 개수 |
| **코드해제 승인률** | 계산 | `(승인 수 / 요청 수) * 100` | 자동 계산 |

**코드해제란?**:
- 사용자 프로필에는 "히든 프로필"(얼굴 사진 등)이 있음
- 상대방이 히든 프로필을 보고 싶으면 "코드해제" 요청
- 요청받은 사람이 승인하면 히든 프로필 공개

---

### Step 5: 종료된 채팅방 KPI 집계

```kotlin
private fun aggregateClosedChatKpi(
    dailyKpi: DailyKpi,
    utcStart: LocalDateTime,
    utcEnd: LocalDateTime
) {
    // 1. 종료된 채팅방 조회
    val closedChatRooms = kpiChatRepository.findClosedByUpdatedAtBetween(utcStart, utcEnd)

    dailyKpi.closedChatroomsCount = closedChatRooms.size

    // 2. 평균 채팅 유지 기간 계산
    if (closedChatRooms.isNotEmpty()) {
        val totalDays = closedChatRooms.sumOf { chatRoom ->
            val createdAt = chatRoom.createdAt
            val closedAt = chatRoom.updatedAt
            Duration.between(createdAt, closedAt).toDays()
        }

        dailyKpi.avgChatDurationDays = BigDecimal(totalDays)
            .divide(BigDecimal(closedChatRooms.size), 2, RoundingMode.HALF_UP)
    }
}
```

#### 📊 수집 데이터

| KPI | 데이터 소스 | 계산 방법 | 의미 |
|-----|-----------|----------|------|
| **종료된 채팅방 수** | `ChatRoom.updatedAt` | `status = CLOSED AND updatedAt BETWEEN ...` | 해당 날짜에 종료된 채팅방 개수 |
| **평균 채팅 유지 기간** | `ChatRoom` | `(종료일 - 생성일) / 종료된 채팅방 수` | 채팅방이 생성부터 종료까지 평균 며칠 유지되었는지 |

**채팅방 종료 시점**:
- 사용자가 직접 "채팅방 나가기"
- 또는 시스템에서 비활성으로 자동 종료

---

## 📊 각 KPI별 상세 설명

### 1️⃣ 시그널 KPI

**목적**: 매칭 성과 측정

```
시그널 흐름:
사용자 A → 시그널 보냄 (signalSentCount ↑)
사용자 B → 시그널 수락 (signalAcceptedCount ↑)
→ 채팅방 생성
```

**핵심 메트릭**:
- **시그널 수락률 = (수락 수 / 보낸 수) × 100**
- 높을수록 좋음: 사용자들이 서로 관심 있는 상대를 잘 찾고 있다는 의미

---

### 2️⃣ 채팅 KPI

**목적**: 채팅 품질 및 참여도 측정

#### 열린 채팅방 vs 활성 채팅방

```
열린 채팅방 (openChatroomsCount):
└─ 종료되지 않은 모든 채팅방

활성 채팅방 (activeChatroomsCount):
└─ 열린 채팅방 중 최근 7일 내 메시지 활동이 있는 채팅방
```

**채팅방 활성률 = (활성 채팅방 / 열린 채팅방) × 100**
- 낮으면: 많은 채팅방이 "유령 채팅방"
- 높으면: 실제로 대화가 잘 이루어지고 있음

#### 채팅 퍼널 분석

```
100개 채팅방 생성
  ↓ FMR 62%
62개 첫 메시지 전송 (템플릿 제외)
  ↓ 3턴 비율 41%
41개 3턴 이상 대화
  ↓ CRR 39%
39개 24시간 내 재방문
```

**각 단계 의미**:
- **FMR (First Message Rate)**: 채팅방 만들고 실제로 대화 시작한 비율
- **3턴 이상 대화**: 서로 관심 있어서 대화가 이어진 비율
- **CRR (Chat Return Rate)**: 하루 지나서 다시 채팅방에 들어온 비율

**템플릿 메시지 제외**:
- 채팅방 생성 시 시스템이 자동으로 6개 메시지 추가
- 이 6개는 실제 대화가 아니므로 제외하고 7번째 메시지부터 카운트

---

### 3️⃣ 질문추천 KPI

**목적**: 질문추천 기능의 효과 측정

#### 질문추천이란?
```
채팅방에서 대화가 막힐 때
→ "질문하기" 버튼 클릭
→ 랜덤 질문 제공
→ 대화 주제 제공으로 대화 촉진
```

#### 핵심 비교: 질문 사용 vs 미사용

| 메트릭 | 질문 사용 채팅방 | 질문 미사용 채팅방 | 기대 결과 |
|-------|----------------|------------------|----------|
| **평균 메시지 수** | 6.3개 | 3.1개 | **질문 사용이 2배 많음** ✅ |
| **3턴 이상 대화 비율** | 68% | 29% | **질문 사용이 2.3배 높음** ✅ |
| **CRR (재방문률)** | 46% | 28% | **질문 사용이 1.6배 높음** ✅ |

**결론**: 질문추천 기능이 대화 품질을 유의미하게 향상시킴

#### 초기 질문 vs 버튼 클릭 질문

```
초기 질문 (isInitial = true):
└─ 채팅방 생성 시 시스템이 자동으로 추가한 6개 질문
   └─ KPI에서 제외 (사용자가 선택한 게 아니므로)

버튼 클릭 질문 (isInitial = false):
└─ 사용자가 "질문하기" 버튼을 눌러서 추가한 질문
   └─ KPI에서 집계 ✅
```

---

### 4️⃣ 코드해제 KPI

**목적**: 히든 프로필 공개 의향 측정

```
코드해제 흐름:
사용자 A → 코드해제 요청 (codeUnlockRequestCount ↑)
사용자 B → 승인 or 거절
  ├─ 승인 → (codeUnlockApprovedCount ↑) → A가 B의 히든 프로필 볼 수 있음
  └─ 거절 → 요청만 카운트됨
```

**코드해제 승인률 = (승인 수 / 요청 수) × 100**
- 낮으면: 사용자들이 히든 프로필 공개를 꺼림
- 높으면: 상대방에 대한 신뢰도가 높음

---

### 5️⃣ 종료된 채팅방 KPI

**목적**: 채팅방 생명주기 분석

```
평균 채팅 유지 기간 = 12.4일

해석:
- 짧으면 (< 7일): 빠르게 관심 잃음
- 적당하면 (7~14일): 탐색 후 결정
- 길면 (> 14일): 지속적인 관심
```

---

## 🎯 실제 데이터 예시

### 예시: 2025년 1월 1일 KPI

```
📅 집계 날짜: 2025-01-01 (KST)
📍 집계 시점: 2025-01-02 01:00 (KST)
📊 UTC 변환: 2024-12-31 15:00 ~ 2025-01-01 14:59 (UTC)

┌─────────────────────────────────────────┐
│ 1. 시그널 KPI                            │
├─────────────────────────────────────────┤
│ 시그널 보낸 수:        417개             │
│ 시그널 수락 수:        138개             │
│ 시그널 수락률:         33.1%            │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ 2. 채팅 KPI                             │
├─────────────────────────────────────────┤
│ 열린 채팅방:          208개             │
│ 활성 채팅방:          127개             │
│ 채팅방 활성률:         61.1%            │
│                                          │
│ FMR (첫메시지율):      62.0%            │
│ 3턴 이상 대화:         41.3%            │
│ CRR (재방문율):        38.9%            │
│ 평균 메시지 수:        4.2개            │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ 3. 질문추천 KPI                          │
├─────────────────────────────────────────┤
│ 질문 클릭 수:         109회             │
│ 질문 사용 채팅방:      72개             │
│                                          │
│ [질문 사용 채팅방]                       │
│ - 평균 메시지:        6.3개             │
│ - 3턴 비율:          68.1%              │
│ - CRR:              46.2%              │
│                                          │
│ [질문 미사용 채팅방]                     │
│ - 평균 메시지:        3.1개             │
│ - 3턴 비율:          28.7%              │
│ - CRR:              27.5%              │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ 4. 코드해제 KPI                          │
├─────────────────────────────────────────┤
│ 코드해제 요청:         56건             │
│ 코드해제 승인:         12건             │
│ 코드해제 승인률:       21.4%            │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ 5. 종료된 채팅방 KPI                     │
├─────────────────────────────────────────┤
│ 종료된 채팅방:         38개             │
│ 평균 유지 기간:        12.4일           │
└─────────────────────────────────────────┘
```

---

## 📈 데이터 활용 방법

### 1. 대시보드에서 확인
```
http://localhost:8080/v1/admin/kpi
```

### 2. API로 조회
```bash
# 특정 날짜 KPI 조회
GET /v1/admin/kpi/daily/2025-01-01

# 기간별 요약
GET /v1/admin/kpi/summary?startDate=2025-01-01&endDate=2025-01-07

# 전체 KPI 목록
GET /v1/admin/kpi/all
```

### 3. 직접 DB 조회
```sql
SELECT * FROM daily_kpi
WHERE target_date = '2025-01-01';
```

---

## 🔍 트러블슈팅

### Q1. KPI 데이터가 없어요
```bash
# 수동 집계 실행
POST /v1/admin/kpi/aggregate?date=2025-01-01

# 또는 앱 재시작 (최근 7일 자동 집계)
```

### Q2. 데이터가 이상해요
```
확인 사항:
1. 타임존 확인 (KST vs UTC)
2. 채팅방/메시지가 실제로 존재하는지
3. 로그 확인 (집계 과정 출력됨)
```

### Q3. 질문 KPI가 0이에요
```
확인:
- ChatRoomQuestion 테이블에 isInitial = false인 데이터가 있는지
- 초기 질문(isInitial = true)은 제외되므로 실제 버튼 클릭이 있었는지
```

---

**작성일**: 2025-12-30
**버전**: 1.0
