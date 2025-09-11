package com.vibechat.service.tag;

import com.vibechat.domain.Tag;
import com.vibechat.dto.TagResponse;

import java.util.List;

public interface TagService {

    List<Tag> findOrCreateTags(List<String> tagNames);

    List<TagResponse> autocomplete(String q);

}
