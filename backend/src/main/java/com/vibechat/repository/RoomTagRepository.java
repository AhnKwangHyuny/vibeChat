package com.vibechat.repository;

import com.vibechat.domain.RoomTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomTagRepository extends JpaRepository<RoomTag, Long> {
    Optional<RoomTag> findByName(String name);
    List<RoomTag> findByNameStartingWithIgnoreCase(String name);
}
