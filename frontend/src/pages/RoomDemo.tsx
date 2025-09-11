import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';

// Import components from our component library
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
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

interface DemoMessage {
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
  parentMessageId?: number;
}

export default function RoomDemo() {
  const navigate = useNavigate();

  // Demo room data
  const room = {
    id: 999,
    title: 'VibeChat 데모 방',
    description: '모든 채팅 기능을 체험해볼 수 있는 데모 방입니다.',
    isPrivate: false,
    tags: ['demo', 'test', 'vibechat']
  };

  // Demo messages
  const [messages, setMessages] = useState<DemoMessage[]>([
    {
      id: 1,
      roomId: 999,
      user: { id: 2, nickname: 'Alice', avatarUrl: '' },
      type: 'TEXT',
      contentText: '안녕하세요! VibeChat 데모 방에 오신 것을 환영합니다! 🎉',
      createdAt: new Date(Date.now() - 600000).toISOString()
    },
    {
      id: 2,
      roomId: 999,
      user: { id: 3, nickname: 'Bob', avatarUrl: '' },
      type: 'TEXT',
      contentText: '여기서 모든 채팅 기능을 테스트해볼 수 있어요!',
      createdAt: new Date(Date.now() - 480000).toISOString()
    },
    {
      id: 3,
      roomId: 999,
      user: { id: 4, nickname: 'Charlie', avatarUrl: '' },
      type: 'TEXT',
      contentText: '메시지에 마우스를 올려보세요. 액션 버튼들이 나타날 거예요! 😊',
      createdAt: new Date(Date.now() - 360000).toISOString()
    },
    {
      id: 4,
      roomId: 999,
      user: { id: 5, nickname: 'Diana', avatarUrl: '' },
      type: 'TEXT',
      contentText: '우클릭하면 컨텍스트 메뉴도 볼 수 있어요!',
      createdAt: new Date(Date.now() - 240000).toISOString()
    },
    {
      id: 5,
      roomId: 999,
      user: { id: 1, nickname: 'You', avatarUrl: '' },
      type: 'TEXT',
      contentText: '이모지 선택기와 반응 기능도 사용해보세요! 👍',
      createdAt: new Date(Date.now() - 120000).toISOString()
    }
  ]);

  const [messageInput, setMessageInput] = useState('');
  const [onlineCount, setOnlineCount] = useState(15);
  const [typingUsers, setTypingUsers] = useState<string[]>([]);
  const [showLeaveModal, setShowLeaveModal] = useState(false);
  const [isLoggedIn, setIsLoggedIn] = useState(true);
  const [user, setUser] = useState({ id: '1', nickname: 'You', avatarUrl: '' });

  // New states for enhanced features
  const [showUserList, setShowUserList] = useState(false);
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  const [showContextMenu, setShowContextMenu] = useState(false);
  const [contextMenuPosition, setContextMenuPosition] = useState({ x: 0, y: 0 });
  const [selectedMessage, setSelectedMessage] = useState<DemoMessage | null>(null);
  const [showThreadView, setShowThreadView] = useState(false);
  const [threadParentMessage, setThreadParentMessage] = useState<DemoMessage | null>(null);
  const [messageReactions, setMessageReactions] = useState<Record<number, Array<{emoji: string; count: number; users: string[]}>>>({
    1: [
      { emoji: '👍', count: 2, users: ['Bob', 'Charlie'] },
      { emoji: '🎉', count: 1, users: ['Diana'] }
    ],
    3: [
      { emoji: '😊', count: 3, users: ['Alice', 'Diana', 'Eve'] }
    ]
  });

  // Mock online users data
  const mockOnlineUsers = [
    { id: '1', nickname: 'You', status: 'online' as const, avatarUrl: '' },
    { id: '2', nickname: 'Alice', status: 'online' as const, avatarUrl: '' },
    { id: '3', nickname: 'Bob', status: 'away' as const, avatarUrl: '' },
    { id: '4', nickname: 'Charlie', status: 'online' as const, avatarUrl: '' },
    { id: '5', nickname: 'Diana', status: 'online' as const, avatarUrl: '' },
    { id: '6', nickname: 'Eve', status: 'offline' as const, avatarUrl: '', lastSeen: '5분 전' },
  ];

  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Auto scroll to bottom when new messages are added
  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages]);

  const handleSendMessage = (content: string) => {
    if (!content.trim()) return;

    const newMessage: DemoMessage = {
      id: Date.now(),
      clientTempId: `temp_${Date.now()}`,
      type: 'TEXT',
      contentText: content,
      roomId: 999,
      user: { id: parseInt(user.id), nickname: user.nickname, avatarUrl: user.avatarUrl },
      createdAt: new Date().toISOString()
    };

    setMessages(prev => [...prev, newMessage]);
    setMessageInput('');

    // Simulate other users typing occasionally
    if (Math.random() > 0.7) {
      const users = ['Alice', 'Bob', 'Charlie'];
      const randomUser = users[Math.floor(Math.random() * users.length)];
      setTypingUsers([randomUser]);
      
      setTimeout(() => {
        setTypingUsers([]);
        
        // Add a response message
        const responseMessages = [
          `좋은 의견이네요!`,
          `동감합니다 👍`,
          `흥미로운 관점이에요!`,
          `더 자세히 설명해주실 수 있나요?`,
          `정말 유용한 정보네요!`
        ];
        
        const responseMessage: DemoMessage = {
          id: Date.now() + 1,
          type: 'TEXT',
          contentText: responseMessages[Math.floor(Math.random() * responseMessages.length)],
          roomId: 999,
          user: { id: Math.floor(Math.random() * 5) + 2, nickname: randomUser, avatarUrl: '' },
          createdAt: new Date().toISOString()
        };
        
        setMessages(prev => [...prev, responseMessage]);
      }, 2000);
    }
  };

  const handleFileUpload = (file: File) => {
    toast.success(`파일 "${file.name}"이 업로드되었습니다! (데모)`);
    
    const newMessage: DemoMessage = {
      id: Date.now(),
      type: file.type.startsWith('image/') ? 'IMAGE' : 'TEXT',
      contentText: file.type.startsWith('image/') ? undefined : `📎 ${file.name}`,
      mediaUrl: file.type.startsWith('image/') ? URL.createObjectURL(file) : undefined,
      roomId: 999,
      user: { id: parseInt(user.id), nickname: user.nickname, avatarUrl: user.avatarUrl },
      createdAt: new Date().toISOString()
    };

    setMessages(prev => [...prev, newMessage]);
  };

  const handleLeaveRoom = () => {
    toast.success('데모 방에서 나왔습니다!');
    navigate('/');
  };

  const handleLogin = () => {
    setIsLoggedIn(true);
    toast.success('로그인되었습니다!');
  };

  const handleLogout = () => {
    setIsLoggedIn(false);
    toast.success('로그아웃되었습니다!');
  };

  const contextMenuItems = [
    {
      id: 'reply',
      label: '답글',
      icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6" /></svg>,
      onClick: () => {
        if (selectedMessage) {
          setThreadParentMessage(selectedMessage);
          setShowThreadView(true);
        }
      }
    },
    {
      id: 'react',
      label: '반응하기',
      icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.828 14.828a4 4 0 01-5.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>,
      onClick: () => {
        if (selectedMessage) {
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
      }
    },
    {
      id: 'copy',
      label: '복사',
      icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" /></svg>,
      onClick: () => {
        if (selectedMessage) {
          navigator.clipboard.writeText(selectedMessage.contentText || '');
          toast.success('메시지가 클립보드에 복사되었습니다.');
        }
      }
    }
  ];

  return (
    <>
    <div className="min-h-screen bg-background-primary flex flex-col">
      <Navbar 
        user={isLoggedIn ? user : undefined}
        onLogin={handleLogin}
        onLogout={handleLogout}
        onProfileClick={() => navigate('/profile')}
      />

      {/* Room Header */}
      <div className="border-b border-border-default bg-background-secondary">
        <div className="w-full px-4 py-4">
          <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
            {/* Room Info */}
            <div className="flex-1 min-w-0">
              <h1 className="text-xl font-bold text-foreground-primary mb-1">{room.title}</h1>
              <p className="text-sm text-foreground-muted mb-3">{room.description}</p>
              
              {/* Tags - More Prominent */}
              <div className="flex flex-wrap gap-2">
                {room.tags.map((tag) => (
                  <Badge key={tag} variant="primary" size="md" className="font-medium">
                    #{tag}
                  </Badge>
                ))}
              </div>
            </div>
            
            {/* Actions */}
            <div className="flex flex-col sm:flex-row items-start sm:items-center gap-3">
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

              <Button
                variant="secondary"
                size="sm"
                onClick={() => setShowLeaveModal(true)}
                className="self-start sm:self-auto"
              >
                방 나가기
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Main Chat Area */}
      <div className="flex-1 flex flex-col min-h-0">
        {/* Messages Area - Only this scrolls */}
        <div className="flex-1 overflow-y-auto">
          <div className="w-full px-4 py-4 space-y-4">
              {messages.map((message) => (
                <div 
                  key={message.id || message.clientTempId}
                  className="group relative w-full"
                  onContextMenu={(e) => {
                    e.preventDefault();
                    setSelectedMessage(message);
                    setContextMenuPosition({ x: e.clientX, y: e.clientY });
                    setShowContextMenu(true);
                  }}
                >
                  <div className="relative w-full">
                    <div className="w-full">
                      {/* Message Actions - Top Right */}
                      <div className="absolute -top-2 right-0 opacity-0 group-hover:opacity-100 transition-opacity duration-200 z-10">
                        <MessageActions
                        messageId={message.id.toString()}
                        onReply={() => {
                          setThreadParentMessage(message);
                          setShowThreadView(true);
                        }}
                        onEdit={() => toast.info('메시지 수정 기능은 곧 제공될 예정입니다!')}
                        onDelete={() => {
                          setMessages(prev => prev.filter(m => m.id !== message.id));
                          toast.success('메시지가 삭제되었습니다.');
                        }}
                        onReport={() => toast.info('신고가 접수되었습니다.')}
                        onReaction={() => {
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
                        isOwn={message.user.id === parseInt(user.id)}
                      />
                      </div>

                      {/* Message Content */}
                      <ChatMessage
                        id={message.id.toString()}
                        user={{
                          id: message.user.id.toString(),
                          nickname: message.user.nickname,
                          avatarUrl: message.user.avatarUrl || ''
                        }}
                        content={message.contentText || ''}
                        type={message.type}
                        mediaUrl={message.mediaUrl}
                        mediaThumbUrl={message.mediaThumbUrl}
                        durationSec={message.mediaDurationSec}
                        createdAt={message.createdAt}
                        isOwn={message.user.id === parseInt(user.id)}
                        isPending={!!message.clientTempId}
                      />

                      {/* Message Reactions - Below Message */}
                      {messageReactions[message.id] && (
                        <div className="mt-2 ml-12">
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
                            currentUserId={user.id}
                          />
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            <div ref={messagesEndRef} />
          </div>
        </div>

        {/* Typing Indicator */}
        {typingUsers.length > 0 && (
          <div className="px-4 py-2">
            <div className="flex items-center gap-2 text-sm text-foreground-muted ml-12">
              <LoadingDots size="sm" color="muted" />
              <span>
                {typingUsers.join(', ')} {typingUsers.length === 1 ? 'is' : 'are'} typing...
              </span>
            </div>
          </div>
        )}

        {/* Message Input - Fixed at bottom */}
        <div className="relative border-t border-border-default bg-background-secondary">
          <div className="px-4 py-3">
            <MessageInput
              onSendMessage={handleSendMessage}
              onFileUpload={handleFileUpload}
              onEmojiClick={() => setShowEmojiPicker(!showEmojiPicker)}
              placeholder="메시지를 입력하세요..."
              disabled={!isLoggedIn}
              onTypingChange={(typing) => {
                // Simulate typing in demo
              }}
            />
            
            {/* Emoji Picker */}
            <div className="absolute bottom-full right-4 z-50">
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
      </div>

      {/* User List Sidebar */}
      {showUserList && (
        <div className="hidden md:block w-80 border-l border-border-default bg-background-secondary">
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

        {/* Mobile User List Modal */}
        {showUserList && (
          <div className="md:hidden fixed inset-0 bg-black/50 z-50 flex items-end">
            <div className="w-full bg-background-secondary rounded-t-lg max-h-[70vh] overflow-hidden">
              <div className="p-4 border-b border-border-default flex justify-between items-center">
                <h3 className="text-lg font-semibold text-foreground-primary">채팅방 참여자</h3>
                <button
                  onClick={() => setShowUserList(false)}
                  className="p-2 rounded-lg hover:bg-background-tertiary"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
              <div className="overflow-y-auto">
                <UserList
                  users={mockOnlineUsers}
                  onUserClick={(userId) => {
                    toast.info(`${mockOnlineUsers.find(u => u.id === userId)?.nickname}님과의 DM 기능은 곧 제공될 예정입니다!`);
                  }}
                  title=""
                  className="border-none rounded-none"
                />
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Leave Room Modal */}
      <Modal 
        isOpen={showLeaveModal} 
        onClose={() => setShowLeaveModal(false)}
        title="방 나가기"
        size="md"
      >
        <div className="space-y-4">
          <p className="text-foreground-secondary">
            정말로 이 방에서 나가시겠습니까?
          </p>
          <div className="flex justify-end gap-2">
            <Button 
              variant="secondary" 
              onClick={() => setShowLeaveModal(false)}
            >
              취소
            </Button>
            <Button 
              variant="danger" 
              onClick={handleLeaveRoom}
            >
              방 나가기
            </Button>
          </div>
        </div>
      </Modal>

      {/* Context Menu */}
      {showContextMenu && selectedMessage && (
        <ContextMenu
          items={contextMenuItems}
          position={contextMenuPosition}
          onClose={() => setShowContextMenu(false)}
        />
      )}

      {/* Thread View */}
      {showThreadView && threadParentMessage && (
        <ThreadView
          parentMessage={threadParentMessage}
          replies={[]}
          onReply={(content) => {
            const newReply: DemoMessage = {
              id: Date.now(),
              clientTempId: `temp_${Date.now()}`,
              type: 'TEXT',
              contentText: content,
              roomId: 999,
              user: { id: parseInt(user.id), nickname: user.nickname, avatarUrl: user.avatarUrl },
              createdAt: new Date().toISOString(),
              parentMessageId: threadParentMessage.id
            };
            
            toast.success('답글이 전송되었습니다!');
            setMessages(prev => [...prev, newReply]);
          }}
          onClose={() => setShowThreadView(false)}
          currentUserId={user.id}
        />
      )}
    </>
  );
}
