package com.vibechat.scheduler;

import com.vibechat.repository.ChatRoomRepository;
import com.vibechat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class MessageTrimJob {

    private static final Logger logger = LoggerFactory.getLogger(MessageTrimJob.class);
    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;

    // Run once every day at 3 AM
    @Scheduled(cron = "0 0 3 * * ?")
    public void trimOldMessages() {
        logger.info("Starting message trim job...");
        try {
            var roomIds = chatRoomRepository.findAllIds();
            for (Long roomId : roomIds) {
                try {
                    messageRepository.trimOldMessages(roomId, 1000);
                } catch (Exception e) {
                    logger.error("Failed to trim messages for room {}", roomId, e);
                }
            }
        } catch (Exception e) {
            logger.error("Trim job failed", e);
        }
        logger.info("Message trim job finished.");
    }
}
