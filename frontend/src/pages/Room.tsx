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

// Import components from our component library
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Spinner } from '../components/ui/Spinner';
import { Modal } from '../components/ui/Modal';
import { ChatMessage } from '../components/demo/ChatMessage';
import { MessageInput } from '../components/demo/MessageInput';
import { Navbar } from '../components/layout/Navbar';
import { cn } from '../utils/cn';

// New chat components
import MessageActions from '../components/demo/MessageActions';
import MessageReactions from '../components/demo/MessageReactions';
import UserList from '../components/demo/UserList';
import EmojiPicker from '../components/ui/EmojiPicker';
import LoadingDots from '../components/ui/LoadingDots';
import NotificationBadge from '../components/ui/NotificationBadge';
import ContextMenu from '../components/ui/ContextMenu';
import ThreadView from '../components/demo/ThreadView';

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

interface Room {
  id: number;
  title: string;
  description?: string;
  isPrivate: boolean;
  tags: string[];
  participantsCount: number;
  lastMessageAt?: string;
}

export default function Room() {
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();
  const parsedRoomId = roomId ? parseInt(roomId) : undefined;
  const { getRoomById, isLoading: isLoadingRoom } = useRooms();

  // State for room data
  const [room, setRoom] = useState<Room | null>(null);

  // Load room data - 임시로 Mock 데이터 사용
  useEffect(() => {
    // const loadRoom = async () => {
    //   if (parsedRoomId) {
    //     try {
    //       const roomData = await getRoomById(parsedRoomId);
    //       setRoom(roomData);
    //     } catch (error) {
    //       console.error('Failed to load room:', error);
    //       toast.error('방을 불러오는데 실패했습니다.');
    //     }
    //   }
    // };
    // loadRoom();

    // 임시: Mock 방 데이터로 설정
    setRoom({
      id: parsedRoomId || 1,
      title: '🚀 테스트 채팅방',
      description: '임시 테스트용 채팅방입니다',
      isPrivate: false,
      tags: ['테스트', '개발'],
      participantsCount: 2,
      lastMessageAt: new Date().toISOString()
    });
  }, [parsedRoomId]);

  // 실제 데이터 훅 사용: 메시지/타이핑/온라인 수
  const { messages, isLoading, sendMessage, typingUsers, onlineCount } = useMessages(parsedRoomId || 0);

  const [messageInput, setMessageInput] = useState('');
  const [showLeaveModal, setShowLeaveModal] = useState(false);

  // Redux에서 사용자 정보 가져오기 (CreateRoom 패턴과 동일) - 임시로 Mock 사용자
  const user = useSelector((state: RootState) => state.user);
  const isAuthenticated = true; // 임시로 항상 인증됨으로 설정
  const dispatch = useDispatch();

  // 임시 Mock 사용자 데이터
  const mockUser = {
    id: 1,
    nickname: '테스트유저',
    email: 'test@example.com'
  };

  // 인증 훅 사용 (Home/CreateRoom 패턴과 동일)
  const { signOut: sessionSignOut } = useSessionAuth();
  const { signOut: supabaseSignOut } = useSupabaseAuth();

  // New states for enhanced features
  const [showUserList, setShowUserList] = useState(false);
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  const [showContextMenu, setShowContextMenu] = useState(false);
  const [contextMenuPosition, setContextMenuPosition] = useState({ x: 0, y: 0 });
  const [selectedMessage, setSelectedMessage] = useState<WebSocketMessageResponse | null>(null);
  const [showThreadView, setShowThreadView] = useState(false);
  const [activeActionsId, setActiveActionsId] = useState<number | string | null>(null);
  const [threadParentMessage, setThreadParentMessage] = useState<WebSocketMessageResponse | null>(null);
  const [messageReactions, setMessageReactions] = useState<Record<number, Array<{emoji: string; count: number; users: string[]}>>>({});

  // Mock online users data
  const mockOnlineUsers = [
    { id: '1', nickname: 'John Doe', status: 'online' as const, avatarUrl: '' },
    { id: '2', nickname: 'Alice', status: 'online' as const, avatarUrl: '' },
    { id: '3', nickname: 'Bob', status: 'away' as const, avatarUrl: '' },
    { id: '4', nickname: 'Charlie', status: 'online' as const, avatarUrl: '' },
    { id: '5', nickname: 'Diana', status: 'offline' as const, avatarUrl: '', lastSeen: '5분 전' },
  ];

  // 데모 리액션 데이터(실제 서버 통합 시 제거 가능)
  useEffect(() => {
    setMessageReactions({});
  }, [parsedRoomId]);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  // room state is already set above

  // Scroll to bottom on new message
  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [messages]);

  const doSendMessage = useCallback((payload: {
    type: 'TEXT' | 'IMAGE' | 'GIF' | 'VIDEO';
    contentText?: string;
    mediaUrl?: string;
    mediaThumbUrl?: string;
    mediaDurationSec?: number;
  }) => {
    if (!messageInput.trim() && payload.type === 'TEXT') return;
    // useMessages 훅의 전송 호출
    sendMessage({
      type: payload.type,
      contentText: payload.contentText || messageInput,
      mediaUrl: payload.mediaUrl,
      mediaThumbUrl: payload.mediaThumbUrl,
      mediaDurationSec: payload.mediaDurationSec,
    });
    setMessageInput('');
  }, [messageInput, sendMessage]);

  const handleSendMessage = () => {
    if (!messageInput.trim()) return;
    doSendMessage({ type: 'TEXT', contentText: messageInput });
  };

  const handleFileUpload = (file: File) => {
    // Simulate file upload
    const mockUrl = URL.createObjectURL(file);
    const mockThumbUrl = file.type.startsWith('image/') ? mockUrl : undefined;
    
    doSendMessage({
      type: file.type.startsWith('image/') ? 'IMAGE' : 'VIDEO',
      mediaUrl: mockUrl,
      mediaThumbUrl: mockThumbUrl,
      mediaDurationSec: file.type.startsWith('video/') ? 30 : undefined
    });
  };

  // 입력창에서 onTypingChange를 통해 처리하므로 별도 핸들러 불필요

  const handleLeaveRoom = () => {
    toast.success("Left the room successfully!");
    navigate('/');
  };

  const handleLogin = () => {
    navigate('/');
  };

  const handleLogout = async () => {
    console.log("로그아웃을 시작합니다...");
    try {
      // 1. (소셜 로그인 사용자만) Supabase 클라이언트 세션 종료
      if (user.provider === 'GOOGLE') {
        await supabaseSignOut();
        console.log("Supabase 세션이 종료되었습니다.");
      }

      // 2. (모든 사용자 공통) VibeChat 백엔드 세션 종료
      await sessionSignOut();
      console.log("VibeChat 백엔드 세션이 종료되었습니다.");

      // 3. 성공 알림 (Redux의 clearUser는 각 signOut 훅에서 이미 처리하고 있음)
      toast.success("성공적으로 로그아웃되었습니다.");

    } catch (error) {
      console.error("로그아웃 중 오류 발생:", error);
      toast.error("로그아웃 중 문제가 발생했습니다. 페이지를 새로고침합니다.");

      // 최악의 경우에도 UI를 초기화하고 새로고침하여 상태를 완전히 정리
      dispatch(clearUser());
      window.location.reload();
    }
  };

  if (isLoadingRoom) {
    return (
      <div className="min-h-screen bg-background-primary flex items-center justify-center">
        <Spinner size="xl" />
      </div>
    );
  }

  if (!room) {
    return (
      <div className="min-h-screen bg-background-primary flex items-center justify-center">
        <Card className="p-8 text-center">
          <h2 className="text-2xl font-bold text-foreground-primary mb-4">방을 찾을 수 없습니다</h2>
          <p className="text-foreground-muted mb-6">찾고 계신 방이 존재하지 않거나 삭제되었습니다.</p>
          <Button onClick={() => navigate('/')}>
            홈으로 가기
          </Button>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background-primary flex flex-col">
      <Navbar
        user={isAuthenticated ? mockUser : undefined}
        onLogin={handleLogin}
        onLogout={handleLogout}
      />

      {/* Room Header */}
      <div className="bg-background-secondary border-b border-border-default">
        <div className="container mx-auto px-4 py-4">
          <div className="flex items-center justify-between">
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
            
            <div className="flex items-center gap-4">
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
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                  </svg>
                </Button>
              </NotificationBadge>
              
              <div className="flex gap-2">
                {room.tags.map((tag) => (
                  <Badge key={tag} variant="default" size="sm">
                    #{tag}
                  </Badge>
                ))}
              </div>

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

      {/* Main Chat Area with Sidebar */}
      <div className="flex-1 flex min-h-0">
      {/* Messages Area */}
      <div className="flex-1 flex flex-col min-h-0">
        <div className="flex-1 overflow-y-auto px-1 sm:px-4 py-4 space-y-3">
          {isLoading ? (
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
            messages.map((message) => (
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
                user={{ id: String(message.user.id), nickname: message.user.nickname, avatarUrl: message.user.avatarUrl }}
                content={message.contentText || ''}
                type={message.type}
                mediaUrl={message.mediaUrl}
                mediaThumbUrl={message.mediaThumbUrl}
                durationSec={message.mediaDurationSec}
                createdAt={message.createdAt}
                isOwn={message.user.id === parseInt(user.id || '0')}
                isPending={!!message.clientTempId}
              />
                  
                  {/* Message Actions (click-to-toggle) */}
                  <div className={cn(
                    'absolute top-0 right-0 transition-opacity',
                    activeActionsId === (message.id || message.clientTempId) ? 'opacity-100' : 'opacity-0 pointer-events-none'
                  )}>
                    <MessageActions
                      messageId={message.id.toString()}
                      active={activeActionsId === (message.id || message.clientTempId)}
                      onReply={() => {
                        // 데모: 답글 UI만 표시 (실서버 연동 시 구현)
                        setThreadParentMessage(message);
                        setShowThreadView(true);
                      }}
                      onEdit={() => toast.info('메시지 수정 기능은 곧 제공될 예정입니다!')}
                      onDelete={() => {
                        toast.success('메시지 삭제는 데모에서 비활성화되어 있습니다.');
                      }}
                      onReport={() => toast.info('신고가 접수되었습니다.')}
                      onReaction={() => {
                        // Add a random reaction
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

                  {/* Message Reactions */}
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
            ))
          )}
        <div ref={messagesEndRef} />
      </div>

        {/* Typing Indicator */}
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

        {/* Message Input */}
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
            
            {/* Emoji Picker */}
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

        {/* User List Sidebar */}
        {showUserList && (
          <div className="w-80 border-l border-border-default bg-background-secondary">
            <UserList
              users={mockOnlineUsers}
              onUserClick={(userId) => {
                toast.info(`${mockOnlineUsers.find(u => u.id === userId)?.nickname}님과의 DM 기능은 곧 제공될 예정입니다!`);
              }}
              title="채팅방 참여자"
              className="h-full border-none rounded-none"
            />
          </div>
        )}
      </div>

      {/* Leave Room Modal */}
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

      {/* Context Menu */}
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

      {/* Thread View */}
      {showThreadView && threadParentMessage && (
        <ThreadView
          parentMessage={threadParentMessage}
          replies={[]}
          onReply={() => {
            // 실서버 연동 시 부모 메시지 ID 기반 전송/조회 구현
            toast.success('답글이 전송되었습니다!');
          }}
          onClose={() => setShowThreadView(false)}
          currentUserId={user.id || ''}
        />
      )}
    </div>
  );
}