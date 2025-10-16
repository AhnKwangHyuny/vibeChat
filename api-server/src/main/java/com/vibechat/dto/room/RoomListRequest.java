package com.vibechat.dto.room;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 방 목록 조회 요청 DTO
 * 
 * Query Parameters로 전달되는 검색/필터/페이지네이션 조건
 * - 커서 기반 페이지네이션 (무한 스크롤)
 * - 다양한 정렬 옵션 지원
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomListRequest {

    /**
     * 검색어 (제목, 설명 검색)
     */
    private String query;

    /**
     * 태그 필터 (OR 조건)
     */
    private List<String> tags;

    /**
     * 공개/비공개 필터
     * - null: 전체
     * - true: 비공개만
     * - false: 공개만
     */
    private Boolean isPrivate;

    /**
     * 정렬 기준
     * - "createdAt": 생성일 기준 (기본값)
     * - "participantsCount": 참가자 수 기준 (향후 구현)
     * - "title": 제목 기준 (향후 구현)
     */
    @Builder.Default
    private String sortBy = "createdAt";

    /**
     * 정렬 순서
     * - "DESC": 내림차순 (최신순, 기본값)
     * - "ASC": 오름차순 (오래된 순)
     */
    @Builder.Default
    private String sortOrder = "DESC";

    /**
     * 페이지 크기 (한 번에 가져올 방 개수)
     * 기본값: 20
     */
    @Builder.Default
    private Integer limit = 20;

    /**
     * 커서 (마지막으로 조회한 방의 ID)
     * 무한 스크롤 구현을 위한 커서 기반 페이지네이션
     * - null: 첫 페이지
     * - 값 있음: 해당 ID 이후의 방들 조회
     */
    private Long lastId;

    /**
     * Validation 및 기본값 설정
     */
    public void validate() {
        if (limit == null || limit <= 0 || limit > 50) {
            limit = 20;
        }
        
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "createdAt";
        }
        
        if (sortOrder == null || sortOrder.isBlank()) {
            sortOrder = "DESC";
        }
        
        // sortBy 값 검증
        if (!List.of("createdAt", "participantsCount", "title").contains(sortBy)) {
            sortBy = "createdAt";
        }
        
        // sortOrder 값 검증
        if (!List.of("ASC", "DESC").contains(sortOrder.toUpperCase())) {
            sortOrder = "DESC";
        } else {
            sortOrder = sortOrder.toUpperCase();
        }
    }
}

