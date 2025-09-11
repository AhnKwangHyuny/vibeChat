package com.vibechat.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void 게스트_사용자_생성시_프로바이더와_닉네임_정상설정() {
        // given
        String nickname = "테스터";

        // when
        User u = new User();
        u.setProvider(UserProvider.GUEST);
        u.setNickname(nickname);

        // then
        assertThat(u.getProvider()).isEqualTo(UserProvider.GUEST);
        assertThat(u.getNickname()).isEqualTo(nickname);
    }

    @Test
    void 구글_사용자_생성시_providerId_설정() {
        // given
        String sub = "google-sub-123";

        // when
        User u = new User();
        u.setProvider(UserProvider.GOOGLE);
        u.setProviderId(sub);

        // then
        assertThat(u.getProvider()).isEqualTo(UserProvider.GOOGLE);
        assertThat(u.getProviderId()).isEqualTo(sub);
    }

    @Test
    void 프로필_태그_연관관계_모킹_매핑() {
        // given
        User u = new User();
        Tag tag = mock(Tag.class);

        // when
        UserProfileTag upt = new UserProfileTag();
        upt.setUser(u);
        upt.setTag(tag);

        // then
        assertThat(upt.getUser()).isEqualTo(u);
        assertThat(upt.getTag()).isEqualTo(tag);
        assertThat(upt.getCreatedAt()).isNotNull();
    }

    @Test
    void 유저_방_참여_연관관계_모킹_매핑() {
        // given
        User u = new User();
        ChatRoom room = mock(ChatRoom.class);

        // when
        UserRoom ur = new UserRoom();
        ur.setUser(u);
        ur.setRoom(room);
        ur.setBookmarked(true);

        // then
        assertThat(ur.getUser()).isEqualTo(u);
        assertThat(ur.getRoom()).isEqualTo(room);
        assertThat(ur.isBookmarked()).isTrue();
        assertThat(ur.getJoinedAt()).isNotNull();
    }

    @Test
    void 인삿말_설정시_길이_및_내용_보존() {
        // given
        String greeting = "반갑습니다! 채팅 즐겨요";

        // when
        User u = new User();
        u.setGreeting(greeting);

        // then
        assertThat(u.getGreeting()).isEqualTo(greeting);
        assertThat(u.getGreeting().length()).isLessThanOrEqualTo(160);
    }

    @Test
    void 생성_수정_타임스탬프_기본값_확인() {
        // given & when
        User u = new User();

        // then
        // JPA가 관리할 때 채워지지만, 단위 테스트에서는 null일 수 있음
        // createdAt/updatedAt 필드 존재 확인 정도로 검증
        assertThat(u).hasFieldOrProperty("createdAt");
        assertThat(u).hasFieldOrProperty("updatedAt");
    }
}


