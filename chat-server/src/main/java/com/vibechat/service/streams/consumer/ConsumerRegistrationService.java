package com.vibechat.service.streams.consumer;

/**
 * Consumer Group 등록 관리 서비스 인터페이스
 *
 * 책임: Redis Streams Consumer Group 생명주기 관리
 * - Consumer Group 동적 등록/제거
 * - Consumer 상태 모니터링
 * - 스트림 리스너 관리
 */
public interface ConsumerRegistrationService {

    /**
     * 방 스트림을 모든 관련 Consumer Group에 추가
     *
     * @param streamKey 방 스트림 키 (stream:room:{roomId})
     */
    void addRoomStreamToConsumerGroups(String streamKey);

    /**
     * 사용자 스트림을 관련 Consumer Group에 추가
     *
     * @param streamKey 사용자 스트림 키 (stream:user:{userId})
     */
    void addUserStreamToConsumerGroups(String streamKey);

    /**
     * 방 스트림을 Consumer Group에서 제거
     *
     * @param streamKey 방 스트림 키
     */
    void removeRoomStreamFromConsumerGroups(String streamKey);

    /**
     * 모든 Consumer Group 상태 조회
     *
     * @return Consumer Group 상태 정보
     */
    ConsumerGroupStats getConsumerGroupStats();

}