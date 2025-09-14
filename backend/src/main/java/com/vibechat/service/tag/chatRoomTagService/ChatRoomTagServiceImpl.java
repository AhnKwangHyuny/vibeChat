package com.vibechat.service.tag.chatRoomTagService;

import com.vibechat.domain.ChatRoom;
import com.vibechat.domain.RoomTag;
import com.vibechat.dto.TagResponse;
import com.vibechat.exception.tag.InvalidTagNameException;
import com.vibechat.exception.tag.TagCreationException;
import com.vibechat.repository.ChatRoomRepository;
import com.vibechat.repository.RoomTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChatRoomTagServiceImpl implements ChatRoomTagService {

    private final RoomTagRepository tagRepository;
    private final ChatRoomRepository chatRoomRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RoomTag> findTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            log.warn("태그 이름 목록이 비어있습니다");
            return Collections.emptyList();
        }

        try {
            return tagNames.stream()
                    .filter(tagName -> tagName != null && !tagName.trim().isEmpty())
                    .map(tagName -> {
                        String normalizedTagName = tagName.trim().toLowerCase();
                        if (normalizedTagName.length() > 20) {
                            throw new InvalidTagNameException("태그 이름이 너무 깁니다. 최대 20자까지 입력 가능합니다: " + tagName);
                        }
                        return tagRepository.findByName(normalizedTagName);
                    })
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("태그 검색 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("태그 검색 중 오류가 발생했습니다", e);
        }
    }

    @Override
    @Transactional
    public List<RoomTag> createTags(List<String> tagNames , Long chatRoomId) {
        if (tagNames == null || tagNames.isEmpty()) {
            log.warn("생성할 태그 이름 목록이 비어있습니다.");
            return Collections.emptyList();
        }

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 아이디가 존재하지 않아 찾아오는데 실패 했습니다. " + chatRoomId));

        try {
            return tagNames.stream()
                    .filter(tagName -> tagName != null && !tagName.trim().isEmpty())
                    .map(tagName -> {
                        String normalizedTagName = tagName.trim().toLowerCase();

                        // 새 태그 생성
                        RoomTag newTag = new RoomTag();
                        newTag.setName(normalizedTagName);
                        RoomTag savedTag = tagRepository.save(newTag);
                        log.info("새 태그가 생성되었습니다: {}", normalizedTagName);

                        return savedTag;
                    })
                    .collect(Collectors.toList());

        } catch (InvalidTagNameException e) {
            log.error("잘못된 태그 이름: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("태그 생성 중 오류 발생: {}", e.getMessage(), e);
            throw new TagCreationException("태그 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    @Cacheable(value = "tags", key = "#q")
    public List<TagResponse> autocomplete(String q) {
        if (q == null || q.trim().isEmpty()) {
            log.debug("자동완성 검색어가 비어있습니다");
            return Collections.emptyList();
        }

        try {
            String normalizedQuery = q.trim().toLowerCase();
            if (normalizedQuery.length() > 32) {
                log.warn("자동완성 검색어가 너무 깁니다: {}", q);
                return Collections.emptyList();
            }

            List<TagResponse> results = tagRepository.findByNameStartingWithIgnoreCase(normalizedQuery)
                    .stream()
                    .map(tag -> new TagResponse(tag.getName()))
                    .collect(Collectors.toList());

            log.debug("자동완성 검색 결과: {} 개의 태그를 찾았습니다", results.size());
            return results;
        } catch (Exception e) {
            log.error("자동완성 검색 중 오류 발생: {}", e.getMessage(), e);
            return Collections.emptyList(); // 자동완성은 실패해도 빈 목록 반환
        }
    }
}