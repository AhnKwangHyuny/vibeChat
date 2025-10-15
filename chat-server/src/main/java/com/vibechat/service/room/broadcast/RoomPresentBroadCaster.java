package com.vibechat.service.room.broadcast;

import com.vibechat.event.RoomPresenceUpdateEvent;

public interface RoomPresentBroadCaster {

    /**
     * Presence 업데이트 이벤트 수신 핸들러
     */
    void handlePresenceUpdateEvent(RoomPresenceUpdateEvent event);
}
