# VibeChat 로그아웃 및 데이터 관리 워크플로우

이 문서는 VibeChat의 게스트 및 소셜 로그인 사용자에 대한 로그아웃 처리와, 비활성 게스트 계정의 데이터를 주기적으로 삭제하는 백엔드 스케줄러의 전체 설계 및 구현 방안을 정의합니다.

## 1. 핵심 설계 원칙

본 아키텍처는 두 가지 핵심 원칙을 기반으로 합니다.

> **원칙 1: 로그아웃의 역할은 '세션 무효화'에 집중한다.**
> 사용자가 어떤 방식으로 로그인했든, 로그아웃 API의 역할은 현재 사용자를 식별하는 서버 세션을 즉시 파기하여 더 이상 인증된 요청을 할 수 없도록 만드는 것입니다. 이 과정은 가볍고 빨라야 합니다.

> **원칙 2: 데이터 삭제는 '주기적인 정리' 작업으로 분리한다.**
> 게스트의 채팅 기록 등 무거운 데이터를 삭제하는 작업은 로그아웃과 별개로, 비동기 스케줄러가 처리합니다. 이를 통해 사용자는 빠른 로그아웃을 경험하고, 서버는 부하가 적은 시간에 안정적으로 데이터를 정리할 수 있습니다.

## 2. 통합 로그아웃 워크플로우

**설계 목표:** 사용자의 로그인 방식(게스트/Google)과 관계없이, 서버의 관점에서는 **하나의 일관된 세션 파기 프로세스**를 제공합니다. 클라이언트는 사용자의 종류에 따라 필요한 부가 작업을 수행한 후, 공통 로그아웃 API를 호출합니다.

### 전체 흐름도

```mermaid
graph TD
    subgraph "Frontend (Client)"
        A[로그아웃 버튼 클릭] --> B{사용자 종류 확인 (Redux)};
        B -->|Google 유저| C[supabase.auth.signOut() 호출];
        C --> D[POST /api/auth/logout 요청];
        B -->|게스트 유저| D;
        D --> E[Redux 스토어 초기화 (clearUser)];
    end

    subgraph "Backend (Server)"
        F(POST /api/auth/logout) --> G{요청의 세션 쿠키 확인};
        G --> H[HttpSession.invalidate()];
        H --> I[204 No Content 응답];
    end

    D -.-> F;
```

### A. 백엔드 구현 (`POST /api/auth/logout`)

백엔드는 `/api/auth/logout`이라는 단일 엔드포인트를 통해 모든 로그아웃 요청을 처리합니다.

1.  **Controller**: `AuthQueryController.java`에 `logout` 메소드를 구현합니다.
    *   이 메소드는 요청과 함께 전달된 `JSESSIONID` 쿠키를 통해 현재 `HttpSession`을 가져옵니다.
    *   세션이 존재하면 `session.invalidate()`를 호출하여 세션을 완전히 파기합니다.
    *   세션이 존재하지 않거나 이미 만료된 경우(즉, 인증되지 않은 요청), Spring Security가 `401 Unauthorized`를 반환하므로 별도의 분기 처리가 필요 없습니다.

    ```java
    // backend/src/main/java/com/vibechat/controller/AuthQueryController.java

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        // 현재 요청에 대한 세션을 가져옵니다. (없으면 null 반환)
        HttpSession session = request.getSession(false);

        if (session != null) {
            // 세션이 존재하면 무효화합니다.
            session.invalidate();
        }

        // 성공적으로 처리되었으며, 반환할 콘텐츠가 없음을 의미하는 204 No Content를 응답합니다.
        return ResponseEntity.noContent().build();
    }
    ```

### B. 프론트엔드 구현 (`handleLogout` 함수)

프론트엔드는 로그아웃 과정을 총괄하는 "오케스트레이터" 역할을 합니다.

1.  **필요 로직**:
    *   Redux 스토어에서 현재 사용자의 `provider` 정보를 확인합니다. (**사전 조건**: `userSlice`에 `provider` 필드가 있어야 합니다.)
    *   `provider`가 'GOOGLE'이면, Supabase 클라이언트 측 로그아웃(`supabase.auth.signOut()`)을 먼저 수행합니다.
    *   그 다음, **모든 사용자는 공통적으로** 백엔드의 로그아웃 API(`POST /api/auth/logout`)를 호출합니다.
    *   마지막으로 Redux 스토어의 사용자 정보를 `clearUser()`로 초기화하여 UI를 즉시 갱신합니다.

2.  **구현 예시**: `Navbar.tsx` 또는 로그아웃 버튼이 있는 컴포넌트

    ```typescript
    // Navbar.tsx 예시

    import { useSelector, useDispatch } from 'react-redux';
    import { RootState, AppDispatch } from '../store';
    import { clearUser } from '../store/userSlice';
    import { useSessionAuth } from '../hooks/useSessionAuth';
    import { useSupabaseAuth } from '../hooks/useSupabaseAuth';
    import { toast } from 'react-toastify';

    // ... 컴포넌트 내부

    const user = useSelector((state: RootState) => state.user);
    const dispatch = useDispatch<AppDispatch>();
    const { signOut: sessionSignOut } = useSessionAuth();
    const { signOut: supabaseSignOut } = useSupabaseAuth();

    const handleLogout = async () => {
      console.log("로그아웃을 시작합니다...");
      try {
        // 1. (소셜 로그인 사용자만) Supabase 클라이언트 세션 종료
        if (user.provider === 'GOOGLE') {
          await supabaseSignOut();
          console.log("Supabase 세션이 종료되었습니다.");
        }

        // 2. (모든 사용자 공통) VibeChat 백엔드 세션 종료
        await sessionSignOut();
        console.log("VibeChat 백엔드 세션이 종료되었습니다.");

        // 3. 성공 알림 (Redux의 clearUser는 각 signOut 훅에서 이미 처리하고 있음)
        toast.success("성공적으로 로그아웃되었습니다.");

      } catch (error) {
        console.error("로그아웃 중 오류 발생:", error);
        toast.error("로그아웃 중 문제가 발생했습니다. 페이지를 새로고침합니다.");
        
        // 최악의 경우에도 UI를 초기화하고 새로고침하여 상태를 완전히 정리
        dispatch(clearUser());
        window.location.reload();
      }
    };
    ```

---

## 3. 비활성 게스트 계정 삭제 스케줄러

**설계 목표:** 로그아웃 이벤트와 분리하여, 주기적으로 비활성 게스트 계정을 찾아 정리하는 '가비지 컬렉터'를 구현합니다. 이 방식은 사용자가 로그아웃 없이 이탈하는 경우까지 모두 처리할 수 있어 훨씬 안정적입니다.

### A. 백엔드 구현

1.  **`User` 엔티티 수정**: `User.java` 엔티티에 `updatedAt` 필드가 `@UpdateTimestamp`와 함께 정의되어 있는지 확인합니다. 이 필드는 해당 사용자와 관련된 마지막 활동 시간을 기록합니다.

    ```java
    // domain/User.java
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    ```

2.  **`UserRepository` 수정**: 특정 시간 이전에 마지막으로 활동한 게스트 사용자를 찾는 쿼리 메소드를 추가합니다.

    ```java
    // repository/UserRepository.java
    public interface UserRepository extends JpaRepository<User, Long> {
        // ... 기존 메소드들 ...

        // 특정 시간 이전에 마지막으로 활동한 게스트 사용자들의 ID 목록을 조회
        @Query("SELECT u.id FROM User u WHERE u.provider = :provider AND u.updatedAt < :threshold")
        List<Long> findInactiveUserIdsByProvider(
            @Param("provider") UserProvider provider, 
            @Param("threshold") LocalDateTime threshold
        );
    }
    ```
    *   **참고**: `MessageRepository` 등 게스트 사용자와 연관된 다른 데이터들을 일괄 삭제하기 위한 `deleteByUserIdIn(List<Long> userIds)` 같은 메소드도 필요할 수 있습니다.

3.  **스케줄러 서비스 생성**: '비활성 게스트 청소부' 역할을 할 스케줄러를 생성합니다.

    ```java
    // scheduler/UserCleanupScheduler.java
    import com.vibechat.domain.UserProvider;
    import com.vibechat.repository.UserRepository;
    import lombok.RequiredArgsConstructor;
    import org.springframework.scheduling.annotation.Scheduled;
    import org.springframework.stereotype.Component;
    import org.springframework.transaction.annotation.Transactional;
    import java.time.LocalDateTime;
    import java.util.List;

    @Component
    @RequiredArgsConstructor
    public class UserCleanupScheduler {

        private final UserRepository userRepository;
        // private final MessageRepository messageRepository; // 예시

        /**
         * 매일 새벽 4시에 실행됩니다.
         * cron = "초 분 시 일 월 요일"
         */
        @Scheduled(cron = "0 0 4 * * *")
        @Transactional
        public void cleanupInactiveGuestUsers() {
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            
            // 1. 7일 이상 활동이 없는 게스트 사용자 ID 목록 조회
            List<Long> userIdsToDelete = userRepository.findInactiveUserIdsByProvider(UserProvider.GUEST, sevenDaysAgo);
            
            if (userIdsToDelete != null && !userIdsToDelete.isEmpty()) {
                System.out.println("Deleting " + userIdsToDelete.size() + " inactive guest users.");
                
                // 2. 관련 데이터 삭제 (메시지, 방 참여 정보 등)
                // messageRepository.deleteByUserIdIn(userIdsToDelete);
                // userRoomRepository.deleteByUserIdIn(userIdsToDelete);
                
                // 3. 사용자 최종 삭제 (JPA의 일괄 삭제 기능 사용)
                userRepository.deleteAllByIdInBatch(userIdsToDelete);
            }
        }
    }
    ```

4.  **스케줄링 활성화**: 메인 애플리케이션 클래스(`VibeChatApplication.java`)에 `@EnableScheduling` 어노테이션을 추가합니다.

    ```java
    @SpringBootApplication
    @EnableScheduling // 스케줄링 기능 활성화
    public class VibeChatApplication {
        // ...
    }
    ```
