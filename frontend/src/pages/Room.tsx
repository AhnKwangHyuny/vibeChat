import { useEffect, useRef, useState, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import { useSelector } from 'react-redux';
import { RootState } from '../store';
import { useRooms } from '../hooks/useRooms';
import { useMessages } from '../hooks/useMessages';
import { stompClient } from '../services/ws/stompClient';

// 새로운 커스텀 훅들 import
import { useRoomUIState } from '../hooks/room/useRoomUIState';
import { useRoomConnection } from '../hooks/room/useRoomConnection';
import { useRoomNavigation } from '../hooks/room/useRoomNavigation';
import { useFileUpload } from '../hooks/room/useFileUpload';
import { useSessionAuth } from '../hooks/useSessionAuth';
import type { WebSocketMessageResponse, Room as RoomType } from '../types/room';

// UI 컴포넌트 임포트
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Spinner } from '../components/ui/Spinner';
import { Modal } from '../components/ui/Modal';
import { ChatMessage } from '../components/demo/ChatMessage';
import { MessageInput } from '../components/demo/MessageInput';
import { Navbar } from '../components/layout/Navbar';
import { cn } from '../utils/cn';

// 채팅 관련 컴포넌트
import MessageActions from '../components/demo/MessageActions';
import MessageReactions from '../components/demo/MessageReactions';
import UserList from '../components/demo/UserList';
import EmojiPicker from '../components/ui/EmojiPicker';
import LoadingDots from '../components/ui/LoadingDots';
import NotificationBadge from '../components/ui/NotificationBadge';
import ContextMenu from '../components/ui/ContextMenu';
import ThreadView from '../components/demo/ThreadView';

// 타입 정의는 ../types/room에서 import

/**
 * 채팅방 페이지 컴포넌트
 *
 * 주요 기능:
 * - 사용자 인증 상태 확인
 * - 방 정보 로딩 및 검증
 * - 실시간 메시징 (WebSocket)
 * - 파일 업로드
 * - 사용자 상호작용 (리액션, 답글 등)
 */
export default function Room() {
  // === 라우터 및 기본 상태 ===
  const { roomId } = useParams<{ roomId: string }>();
  const parsedRoomId = roomId ? parseInt(roomId) : undefined;

  // === Redux 상태 ===
  const user = useSelector((state: RootState) => state.user);

  // === 세션 인증 (단일 소스) ===
  const { isAuthenticated, isCheckingAuth } = useSessionAuth();

  // === 커스텀 훅들 (리팩토링됨) ===
  const { getRoomById, isLoading: isLoadingRoom } = useRooms();
  const { messages, isLoading, sendMessage, typingUsers } = useMessages(parsedRoomId && parsedRoomId > 0 ? parsedRoomId : 0);

  // UI 상태 관리 훅
  const uiState = useRoomUIState();

  // WebSocket 연결 관리 훅
  const roomConnection = useRoomConnection({
    roomId: parsedRoomId || 0,
    userId: user.id,
    nickname: user.nickname,
    avatarUrl: user.avatarUrl,
    isAuthenticated,
  });

  // 네비게이션 관리 훅
  const roomNavigation = useRoomNavigation({
    roomId: parsedRoomId || 0,
    userProvider: user.provider,
    onRoomExit: roomConnection.disconnect,
  });

  // 파일 업로드 훅
  const { handleFileUpload } = useFileUpload();

  // === 간소화된 로컬 상태 (UI 상태는 uiState 훅으로 이관) ===
  const [room, setRoom] = useState<RoomType | null>(null);
  const [onlineCount, setOnlineCount] = useState(0); // Presence에서 관리
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // === 디버깅용 로그 ===
  useEffect(() => {
    console.log('[ROOM] User state:', {
      id: user.id,
      nickname: user.nickname,
      isAuthenticated
    });
  }, [user.id, user.nickname, isAuthenticated]);

  // === Presence 구독 (온라인 참가자 목록 + 카운트 업데이트) ===
  useEffect(() => {
    if (!parsedRoomId) return;

    const presenceSubscription = stompClient.subscribe(
      `/topic/rooms/${parsedRoomId}/presence`,
      (message) => {
        try {
          const presenceData = JSON.parse(message.body);
          console.log('[Presence 수신]', presenceData);

          // 1. 온라인 카운트 업데이트
          if (typeof presenceData.count === 'number') {
            setOnlineCount(presenceData.count);
            console.log('[Presence] onlineCount 업데이트:', presenceData.count);
          }

          // 2. 참가자 목록 업데이트
          // TODO: participantIds를 기반으로 사용자 상세 정보 조회하여 onlineUsers 업데이트
          // 현재는 participantIds만 받음 (nickname, avatarUrl은 별도 API 필요)
          if (presenceData.participantIds && Array.isArray(presenceData.participantIds)) {
            const onlineUsersList = presenceData.participantIds.map((id: number) => ({
              id: String(id),
              nickname: `사용자${id}`, // TODO: 실제 닉네임으로 교체
              status: 'online' as const,
            }));
            uiState.setOnlineUsers(onlineUsersList);
            console.log('[Presence] onlineUsers 업데이트:', onlineUsersList.length, '명');
          }
        } catch (error) {
          console.error('[Presence 처리 실패]', error);
        }
      }
    );

    // 구독 완료 후 현재 Presence 상태 요청 (구독 타이밍 문제 해결)
    setTimeout(() => {
      try {
        stompClient.publish(`/app/rooms/${parsedRoomId}/presence/status`, JSON.stringify({
          requestedAt: new Date().toISOString()
        }));
        console.log('[Presence] 수동 상태 요청 전송:', parsedRoomId);
      } catch (e) {
        console.error('[Presence] 상태 요청 실패:', e);
      }
    }, 500); // 500ms 후 요청

    return () => {
      presenceSubscription?.unsubscribe();
    };
  }, [parsedRoomId, uiState.setOnlineUsers]);

  // === 라이프사이클: 방 정보 로딩 ===
  /**
   * 인증 완료 후 방 정보 로딩
   */
  useEffect(() => {
    const loadRoom = async () => {
      // 인증 체크 중이거나 인증되지 않은 경우 API 호출하지 않음
      if (isCheckingAuth || !isAuthenticated || !parsedRoomId) {
        console.log('[ROOM] Skipping room load:', { isCheckingAuth, isAuthenticated, parsedRoomId });
        return;
      }

      if (parsedRoomId && isAuthenticated) {
        try {
          console.log('[ROOM] Loading room data', { roomId: parsedRoomId });
          const roomData = await getRoomById(parsedRoomId);

          if (roomData) {
            setRoom(roomData);
            console.log('[ROOM] Room loaded successfully', roomData);
          } else {
            console.warn('[ROOM] Room not found');
            toast.error('존재하지 않는 방입니다.');
            roomNavigation.navigateToHome();
          }
        } catch (error: any) {
          console.error('[ROOM] Failed to load room', error);

          if (error?.status === 404 || error?.status === 400) {
            toast.error('존재하지 않는 방입니다.');
            roomNavigation.navigateToHome();
          } else {
            toast.error('방을 불러오는데 실패했습니다.');
          }
        }
      }
    };

    loadRoom();
  }, [parsedRoomId, isAuthenticated, isCheckingAuth, getRoomById]);

  // WebSocket 연결 관리는 useRoomConnection 훅에서 처리됨

  // === 라이프사이클: 스크롤 관리 ===
  /**
   * 새 메시지 도착 시 자동 스크롤
   */
  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [messages]);

  /**
   * 방 변경 시 UI 상태 초기화
   */
  useEffect(() => {
    uiState.resetUI();
  }, [parsedRoomId, uiState.resetUI]);

  // === 이벤트 핸들러: 메시지 전송 (간소화됨) ===
  /**
   * 텍스트 메시지 전송 핸들러
   */
  const handleSendMessage = useCallback((content: string) => {
    if (!content.trim()) return;

    sendMessage({
      type: 'TEXT',
      contentText: content.trim(),
    });
  }, [sendMessage]);

  /**
   * 파일 업로드 핸들러 (최적화됨)
   */
  const handleFileUploadOptimized = useCallback(async (file: File) => {
    const result = await handleFileUpload(file);

    if (result) {
      sendMessage({
        type: result.type,
        mediaUrl: result.mediaUrl,
        mediaThumbUrl: result.mediaThumbUrl,
        mediaDurationSec: result.mediaDurationSec,
      });
    }
  }, [handleFileUpload, sendMessage]);

  // 방 관리 로직은 roomNavigation 훅에서 처리됨

  // === 조건부 렌더링 ===
  // 세션 체크 중
  if (isCheckingAuth) {
    return (
      <div className="min-h-screen bg-background-primary flex items-center justify-center">
        <div className="text-center">
          <Spinner size="xl" />
          <p className="mt-4 text-foreground-muted">인증 확인 중...</p>
        </div>
      </div>
    );
  }

  // 인증되지 않은 사용자
  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-background-primary flex items-center justify-center">
        <Card className="p-8 text-center">
          <h2 className="text-2xl font-bold text-foreground-primary mb-4">로그인이 필요합니다</h2>
          <p className="text-foreground-muted mb-6">채팅방에 참여하려면 먼저 로그인해주세요.</p>
          <Button onClick={roomNavigation.handleLogin}>
            홈으로 가서 로그인하기
          </Button>
        </Card>
      </div>
    );
  }

  if (isLoadingRoom || !room) {
    return (
      <div className="min-h-screen bg-background-primary flex items-center justify-center">
        <div className="text-center">
          <Spinner size="xl" />
          <p className="mt-4 text-foreground-muted">방 정보를 불러오는 중...</p>
        </div>
      </div>
    );
  }

  // === 메인 렌더링 ===
  return (
    <div className="min-h-screen bg-background-primary flex flex-col">
      {/* 상단 네비게이션 */}
      <Navbar
        user={isAuthenticated ? {
          id: user.id || '',
          nickname: user.nickname || '',
          avatarUrl: user.avatarUrl || undefined
        } : undefined}
        onLogin={roomNavigation.handleLogin}
        onLogout={roomNavigation.handleLogout}
      />

      {/* 방 헤더 */}
      <div className="bg-background-secondary border-b border-border-default">
        <div className="container mx-auto px-4 py-4">
          <div className="flex items-center justify-between">
            {/* 좌측: 뒤로가기 버튼 및 방 정보 */}
            <div className="flex items-center gap-4">
              <Button
                variant="ghost"
                size="sm"
                onClick={roomNavigation.navigateToHome}
                className="flex items-center gap-2"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                </svg>
                뒤로
              </Button>
              <div>
                <h1 className="text-xl font-bold text-foreground-primary">{room.title}</h1>
                <p className="text-sm text-foreground-muted">{room.description}</p>
              </div>
            </div>

            {/* 우측: 상태 정보 및 액션 버튼 */}
            <div className="flex items-center gap-4">
              {/* WebSocket 연결 상태 */}
              <div className="flex items-center gap-2">
                <div className={`w-2 h-2 rounded-full ${roomConnection.isWebSocketConnected ? 'bg-green-500' : 'bg-red-500'}`}></div>
                <span className="text-xs text-foreground-muted">
                  {roomConnection.isWebSocketConnected ? '실시간' : '연결 중...'}
                </span>
              </div>

              {/* 온라인 사용자 수 */}
              <NotificationBadge count={onlineCount} variant="success" className="mr-2">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={uiState.toggleUserList}
                  className="flex items-center gap-2"
                >
                  <div className="w-2 h-2 bg-semantic-success rounded-full"></div>
                  <span className="text-sm text-foreground-muted">{onlineCount}명 온라인</span>
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                  </svg>
                </Button>
              </NotificationBadge>

              {/* 방 태그 */}
              <div className="flex gap-2">
                {room.tags.map((tag) => (
                  <Badge key={tag} variant="default" size="sm">
                    #{tag}
                  </Badge>
                ))}
              </div>

              {/* 방 나가기 버튼 */}
              <Button
                variant="secondary"
                size="sm"
                onClick={uiState.toggleLeaveModal}
              >
                Leave Room
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* 메인 채팅 영역 */}
      <div className="flex-1 flex min-h-0">
        {/* 메시지 영역 */}
        <div className="flex-1 flex flex-col min-h-0">
          {/* 메시지 목록 */}
          <div className="flex-1 overflow-y-auto px-1 sm:px-4 py-4 space-y-3">
            {isLoading ? (
              /* 로딩 스켈레톤 */
              <div className="space-y-4">
                {[...Array(5)].map((_, i) => (
                  <div key={i} className={`flex ${i % 2 === 0 ? 'justify-end' : 'justify-start'}`}>
                    <div className="animate-pulse">
                      <div className="h-12 bg-background-tertiary rounded-lg w-64"></div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              /* 실제 메시지 목록 */
              messages.filter(message => message && message.user).map((message) => {
                // 메시지 유효성 검사
                if (!message.user?.id) {
                  console.warn('[ROOM] Invalid message user data:', message);
                  return null;
                }

                return (
                <div
                  key={message.id || message.clientTempId}
                  className="relative"
                  onContextMenu={(e) => {
                    e.preventDefault();
                    uiState.showContextMenu(e.clientX, e.clientY, message);
                  }}
                  onClick={() => uiState.setActiveActions(
                    uiState.activeActionsId === (message.id || message.clientTempId)
                      ? null
                      : (message.id || message.clientTempId)!
                  )}
                >
                  <ChatMessage
                    id={(message.id || message.clientTempId || '').toString()}
                    user={{
                      id: String(message.user?.id || '0'),
                      nickname: message.user?.nickname || 'Unknown User',
                      avatarUrl: message.user?.avatarUrl || null
                    }}
                    content={message.contentText || ''}
                    type={message.type}
                    mediaUrl={message.mediaUrl}
                    mediaThumbUrl={message.mediaThumbUrl}
                    durationSec={message.mediaDurationSec}
                    createdAt={message.createdAt}
                    isOwn={message.user.id === parseInt(user.id || '0')}
                    isPending={message.user.id === parseInt(user.id || '0') && !!message.clientTempId}
                  />

                  {/* 메시지 액션 버튼 */}
                  <div className={cn(
                    'absolute top-0 right-0 transition-opacity',
                    uiState.activeActionsId === (message.id || message.clientTempId) ? 'opacity-100' : 'opacity-0 pointer-events-none'
                  )}>
                    <MessageActions
                      messageId={message.id.toString()}
                      active={uiState.activeActionsId === (message.id || message.clientTempId)}
                      onReply={() => {
                        uiState.showThreadView(message);
                      }}
                      onEdit={() => toast.info('메시지 수정 기능은 곧 제공될 예정입니다!')}
                      onDelete={() => {
                        toast.success('메시지 삭제는 데모에서 비활성화되어 있습니다.');
                      }}
                      onReport={() => toast.info('신고가 접수되었습니다.')}
                      onReaction={() => {
                        // 임시 랜덤 리액션 추가
                        const emojis = ['👍', '❤️', '😂', '😮', '😢', '😡'];
                        const randomEmoji = emojis[Math.floor(Math.random() * emojis.length)];

                        uiState.addMessageReaction(message.id, randomEmoji, user.nickname || 'Unknown');
                      }}
                      isOwn={message.user.id === parseInt(user.id || '0')}
                    />
                  </div>

                  {/* 메시지 리액션 */}
                  {uiState.messageReactions[message.id] && (
                    <MessageReactions
                      reactions={uiState.messageReactions[message.id]}
                      onReactionAdd={(emoji) => {
                        uiState.addMessageReaction(message.id, emoji, user.nickname || 'Unknown');
                      }}
                      onReactionRemove={(emoji) => {
                        uiState.removeMessageReaction(message.id, emoji);
                      }}
                      currentUserId={user.id || ''}
                    />
                  )}
                </div>
                );
              })
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* 타이핑 인디케이터 */}
          {typingUsers.length > 0 && (
            <div className="px-4 py-2">
              <div className="flex items-center gap-2 text-sm text-foreground-muted">
                <LoadingDots size="sm" color="muted" />
                <span>
                  {typingUsers.join(', ')} {typingUsers.length === 1 ? 'is' : 'are'} typing...
                </span>
              </div>
            </div>
          )}

          {/* 메시지 입력창 */}
          <div className="relative">
            <MessageInput
              onSendMessage={handleSendMessage}
              onFileUpload={handleFileUploadOptimized}
              onEmojiClick={uiState.toggleEmojiPicker}
              placeholder="메시지를 입력하세요..."
              disabled={!roomConnection.isWebSocketConnected}
              onTypingChange={(typing) => {
                if (parsedRoomId) stompClient.sendTyping(parsedRoomId, !!typing);
              }}
            />

            {/* 이모지 피커 */}
            <div className="absolute bottom-full right-4">
              <EmojiPicker
                isOpen={uiState.showEmojiPicker}
                onClose={uiState.toggleEmojiPicker}
                onEmojiSelect={(emoji) => {
                  // MessageInput 컴포넌트에서 직접 처리하도록 수정 필요
                  console.log('[ROOM] Emoji selected:', emoji);
                  uiState.toggleEmojiPicker();
                }}
              />
            </div>
          </div>
        </div>

        {/* 사용자 목록 사이드바 */}
        {uiState.showUserList && (
          <div className="w-80 border-l border-border-default bg-background-secondary">
            <UserList
              users={uiState.onlineUsers}
              onUserClick={(userId) => {
                toast.info(`${uiState.onlineUsers.find(u => u.id === userId)?.nickname}님과의 DM 기능은 곧 제공될 예정입니다!`);
              }}
              title="채팅방 참여자"
              className="h-full border-none rounded-none"
            />
          </div>
        )}
      </div>

      {/* 방 나가기 모달 */}
      <Modal isOpen={uiState.showLeaveModal} onClose={uiState.toggleLeaveModal} title="Leave Room">
        <p className="text-foreground-muted">
          Are you sure you want to leave "{room.title}"? You'll need to rejoin to continue the conversation.
        </p>
        <div className="mt-6 flex justify-end gap-2">
          <Button variant="secondary" onClick={uiState.toggleLeaveModal}>
            Cancel
          </Button>
          <Button variant="primary" onClick={roomNavigation.handleLeaveRoom}>
            Leave Room
          </Button>
        </div>
      </Modal>

      {/* 컨텍스트 메뉴 */}
      {uiState.showContextMenu && uiState.selectedMessage && (
        <ContextMenu
          items={[
            {
              id: 'reply',
              label: '답글',
              icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6" /></svg>,
              onClick: () => {
                uiState.showThreadView(uiState.selectedMessage!);
              }
            },
            {
              id: 'react',
              label: '반응하기',
              icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.828 14.828a4 4 0 01-5.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>,
              onClick: () => {
                // const emojis = ['👍', '❤️', '😂', '😮', '😢', '😡'];
                const randomEmoji = emojis[Math.floor(Math.random() * emojis.length)];

                if (uiState.selectedMessage) {
                  uiState.addMessageReaction(uiState.selectedMessage.id, randomEmoji, user.nickname || 'Unknown');
                }
              }
            },
            {
              id: 'copy',
              label: '복사',
              icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" /></svg>,
              onClick: () => {
                navigator.clipboard.writeText(uiState.selectedMessage?.contentText || '');
                toast.success('메시지가 클립보드에 복사되었습니다.');
              }
            },
            ...(uiState.selectedMessage?.user.id === parseInt(user.id || '0') ? [
              {
                id: 'edit',
                label: '수정',
                icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" /></svg>,
                onClick: () => toast.info('메시지 수정 기능은 곧 제공될 예정입니다!')
              },
              {
                id: 'delete',
                label: '삭제',
                icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>,
                onClick: () => {
                  toast.success('메시지 삭제는 데모에서 비활성화되어 있습니다.');
                },
                destructive: true
              }
            ] : [
              {
                id: 'report',
                label: '신고',
                icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.732-.833-2.464 0L4.35 16.5c-.77.833.192 2.5 1.732 2.5z" /></svg>,
                onClick: () => toast.info('신고가 접수되었습니다.'),
                destructive: true
              }
            ])
          ]}
          position={uiState.contextMenuPosition}
          onClose={uiState.hideContextMenu}
        />
      )}

      {/* 스레드 뷰 */}
      {uiState.showThreadView && uiState.threadParentMessage && (
        <ThreadView
          parentMessage={uiState.threadParentMessage}
          replies={[]}
          onReply={() => {
            toast.success('답글이 전송되었습니다!');
          }}
          onClose={uiState.hideThreadView}
          currentUserId={user.id || ''}
        />
      )}
    </div>
  );
}