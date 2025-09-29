package com.vibechat.service.room;

/**
 * 방 참가자 통계 정보 DTO
 *
 * Clean Architecture 원칙에 따라 인터페이스와 구현체에서 공통으로 사용 가능한 독립적인 클래스
 */
public class RoomParticipantStats {
    private final Long roomId;
    private final int totalParticipants;
    private final int onlineParticipants;

    public RoomParticipantStats(Long roomId, int totalParticipants, int onlineParticipants) {
        this.roomId = roomId;
        this.totalParticipants = totalParticipants;
        this.onlineParticipants = onlineParticipants;
    }

    public Long getRoomId() {
        return roomId;
    }

    public int getTotalParticipants() {
        return totalParticipants;
    }

    public int getOnlineParticipants() {
        return onlineParticipants;
    }

    public int getOfflineParticipants() {
        return totalParticipants - onlineParticipants;
    }

    @Override
    public String toString() {
        return String.format("RoomStats{roomId=%d, total=%d, online=%d, offline=%d}",
            roomId, totalParticipants, onlineParticipants, getOfflineParticipants());
    }
}