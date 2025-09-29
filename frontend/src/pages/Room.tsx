import { useEffect, useRef, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { useSelector, useDispatch } from 'react-redux';
import { RootState } from '../store';
import { clearUser } from '../store/userSlice';
import { useRooms } from '../hooks/useRooms';
import { useMessages } from '../hooks/useMessages';
import { useSessionAuth } from '../hooks/useSessionAuth';
import { useSupabaseAuth } from '../hooks/useSupabaseAuth';
import { stompClient } from '../services/ws/stompClient';

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

/**
 * WebSocket 메시지 응답 타입
 */
interface WebSocketMessageResponse {
  id: number;
  clientTempId?: string;
  roomId: number;
  user: { id: number; nickname: string; avatarUrl?: string };
  type: 'TEXT' | 'IMAGE' | 'GIF' | 'VIDEO';
  contentText?: string;
  mediaUrl?: string;
  mediaThumbUrl?: string;
  mediaDurationSec?: number;
  createdAt: string;
}

/**
 * 방 정보 타입
 */
interface Room {
  id: number;
  title: string;
  description?: string;
  isPrivate: boolean;
  tags: string[];
  participantsCount: number;
  lastMessageAt?: string;
}

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
  const navigate = useNavigate();
  const parsedRoomId = roomId ? parseInt(roomId) : undefined;

  // === 커스텀 훅 ===
  const { getRoomById, isLoading: isLoadingRoom } = useRooms();
  const { messages, isLoading, sendMessage, typingUsers, onlineCount } = useMessages(parsedRoomId || 0);
  const { signOut: sessionSignOut } = useSessionAuth();
  const { signOut: supabaseSignOut } = useSupabaseAuth();

  // === Redux 상태 ===
  const user = useSelector((state: RootState) => state.user);
  const dispatch = useDispatch();

  // 인증 상태 계산 (엄격한 검증)
  const isAuthenticated = !!(
    user.id &&
    user.nickname &&
    user.id !== '' &&
    user.nickname !== '' &&
    user.id !== null &&
    user.nickname !== null
  );

  // === 로컬 상태 ===
  const [room, setRoom] = useState<Room | null>(null);
  const [messageInput, setMessageInput] = useState('');
  const [showLeaveModal, setShowLeaveModal] = useState(false);
  const [isWebSocketConnected, setIsWebSocketConnected] = useState(false);

  // UI 상태
  const [showUserList, setShowUserList] = useState(false);
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  const [showContextMenu, setShowContextMenu] = useState(false);
  const [contextMenuPosition, setContextMenuPosition] = useState({ x: 0, y: 0 });
  const [selectedMessage, setSelectedMessage] = useState<WebSocketMessageResponse | null>(null);
  const [showThreadView, setShowThreadView] = useState(false);
  const [activeActionsId, setActiveActionsId] = useState<number | string | null>(null);
  const [threadParentMessage, setThreadParentMessage] = useState<WebSocketMessageResponse | null>(null);
  const [messageReactions, setMessageReactions] = useState<Record<number, Array<{emoji: string; count: number; users: string[]}>>>({});

  // 온라인 사용자 목록 (향후 실제 API 연동)
  const [onlineUsers, setOnlineUsers] = useState<Array<{
    id: string;
    nickname: string;
    status: 'online' | 'away' | 'offline';
    avatarUrl?: string;
    lastSeen?: string;
  }>>([]);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  // 전역 에러 핸들러
  const handleFatalError = useCallback((error: Error, context: string) => {
    console.error(`[ROOM] Fatal error in ${context}:`, error);
    toast.error(`오류가 발생했습니다: ${error.message}`);

    // 3초 후 자동으로 홈으로 리다이렉트
    setTimeout(() => {
      navigate('/', { replace: true });
    }, 3000);
  }, [navigate]);

  // === 디버깅용 로그 ===
  useEffect(() => {
    console.log('[ROOM] User state:', {
      id: user.id,
      nickname: user.nickname,
      isAuthenticated
    });
  }, [user.id, user.nickname, isAuthenticated]);

  // === 라이프사이클: 방 정보 로딩 ===
  /**
   * 인증 완료 후 방 정보 로딩
   */
  useEffect(() => {
    const loadRoom = async () => {
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
            navigate('/');
          }
        } catch (error: any) {
          console.error('[ROOM] Failed to load room', error);

          if (error?.status === 404 || error?.status === 400) {
            toast.error('존재하지 않는 방입니다.');
            navigate('/');
          } else {
            toast.error('방을 불러오는데 실패했습니다.');
          }
        }
      }
    };

    loadRoom();
  }, [parsedRoomId, isAuthenticated, getRoomById, navigate]);

  // === 통합 방 입장 워크플로우 ===
  useEffect(() => {
    if (!isAuthenticated || !parsedRoomId) {
      return; // 인증되지 않거나 방 ID가 없으면 연결하지 않음
    }

    const enterRoomWorkflow = async () => {
      try {
        console.log('[ROOM] Starting unified room entry workflow', {
          roomId: parsedRoomId,
          userId: user.id,
          nickname: user.nickname
        });

        // 통합 방 입장 워크플로우 실행
        await stompClient.enterRoom(parsedRoomId, user.id, user.nickname, user.avatarUrl || undefined);

        setIsWebSocketConnected(true);
        toast.success('실시간 채팅에 연결되었습니다.');

        console.log('[ROOM] Room entry workflow completed successfully');
      } catch (error) {
        console.error('[ROOM] Room entry workflow failed', error);
        toast.error('방 입장에 실패했습니다.');
        setIsWebSocketConnected(false);
      }
    };

    enterRoomWorkflow();

    return () => {
      // 컴포넌트 언마운트 시 방 퇴장 워크플로우 실행
      if (parsedRoomId) {
        console.log('[ROOM] Starting room exit workflow', { roomId: parsedRoomId });
        // 타임아웃 처리와 함께 방 퇴장
        Promise.race([
          stompClient.exitRoom(parsedRoomId),
          new Promise((_, reject) => setTimeout(() => reject(new Error('Exit timeout')), 3000))
        ]).catch((error) => {
          console.error('[ROOM] Room exit workflow failed', error);
          // 타임아웃이나 오류 발생 시에도 상태 정리
          setIsWebSocketConnected(false);
        });
      }
    };
  }, [isAuthenticated, user.id, user.nickname, user.avatarUrl, parsedRoomId]);

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
   * 방 변경 시 리액션 데이터 초기화
   */
  useEffect(() => {
    setMessageReactions({});
  }, [parsedRoomId]);

  // === 이벤트 핸들러: 메시지 전송 ===
  /**
   * 메시지 전송 공통 로직
   */
  const doSendMessage = useCallback((payload: {
    type: 'TEXT' | 'IMAGE' | 'GIF' | 'VIDEO';
    contentText?: string;
    mediaUrl?: string;
    mediaThumbUrl?: string;
    mediaDurationSec?: number;
  }) => {
    if (!messageInput.trim() && payload.type === 'TEXT') return;

    console.log('[ROOM] Sending message', payload);
    sendMessage({
      type: payload.type,
      contentText: payload.contentText || messageInput,
      mediaUrl: payload.mediaUrl,
      mediaThumbUrl: payload.mediaThumbUrl,
      mediaDurationSec: payload.mediaDurationSec,
    });
    setMessageInput('');
  }, [messageInput, sendMessage]);

  /**
   * 텍스트 메시지 전송 핸들러
   */
  const handleSendMessage = () => {
    if (!messageInput.trim()) return;
    doSendMessage({ type: 'TEXT', contentText: messageInput });
  };

  /**
   * 파일 업로드 핸들러 (임시 구현)
   */
  const handleFileUpload = (file: File) => {
    console.log('[ROOM] File upload requested', { fileName: file.name, fileType: file.type });

    // 임시 URL 생성 (실제 구현 시 서버 업로드 필요)
    const mockUrl = URL.createObjectURL(file);
    const mockThumbUrl = file.type.startsWith('image/') ? mockUrl : undefined;

    doSendMessage({
      type: file.type.startsWith('image/') ? 'IMAGE' : 'VIDEO',
      mediaUrl: mockUrl,
      mediaThumbUrl: mockThumbUrl,
      mediaDurationSec: file.type.startsWith('video/') ? 30 : undefined
    });
  };

  // === 이벤트 핸들러: 방 관리 ===
  /**
   * 방 나가기 핸들러 (통합 워크플로우 사용)
   */
  const handleLeaveRoom = async () => {
    console.log('[ROOM] User leaving room');

    try {
      // 통합 방 퇴장 워크플로우 실행
      if (parsedRoomId) {
        console.log('[ROOM] Executing room exit workflow');
        await stompClient.exitRoom(parsedRoomId);
      }

      toast.success("방에서 나왔습니다.");
      navigate('/');
    } catch (error) {
      console.error('[ROOM] Error during room leave:', error);
      toast.error("방 나가기 중 오류가 발생했습니다.");
      // 에러가 발생해도 페이지는 이동
      navigate('/');
    }
  };

  /**
   * 로그인 페이지로 이동
   */
  const handleLogin = () => {
    navigate('/');
  };

  /**
   * 로그아웃 핸들러
   * 소셜 로그인과 세션 로그인을 모두 처리
   */
  const handleLogout = async () => {
    console.log('[ROOM] Initiating logout process');

    try {
      // 1. 소셜 로그인 사용자의 경우 Supabase 세션 종료
      if (user.provider === 'GOOGLE') {
        await supabaseSignOut();
        console.log('[ROOM] Supabase session terminated');
      }

      // 2. 백엔드 세션 종료 (모든 사용자 공통)
      await sessionSignOut();
      console.log('[ROOM] Backend session terminated');

      toast.success("성공적으로 로그아웃되었습니다.");
    } catch (error) {
      console.error('[ROOM] Logout failed', error);
      toast.error("로그아웃 중 문제가 발생했습니다. 페이지를 새로고침합니다.");

      // 강제 정리 및 새로고침
      dispatch(clearUser());
      window.location.reload();
    }
  };

  // === 조건부 렌더링 ===
  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-background-primary flex items-center justify-center">
        <Card className="p-8 text-center">
          <h2 className="text-2xl font-bold text-foreground-primary mb-4">로그인이 필요합니다</h2>
          <p className="text-foreground-muted mb-6">채팅방에 참여하려면 먼저 로그인해주세요.</p>
          <Button onClick={handleLogin}>
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
        user={isAuthenticated ? { id: user.id, nickname: user.nickname, avatarUrl: user.avatarUrl } : undefined}
        onLogin={handleLogin}
        onLogout={handleLogout}
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
                onClick={() => navigate('/')}
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
                <div className={`w-2 h-2 rounded-full ${isWebSocketConnected ? 'bg-green-500' : 'bg-red-500'}`}></div>
                <span className="text-xs text-foreground-muted">
                  {isWebSocketConnected ? '실시간' : '연결 중...'}
                </span>
              </div>

              {/* 온라인 사용자 수 */}
              <NotificationBadge count={onlineCount} variant="success" className="mr-2">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => setShowUserList(!showUserList)}
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
                onClick={() => setShowLeaveModal(true)}
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
                    setSelectedMessage(message);
                    setContextMenuPosition({ x: e.clientX, y: e.clientY });
                    setShowContextMenu(true);
                  }}
                  onClick={() => setActiveActionsId(prev => (prev === (message.id || message.clientTempId) ? null : (message.id || message.clientTempId)!))}
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
                    isPending={!!message.clientTempId}
                  />

                  {/* 메시지 액션 버튼 */}
                  <div className={cn(
                    'absolute top-0 right-0 transition-opacity',
                    activeActionsId === (message.id || message.clientTempId) ? 'opacity-100' : 'opacity-0 pointer-events-none'
                  )}>
                    <MessageActions
                      messageId={message.id.toString()}
                      active={activeActionsId === (message.id || message.clientTempId)}
                      onReply={() => {
                        setThreadParentMessage(message);
                        setShowThreadView(true);
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

                        setMessageReactions(prev => {
                          const existing = prev[message.id] || [];
                          const existingReaction = existing.find(r => r.emoji === randomEmoji);

                          if (existingReaction) {
                            return {
                              ...prev,
                              [message.id]: existing.map(r =>
                                r.emoji === randomEmoji
                                  ? { ...r, count: r.count + 1, users: [...r.users, user.nickname] }
                                  : r
                              )
                            };
                          } else {
                            return {
                              ...prev,
                              [message.id]: [...existing, { emoji: randomEmoji, count: 1, users: [user.nickname] }]
                            };
                          }
                        });
                      }}
                      isOwn={message.user.id === parseInt(user.id || '0')}
                    />
                  </div>

                  {/* 메시지 리액션 */}
                  {messageReactions[message.id] && (
                    <MessageReactions
                      reactions={messageReactions[message.id]}
                      onReactionAdd={(emoji) => {
                        setMessageReactions(prev => {
                          const existing = prev[message.id] || [];
                          const existingReaction = existing.find(r => r.emoji === emoji);

                          if (existingReaction) {
                            return {
                              ...prev,
                              [message.id]: existing.map(r =>
                                r.emoji === emoji
                                  ? { ...r, count: r.count + 1, users: [...r.users, user.nickname] }
                                  : r
                              )
                            };
                          } else {
                            return {
                              ...prev,
                              [message.id]: [...existing, { emoji, count: 1, users: [user.nickname] }]
                            };
                          }
                        });
                      }}
                      onReactionRemove={(emoji) => {
                        setMessageReactions(prev => ({
                          ...prev,
                          [message.id]: (prev[message.id] || []).filter(r => r.emoji !== emoji)
                        }));
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
              onFileUpload={handleFileUpload}
              onEmojiClick={() => setShowEmojiPicker(!showEmojiPicker)}
              placeholder="메시지를 입력하세요..."
              disabled={!isAuthenticated}
              onTypingChange={(typing) => {
                if (parsedRoomId) stompClient.sendTyping(parsedRoomId, !!typing);
              }}
            />

            {/* 이모지 피커 */}
            <div className="absolute bottom-full right-4">
              <EmojiPicker
                isOpen={showEmojiPicker}
                onClose={() => setShowEmojiPicker(false)}
                onEmojiSelect={(emoji) => {
                  setMessageInput(prev => prev + emoji);
                  setShowEmojiPicker(false);
                }}
              />
            </div>
          </div>
        </div>

        {/* 사용자 목록 사이드바 */}
        {showUserList && (
          <div className="w-80 border-l border-border-default bg-background-secondary">
            <UserList
              users={onlineUsers}
              onUserClick={(userId) => {
                toast.info(`${onlineUsers.find(u => u.id === userId)?.nickname}님과의 DM 기능은 곧 제공될 예정입니다!`);
              }}
              title="채팅방 참여자"
              className="h-full border-none rounded-none"
            />
          </div>
        )}
      </div>

      {/* 방 나가기 모달 */}
      <Modal isOpen={showLeaveModal} onClose={() => setShowLeaveModal(false)} title="Leave Room">
        <p className="text-foreground-muted">
          Are you sure you want to leave "{room.title}"? You'll need to rejoin to continue the conversation.
        </p>
        <div className="mt-6 flex justify-end gap-2">
          <Button variant="secondary" onClick={() => setShowLeaveModal(false)}>
            Cancel
          </Button>
          <Button variant="primary" onClick={handleLeaveRoom}>
            Leave Room
          </Button>
        </div>
      </Modal>

      {/* 컨텍스트 메뉴 */}
      {showContextMenu && selectedMessage && (
        <ContextMenu
          items={[
            {
              id: 'reply',
              label: '답글',
              icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6" /></svg>,
              onClick: () => {
                setThreadParentMessage(selectedMessage);
                setShowThreadView(true);
              }
            },
            {
              id: 'react',
              label: '반응하기',
              icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.828 14.828a4 4 0 01-5.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>,
              onClick: () => {
                const emojis = ['👍', '❤️', '😂', '😮', '😢', '😡'];
                const randomEmoji = emojis[Math.floor(Math.random() * emojis.length)];

                setMessageReactions(prev => {
                  const existing = prev[selectedMessage.id] || [];
                  const existingReaction = existing.find(r => r.emoji === randomEmoji);

                  if (existingReaction) {
                    return {
                      ...prev,
                      [selectedMessage.id]: existing.map(r =>
                        r.emoji === randomEmoji
                          ? { ...r, count: r.count + 1, users: [...r.users, user.nickname] }
                          : r
                      )
                    };
                  } else {
                    return {
                      ...prev,
                      [selectedMessage.id]: [...existing, { emoji: randomEmoji, count: 1, users: [user.nickname] }]
                    };
                  }
                });
              }
            },
            {
              id: 'copy',
              label: '복사',
              icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" /></svg>,
              onClick: () => {
                navigator.clipboard.writeText(selectedMessage.contentText || '');
                toast.success('메시지가 클립보드에 복사되었습니다.');
              }
            },
            ...(selectedMessage.user.id === parseInt(user.id || '0') ? [
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
          position={contextMenuPosition}
          onClose={() => setShowContextMenu(false)}
        />
      )}

      {/* 스레드 뷰 */}
      {showThreadView && threadParentMessage && (
        <ThreadView
          parentMessage={threadParentMessage}
          replies={[]}
          onReply={() => {
            toast.success('답글이 전송되었습니다!');
          }}
          onClose={() => setShowThreadView(false)}
          currentUserId={user.id || ''}
        />
      )}
    </div>
  );
}