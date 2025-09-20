package com.vibechat.repository.tags;

import com.vibechat.domain.ChatRoom;
import com.vibechat.domain.tags.RoomTag;
import com.vibechat.domain.tags.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomTagRepository extends JpaRepository<RoomTag, Long> {

    List<RoomTag> findByChatRoom(ChatRoom chatRoom);

    Optional<RoomTag> findByChatRoomAndTag(ChatRoom chatRoom, Tag tag);

    void deleteByChatRoomAndTag(ChatRoom chatRoom, Tag tag);
}