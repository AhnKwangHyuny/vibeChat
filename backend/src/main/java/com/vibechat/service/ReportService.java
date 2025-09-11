package com.vibechat.service;

import com.vibechat.domain.Message;
import com.vibechat.domain.MessageReport;
import com.vibechat.domain.User;
import com.vibechat.dto.ReportRequest;
import com.vibechat.repository.MessageReportRepository;
import com.vibechat.repository.MessageRepository;
import com.vibechat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    private final MessageReportRepository reportRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public void reportMessage(Long messageId, Long reporterUserId, ReportRequest request) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        
        User reporter = userRepository.findById(reporterUserId)
                .orElseThrow(() -> new IllegalArgumentException("Reporter user not found"));

        if (reportRepository.existsByMessageAndReporter(message, reporter)) {
            throw new IllegalStateException("You have already reported this message.");
        }

        MessageReport report = new MessageReport();
        report.setMessage(message);
        report.setReporter(reporter);
        report.setReason(request.getReason().name());
        report.setDetails(request.getDetails());

        reportRepository.save(report);

        // Audit log
        logger.info("Message {} reported by user {} for reason: {}", messageId, reporterUserId, request.getReason());
    }
}
