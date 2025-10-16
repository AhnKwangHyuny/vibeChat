# Controller 설계 결정: 태그 기반 방 검색

## 🤔 **질문**
태그 기반 방 검색 API를 `ChatRoomTagController`와 병합해야 하는가?

## ✅ **결정: 병합하지 않음 (현재 구조 유지)**

---

## 📊 **근거**

### **1. RESTful 리소스 중심 설계**

#### **올바른 설계:**
```
GET /api/rooms?tags=react             # Room 리소스의 필터링
GET /api/tags/autocomplete?q=react    # Tag 리소스의 기능
```

#### **잘못된 설계 (병합 시):**
```
GET /api/tags/rooms?tags=react        # 혼란스러움
GET /api/rooms-by-tags?tags=react     # RESTful 위반
```

**판단:** 태그로 방을 검색하는 것은 **방 리소스에 대한 쿼리**.

---

### **2. 단일 책임 원칙 (SRP)**

| Controller | 주 리소스 | 책임 |
|------------|-----------|------|
| `RoomListController` | Room | 방 목록 조회/검색/필터링 |
| `ChatRoomTagController` | Tag | 태그 관리/통계/추천 |

**예상 확장:**

**RoomListController (방 중심):**
- 방 검색 (제목, 설명, **태그**)
- 방 필터링 (공개/비공개, 참가자 수)
- 방 정렬 (최신순, 인기순)
- 페이지네이션

**ChatRoomTagController (태그 중심):**
- 태그 자동완성
- 인기 태그 목록
- 태그 통계 (사용 횟수)
- 연관 태그 추천
- 태그 CRUD

**판단:** 관심사가 명확히 분리됨. 병합 시 SRP 위반.

---

### **3. URL 일관성**

#### **현재 (일관됨):**
```
# Room 리소스
GET /api/rooms                        # 모든 방
GET /api/rooms?tags=react             # 태그 필터
GET /api/rooms?query=hello            # 검색어 필터
GET /api/rooms/popular                # 인기 방

# Tag 리소스
GET /api/tags/autocomplete            # 자동완성
GET /api/tags/popular                 # 인기 태그
GET /api/tags/{tagId}/stats           # 태그 통계
```

#### **병합 시 (일관성 깨짐):**
```
GET /api/tags/rooms?tags=react        # ❌ 이상함
GET /api/rooms?tags=react             # ✅ 자연스러움
```

---

## 🏗️ **추천 아키텍처**

### **Controller Layer (유지):**
```
controller/
├── room/
│   ├── RoomListController       ← 방 목록/검색 (태그 필터 포함)
│   ├── RoomCommandController
│   └── RoomQueryController
└── ChatRoomTagController         ← 태그 자동완성/통계
```

### **Service Layer (강화):**
```
service/
├── room/
│   ├── list/
│   │   ├── RoomListService           # 일반 목록
│   │   └── RoomTagSearchService      # 태그 검색 전용 ← 새로 추가!
│   └── ...
└── tag/
    ├── chatRoomTagService/
    │   ├── ChatRoomTagService        # 태그 CRUD
    │   ├── TagAutocompleteService    # 자동완성 ← 향후 분리
    │   └── TagStatisticsService      # 통계 ← 향후 추가
```

**핵심 전략:**
- ✅ Controller는 리소스 중심 유지
- ✅ Service를 세분화하여 책임 분리
- ✅ 태그 로직 증가 → Service 추가, Controller 병합 ❌

---

## 📈 **복잡도 증가 시 대응**

### **Scenario: 태그 검색 로직이 매우 복잡해지는 경우**

#### **❌ 나쁜 접근 (Controller 병합):**
```java
@RestController
@RequestMapping("/api/tags")
public class UnifiedTagController {
    // 방 검색 (어색함!)
    @GetMapping("/rooms")
    public RoomListPageResponse searchRoomsByTags(...) {
        // 복잡한 로직...
    }
    
    // 태그 자동완성
    @GetMapping("/autocomplete")
    public List<TagResponse> autocomplete(...) { }
}
```
→ SRP 위반, URL 혼란

#### **✅ 좋은 접근 (Service 분리):**

**1단계: 전용 Service 생성**
```java
// RoomTagSearchService.java
@Service
public class RoomTagSearchService {
    
    /**
     * 태그 기반 고급 검색
     * - AND/OR 조건
     * - 태그 가중치
     * - 연관 태그 확장
     */
    public List<ChatRoom> searchByTagsAdvanced(
        List<String> requiredTags,
        List<String> optionalTags,
        TagSearchStrategy strategy
    ) {
        // 복잡한 태그 검색 로직
        // 1. 태그 정규화
        // 2. 연관 태그 조회
        // 3. 가중치 계산
        // 4. Repository 호출
    }
}
```

**2단계: Controller는 간결 유지**
```java
@RestController
@RequestMapping("/api/rooms")
public class RoomListController {
    
    private final RoomListService roomListService;
    private final RoomTagSearchService roomTagSearchService; // ← 전용 서비스
    
    // 일반 검색 (태그는 단순 필터)
    @GetMapping
    public RoomListPageResponse getRooms(...) {
        return roomListService.getRoomList(request);
    }
    
    // 고급 태그 검색
    @GetMapping("/search/by-tags")
    public RoomListPageResponse advancedTagSearch(
        @RequestParam List<String> requiredTags,
        @RequestParam(required = false) List<String> optionalTags
    ) {
        // 오케스트레이션만!
        return roomTagSearchService.searchAdvanced(requiredTags, optionalTags);
    }
}
```

**장점:**
- ✅ URL 일관성 유지 (`/api/rooms/...`)
- ✅ SRP 준수 (Service에서 복잡도 처리)
- ✅ Controller는 여전히 간결
- ✅ 테스트 용이

---

## 🌐 **실제 대규모 프로젝트 사례**

### **GitHub REST API:**
```
GET /search/repositories?q=topic:react    # Repository 검색
GET /topics                               # Topic 목록
GET /topics/:topic                        # Topic 정보
```

### **Stack Overflow API:**
```
GET /questions?tagged=java                # 질문 검색
GET /tags                                 # 태그 목록
GET /tags/:tag/info                       # 태그 정보
GET /tags/:tag/wikis                      # 태그 위키
```

### **Medium API:**
```
GET /stories?tags=javascript              # 스토리 검색
GET /tags                                 # 태그 목록
GET /tags/:tag/stories                    # 특정 태그의 스토리
```

**공통 패턴:**
- 리소스 중심 URL 설계
- 태그는 필터/검색 조건으로 사용
- 태그 자체 기능은 `/tags` 경로

---

## 📌 **최종 결론**

### **현재 구조 유지 이유:**
1. ✅ RESTful 원칙 준수
2. ✅ SRP 유지 (책임 명확)
3. ✅ URL 일관성 유지
4. ✅ 확장 용이 (Service 세분화)
5. ✅ 업계 표준 패턴

### **복잡도 증가 대응:**
- Controller 병합 ❌
- Service 세분화 ✅
- 전용 Service 생성 (`RoomTagSearchService`)

### **향후 확장 계획:**
```
Phase 1 (현재):
- RoomListService: 태그 포함 일반 검색

Phase 2 (복잡도 증가 시):
- RoomTagSearchService: 태그 전용 고급 검색
  - AND/OR 조건
  - 가중치 계산
  - 연관 태그 확장

Phase 3 (대규모 시):
- ElasticsearchService: 전체 텍스트 검색
- TagRecommendationService: ML 기반 추천
```

---

## 🎓 **교훈**

> **"복잡도는 Service Layer에서 관리하고,  
> Controller는 리소스 중심 설계를 유지하라."**

- Controller 병합은 단기적으로 편해 보이지만 장기적으로 유지보수 악몽
- Service 세분화는 초기 공수가 들지만 확장성과 테스트 용이성 확보
- RESTful 원칙과 SRP는 이유가 있어서 존재하는 것

---

**결정자:** Tech Lead  
**날짜:** 2025-10-16  
**상태:** Approved ✅

