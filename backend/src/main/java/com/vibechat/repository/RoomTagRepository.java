package com.vibechat.repository;

import com.vibechat.domain.RoomTag;
import com.vibechat.domain.RoomTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomTagRepository extends JpaRepository<RoomTag, RoomTagId> {
}
