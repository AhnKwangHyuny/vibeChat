package com.vibechat.service.tag;

import com.vibechat.domain.Tag;
import com.vibechat.dto.TagResponse;
import com.vibechat.repository.TagRepository;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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
        Optional<Tag> existingTag = tagRepository.findByName(tagName.toLowerCase());
        if (existingTag.isPresent()) {
            return existingTag.get();
        } else {
            Tag newTag = new Tag();
            newTag.setName(tagName.toLowerCase());
            return tagRepository.save(newTag);
        }
    }

    @Override
    @Cacheable(value = "tags", key = "#q")
    public List<TagResponse> autocomplete(String q) {
        if (q == null || q.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return tagRepository.findByNameStartingWithIgnoreCase(q.toLowerCase())
                .stream()
                .map(tag -> new TagResponse(tag.getName(), tag.getPopularity()))
                .collect(Collectors.toList());
    }




}


