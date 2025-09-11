package com.vibechat.repository;

import com.vibechat.domain.Message;
import com.vibechat.domain.MessageReport;
import com.vibechat.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageReportRepository extends JpaRepository<MessageReport, Long> {
    boolean existsByMessageAndReporter(Message message, User reporter);
}
