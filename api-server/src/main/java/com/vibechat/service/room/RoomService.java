package com.vibechat.service.room;

import com.vibechat.domain.auth.UserPrincipal;
import com.vibechat.dto.room.RoomCreateRequest;
import com.vibechat.dto.room.RoomResponse;

import java.util.List;

/**
 * 레거시 RoomService (Deprecated)
 * 
 * @deprecated 새로운 Service Layer로 마이그레이션됨:
 * - {@link com.vibechat.service.room.command.RoomCommandService} (방 생성/수정/삭제)
 * - {@link com.vibechat.service.room.query.RoomQueryService} (방 조회)
 * - {@link com.vibechat.service.room.list.RoomListService} (방 목록)
 * 
 * TODO: 완전히 미사용 확인 후 삭제 예정
 */
@Deprecated
public interface RoomService {
    
    /**
     * @deprecated Use {@link com.vibechat.service.room.command.RoomCommandService#createRoom}
     */
    @Deprecated
    RoomResponse createRoom(RoomCreateRequest roomCreateRequest, UserPrincipal principal);
    
    /**
     * @deprecated Use {@link com.vibechat.service.room.list.RoomListService#getRoomListByTags}
     */
    @Deprecated
    List<RoomResponse> searchRooms(List<String> tags);
    
    /**
     * @deprecated Use {@link com.vibechat.service.room.query.RoomQueryService#getRoomById}
     */
    @Deprecated
    RoomResponse getRoomById(Long roomId);
    
    /**
     * @deprecated Use {@link com.vibechat.service.room.command.RoomCommandService#joinRoom}
     */
    @Deprecated
    void joinRoom(Long roomId, String inviteCode, Long userId, String nickname);
}


