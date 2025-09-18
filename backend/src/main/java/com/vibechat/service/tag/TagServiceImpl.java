package com.vibechat.service.tag;

import com.vibechat.domain.tags.Tag;
import com.vibechat.repository.tags.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    @Override
    @Transactional
    public List<Tag> findOrCreateTags(List<String> tagNames) {
        return tagNames.stream()
                .map(this::findOrCreateTag)
                .collect(Collectors.toList());
    }

    private Tag findOrCreateTag(String tagName) {
        // 태그 이름 정규화 (소문자, 공백제거)
        String normalizedTagName = tagName.trim().toLowerCase();
        
        return tagRepository.findByName(normalizedTagName).orElseGet(() -> {
            Tag newTag = new Tag();
            newTag.setName(normalizedTagName);
            return tagRepository.save(newTag);
        });
    }
}
