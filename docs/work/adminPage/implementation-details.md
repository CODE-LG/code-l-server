# 관리자 페이지 필터링 기능 수정 - 구현 상세 문서

이 문서는 이슈 #384에서 수정한 관리자 페이지 필터링 기능의 각 커밋별 문제점, 원인 분석, 해결 방법을 상세히 기록합니다.

---

## 커밋 1: 회원 리스트 이름 검색을 codeName 필드만 대상으로 수정

**커밋 해시**: `4cd1790`
**커밋 메시지**: `[feat] 회원 리스트 이름 검색을 codeName 필드만 대상으로 수정`

### 문제점
회원 리스트에서 "이름 검색" 기능이 의도와 다르게 동작했습니다.
- 이름(닉네임) 검색 시 **이메일, 가입일 등 다른 컬럼까지 검색 대상**에 포함됨
- 예: "2"를 입력하면 닉네임에 2가 없어도 이메일에 2가 포함되거나 가입일에 2가 포함된 회원까지 모두 검색됨
- 사용자가 원하는 회원을 정확히 찾기 어려움

### 원인 분석
`MemberJpaRepository.kt`의 `findMembersWithFilterAdvanced()` 쿼리를 확인한 결과:

```kotlin
WHERE (:status IS NULL OR m.memberStatus = :status)
  AND (
    :keyword IS NULL OR :keyword = ''
    OR LOWER(m.email) LIKE LOWER(CONCAT('%', :keyword, '%'))  // 이메일도 검색 대상
    OR LOWER(p.codeName) LIKE LOWER(CONCAT('%', :keyword, '%'))
  )
```

키워드가 **이메일(m.email)과 닉네임(p.codeName) 모두**를 OR 조건으로 검색하고 있었습니다.

### 해결 방법
이메일 검색 조건을 제거하고 **codeName만 검색**하도록 수정:

```kotlin
WHERE (:status IS NULL OR m.memberStatus = :status)
  AND (
    :keyword IS NULL OR :keyword = ''
    OR LOWER(p.codeName) LIKE LOWER(CONCAT('%', :keyword, '%'))  // codeName만 검색
  )
```

**변경 파일**:
- `src/main/kotlin/codel/member/infrastructure/MemberJpaRepository.kt`

**효과**:
- 이름 검색 시 닉네임(codeName) 필드만 대상으로 검색
- 사용자가 원하는 회원을 정확하게 찾을 수 있음

---

## 커밋 2: 회원 리스트에 가입일 기준 날짜 범위 필터링 기능 추가

**커밋 해시**: `2ecd3ef`
**커밋 메시지**: `[feat] 회원 리스트에 가입일 기준 날짜 범위 필터링 기능 추가`

### 문제점
시작일/종료일 필터를 선택해도 **필터링이 적용되지 않고** 모든 날짜의 결과가 출력되었습니다.
- UI에는 날짜 입력 필드가 있지만 실제로 동작하지 않음
- 특정 기간의 가입자를 조회할 수 없음

### 원인 분석

1. **Controller 레벨 (`AdminController.kt`)**: 날짜 파라미터는 전달받고 있었음
2. **Service 레벨 (`MemberService.kt`)**:
   ```kotlin
   fun findMembersWithFilter(
       keyword: String?,
       status: String?,
       startDate: String?,  // 파라미터는 받음
       endDate: String?,    // 파라미터는 받음
       sort: String?,
       direction: String?,
       pageable: Pageable
   ): Page<Member> {
       // ...
       return memberJpaRepository.findMembersWithFilterAdvanced(keyword, statusEnum, sortedPageable)
       // ❌ Repository에 날짜 파라미터를 전달하지 않음!
   }
   ```

3. **Repository 레벨 (`MemberJpaRepository.kt`)**:
   ```kotlin
   fun findMembersWithFilterAdvanced(
       @Param("keyword") keyword: String?,
       @Param("status") status: MemberStatus?,
       pageable: Pageable
   ): Page<Member>
   // ❌ 날짜 파라미터를 받지 않음!
   ```

**문제의 핵심**: 날짜 파라미터가 Service까지만 전달되고 **Repository로 전달되지 않아** 쿼리에 반영되지 않았습니다.

### 해결 방법

#### 1. Repository 수정
쿼리에 날짜 조건 추가 및 파라미터 추가:

```kotlin
@Query(
    """
    SELECT m FROM Member m JOIN FETCH m.profile p
    WHERE (:status IS NULL OR m.memberStatus = :status)
      AND (
        :keyword IS NULL OR :keyword = ''
        OR LOWER(p.codeName) LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
      AND (:startDate IS NULL OR m.createdAt >= :startDate)  // 시작일 조건 추가
      AND (:endDate IS NULL OR m.createdAt < :endDate)       // 종료일 조건 추가
    """
)
fun findMembersWithFilterAdvanced(
    @Param("keyword") keyword: String?,
    @Param("status") status: MemberStatus?,
    @Param("startDate") startDate: LocalDateTime?,  // 파라미터 추가
    @Param("endDate") endDate: LocalDateTime?,      // 파라미터 추가
    pageable: Pageable
): Page<Member>
```

#### 2. Service 수정
날짜 문자열을 LocalDateTime으로 파싱하고 Repository에 전달:

```kotlin
fun findMembersWithFilter(
    keyword: String?,
    status: String?,
    startDate: String?,
    endDate: String?,
    sort: String?,
    direction: String?,
    pageable: Pageable
): Page<Member> {
    // 상태 파싱
    val statusEnum = if (!status.isNullOrBlank()) {
        try {
            MemberStatus.valueOf(status)
        } catch (e: IllegalArgumentException) {
            null
        }
    } else {
        null
    }

    // 날짜 파싱 - yyyy-MM-dd 형식의 문자열을 LocalDateTime으로 변환
    val startDateTime = if (!startDate.isNullOrBlank()) {
        try {
            LocalDate.parse(startDate).atStartOfDay() // 00:00:00
        } catch (e: Exception) {
            null
        }
    } else {
        null
    }

    val endDateTime = if (!endDate.isNullOrBlank()) {
        try {
            LocalDate.parse(endDate).plusDays(1).atStartOfDay() // 다음 날 00:00:00 (해당일 23:59:59까지 포함)
        } catch (e: Exception) {
            null
        }
    } else {
        null
    }

    val sortedPageable = createSortedPageable(pageable, sort, direction)

    // 날짜 파라미터 전달
    return memberJpaRepository.findMembersWithFilterAdvanced(
        keyword,
        statusEnum,
        startDateTime,  // 시작일 전달
        endDateTime,    // 종료일 전달
        sortedPageable
    )
}
```

**변경 파일**:
- `src/main/kotlin/codel/member/infrastructure/MemberJpaRepository.kt`
- `src/main/kotlin/codel/member/business/MemberService.kt`

**효과**:
- 시작일: 해당 날짜 00:00:00 이상
- 종료일: 해당 날짜 23:59:59 이하 (다음날 00:00:00 미만으로 처리)
- 시작일만 선택 시: 시작일 이후 가입자만
- 종료일만 선택 시: 종료일 이전 가입자만
- 둘 다 선택 시: 범위 내 가입자만 조회 가능

---

## 커밋 3: 회원 리스트 상태 필터에 탈퇴 및 오픈프로필 작성 완료 추가

**커밋 해시**: `26f90e1`
**커밋 메시지**: `[feat] 회원 리스트 상태 필터에 탈퇴 및 오픈프로필 작성 완료 추가`

### 문제점
1. **Backend**: `WITHDRAWN`(탈퇴), `PERSONALITY_COMPLETED`(오픈프로필 작성 완료) 상태 필터링이 동작하지 않음
2. **Frontend**: UI에 해당 상태 필터 버튼/옵션이 없음

### 원인 분석

#### Backend 문제
`AdminController.kt`의 `memberList()` 메서드:

```kotlin
val statusCounts = mapOf(
    "total" to adminService.countAllMembers(),
    "PENDING" to adminService.countMembersByStatus("PENDING"),
    "DONE" to adminService.countMembersByStatus("DONE"),
    "REJECT" to adminService.countMembersByStatus("REJECT"),
    "PHONE_VERIFIED" to adminService.countMembersByStatus("PHONE_VERIFIED")
    // ❌ WITHDRAWN, PERSONALITY_COMPLETED 누락!
)
```

상태별 회원 수 집계에 두 상태가 **포함되지 않아** 프론트엔드에서 표시할 수 없었습니다.

#### Frontend 문제
`memberList.html` 템플릿에:
- 통계 카드에 해당 상태 표시 없음
- 필터 select box에 옵션 없음
- 탭에 해당 상태 버튼 없음
- 테이블에 상태 배지 정의 없음

### 해결 방법

#### 1. Backend 수정 (`AdminController.kt`)
상태 집계에 두 상태 추가:

```kotlin
val statusCounts = mapOf(
    "total" to adminService.countAllMembers(),
    "PENDING" to adminService.countMembersByStatus("PENDING"),
    "DONE" to adminService.countMembersByStatus("DONE"),
    "REJECT" to adminService.countMembersByStatus("REJECT"),
    "PHONE_VERIFIED" to adminService.countMembersByStatus("PHONE_VERIFIED"),
    "WITHDRAWN" to adminService.countMembersByStatus("WITHDRAWN"),  // 추가
    "PERSONALITY_COMPLETED" to adminService.countMembersByStatus("PERSONALITY_COMPLETED")  // 추가
)
```

#### 2. Frontend 수정 (`memberList.html`)

**2-1. 통계 카드 수정**
```html
<div class="col-md-2">
    <h4 th:text="${statusCounts.WITHDRAWN}">0</h4>
    <small>탈퇴</small>
</div>
```

**2-2. 필터 select box에 옵션 추가**
```html
<select class="form-select" name="status">
    <option value="">전체 상태</option>
    <option th:selected="${param.status == 'PENDING'}" value="PENDING">승인 대기</option>
    <option th:selected="${param.status == 'DONE'}" value="DONE">승인 완료</option>
    <option th:selected="${param.status == 'REJECT'}" value="REJECT">거부됨</option>
    <option th:selected="${param.status == 'PHONE_VERIFIED'}" value="PHONE_VERIFIED">핸드폰 인증 완료</option>
    <option th:selected="${param.status == 'PERSONALITY_COMPLETED'}" value="PERSONALITY_COMPLETED">오픈프로필 작성 완료</option>
    <option th:selected="${param.status == 'WITHDRAWN'}" value="WITHDRAWN">탈퇴</option>
</select>
```

**2-3. 탭 버튼 추가**
```html
<li class="nav-item">
    <a class="nav-link d-flex align-items-center"
       th:classappend="${param.status == 'PERSONALITY_COMPLETED'} ? 'active'"
       th:href="@{/v1/admin/members(status='PERSONALITY_COMPLETED')}">
        <span>오픈프로필 작성 완료</span>
        <span class="badge bg-info ms-2" th:text="${statusCounts.PERSONALITY_COMPLETED}">0</span>
    </a>
</li>
<li class="nav-item">
    <a class="nav-link d-flex align-items-center"
       th:classappend="${param.status == 'WITHDRAWN'} ? 'active'"
       th:href="@{/v1/admin/members(status='WITHDRAWN')}">
        <span>탈퇴</span>
        <span class="badge bg-secondary ms-2" th:text="${statusCounts.WITHDRAWN}">0</span>
    </a>
</li>
```

**2-4. 테이블 상태 배지 추가**
```html
<span class="badge bg-info text-white" th:case="'PERSONALITY_COMPLETED'">
    <i class="fa-solid fa-user-edit me-1"></i>오픈프로필 작성 완료
</span>
<span class="badge bg-secondary" th:case="'WITHDRAWN'">
    <i class="fa-solid fa-user-slash me-1"></i>탈퇴
</span>
```

**변경 파일**:
- `src/main/kotlin/codel/admin/presentation/AdminController.kt`
- `src/main/resources/templates/memberList.html`

**효과**:
- 탈퇴한 회원만 필터링하여 조회 가능
- 오픈프로필까지 작성 완료한 회원만 필터링하여 조회 가능
- 각 상태의 회원 수를 대시보드에서 확인 가능

---

## 커밋 4: 질문 관리 페이지에서 수정/상태 변경/삭제 후 필터 조건 유지

**커밋 해시**: `1ff03b3`
**커밋 메시지**: `[feat] 질문 관리 페이지에서 수정/상태 변경/삭제 후 필터 조건 유지`

### 문제점
질문 관리 페이지에서 필터를 적용한 상태에서:
- 질문 1개를 수정하고 저장하면 **필터가 해제**되어 초기 화면으로 돌아감
- 질문 상태 변경(활성/비활성)을 해도 **필터가 해제**됨
- 질문 삭제 후에도 **필터가 해제**됨

**사용자 경험 문제**:
- "추억" 카테고리 질문들을 연속으로 수정하려면 수정할 때마다 다시 "추억" 필터를 선택해야 함
- 매우 비효율적이고 불편함

### 원인 분석

#### 1. Controller의 POST 핸들러가 필터 파라미터를 받지 않음

**질문 수정 핸들러**:
```kotlin
@PostMapping("/v1/admin/questions/{questionId}")
fun updateQuestion(
    @PathVariable questionId: Long,
    @RequestParam content: String,
    @RequestParam category: String,
    @RequestParam(required = false) description: String?,
    @RequestParam(defaultValue = "false") isActive: Boolean,
    // ❌ 필터 파라미터를 받지 않음!
    redirectAttributes: RedirectAttributes
): String {
    // ...
    return "redirect:/v1/admin/questions"  // 파라미터 없이 리다이렉트
}
```

**상태 토글 핸들러**:
```kotlin
@PostMapping("/v1/admin/questions/{questionId}/toggle")
fun toggleQuestionStatus(
    @PathVariable questionId: Long,
    // ❌ 필터 파라미터를 받지 않음!
    redirectAttributes: RedirectAttributes
): String {
    // ...
    return "redirect:/v1/admin/questions"  // 파라미터 없이 리다이렉트
}
```

#### 2. questionList.html에서 form 제출 시 필터 파라미터를 전달하지 않음

**수정 버튼**:
```html
<a th:href="@{/v1/admin/questions/{id}/edit(id=${question.id})}"
   class="btn btn-outline-primary btn-sm" title="수정">
    <!-- ❌ 현재 필터 파라미터를 전달하지 않음 -->
</a>
```

**상태 토글 form**:
```html
<form th:action="@{/v1/admin/questions/{id}/toggle(id=${question.id})}"
      method="post" style="display: inline;">
    <!-- ❌ hidden input으로 필터 파라미터를 전달하지 않음 -->
    <button type="submit">...</button>
</form>
```

#### 3. questionEditForm.html에서 수정 제출 시 필터 파라미터를 전달하지 않음

```html
<form th:action="@{/v1/admin/questions/{id}(id=${question.id})}" method="post">
    <!-- ❌ hidden input으로 필터 파라미터를 전달하지 않음 -->
    <div class="mb-4">
        <label for="content">질문 내용</label>
        <textarea id="content" name="content">...</textarea>
    </div>
    <!-- ... -->
</form>
```

### 해결 방법

#### 1. Controller 수정 - 필터 파라미터 수신 및 리다이렉트 시 전달

**질문 수정 핸들러**:
```kotlin
@PostMapping("/v1/admin/questions/{questionId}")
fun updateQuestion(
    @PathVariable questionId: Long,
    @RequestParam content: String,
    @RequestParam category: String,
    @RequestParam(required = false) description: String?,
    @RequestParam(defaultValue = "false") isActive: Boolean,
    @RequestParam(required = false) keyword: String?,          // 추가
    @RequestParam(required = false) filterCategory: String?,   // 추가
    @RequestParam(required = false) filterIsActive: String?,   // 추가
    @RequestParam(required = false, defaultValue = "0") page: Int,    // 추가
    @RequestParam(required = false, defaultValue = "20") size: Int,   // 추가
    redirectAttributes: RedirectAttributes
): String {
    try {
        val questionCategory = QuestionCategory.valueOf(category)
        adminService.updateQuestion(questionId, content, questionCategory, description, isActive)
        redirectAttributes.addFlashAttribute("success", "질문이 성공적으로 수정되었습니다.")
    } catch (e: Exception) {
        redirectAttributes.addFlashAttribute("error", "질문 수정에 실패했습니다: ${e.message}")
    }

    // 필터 조건 유지하여 리다이렉트
    val params = mutableListOf<String>()
    keyword?.let { if (it.isNotBlank()) params.add("keyword=$it") }
    filterCategory?.let { if (it.isNotBlank()) params.add("category=$it") }
    filterIsActive?.let { if (it.isNotBlank()) params.add("isActive=$it") }
    params.add("page=$page")
    params.add("size=$size")

    val queryString = if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
    return "redirect:/v1/admin/questions$queryString"
}
```

**상태 토글/삭제 핸들러도 동일한 방식으로 수정**

#### 2. questionList.html 수정 - 필터 파라미터 전달

**수정 버튼**:
```html
<a th:href="@{/v1/admin/questions/{id}/edit(
       id=${question.id},
       keyword=${param.keyword},
       category=${param.category},
       isActive=${param.isActive},
       page=${questions.number},
       size=${questions.size}
   )}"
   class="btn btn-outline-primary btn-sm" title="수정">
    <i class="fa-solid fa-edit"></i>
</a>
```

**상태 토글 form**:
```html
<form th:action="@{/v1/admin/questions/{id}/toggle(id=${question.id})}"
      method="post" style="display: inline;">
    <input type="hidden" name="keyword" th:value="${param.keyword}">
    <input type="hidden" name="category" th:value="${param.category}">
    <input type="hidden" name="isActive" th:value="${param.isActive}">
    <input type="hidden" name="page" th:value="${questions.number}">
    <input type="hidden" name="size" th:value="${questions.size}">
    <button type="submit">...</button>
</form>
```

**삭제 form도 동일한 방식으로 수정**

#### 3. editQuestionForm Controller 핸들러 수정

```kotlin
@GetMapping("/v1/admin/questions/{questionId}/edit")
fun editQuestionForm(
    @PathVariable questionId: Long,
    @RequestParam(required = false) keyword: String?,          // 추가
    @RequestParam(required = false) category: String?,         // 추가
    @RequestParam(required = false) isActive: String?,         // 추가
    @RequestParam(required = false, defaultValue = "0") page: Int,    // 추가
    @RequestParam(required = false, defaultValue = "20") size: Int,   // 추가
    model: Model
): String {
    val question = adminService.findQuestionById(questionId)
    model.addAttribute("question", question)
    model.addAttribute("categories", QuestionCategory.values())
    model.addAttribute("filterKeyword", keyword)
    model.addAttribute("filterCategory", category)
    model.addAttribute("filterIsActive", isActive)
    model.addAttribute("filterPage", page)
    model.addAttribute("filterSize", size)
    return "questionEditForm"
}
```

#### 4. questionEditForm.html 수정 - hidden input 추가

```html
<form th:action="@{/v1/admin/questions/{id}(id=${question.id})}" method="post">
    <!-- 필터 파라미터 유지 -->
    <input type="hidden" name="keyword" th:value="${filterKeyword}">
    <input type="hidden" name="filterCategory" th:value="${filterCategory}">
    <input type="hidden" name="filterIsActive" th:value="${filterIsActive}">
    <input type="hidden" name="page" th:value="${filterPage}">
    <input type="hidden" name="size" th:value="${filterSize}">

    <div class="mb-4">
        <label for="content">질문 내용</label>
        <textarea id="content" name="content" th:text="${question.content}"></textarea>
    </div>
    <!-- ... -->
</form>
```

**"목록으로", "취소" 버튼도 필터 파라미터 포함**:
```html
<a th:href="@{/v1/admin/questions(
       keyword=${filterKeyword},
       category=${filterCategory},
       isActive=${filterIsActive},
       page=${filterPage},
       size=${filterSize}
   )}"
   class="btn btn-outline-secondary">
    <i class="fa-solid fa-arrow-left"></i> 목록으로
</a>
```

**변경 파일**:
- `src/main/kotlin/codel/admin/presentation/AdminController.kt`
- `src/main/resources/templates/questionList.html`
- `src/main/resources/templates/questionEditForm.html`

**효과**:
- 질문 수정 후에도 카테고리, 검색어, 활성화 상태 필터가 유지됨
- 상태 토글 후에도 필터가 유지됨
- 삭제 후에도 필터가 유지됨
- 페이지 번호와 정렬 정보도 유지됨
- 연속으로 질문을 수정할 때 매번 필터를 다시 선택할 필요 없음

---

## 커밋 5: 질문 관리 페이지 검색 필터 선택 값 유지 기능 추가

**커밋 해시**: `cd2516d`
**커밋 메시지**: `[feat] 질문 관리 페이지 검색 필터 선택 값 유지 기능 추가`

### 문제점
질문 관리 페이지에서:
- 카테고리를 선택하고 검색하면 검색은 되지만 **select box가 "전체 카테고리"로 초기화**됨
- 상태를 선택하고 검색하면 검색은 되지만 **select box가 "전체"로 초기화**됨
- 사용자가 현재 어떤 필터를 선택했는지 UI에서 확인할 수 없음

### 원인 분석

#### 1. Controller에서 잘못된 model attribute 사용

```kotlin
@GetMapping("/v1/admin/questions")
fun questionList(
    @RequestParam(required = false) keyword: String?,
    @RequestParam(required = false) category: String?,
    @RequestParam(required = false) isActive: Boolean?,
    model: Model
): String {
    val questions = adminService.findQuestionsWithFilter(keyword, category, isActive, pageable)
    model.addAttribute("questions", questions)
    model.addAttribute("categories", QuestionCategory.values())
    model.addAttribute("param", mapOf(  // ❌ 문제!
        "keyword" to (keyword ?: ""),
        "category" to (category ?: ""),
        "isActive" to (isActive?.toString() ?: "")
    ))
    return "questionList"
}
```

**문제**: `param`이라는 이름의 model attribute를 추가했는데, Thymeleaf에는 이미 **`param`이라는 기본 객체(request parameter)**가 있습니다.

#### 2. questionList.html에서 param 객체 사용

```html
<select class="form-select" id="category" name="category">
    <option value="">전체 카테고리</option>
    <option th:each="cat : ${categories}"
            th:value="${cat.name}"
            th:text="${cat.displayName}"
            th:selected="${param.category == cat.name}"></option>  <!-- param 사용 -->
</select>
```

**충돌 발생**:
- Controller가 추가한 `param` model attribute
- Thymeleaf 기본 제공 `param` 객체 (request parameter)

이 두 개가 충돌하면서 예상대로 동작하지 않았습니다.

#### 3. Thymeleaf의 param 객체 특성
Thymeleaf의 `param` 객체는 **배열 형태**로 파라미터를 반환합니다.
- `param.category`는 `String[]` 타입
- `param.category == cat.name` 비교가 제대로 작동하지 않음
- GET 방식으로 페이지 로드 시에는 동작하지만, 일부 경우에 문제 발생

### 해결 방법

#### 1. Controller 수정 - 명확한 model attribute 이름 사용

```kotlin
@GetMapping("/v1/admin/questions")
fun questionList(
    @RequestParam(required = false) keyword: String?,
    @RequestParam(required = false) category: String?,
    @RequestParam(required = false) isActive: Boolean?,
    model: Model
): String {
    val questions = adminService.findQuestionsWithFilter(keyword, category, isActive, pageable)
    model.addAttribute("questions", questions)
    model.addAttribute("categories", QuestionCategory.values())

    // 명확한 이름으로 변경
    model.addAttribute("selectedKeyword", keyword ?: "")
    model.addAttribute("selectedCategory", category ?: "")
    model.addAttribute("selectedIsActive", isActive?.toString() ?: "")

    return "questionList"
}
```

#### 2. questionList.html 수정 - 명확한 model attribute 사용

**키워드 입력**:
```html
<input type="text" class="form-control" id="keyword" name="keyword"
       th:value="${selectedKeyword}" placeholder="질문 내용 또는 설명 검색">
```

**카테고리 select**:
```html
<select class="form-select" id="category" name="category">
    <option value="" th:selected="${selectedCategory == ''}">전체 카테고리</option>
    <option th:each="cat : ${categories}"
            th:value="${cat.name}"
            th:text="${cat.displayName}"
            th:selected="${selectedCategory == cat.name}"></option>
</select>
```

**상태 select**:
```html
<select class="form-select" id="isActive" name="isActive">
    <option value="" th:selected="${selectedIsActive == ''}">전체</option>
    <option value="true" th:selected="${selectedIsActive == 'true'}">활성</option>
    <option value="false" th:selected="${selectedIsActive == 'false'}">비활성</option>
</select>
```

**변경 파일**:
- `src/main/kotlin/codel/admin/presentation/AdminController.kt`
- `src/main/resources/templates/questionList.html`

**효과**:
- 카테고리를 선택하고 검색하면 해당 카테고리가 select box에서 선택된 상태로 유지됨
- 상태를 선택하고 검색하면 해당 상태가 select box에서 선택된 상태로 유지됨
- 사용자가 현재 적용된 필터를 UI에서 명확하게 확인 가능
- 검색 조건을 변경하기 위해 다시 선택할 때 현재 값을 참고할 수 있음

---

## 커밋 6: 회원 리스트 가입 시간 표시를 UTC에서 KST로 변경

**커밋 해시**: `3fca098`
**커밋 메시지**: `[feat] 회원 리스트 가입 시간 표시를 UTC에서 KST로 변경`
**관련 커밋**: `921ab93` (날짜 검색 KST→UTC 변환 추가)

### 문제점
회원 리스트에서 가입 시간이 UTC 시간대로 표시되어:
- 한국 관리자가 보기에 시간이 9시간 빠르게 표시됨
- 예: 실제 2026-01-11 15:30에 가입했는데 "2026-01-11 06:30"으로 표시
- 관리자가 가입 시간을 직관적으로 파악하기 어려움

### 원인 분석

#### DB에 저장된 시간
```kotlin
@Entity
class Member : BaseTimeEntity() {
    var createdAt: LocalDateTime = LocalDateTime.now()  // UTC로 저장됨
}
```

데이터베이스에는 UTC 기준으로 시간이 저장되어 있습니다.

#### 템플릿에서의 표시
```html
<td class="text-muted">
    <small th:text="${#temporals.format(member.createdAt, 'yyyy-MM-dd HH:mm')}"></small>
</td>
```

Thymeleaf의 `#temporals.format()`은 단순히 LocalDateTime을 포맷팅만 하고, **시간대 변환을 하지 않습니다.**

따라서 UTC로 저장된 시간이 그대로 표시되었습니다.

### 해결 방법

#### DateTimeFormatter 유틸리티 활용

프로젝트에 이미 존재하는 `DateTimeFormatter` 유틸리티 클래스에 `convertUtcToKst()` 메서드가 있습니다:

```kotlin
// src/main/kotlin/codel/common/util/DateTimeFormatter.kt
fun convertUtcToKst(utcDateTime: LocalDateTime): LocalDateTime {
    return utcDateTime
        .atZone(ZoneId.of("UTC"))
        .withZoneSameInstant(ZoneId.of("Asia/Seoul"))
        .toLocalDateTime()
}
```

이 메서드는:
1. UTC LocalDateTime을 UTC ZonedDateTime으로 변환
2. "Asia/Seoul" 시간대로 변환
3. 다시 LocalDateTime으로 변환 (KST 기준)

#### 템플릿 수정

Thymeleaf의 `T()` 연산자를 사용하여 유틸리티 클래스의 static 메서드를 호출:

```html
<td class="text-muted">
    <small th:text="${#temporals.format(
        T(codel.common.util.DateTimeFormatter).convertUtcToKst(member.createdAt),
        'yyyy-MM-dd HH:mm'
    )}"></small>
</td>
```

**동작 순서**:
1. `member.createdAt` (UTC LocalDateTime)을 가져옴
2. `DateTimeFormatter.convertUtcToKst()`로 KST LocalDateTime으로 변환
3. `#temporals.format()`으로 "yyyy-MM-dd HH:mm" 형식으로 포맷

**변경 파일**:
- `src/main/resources/templates/memberList.html`

**효과**:
- 가입 시간이 한국 시간대(KST)로 표시됨
- 관리자가 회원 가입 시간을 직관적으로 확인 가능
- 예: UTC 2026-01-11 06:30 → KST 2026-01-11 15:30으로 표시

### 추가 수정: 날짜 검색 필터링도 KST 기준으로 동작하도록 수정

표시만 KST로 바꾸고 검색은 여전히 UTC 기준이면 문제가 발생합니다:
- 사용자가 "2026-01-11"로 검색하면 KST 기준 2026-01-11 하루를 원하는 것
- 하지만 기존에는 UTC 2026-01-11 00:00:00 ~ 23:59:59로 검색
- 실제로는 UTC 2026-01-10 15:00:00 ~ 2026-01-11 14:59:59를 검색해야 함

#### MemberService.kt 날짜 파싱 로직 수정

**변경 전**:
```kotlin
val startDateTime = if (!startDate.isNullOrBlank()) {
    try {
        LocalDate.parse(startDate).atStartOfDay() // KST를 UTC로 착각
    } catch (e: Exception) {
        null
    }
} else {
    null
}

val endDateTime = if (!endDate.isNullOrBlank()) {
    try {
        LocalDate.parse(endDate).plusDays(1).atStartOfDay()
    } catch (e: Exception) {
        null
    }
} else {
    null
}
```

**변경 후**:
```kotlin
val startDateTime = if (!startDate.isNullOrBlank()) {
    try {
        val kstDate = LocalDate.parse(startDate)
        // KST 날짜의 시작 시간(00:00:00)을 UTC로 변환
        codel.common.util.DateTimeFormatter.getUtcRangeForKstDate(kstDate).first
    } catch (e: Exception) {
        null
    }
} else {
    null
}

val endDateTime = if (!endDate.isNullOrBlank()) {
    try {
        val kstDate = LocalDate.parse(endDate)
        // KST 날짜의 종료 시간(23:59:59.999...)을 UTC로 변환
        codel.common.util.DateTimeFormatter.getUtcRangeForKstDate(kstDate).second
    } catch (e: Exception) {
        null
    }
} else {
    null
}
```

**동작 예시**:
- 사용자가 startDate="2026-01-11" 선택
  - KST 2026-01-11 00:00:00 → UTC 2026-01-10 15:00:00로 변환
  - 이 시간 이후의 회원 검색
- 사용자가 endDate="2026-01-11" 선택
  - KST 2026-01-11 23:59:59 → UTC 2026-01-11 14:59:59로 변환
  - 이 시간 이전의 회원 검색

**추가 변경 파일**:
- `src/main/kotlin/codel/member/business/MemberService.kt`

**추가 효과**:
- 날짜 검색도 KST 기준으로 정확하게 동작
- 사용자가 2026-01-11을 선택하면 KST 기준 2026-01-11 하루 동안 가입한 회원이 검색됨
- 표시와 검색 모두 일관되게 KST 기준으로 동작

---

## 전체 요약

이번 수정을 통해 관리자 페이지의 사용성이 크게 개선되었습니다:

### 해결된 문제
1. ✅ 회원 이름 검색이 정확하게 동작 (codeName만 검색)
2. ✅ 회원 가입일 범위 필터링 기능 추가
3. ✅ 탈퇴/오픈프로필 작성 완료 상태 필터링 가능
4. ✅ 질문 수정/삭제/상태 변경 후 필터 유지
5. ✅ 질문 검색 후 선택된 필터 값이 UI에 표시
6. ✅ 회원 가입 시간이 KST로 표시 및 날짜 검색도 KST 기준으로 동작

### 핵심 교훈
1. **레이어 간 데이터 전달 체크**: Controller → Service → Repository로 파라미터가 제대로 전달되는지 확인 필요
2. **Thymeleaf 기본 객체 이해**: `param`, `session` 등 기본 제공 객체와 이름 충돌 주의
3. **사용자 경험 고려**: 필터 적용 후 작업을 수행할 때 필터 상태를 유지하는 것이 중요
4. **전체 플로우 확인**: 프론트엔드(HTML) → Controller → Service → Repository → 다시 프론트엔드로 돌아오는 전체 흐름을 검증해야 함
5. **시간대 일관성**: 표시(Display)와 검색(Search) 모두 같은 시간대 기준으로 동작해야 함. DB에 UTC로 저장되어 있어도 사용자는 KST 기준으로 생각하므로 양방향 변환 필요

---

## UTC 시간대 처리 및 KST 변환

### 개요
CODE-L 프로젝트는 데이터베이스에 모든 시간 데이터를 **UTC(협정 세계시)** 기준으로 저장하고, 사용자에게 표시할 때는 **KST(한국 표준시, UTC+9)** 로 변환하는 방식을 사용합니다.

### 시간대 설정

#### 1. application.yml 설정
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          time_zone: UTC
```

**동작 방식**:
- JPA/Hibernate가 데이터베이스와 통신할 때 UTC 시간대를 사용하도록 강제
- `LocalDateTime.now()`로 생성된 시간은 JVM 시간대가 아닌 UTC 기준으로 DB에 저장됨
- MySQL의 `DATETIME` 타입 컬럼에 UTC 시간이 저장됨

**장점**:
- 글로벌 서비스 확장 시 시간대 문제 방지
- 서버 위치나 JVM 시간대 설정과 무관하게 일관된 시간 저장
- 타임존 변경 시 데이터 마이그레이션 불필요

**주의사항**:
- 애플리케이션 코드에서 시간을 다룰 때 UTC 기준임을 항상 인지해야 함
- 사용자에게 표시할 때는 반드시 KST로 변환 필요

#### 2. DB 저장 예시
```kotlin
@Entity
class Member : BaseTimeEntity() {
    var createdAt: LocalDateTime = LocalDateTime.now()
    // JVM 시간: 2026-01-12 15:30:00 (KST)
    // DB 저장: 2026-01-12 06:30:00 (UTC) ← 9시간 차이
}
```

### DateTimeFormatter 유틸리티

프로젝트에는 UTC↔KST 변환을 위한 유틸리티 클래스가 구현되어 있습니다.

**파일 위치**: `src/main/kotlin/codel/common/util/DateTimeFormatter.kt`

#### 주요 메서드

##### 1. convertUtcToKst()
```kotlin
fun convertUtcToKst(utcDateTime: LocalDateTime): LocalDateTime {
    return utcDateTime
        .atZone(ZoneId.of("UTC"))
        .withZoneSameInstant(ZoneId.of("Asia/Seoul"))
        .toLocalDateTime()
}
```

**용도**: DB에서 조회한 UTC 시간을 KST로 변환하여 사용자에게 표시

**동작**:
1. UTC LocalDateTime → UTC ZonedDateTime으로 변환
2. 같은 순간을 Asia/Seoul 시간대로 변환
3. KST LocalDateTime 반환 (9시간 추가)

**예시**:
```kotlin
val utcTime = LocalDateTime.of(2026, 1, 12, 6, 30)  // UTC 06:30
val kstTime = DateTimeFormatter.convertUtcToKst(utcTime)
// 결과: 2026-01-12 15:30 (KST)
```

##### 2. convertKstToUtc()
```kotlin
fun convertKstToUtc(kstDateTime: LocalDateTime): LocalDateTime {
    return kstDateTime
        .atZone(ZoneId.of("Asia/Seoul"))
        .withZoneSameInstant(ZoneId.of("UTC"))
        .toLocalDateTime()
}
```

**용도**: 사용자가 입력한 KST 시간을 UTC로 변환하여 DB 조회

**예시**:
```kotlin
val kstTime = LocalDateTime.of(2026, 1, 12, 15, 30)  // KST 15:30
val utcTime = DateTimeFormatter.convertKstToUtc(kstTime)
// 결과: 2026-01-12 06:30 (UTC)
```

##### 3. getUtcRangeForKstDate()
```kotlin
fun getUtcRangeForKstDate(kstDate: LocalDate): Pair<LocalDateTime, LocalDateTime> {
    val kstZone = ZoneId.of("Asia/Seoul")
    val utcZone = ZoneId.of("UTC")

    // 한국 날짜의 시작 (00:00:00 KST)
    val kstStartOfDay = kstDate.atStartOfDay(kstZone)

    // 한국 날짜의 종료 (23:59:59.999999999 KST)
    val kstEndOfDay = kstDate.atTime(LocalTime.MAX).atZone(kstZone)

    // UTC로 변환
    val utcStart = kstStartOfDay.withZoneSameInstant(utcZone).toLocalDateTime()
    val utcEnd = kstEndOfDay.withZoneSameInstant(utcZone).toLocalDateTime()

    return Pair(utcStart, utcEnd)
}
```

**용도**: 날짜 범위 검색 시 KST 날짜를 UTC 시간 범위로 변환

**예시**:
```kotlin
val kstDate = LocalDate.of(2026, 1, 12)
val (utcStart, utcEnd) = DateTimeFormatter.getUtcRangeForKstDate(kstDate)
// utcStart: 2026-01-11 15:00:00 (UTC) ← KST 2026-01-12 00:00:00
// utcEnd:   2026-01-12 14:59:59 (UTC) ← KST 2026-01-12 23:59:59
```

**왜 필요한가?**:
- 사용자가 "2026-01-12"를 검색하면 KST 기준 하루를 의미
- DB에는 UTC로 저장되어 있으므로, UTC 기준으로는 "2026-01-11 15:00 ~ 2026-01-12 14:59"를 검색해야 함
- 이 메서드가 정확한 UTC 범위를 계산해줌

### 현재 구현 상태

#### ❌ 미적용 영역

**memberList.html - 가입 시간 표시**
```html
<!-- 현재: UTC 그대로 표시 -->
<td class="text-muted">
    <small th:text="${#temporals.format(member.createdAt, 'yyyy-MM-dd HH:mm')}"></small>
</td>
```

**문제점**:
- DB의 UTC 시간이 그대로 표시됨
- 관리자가 보기에 9시간 빠른 시간으로 보임
- 예: 실제 2026-01-12 15:30 가입 → "2026-01-12 06:30"으로 표시

**개선 방향**:
```html
<!-- 개선안: KST로 변환하여 표시 -->
<td class="text-muted">
    <small th:text="${#temporals.format(
        T(codel.common.util.DateTimeFormatter).convertUtcToKst(member.createdAt),
        'yyyy-MM-dd HH:mm'
    )}"></small>
</td>
```

**MemberService.kt - 날짜 필터링**
```kotlin
// 현재: KST를 UTC로 착각
val startDateTime = if (!startDate.isNullOrBlank()) {
    try {
        LocalDate.parse(startDate).atStartOfDay() // KST를 UTC로 착각
    } catch (e: Exception) {
        null
    }
} else {
    null
}
```

**문제점**:
- 사용자가 "2026-01-12" 검색 시 UTC 2026-01-12 00:00를 검색
- 실제로는 KST 2026-01-12 09:00부터 검색됨
- KST 기준 00:00~08:59 가입자가 누락됨

**개선 방향**:
```kotlin
// 개선안: KST 날짜를 UTC 범위로 정확히 변환
val startDateTime = if (!startDate.isNullOrBlank()) {
    try {
        val kstDate = LocalDate.parse(startDate)
        codel.common.util.DateTimeFormatter.getUtcRangeForKstDate(kstDate).first
    } catch (e: Exception) {
        null
    }
} else {
    null
}

val endDateTime = if (!endDate.isNullOrBlank()) {
    try {
        val kstDate = LocalDate.parse(endDate)
        codel.common.util.DateTimeFormatter.getUtcRangeForKstDate(kstDate).second
    } catch (e: Exception) {
        null
    }
} else {
    null
}
```

#### ✅ 적용 영역

**KpiBatchService.kt - KPI 집계**
```kotlin
// KPI 집계 시 정확한 UTC 범위 사용
val (utcStart, utcEnd) = DateTimeFormatter.getUtcRangeForKstDate(kstDate)
val signals = signalJpaRepository.findAllByCreatedAtBetween(utcStart, utcEnd)
```

**장점**:
- KST 날짜 기준으로 정확한 일일 통계 집계
- 00:00~23:59 전체 데이터 포함

### 시간대 변환 체크리스트

프로젝트에서 시간을 다룰 때 다음을 확인하세요:

#### DB 저장 시
- ✅ application.yml의 `time_zone: UTC` 설정 활성화
- ✅ `LocalDateTime.now()` 사용 시 UTC로 저장됨을 인지
- ❌ 절대 `ZonedDateTime.now(ZoneId.of("Asia/Seoul"))`를 DB에 저장하지 말 것

#### 사용자 표시 시
- ✅ Thymeleaf 템플릿에서 `DateTimeFormatter.convertUtcToKst()` 사용
- ✅ API Response DTO에서 KST로 변환하여 반환
- ❌ UTC 시간을 그대로 표시하지 말 것

#### 날짜 검색 시
- ✅ 사용자 입력(KST)을 UTC 범위로 변환
- ✅ `getUtcRangeForKstDate()` 메서드 활용
- ❌ KST 날짜를 UTC로 착각하여 직접 사용하지 말 것

#### 비즈니스 로직 시
- ✅ 시간 비교/계산 시 UTC 기준임을 명시
- ✅ 로그에 시간 출력 시 "(UTC)" 또는 "(KST)" 표기
- ❌ 시간대를 불명확하게 두지 말 것

### 시간대 디버깅 팁

#### 1. 로그 확인
```kotlin
log.info { "Created at (UTC): ${member.createdAt}" }
log.info { "Created at (KST): ${DateTimeFormatter.convertUtcToKst(member.createdAt)}" }
```

#### 2. SQL 쿼리 확인
```sql
-- DB에 저장된 실제 값 확인
SELECT id, created_at FROM member WHERE id = 1;
-- 결과: 2026-01-12 06:30:00 (UTC)

-- KST로 변환하여 확인
SELECT id, CONVERT_TZ(created_at, '+00:00', '+09:00') as created_at_kst
FROM member WHERE id = 1;
-- 결과: 2026-01-12 15:30:00 (KST)
```

#### 3. 테스트 시
```kotlin
@Test
fun `UTC 저장 및 KST 변환 테스트`() {
    val member = Member(...)
    memberRepository.save(member)

    val saved = memberRepository.findById(member.id!!).get()
    val kstTime = DateTimeFormatter.convertUtcToKst(saved.createdAt)

    // KST 기준으로 검증
    assertThat(kstTime.hour).isEqualTo(15)  // KST 15시
}
```

### 추후 개선 과제

1. **프론트엔드 일괄 적용**
   - 모든 HTML 템플릿에서 시간 표시 시 KST 변환 적용
   - 공통 Thymeleaf fragment 생성 검토

2. **API Response DTO 개선**
   - `@JsonFormat` 어노테이션으로 자동 변환 검토
   - Custom JsonSerializer 구현 검토

3. **문서화**
   - CLAUDE.md에 UTC 정책 명시
   - 신규 개발자 온보딩 가이드에 포함

4. **모니터링**
   - 시간대 오류 감지 로직 추가
   - KST/UTC 혼용 여부 체크 스크립트 작성

---

**작성일**: 2026-01-11
**이슈**: #384
**PR**: #385
