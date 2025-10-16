package com.vibechat.dto.room;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 방 목록 페이지 응답 DTO
 * 
 * 무한 스크롤을 위한 커서 기반 페이지네이션 정보 포함
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomListPageResponse {

    /**
     * 조회된 방 목록
     */
    private List<RoomListResponse> rooms;

    /**
     * 다음 페이지가 있는지 여부
     * - true: 더 로드할 방이 있음 (무한 스크롤 계속)
     * - false: 마지막 페이지 (무한 스크롤 종료)
     */
    private boolean hasNext;

    /**
     * 다음 페이지 조회를 위한 커서 (마지막 방의 ID)
     * hasNext가 true일 때만 사용
     */
    private Long nextCursor;

    /**
     * 현재 페이지의 방 개수
     */
    private int size;

    /**
     * 전체 방 개수 (선택적, 성능상 이유로 null 가능)
     */
    private Long totalCount;

    /**
     * 팩토리 메서드: 페이지네이션 결과 생성
     * 
     * @param rooms 조회된 방 목록
     * @param requestedLimit 요청한 페이지 크기
     * @return 페이지네이션 응답
     */
    public static RoomListPageResponse of(List<RoomListResponse> rooms, int requestedLimit) {
        boolean hasNext = rooms.size() >= requestedLimit;
        Long nextCursor = hasNext && !rooms.isEmpty() 
            ? rooms.get(rooms.size() - 1).getId() 
            : null;

        return RoomListPageResponse.builder()
            .rooms(rooms)
            .hasNext(hasNext)
            .nextCursor(nextCursor)
            .size(rooms.size())
            .totalCount(null) // 성능상 이유로 전체 카운트는 별도 API에서 제공
            .build();
    }
}

