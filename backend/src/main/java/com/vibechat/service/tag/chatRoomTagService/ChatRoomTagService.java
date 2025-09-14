package com.vibechat.service.tag.chatRoomTagService;

import com.vibechat.domain.RoomTag;
import com.vibechat.domain.RoomTag;
import com.vibechat.dto.TagResponse;

import java.util.List;

public interface ChatRoomTagService {

    List<RoomTag> findTags(List<String> tagNames);
    
    List<RoomTag> createTags(List<String> tagNames , Long chatRoomId);

    List<TagResponse> autocomplete(String q);

}
