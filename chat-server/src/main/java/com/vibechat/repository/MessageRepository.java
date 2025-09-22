package com.vibechat.repository;

import com.vibechat.domain.ChatRoom;
import com.vibechat.domain.Message;
import io.lettuce.core.dynamic.annotation.Param;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {


    Optional<Message> findTopByChatRoomOrderByIdDesc(ChatRoom chatRoom);

    @Modifying
    @Query(value = "DELETE m FROM messages m WHERE m.chat_room_id = :roomId AND m.id NOT IN (SELECT id FROM (SELECT id FROM messages WHERE chat_room_id = :roomId ORDER BY created_at DESC LIMIT :limit) as tmp)", nativeQuery = true)
    void trimOldMessages(@Param("roomId") Long roomId, @Param("limit") int limit);
}
