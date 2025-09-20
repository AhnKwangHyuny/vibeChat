package com.vibechat.repository.chatRoom;

import com.vibechat.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findByRoomTags_Tag_NameIn(List<String> tagNames);

    @Query("select c.id from ChatRoom c")
    List<Long> findAllIds();
}

