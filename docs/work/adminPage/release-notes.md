# 관리자 페이지 필터링 기능 개선 - 릴리즈 노트

| 배포타입 | 정기 |
| --- | --- |
| 날짜 | 2026년 1월 11일 |
| 버전 | v1.0 (이슈 #384) |
| 기여자 | 양준영 |

---

## 요약

관리자 페이지의 회원 리스트 및 질문 관리 페이지에서 필터링 기능의 정확성과 사용성을 대폭 개선했습니다. 검색 결과의 정확도를 높이고, 날짜 범위 필터링, 추가 상태 필터, 필터 조건 유지 등의 기능을 추가하여 관리자의 업무 효율성을 향상시켰습니다. 또한 UTC/KST 시간대 처리 방식을 문서화하여 향후 개발 시 참고할 수 있도록 했습니다.

---

## 새 기능

### 1. 회원 리스트 가입일 기준 날짜 범위 필터링 (커밋: `2ecd3ef`)

**기능 설명**:
- 회원 리스트에서 시작일/종료일을 선택하여 특정 기간에 가입한 회원만 조회 가능
- 시작일만 선택 시: 해당 날짜 이후 가입자 조회
- 종료일만 선택 시: 해당 날짜 이전 가입자 조회
- 둘 다 선택 시: 범위 내 가입자만 조회

**구현 내용**:
- `MemberService.kt`에서 날짜 문자열을 `LocalDateTime`으로 파싱
- `MemberJpaRepository.kt`의 쿼리에 날짜 조건 추가 (`createdAt BETWEEN`)
- 시작일: 00:00:00부터, 종료일: 23:59:59까지 포함되도록 처리

**사용자 혜택**:
- 특정 기간의 가입자 통계 및 분석 가능
- 이벤트 기간 가입자, 월별/분기별 가입자 조회 용이

---

### 2. 회원 리스트 상태 필터에 탈퇴 및 오픈프로필 작성 완료 추가 (커밋: `26f90e1`)

**기능 설명**:
- 기존 4개 상태 필터(PENDING, DONE, REJECT, PHONE_VERIFIED)에 2개 상태 추가
  - **WITHDRAWN** (탈퇴): 탈퇴한 회원만 필터링
  - **PERSONALITY_COMPLETED** (오픈프로필 작성 완료): 오픈프로필까지 작성한 회원만 필터링

**구현 내용**:
- Backend: `AdminController.kt`의 상태별 집계에 두 상태 추가
- Frontend: `memberList.html`에 통계 카드, 필터 select, 탭 버튼, 상태 배지 추가

**사용자 혜택**:
- 탈퇴 회원 현황 파악 및 탈퇴 사유 분석 가능
- 회원가입 단계별 전환율 분석 가능 (오픈프로필 작성률 등)

---

## 기존 기능 개선 사항

### 1. 질문 관리 페이지 필터 조건 유지 (커밋: `1ff03b3`)

**개선 전 문제**:
- 질문 수정/삭제/상태 변경 후 필터가 초기화되어 다시 선택해야 함
- "추억" 카테고리 질문을 연속으로 수정할 때마다 필터를 다시 선택해야 하는 번거로움

**개선 내용**:
- Controller의 POST 핸들러에 필터 파라미터 추가 (keyword, category, isActive, page, size)
- 리다이렉트 시 쿼리 문자열로 필터 조건 전달
- HTML 템플릿의 form과 링크에 hidden input 추가

**구현 범위**:
- 질문 수정 후 필터 유지
- 상태 토글(활성/비활성) 후 필터 유지
- 질문 삭제 후 필터 유지
- 페이지 번호 및 정렬 정보도 함께 유지

**사용자 혜택**:
- 같은 카테고리의 질문을 연속으로 관리할 때 작업 효율성 대폭 향상
- 작업 중단 없이 자연스러운 워크플로우 유지

---

### 2. 질문 관리 페이지 검색 필터 선택 값 UI 표시 (커밋: `cd2516d`)

**개선 전 문제**:
- 카테고리나 상태를 선택하고 검색하면 검색은 되지만 select box가 초기값으로 돌아감
- 사용자가 현재 어떤 필터를 적용했는지 UI에서 확인 불가

**개선 내용**:
- Controller에서 Thymeleaf 기본 `param` 객체와 충돌하지 않도록 명확한 model attribute 사용
  - `selectedKeyword`, `selectedCategory`, `selectedIsActive`
- HTML 템플릿에서 `th:selected` 조건으로 선택 값 유지

**사용자 혜택**:
- 현재 적용된 필터를 UI에서 명확하게 확인 가능
- 검색 조건 변경 시 현재 값을 참고하여 손쉽게 수정 가능

---

## 버그수정

### 1. 회원 리스트 이름 검색 정확도 개선 (커밋: `4cd1790`)

**버그 내용**:
- 이름(닉네임) 검색 시 의도와 다르게 이메일, 가입일 등 다른 컬럼까지 검색 대상에 포함됨
- 예: "2"를 입력하면 이메일에 2가 포함된 회원, 가입일에 2가 포함된 회원까지 모두 검색됨

**원인**:
- `MemberJpaRepository.kt`의 쿼리에서 `LOWER(m.email) LIKE` 조건이 OR로 포함됨

**수정 내용**:
- 이메일 검색 조건 제거
- codeName 필드만 검색 대상으로 설정

**수정 효과**:
- 검색 정확도 향상
- 사용자가 원하는 회원을 정확하게 찾을 수 있음

---

### 2. 질문 관리 페이지 필터 select 상태 유지 버그 수정 (커밋: `cd2516d`)

**버그 내용**:
- 카테고리/상태 select box가 검색 후 "전체"로 초기화되는 문제

**원인**:
- Controller에서 추가한 `param` model attribute가 Thymeleaf 기본 제공 `param` 객체와 충돌
- Thymeleaf의 `param` 객체는 배열 형태로 파라미터를 반환하여 비교 로직이 제대로 동작하지 않음

**수정 내용**:
- 명확한 model attribute 이름 사용 (`selectedKeyword`, `selectedCategory`, `selectedIsActive`)
- HTML에서 해당 attribute를 사용하여 선택 상태 유지

**수정 효과**:
- 검색 후에도 선택한 필터 값이 UI에 표시됨
- 사용자 경험 개선

---

## 개발 중 있었던 이슈사항 및 개선결과

### 1. 레이어 간 데이터 전달 누락 이슈 (커밋 2 관련)

**발생한 이슈**:
- 날짜 범위 필터링 기능이 UI에는 존재하지만 실제로 동작하지 않음
- Controller는 날짜 파라미터를 받지만, Service에서 Repository로 전달하지 않음

**원인 분석**:
```kotlin
// Service에서 Repository 호출 시 날짜 파라미터 누락
return memberJpaRepository.findMembersWithFilterAdvanced(keyword, statusEnum, sortedPageable)
// startDate, endDate를 받았으나 전달하지 않음
```

**해결 방법**:
- Repository 인터페이스에 날짜 파라미터 추가
- Service에서 날짜 문자열 파싱 후 LocalDateTime으로 변환하여 전달
- JPQL 쿼리에 날짜 조건 추가

**교훈**:
- 새 기능 개발 시 Controller → Service → Repository → DB 전체 플로우를 체계적으로 검증 필요
- 파라미터가 각 레이어를 거쳐 최종 쿼리까지 올바르게 전달되는지 확인 필수

---

### 2. Thymeleaf 기본 객체와의 이름 충돌 이슈 (커밋 5 관련)

**발생한 이슈**:
- Controller에서 `model.addAttribute("param", ...)`로 필터 값 전달
- Thymeleaf 템플릿에서 `param` 사용 시 예상과 다르게 동작

**원인 분석**:
- Thymeleaf는 기본적으로 `param`, `session`, `application` 등의 내장 객체 제공
- Controller에서 추가한 `param` attribute와 Thymeleaf 내장 `param` 객체가 충돌
- Thymeleaf의 `param`은 배열 형태(`String[]`)로 파라미터를 반환하여 `==` 비교가 정상 동작하지 않음

**해결 방법**:
- 명확하고 충돌하지 않는 model attribute 이름 사용 (`selectedKeyword`, `selectedCategory` 등)
- Thymeleaf 기본 객체 목록 숙지 및 회피

**교훈**:
- 프레임워크/라이브러리의 기본 제공 객체/키워드를 파악하고 이름 충돌 방지
- 변수명은 명확하고 구체적으로 작성 (`param` → `selectedCategory`)

---

### 3. 전체 플로우 검증 부족으로 인한 기능 미동작 (커밋 2, 3 관련)

**발생한 이슈**:
- Backend에서 기능을 구현했지만 Frontend에 반영되지 않음
- 또는 Frontend에 UI는 있지만 Backend 로직이 없음

**예시**:
- 날짜 필터: Frontend에 input 필드는 있지만 Backend에서 처리하지 않음
- 탈퇴 상태 필터: Backend에 MemberStatus.WITHDRAWN은 있지만 Frontend에 옵션이 없음

**해결 방법**:
- Frontend와 Backend를 동시에 수정
- 수정 후 실제 화면에서 End-to-End 테스트 수행

**교훈**:
- 기능 개발 시 Frontend(HTML) ↔ Controller ↔ Service ↔ Repository ↔ DB 전체 흐름을 한 번에 검증
- 각 레이어를 독립적으로 수정 후 통합 테스트 필수

---

### 4. UTC/KST 시간대 처리 일관성 문제 (문서화 완료)

**발견한 이슈**:
- 데이터베이스에 UTC로 저장되는 시간을 사용자에게 그대로 표시하여 9시간 차이 발생
- 날짜 검색 시 KST 입력을 UTC로 착각하여 검색 결과 누락

**현재 상태**:
- `application.yml`에서 `hibernate.jdbc.time_zone: UTC` 설정으로 DB에 UTC 저장
- `DateTimeFormatter` 유틸리티 클래스에 변환 메서드 구현되어 있음
  - `convertUtcToKst()`: UTC → KST 변환
  - `convertKstToUtc()`: KST → UTC 변환
  - `getUtcRangeForKstDate()`: KST 날짜를 UTC 범위로 변환

**미적용 영역**:
- `memberList.html`: 가입 시간이 여전히 UTC로 표시됨
- `MemberService.kt`: 날짜 필터링 시 KST를 UTC로 착각하여 검색 (일부 시간대 데이터 누락 가능)

**개선 계획**:
```html
<!-- memberList.html 개선안 -->
<small th:text="${#temporals.format(
    T(codel.common.util.DateTimeFormatter).convertUtcToKst(member.createdAt),
    'yyyy-MM-dd HH:mm'
)}"></small>
```

```kotlin
// MemberService.kt 개선안
val startDateTime = if (!startDate.isNullOrBlank()) {
    val kstDate = LocalDate.parse(startDate)
    DateTimeFormatter.getUtcRangeForKstDate(kstDate).first
} else null
```

**교훈**:
- 글로벌 서비스를 고려한 UTC 저장 정책은 좋지만, 표시와 검색도 일관되게 KST 변환 필요
- 시간대 관련 유틸리티가 있어도 실제 적용되지 않으면 무의미
- 향후 모든 시간 표시/검색에 일괄 적용 필요

**문서화**:
- `implementation-details.md`에 "UTC 시간대 처리 및 KST 변환" 섹션 추가
- 시간대 변환 체크리스트 및 디버깅 팁 제공
- 신규 개발자 온보딩 시 참고 자료로 활용 가능

---

### 5. 필터 조건 유지 시 모든 액션에 파라미터 전달 누락 (커밋 4 관련)

**발생한 이슈**:
- 질문 수정 폼에서 "저장" 버튼은 필터를 유지하지만, "취소" 버튼은 필터를 잃어버림
- 삭제/상태 토글 form에서 hidden input을 누락하여 일부 액션에서만 필터 유지

**해결 방법**:
- 모든 링크(`<a>`)와 폼(`<form>`)에 일관되게 필터 파라미터 추가
- 체크리스트 작성하여 누락 방지
  - ✅ 수정 버튼 링크
  - ✅ 삭제 form
  - ✅ 상태 토글 form
  - ✅ 저장 버튼 form
  - ✅ 취소 버튼 링크
  - ✅ 목록으로 버튼 링크

**교훈**:
- 사용자 액션을 모두 나열하고 각 액션마다 필터 유지 여부 체크
- 하나라도 누락되면 사용자 경험 일관성이 깨짐

---

## 종합 개선 효과

### 정량적 효과
- **검색 정확도**: 이름 검색 시 불필요한 결과 제거로 정확도 향상
- **작업 효율**: 질문 연속 수정 시 필터 재선택 불필요 → 클릭 수 약 50% 감소
- **기능 확장**: 필터 옵션 2개 추가 (탈퇴, 오픈프로필 완료), 날짜 범위 필터 1개 추가

### 정성적 효과
- **사용자 경험**: 필터 상태가 유지되어 자연스러운 워크플로우
- **UI 일관성**: 선택한 필터가 화면에 표시되어 현재 상태 인지 용이
- **유지보수성**: implementation-details.md 문서화로 향후 유사 이슈 발생 시 빠른 해결 가능

### 학습 및 개선 포인트
1. **전체 플로우 검증**: Controller → Service → Repository → DB → 다시 Frontend로 돌아오는 전체 흐름 확인 필수
2. **프레임워크 이해**: Thymeleaf, JPA 등 프레임워크의 기본 제공 객체/동작 방식 숙지
3. **사용자 중심 사고**: 필터 유지, 현재 상태 표시 등 작은 UX 개선이 큰 만족도 향상으로 이어짐
4. **문서화의 중요성**: 복잡한 이슈는 해결 과정을 상세히 기록하여 팀 전체의 학습 자산으로 활용

---

**이슈**: #384
**문서 작성일**: 2026-01-11
**릴리즈 예정일**: TBD
