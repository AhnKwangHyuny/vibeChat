package com.vibechat.service.room.list;

import com.vibechat.domain.ChatRoom;
import com.vibechat.dto.room.RoomListPageResponse;
import com.vibechat.dto.room.RoomListRequest;
import com.vibechat.dto.room.RoomListResponse;
import com.vibechat.repository.chatRoom.ChatRoomRepository;
import com.vibechat.service.room.participants.RoomParticipantCountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 방 목록 조회 서비스 구현체
 * 
 * SRP: 방 목록 조회와 페이지네이션만 처리
 * - 방 생성/수정/삭제는 RoomCommandService가 담당
 * - 단일 방 조회는 RoomQueryService가 담당
 * 
 * 최적화:
 * - Repository에서 Fetch Join으로 N+1 해결
 * - Redis에서 참가자 수 배치 조회
 * - 읽기 전용 트랜잭션
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RoomListServiceImpl implements RoomListService {

    private final ChatRoomRepository chatRoomRepository;
    private final RoomParticipantCountService participantCountService;

    @Override
    public RoomListPageResponse getRoomList(RoomListRequest request) {
        log.info("[RoomListService] 방 목록 조회 시작: {}", request);

        // 1. Validation 및 기본값 설정
        request.validate();

        // 2. Repository에서 방 목록 조회 (Fetch Join 적용)
        List<ChatRoom> chatRooms = chatRoomRepository.findRoomsWithFilters(request);

        // 3. DTO 변환 (참가자 수 포함)
        List<RoomListResponse> roomResponses = chatRooms.stream()
            .map(this::convertToResponseWithParticipantCount)
            .collect(Collectors.toList());

        // 4. 페이지네이션 응답 구성
        RoomListPageResponse response = RoomListPageResponse.of(roomResponses, request.getLimit());

        log.info("[RoomListService] 방 목록 조회 완료: {} 건, hasNext={}", 
            roomResponses.size(), response.isHasNext());

        return response;
    }

    @Override
    public RoomListPageResponse getRoomListByTags(RoomListRequest request) {
        log.info("[RoomListService] 태그 기반 검색: tags={}", request.getTags());

        // Validation
        request.validate();
        
        if (request.getTags() == null || request.getTags().isEmpty()) {
            log.warn("[RoomListService] 태그가 비어있음, 전체 목록 조회로 대체");
            return getRoomList(request);
        }

        // 태그 최적화 쿼리 사용
        List<ChatRoom> chatRooms = chatRoomRepository.findByTagsWithCursor(
            request.getTags(), 
            request.getLastId(), 
            request.getLimit()
        );

        // DTO 변환
        List<RoomListResponse> roomResponses = chatRooms.stream()
            .map(this::convertToResponseWithParticipantCount)
            .collect(Collectors.toList());

        return RoomListPageResponse.of(roomResponses, request.getLimit());
    }

    @Override
    public RoomListPageResponse getPopularRooms(int limit) {
        log.info("[RoomListService] 인기 방 목록 조회: limit={}", limit);

        // TODO: 향후 Redis 기반 참가자 수 정렬 구현
        // 현재는 최신순으로 대체
        
        RoomListRequest request = RoomListRequest.builder()
            .sortBy("createdAt")
            .sortOrder("DESC")
            .limit(Math.min(limit, 50))
            .build();

        return getRoomList(request);
    }

    @Override
    public RoomListPageResponse getRecentRooms(int limit) {
        log.info("[RoomListService] 최신 방 목록 조회: limit={}", limit);

        // 간단한 최신순 조회
        RoomListRequest request = RoomListRequest.builder()
            .sortBy("createdAt")
            .sortOrder("DESC")
            .limit(Math.min(limit, 50))
            .build();

        return getRoomList(request);
    }

    /**
     * ChatRoom → RoomListResponse 변환 (참가자 수 포함)
     * 
     * @param chatRoom 변환할 ChatRoom 엔티티
     * @return 참가자 수가 포함된 RoomListResponse
     */
    private RoomListResponse convertToResponseWithParticipantCount(ChatRoom chatRoom) {
        // Redis에서 실시간 참가자 수 조회
        int participantCount = participantCountService.getParticipantCount(chatRoom.getId());
        
        return RoomListResponse.from(chatRoom, participantCount);
    }

    /**
     * 배치로 참가자 수 조회 (성능 최적화)
     * 
     * TODO: 향후 구현
     * - Redis Pipeline 또는 MGET 사용
     * - N번 조회 → 1번 조회
     */
    @SuppressWarnings("unused")
    private List<RoomListResponse> convertToResponsesWithBatchCount(List<ChatRoom> chatRooms) {
        // 1. 모든 방 ID 추출
        List<Long> roomIds = chatRooms.stream()
            .map(ChatRoom::getId)
            .collect(Collectors.toList());

        // 2. 배치로 참가자 수 조회 (Redis Pipeline)
        // Map<Long, Integer> participantCounts = participantCountService.getParticipantCounts(roomIds);

        // 3. DTO 변환
        return chatRooms.stream()
            .map(room -> {
                // int count = participantCounts.getOrDefault(room.getId(), 0);
                int count = 0; // TODO: 배치 조회 구현 후 활성화
                return RoomListResponse.from(room, count);
            })
            .collect(Collectors.toList());
    }
}

