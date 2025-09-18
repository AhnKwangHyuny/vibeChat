package com.vibechat.service.tag.chatRoomTagService;

import com.vibechat.domain.ChatRoom;
import com.vibechat.domain.tags.RoomTag;
import com.vibechat.domain.tags.Tag;
import com.vibechat.dto.TagResponse;
import com.vibechat.repository.chatRoom.ChatRoomRepository;
import com.vibechat.repository.tags.RoomTagRepository;
import com.vibechat.service.tag.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ChatRoomTagServiceImpl implements ChatRoomTagService {

    private final ChatRoomRepository chatRoomRepository;
    private final RoomTagRepository roomTagRepository;
    private final TagService tagService;

    @Override
    public void assignTagsToRoom(Long roomId, List<String> tagNames) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다. ID: " + roomId));

        List<Tag> tags = tagService.findOrCreateTags(tagNames);

        // TODO: 기존 태그를 모두 지우고 새로 할당할지, 아니면 추가만 할지 정책 결정 필요. 우선은 추가만 구현.

        for (Tag tag : tags) {
            // 이미 관계가 존재하는지 확인하여 중복 생성을 방지
            roomTagRepository.findByChatRoomAndTag(chatRoom, tag).orElseGet(() -> {
                RoomTag newRoomTag = new RoomTag();
                newRoomTag.setChatRoom(chatRoom);
                newRoomTag.setTag(tag);
                return roomTagRepository.save(newRoomTag);
            });
        }
        log.info("{}번 채팅방에 {}개의 태그를 할당했습니다.", roomId, tags.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> autocomplete(String query) {
        // 자동완성 기능은 TagService로 책임이 위임될 수 있으나, 현재 구조에서는 여기서 처리.
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        // 이 부분은 TagService에 findTagsByPrefix 같은 메서드를 만들어 위임하는 것이 더 나은 설계일 수 있음.
        List<Tag> tags = tagService.findOrCreateTags(List.of(query)); // 임시 구현, 실제로는 findByNameStartingWith 사용 필요
        return tags.stream()
                .map(tag -> new TagResponse(tag.getName()))
                .collect(Collectors.toList());
    }
}
