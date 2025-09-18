package com.vibechat.service.tag.chatRoomTagService;

import com.vibechat.dto.TagResponse;

import java.util.List;

public interface ChatRoomTagService {

    /**
     * 특정 채팅방에 태그들을 할당합니다.
     * @param roomId 태그를 할당할 채팅방의 ID
     * @param tagNames 할당할 태그 이름 목록
     */
    void assignTagsToRoom(Long roomId, List<String> tagNames);

    /**
     * 자동완성을 위한 태그 목록을 조회합니다.
     * @param query 검색어
     * @return 태그 응답 DTO 목록
     */
    List<TagResponse> autocomplete(String query);

}