package com.vibechat.scheduler;

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
//            userRepository.deleteAllByIdInBatch(userIdsToDelete);
        }
    }
}
