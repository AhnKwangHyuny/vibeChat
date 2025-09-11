package com.vibechat.controller;

import com.vibechat.dto.ReportRequest;
import com.vibechat.service.ReportService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages/{messageId}/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<Void> reportMessage(
            @PathVariable Long messageId,
            @Valid @RequestBody ReportRequest request,
            HttpSession session) {

        Long reporterUserId = (Long) session.getAttribute("userId");
        if (reporterUserId == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        reportService.reportMessage(messageId, reporterUserId, request);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
