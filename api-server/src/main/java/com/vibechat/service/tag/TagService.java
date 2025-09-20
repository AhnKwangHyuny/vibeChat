package com.vibechat.service.tag;

import com.vibechat.domain.tags.Tag;

import java.util.List;

public interface TagService {

    /**
     * 태그 이름 목록을 받아, 각 이름에 해당하는 Tag 엔티티 목록을 반환합니다.
     * DB에 존재하지 않는 태그는 새로 생성하여 저장합니다.
     * @param tagNames 태그 이름 목록
     * @return Tag 엔티티 목록
     */
    List<Tag> findOrCreateTags(List<String> tagNames);

}
