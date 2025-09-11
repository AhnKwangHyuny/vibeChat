import React, { useState } from 'react';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { Input } from '../components/ui/Input';
import { Avatar } from '../components/ui/Avatar';
import { Badge } from '../components/ui/Badge';
import { Modal } from '../components/ui/Modal';
import { ChatMessage } from '../components/demo/ChatMessage';
import { RoomCard } from '../components/demo/RoomCard';
import { MessageInput } from '../components/demo/MessageInput';
import { TagInput } from '../components/demo/TagInput';
import { Carousel } from '../components/ui/Carousel';
import { ImageCard } from '../components/ui/ImageCard';
import { FeatureShowcase } from '../components/demo/FeatureShowcase';
import { Navbar } from '../components/layout/Navbar';
import { Footer } from '../components/layout/Footer';
import { Hero } from '../components/layout/Hero';
import { Toast } from '../components/ui/Toast';
import { ToastContainer } from '../components/ui/ToastContainer';
import { Spinner } from '../components/ui/Spinner';
import { Progress } from '../components/ui/Progress';
import { Tooltip } from '../components/ui/Tooltip';

// New Chat Components
import EmojiPicker from '../components/ui/EmojiPicker';
import MessageReactions from '../components/demo/MessageReactions';
import UserList from '../components/demo/UserList';
import MessageActions from '../components/demo/MessageActions';
import ThreadView from '../components/demo/ThreadView';
import SearchBar from '../components/ui/SearchBar';
import VoiceMessage from '../components/demo/VoiceMessage';
import FilePreview from '../components/ui/FilePreview';
import NotificationBadge from '../components/ui/NotificationBadge';
import ContextMenu from '../components/ui/ContextMenu';
import LoadingDots from '../components/ui/LoadingDots';

const Components: React.FC = () => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [tags, setTags] = useState<string[]>(['React', 'TypeScript']);
  const [message, setMessage] = useState('');
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [toasts, setToasts] = useState<Array<{
    id: string;
    type: 'success' | 'error' | 'warning' | 'info';
    title: string;
    description?: string;
  }>>([]);
  const [progressValue, setProgressValue] = useState(0);

  // New component states
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  const [showThreadView, setShowThreadView] = useState(false);
  const [showContextMenu, setShowContextMenu] = useState(false);
  const [contextMenuPosition, setContextMenuPosition] = useState({ x: 0, y: 0 });
  const [isVoicePlaying, setIsVoicePlaying] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [reactions, setReactions] = useState([
    { emoji: '👍', count: 3, users: ['Alice', 'Bob', 'Charlie'] },
    { emoji: '❤️', count: 2, users: ['Diana', 'Eve'] },
    { emoji: '😂', count: 1, users: ['Frank'] }
  ]);

  const sampleMessages = [
    {
      id: '1',
      user: { id: '1', nickname: 'Alice', avatarUrl: '' },
      content: '안녕하세요! 이 방에 오신 것을 환영합니다.',
      type: 'TEXT' as const,
      createdAt: new Date().toISOString(),
      isOwn: false
    },
    {
      id: '2',
      user: { id: '2', nickname: 'Bob', avatarUrl: '' },
      content: 'React와 TypeScript로 개발하고 있어요!',
      type: 'TEXT' as const,
      createdAt: new Date(Date.now() - 300000).toISOString(),
      isOwn: true
    },
    {
      id: '3',
      user: { id: '1', nickname: 'Alice', avatarUrl: '' },
      content: '정말 멋진 프로젝트네요!',
      type: 'TEXT' as const,
      createdAt: new Date(Date.now() - 600000).toISOString(),
      isOwn: false
    }
  ];

  const sampleRooms = [
    {
      id: '1',
      title: 'React 개발자 모임',
      description: 'React와 관련된 모든 것을 이야기하는 공간입니다.',
      tags: ['React', 'JavaScript', 'Frontend'],
      isPrivate: false,
      participantsCount: 12,
      lastMessageAt: new Date().toISOString()
    },
    {
      id: '2',
      title: '비공개 프로젝트',
      description: '비밀 프로젝트에 대한 논의',
      tags: ['Private', 'Project'],
      isPrivate: true,
      participantsCount: 3,
      lastMessageAt: new Date(Date.now() - 3600000).toISOString()
    }
  ];

  const tagSuggestions = [
    'React', 'TypeScript', 'JavaScript', 'Node.js', 'Python', 'Java',
    'Frontend', 'Backend', 'Fullstack', 'Mobile', 'AI', 'Machine Learning',
    'Web3', 'Blockchain', 'Design', 'UX', 'UI', 'DevOps'
  ];

  // Mock data for new components
  const mockUsers = [
    { id: '1', nickname: 'Alice', status: 'online' as const, avatarUrl: '' },
    { id: '2', nickname: 'Bob', status: 'away' as const, avatarUrl: '' },
    { id: '3', nickname: 'Charlie', status: 'online' as const, avatarUrl: '' },
    { id: '4', nickname: 'Diana', status: 'offline' as const, avatarUrl: '', lastSeen: '5분 전' },
    { id: '5', nickname: 'Eve', status: 'online' as const, avatarUrl: '' }
  ];

  const mockSearchSuggestions = [
    'React Hooks', 'TypeScript 타입', '웹 성능 최적화', 'GraphQL API',
    'Next.js 프로젝트', '리덕스 툴킷', 'CSS Grid', '반응형 디자인'
  ];

  const contextMenuItems = [
    {
      id: 'copy',
      label: '복사',
      icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" /></svg>,
      onClick: () => console.log('Copy clicked')
    },
    {
      id: 'edit',
      label: '수정',
      icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" /></svg>,
      onClick: () => console.log('Edit clicked')
    },
    {
      id: 'delete',
      label: '삭제',
      icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>,
      onClick: () => console.log('Delete clicked'),
      destructive: true
    }
  ];

  const mockFile = new File([''], 'example-image.jpg', { type: 'image/jpeg' });

  return (
    <div className="min-h-screen bg-background-primary">
      {/* Header */}
      <header className="border-b border-border-default bg-background-secondary">
        <div className="max-w-6xl mx-auto px-6 py-4">
          <h1 className="text-2xl font-semibold text-foreground-primary">
            VibeChat 컴포넌트 라이브러리
          </h1>
          <p className="text-foreground-muted mt-1">
            Linear 테마를 기반으로 한 재사용 가능한 UI 컴포넌트들
          </p>
        </div>
      </header>

      <div className="max-w-6xl mx-auto px-6 py-8">
        {/* Buttons Section */}
        <section className="mb-12">
          <h2 className="text-xl font-semibold text-foreground-primary mb-6">버튼 (Buttons)</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <Card>
              <h3 className="text-lg font-medium text-foreground-primary mb-4">Primary Buttons</h3>
              <div className="space-y-3">
                <Button variant="primary" size="sm">Small</Button>
                <Button variant="primary" size="md">Medium</Button>
                <Button variant="primary" size="lg">Large</Button>
                <Button variant="primary" disabled>Disabled</Button>
              </div>
            </Card>

            <Card>
              <h3 className="text-lg font-medium text-foreground-primary mb-4">Secondary Buttons</h3>
              <div className="space-y-3">
                <Button variant="secondary" size="sm">Small</Button>
                <Button variant="secondary" size="md">Medium</Button>
                <Button variant="secondary" size="lg">Large</Button>
                <Button variant="secondary" disabled>Disabled</Button>
              </div>
            </Card>

            <Card>
              <h3 className="text-lg font-medium text-foreground-primary mb-4">Ghost Buttons</h3>
              <div className="space-y-3">
                <Button variant="ghost" size="sm">Small</Button>
                <Button variant="ghost" size="md">Medium</Button>
                <Button variant="ghost" size="lg">Large</Button>
                <Button variant="ghost" disabled>Disabled</Button>
              </div>
            </Card>
          </div>
        </section>

        {/* Form Elements Section */}
        <section className="mb-12">
          <h2 className="text-xl font-semibold text-foreground-primary mb-6">폼 요소 (Form Elements)</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <Card>
              <h3 className="text-lg font-medium text-foreground-primary mb-4">입력 필드</h3>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-foreground-secondary mb-2">
                    기본 입력
                  </label>
                  <Input placeholder="텍스트를 입력하세요..." />
                </div>
                <div>
                  <label className="block text-sm font-medium text-foreground-secondary mb-2">
                    검색 입력
                  </label>
                  <Input variant="search" placeholder="검색어를 입력하세요..." />
                </div>
                <div>
                  <label className="block text-sm font-medium text-foreground-secondary mb-2">
                    에러 상태
                  </label>
                  <Input placeholder="에러가 있는 입력" error />
                </div>
              </div>
            </Card>

            <Card>
              <h3 className="text-lg font-medium text-foreground-primary mb-4">태그 입력</h3>
              <TagInput
                tags={tags}
                onTagsChange={setTags}
                suggestions={tagSuggestions}
                placeholder="태그를 입력하세요..."
                maxTags={5}
              />
            </Card>
          </div>
        </section>

        {/* Avatar & Badge Section */}
        <section className="mb-12">
          <h2 className="text-xl font-semibold text-foreground-primary mb-6">아바타 & 배지 (Avatar & Badge)</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <Card>
              <h3 className="text-lg font-medium text-foreground-primary mb-4">아바타</h3>
              <div className="flex items-center gap-4">
                <Avatar size="sm" fallback="A" />
                <Avatar size="md" fallback="B" />
                <Avatar size="lg" fallback="C" />
                <Avatar size="xl" fallback="D" />
                <Avatar size="md" fallback="E" online />
              </div>
            </Card>

            <Card>
              <h3 className="text-lg font-medium text-foreground-primary mb-4">배지</h3>
              <div className="flex flex-wrap gap-2">
                <Badge variant="default">기본</Badge>
                <Badge variant="success">성공</Badge>
                <Badge variant="warning">경고</Badge>
                <Badge variant="error">에러</Badge>
                <Badge variant="info">정보</Badge>
              </div>
            </Card>
          </div>
        </section>

        {/* Chat Components Section */}
        <section className="mb-12">
          <h2 className="text-xl font-semibold text-foreground-primary mb-6">채팅 컴포넌트 (Chat Components)</h2>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <Card>
              <h3 className="text-lg font-medium text-foreground-primary mb-4">채팅 메시지</h3>
              <div className="space-y-2 max-h-96 overflow-y-auto">
                {sampleMessages.map((msg) => (
                  <ChatMessage key={msg.id} {...msg} />
                ))}
              </div>
            </Card>

            <Card>
              <h3 className="text-lg font-medium text-foreground-primary mb-4">메시지 입력</h3>
              <MessageInput
                onSendMessage={(content) => {
                  setMessage(content);
                  console.log('Sending message:', content);
                }}
                onSendMedia={(file) => {
                  console.log('Sending media:', file);
                }}
                placeholder="메시지를 입력하세요..."
              />
            </Card>
          </div>
        </section>

        {/* Room Components Section */}
        <section className="mb-12">
          <h2 className="text-xl font-semibold text-foreground-primary mb-6">방 컴포넌트 (Room Components)</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {sampleRooms.map((room) => (
              <RoomCard
                key={room.id}
                {...room}
                onJoin={(roomId) => {
                  console.log('Joining room:', roomId);
                }}
              />
            ))}
          </div>
        </section>

        {/* Feature Showcase Section */}
        <section className="mb-12">
          <h2 className="text-xl font-semibold text-foreground-primary mb-6">기능 쇼케이스 (Feature Showcase)</h2>
          <FeatureShowcase
            title="VibeChat의 핵심 기능"
            description="관심사 기반 실시간 채팅으로 사람들과 연결되는 새로운 경험을 제공합니다."
            imageUrl="https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=800&h=400&fit=crop"
            tags={['실시간', '채팅', '커뮤니티']}
            features={[
              {
                icon: (
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                  </svg>
                ),
                title: "실시간 채팅",
                description: "WebSocket을 통한 즉시 메시지 전송과 수신"
              },
              {
                icon: (
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
                  </svg>
                ),
                title: "태그 기반 검색",
                description: "관심사로 원하는 방을 쉽게 찾을 수 있습니다"
              },
              {
                icon: (
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                  </svg>
                ),
                title: "미디어 공유",
                description: "이미지, GIF, 영상을 자유롭게 공유하세요"
              },
              {
                icon: (
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                  </svg>
                ),
                title: "보안 및 신고",
                description: "안전한 채팅 환경을 위한 신고 시스템"
              }
            ]}
          />
        </section>

        {/* Carousel Section */}
        <section className="mb-12">
          <h2 className="text-xl font-semibold text-foreground-primary mb-6">캐러셀 (Carousel)</h2>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <Card>
              <h3 className="text-lg font-medium text-foreground-primary mb-4">기본 캐러셀</h3>
              <div className="h-64">
                <Carousel>
                  <div className="h-64 bg-gradient-to-br from-primary-500 to-primary-900 flex items-center justify-center text-foreground-primary text-xl font-semibold">
                    슬라이드 1
                  </div>
                  <div className="h-64 bg-gradient-to-br from-semantic-info to-semantic-info/70 flex items-center justify-center text-foreground-primary text-xl font-semibold">
                    슬라이드 2
                  </div>
                  <div className="h-64 bg-gradient-to-br from-semantic-success to-semantic-success/70 flex items-center justify-center text-foreground-primary text-xl font-semibold">
                    슬라이드 3
                  </div>
                </Carousel>
              </div>
            </Card>

            <Card>
              <h3 className="text-lg font-medium text-foreground-primary mb-4">자동 재생 캐러셀</h3>
              <div className="h-64">
                <Carousel autoPlay interval={2000}>
                  <div className="h-64 bg-gradient-to-br from-semantic-warning to-semantic-warning/70 flex items-center justify-center text-foreground-primary text-xl font-semibold">
                    자동 슬라이드 1
                  </div>
                  <div className="h-64 bg-gradient-to-br from-semantic-error to-semantic-error/70 flex items-center justify-center text-foreground-primary text-xl font-semibold">
                    자동 슬라이드 2
                  </div>
                  <div className="h-64 bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center text-foreground-primary text-xl font-semibold">
                    자동 슬라이드 3
                  </div>
                </Carousel>
              </div>
            </Card>
          </div>
        </section>

        {/* Image Card Section */}
        <section className="mb-12">
          <h2 className="text-xl font-semibold text-foreground-primary mb-6">이미지 카드 (Image Card)</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <ImageCard
              src="https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=400&h=300&fit=crop"
              alt="코딩하는 개발자"
              title="개발자 워크스페이스"
              description="모던한 개발 환경에서 작업하는 개발자의 모습을 담은 이미지입니다."
              aspectRatio="video"
              onImageClick={() => console.log('이미지 클릭됨')}
            />
            <ImageCard
              src="https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=400&h=400&fit=crop"
              alt="팀 협업"
              title="팀 협업"
              description="함께 일하는 팀의 모습을 보여주는 이미지입니다."
              aspectRatio="square"
              overlay
            >
              <Button variant="secondary" size="sm">
                자세히 보기
              </Button>
            </ImageCard>
            <ImageCard
              src="https://images.unsplash.com/photo-1551434678-e076c223a692?w=400&h=500&fit=crop"
              alt="회의하는 사람들"
              title="비즈니스 미팅"
              description="회의실에서 열정적으로 토론하는 모습입니다."
              aspectRatio="portrait"
            />
          </div>
        </section>

        {/* Additional UI Components Section */}
        <section className="mb-12">
          <h2 className="text-xl font-semibold text-foreground-primary mb-6">추가 UI 컴포넌트 (Additional UI Components)</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {/* Toast Demo */}
            <Card>
              <h3 className="text-lg font-medium text-foreground-primary mb-4">토스트 (Toast)</h3>
              <div className="space-y-3">
                <Button
                  onClick={() => {
                    const id = Date.now().toString();
                    setToasts(prev => [...prev, {
                      id,
                      type: 'success',
                      title: '성공!',
                      description: '작업이 완료되었습니다.'
                    }]);
                  }}
                  variant="primary"
                  size="sm"
                >
                  성공 토스트
                </Button>
                <Button
                  onClick={() => {
                    const id = Date.now().toString();
                    setToasts(prev => [...prev, {
                      id,
                      type: 'error',
                      title: '오류 발생',
                      description: '문제가 발생했습니다.'
                    }]);
                  }}
                  variant="secondary"
                  size="sm"
                >
                  에러 토스트
                </Button>
                <Button
                  onClick={() => {
                    const id = Date.now().toString();
                    setToasts(prev => [...prev, {
                      id,
                      type: 'warning',
                      title: '주의',
                      description: '주의가 필요합니다.'
                    }]);
                  }}
                  variant="secondary"
                  size="sm"
                >
                  경고 토스트
                </Button>
              </div>
            </Card>

            {/* Spinner Demo */}
            <Card>
              <h3 className="text-lg font-medium text-foreground-primary mb-4">스피너 (Spinner)</h3>
              <div className="space-y-4">
                <div className="flex items-center gap-4">
                  <Spinner size="sm" />
                  <Spinner size="md" />
                  <Spinner size="lg" />
                  <Spinner size="xl" />
                </div>
                <div className="flex items-center gap-4">
                  <Spinner size="md" color="primary" />
                  <Spinner size="md" color="secondary" />
                  <Spinner size="md" color="muted" />
                </div>
              </div>
            </Card>

            {/* Progress Demo */}
            <Card>
              <h3 className="text-lg font-medium text-foreground-primary mb-4">진행률 (Progress)</h3>
              <div className="space-y-4">
                <Progress
                  value={progressValue}
                  showLabel
                  label="업로드 진행률"
                />
                <div className="flex gap-2">
                  <Button
                    onClick={() => setProgressValue(Math.max(0, progressValue - 10))}
                    variant="secondary"
                    size="sm"
                  >
                    -10%
                  </Button>
                  <Button
                    onClick={() => setProgressValue(Math.min(100, progressValue + 10))}
                    variant="secondary"
                    size="sm"
                  >
                    +10%
                  </Button>
                </div>
                <Progress
                  value={75}
                  variant="success"
                  showLabel
                  label="성공률"
                />
                <Progress
                  value={30}
                  variant="warning"
                  showLabel
                  label="경고"
                />
                <Progress
                  value={90}
                  variant="error"
                  showLabel
                  label="에러"
                />
              </div>
            </Card>

            {/* Tooltip Demo */}
            <Card>
              <h3 className="text-lg font-medium text-foreground-primary mb-4">툴팁 (Tooltip)</h3>
              <div className="space-y-4">
                <div className="flex flex-wrap gap-4">
                  <Tooltip content="위쪽 툴팁입니다">
                    <Button variant="secondary" size="sm">위쪽</Button>
                  </Tooltip>
                  <Tooltip content="아래쪽 툴팁입니다" position="bottom">
                    <Button variant="secondary" size="sm">아래쪽</Button>
                  </Tooltip>
                  <Tooltip content="왼쪽 툴팁입니다" position="left">
                    <Button variant="secondary" size="sm">왼쪽</Button>
                  </Tooltip>
                  <Tooltip content="오른쪽 툴팁입니다" position="right">
                    <Button variant="secondary" size="sm">오른쪽</Button>
                  </Tooltip>
                </div>
                <div className="flex gap-4">
                  <Tooltip content="긴 툴팁 메시지입니다. 여러 줄로 표시될 수 있습니다.">
                    <span className="text-foreground-muted cursor-help">도움말 아이콘</span>
                  </Tooltip>
                </div>
              </div>
            </Card>
          </div>
        </section>

        {/* Modal Section */}
        <section className="mb-12">
          <h2 className="text-xl font-semibold text-foreground-primary mb-6">모달 (Modal)</h2>
          <Card>
            <h3 className="text-lg font-medium text-foreground-primary mb-4">모달 예제</h3>
            <div className="space-y-4">
              <Button onClick={() => setIsModalOpen(true)}>
                모달 열기
              </Button>
              <p className="text-sm text-foreground-muted">
                버튼을 클릭하여 모달을 확인해보세요.
              </p>
            </div>
          </Card>
        </section>

        {/* Layout Components Section */}
        <section className="mb-12">
          <h2 className="text-xl font-semibold text-foreground-primary mb-6">레이아웃 컴포넌트 (Layout Components)</h2>
          
          {/* Navbar Demo */}
          <div className="mb-8">
            <h3 className="text-lg font-medium text-foreground-primary mb-4">네비게이션 바 (Navbar)</h3>
            <div className="border border-border-default rounded-lg overflow-hidden">
              <Navbar
                user={isLoggedIn ? {
                  id: '1',
                  nickname: '김개발',
                  avatarUrl: ''
                } : undefined}
                onLogin={() => setIsLoggedIn(true)}
                onLogout={() => setIsLoggedIn(false)}
                onProfileClick={() => console.log('프로필 클릭')}
              />
            </div>
            <div className="mt-4 flex gap-2">
              <Button
                onClick={() => setIsLoggedIn(!isLoggedIn)}
                variant="secondary"
                size="sm"
              >
                {isLoggedIn ? '로그아웃' : '로그인'} 상태 토글
              </Button>
            </div>
          </div>

          {/* Hero Demo */}
          <div className="mb-8">
            <h3 className="text-lg font-medium text-foreground-primary mb-4">히어로 섹션 (Hero)</h3>
            <div className="border border-border-default rounded-lg overflow-hidden">
              <Hero
                subtitle="관심사 기반 실시간 채팅"
                title="사람들과 연결되는 새로운 경험"
                description="같은 관심사를 가진 사람들이 '지금' 연결되어 대화를 시작할 수 있는 가장 간편한 실시간 채팅 경험을 제공합니다."
                badges={['실시간', '채팅', '커뮤니티']}
                primaryAction={{
                  label: '시작하기',
                  onClick: () => console.log('시작하기 클릭')
                }}
                secondaryAction={{
                  label: '자세히 보기',
                  onClick: () => console.log('자세히 보기 클릭')
                }}
                stats={[
                  { value: '10K+', label: '활성 사용자' },
                  { value: '500+', label: '채팅방' },
                  { value: '99.9%', label: '가동률' }
                ]}
                backgroundImage="https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=1200&h=600&fit=crop"
              />
            </div>
          </div>

          {/* Footer Demo */}
          <div className="mb-8">
            <h3 className="text-lg font-medium text-foreground-primary mb-4">푸터 (Footer)</h3>
            <div className="border border-border-default rounded-lg overflow-hidden">
              <Footer />
            </div>
          </div>
        </section>

        {/* Color Palette Section */}
        <section className="mb-12">
          <h2 className="text-xl font-semibold text-foreground-primary mb-6">색상 팔레트 (Color Palette)</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            <Card>
              <h3 className="text-lg font-medium text-foreground-primary mb-4">Primary</h3>
              <div className="space-y-2">
                <div className="flex items-center gap-2">
                  <div className="w-6 h-6 rounded bg-primary-50"></div>
                  <span className="text-sm text-foreground-secondary">50</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-6 h-6 rounded bg-primary-100"></div>
                  <span className="text-sm text-foreground-secondary">100</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-6 h-6 rounded bg-primary-200"></div>
                  <span className="text-sm text-foreground-secondary">200</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-6 h-6 rounded bg-primary-500"></div>
                  <span className="text-sm text-foreground-secondary">500</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-6 h-6 rounded bg-primary-900"></div>
                  <span className="text-sm text-foreground-secondary">900</span>
                </div>
              </div>
            </Card>

            <Card>
              <h3 className="text-lg font-medium text-foreground-primary mb-4">Semantic</h3>
              <div className="space-y-2">
                <div className="flex items-center gap-2">
                  <div className="w-6 h-6 rounded bg-semantic-success"></div>
                  <span className="text-sm text-foreground-secondary">Success</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-6 h-6 rounded bg-semantic-warning"></div>
                  <span className="text-sm text-foreground-secondary">Warning</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-6 h-6 rounded bg-semantic-error"></div>
                  <span className="text-sm text-foreground-secondary">Error</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-6 h-6 rounded bg-semantic-info"></div>
                  <span className="text-sm text-foreground-secondary">Info</span>
                </div>
              </div>
            </Card>

            <Card>
              <h3 className="text-lg font-medium text-foreground-primary mb-4">Background</h3>
              <div className="space-y-2">
                <div className="flex items-center gap-2">
                  <div className="w-6 h-6 rounded bg-background-primary border border-border-default"></div>
                  <span className="text-sm text-foreground-secondary">Primary</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-6 h-6 rounded bg-background-secondary border border-border-default"></div>
                  <span className="text-sm text-foreground-secondary">Secondary</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-6 h-6 rounded bg-background-tertiary border border-border-default"></div>
                  <span className="text-sm text-foreground-secondary">Tertiary</span>
                </div>
              </div>
            </Card>

            <Card>
              <h3 className="text-lg font-medium text-foreground-primary mb-4">Foreground</h3>
              <div className="space-y-2">
                <div className="flex items-center gap-2">
                  <div className="w-6 h-6 rounded bg-foreground-primary"></div>
                  <span className="text-sm text-foreground-secondary">Primary</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-6 h-6 rounded bg-foreground-secondary"></div>
                  <span className="text-sm text-foreground-secondary">Secondary</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-6 h-6 rounded bg-foreground-muted"></div>
                  <span className="text-sm text-foreground-secondary">Muted</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-6 h-6 rounded bg-foreground-disabled"></div>
                  <span className="text-sm text-foreground-secondary">Disabled</span>
                </div>
              </div>
            </Card>
          </div>
        </section>

        {/* New Chat Components Section */}
        <section className="mb-12">
          <h2 className="text-xl font-semibold text-foreground-primary mb-6">🚀 새로운 채팅 컴포넌트들</h2>
          <p className="text-foreground-muted mb-8">채팅 애플리케이션을 위한 고급 컴포넌트들입니다.</p>

          {/* EmojiPicker Demo */}
          <div className="mb-8">
            <h3 className="text-lg font-medium text-foreground-primary mb-4">이모지 선택기 (EmojiPicker)</h3>
            <Card className="p-6">
              <div className="relative">
                <Button
                  variant="secondary"
                  onClick={() => setShowEmojiPicker(!showEmojiPicker)}
                  className="mb-4"
                >
                  😀 이모지 선택
                </Button>
                <EmojiPicker
                  isOpen={showEmojiPicker}
                  onClose={() => setShowEmojiPicker(false)}
                  onEmojiSelect={(emoji) => {
                    console.log('Selected emoji:', emoji);
                    setShowEmojiPicker(false);
                  }}
                />
              </div>
              <p className="text-sm text-foreground-muted">
                카테고리별로 정리된 이모지를 선택할 수 있습니다.
              </p>
            </Card>
          </div>

          {/* MessageReactions Demo */}
          <div className="mb-8">
            <h3 className="text-lg font-medium text-foreground-primary mb-4">메시지 반응 (MessageReactions)</h3>
            <Card className="p-6">
              <div className="mb-4">
                <p className="text-foreground-primary mb-2">샘플 메시지입니다!</p>
                <MessageReactions
                  reactions={reactions}
                  onReactionAdd={(emoji) => {
                    setReactions(prev => {
                      const existing = prev.find(r => r.emoji === emoji);
                      if (existing) {
                        return prev.map(r => 
                          r.emoji === emoji 
                            ? { ...r, count: r.count + 1, users: [...r.users, 'You'] }
                            : r
                        );
                      }
                      return [...prev, { emoji, count: 1, users: ['You'] }];
                    });
                  }}
                  onReactionRemove={(emoji) => {
                    setReactions(prev => prev.filter(r => r.emoji !== emoji));
                  }}
                  currentUserId="current-user"
                />
              </div>
              <p className="text-sm text-foreground-muted">
                메시지에 반응을 추가하고 누가 반응했는지 확인할 수 있습니다.
              </p>
            </Card>
          </div>

          {/* UserList Demo */}
          <div className="mb-8">
            <h3 className="text-lg font-medium text-foreground-primary mb-4">사용자 목록 (UserList)</h3>
            <Card className="p-6">
              <div className="max-w-sm">
                <UserList
                  users={mockUsers}
                  onUserClick={(userId) => console.log('User clicked:', userId)}
                  title="채팅방 참여자"
                />
              </div>
              <p className="text-sm text-foreground-muted mt-4">
                온라인/오프라인 상태와 함께 사용자 목록을 표시합니다.
              </p>
            </Card>
          </div>

          {/* SearchBar Demo */}
          <div className="mb-8">
            <h3 className="text-lg font-medium text-foreground-primary mb-4">검색바 (SearchBar)</h3>
            <Card className="p-6">
              <SearchBar
                placeholder="메시지나 사용자 검색..."
                onSearch={(query) => {
                  setSearchQuery(query);
                  console.log('Search:', query);
                }}
                suggestions={mockSearchSuggestions}
                value={searchQuery}
                className="mb-4"
              />
              <p className="text-sm text-foreground-muted">
                자동완성 기능이 있는 검색바입니다. 검색어: "{searchQuery}"
              </p>
            </Card>
          </div>

          {/* VoiceMessage Demo */}
          <div className="mb-8">
            <h3 className="text-lg font-medium text-foreground-primary mb-4">음성 메시지 (VoiceMessage)</h3>
            <Card className="p-6">
              <VoiceMessage
                audioUrl="data:audio/wav;base64,UklGRigAAABXQVZFZm10IBAAAAABAAEARKwAAIhYAQACABAAZGF0YQAAAAA="
                duration={45}
                isPlaying={isVoicePlaying}
                onPlay={() => setIsVoicePlaying(true)}
                onPause={() => setIsVoicePlaying(false)}
                className="mb-4"
              />
              <p className="text-sm text-foreground-muted">
                웨이브폼과 재생 컨트롤이 있는 음성 메시지 컴포넌트입니다.
              </p>
            </Card>
          </div>

          {/* FilePreview Demo */}
          <div className="mb-8">
            <h3 className="text-lg font-medium text-foreground-primary mb-4">파일 미리보기 (FilePreview)</h3>
            <Card className="p-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <FilePreview
                  file={{
                    name: 'example-document.pdf',
                    size: 2048576,
                    type: 'application/pdf'
                  }}
                  onRemove={() => console.log('File removed')}
                />
                <FilePreview
                  file={{
                    name: 'uploading-image.jpg',
                    size: 1024000,
                    type: 'image/jpeg'
                  }}
                  showProgress={true}
                  uploadProgress={75}
                  onRemove={() => console.log('File removed')}
                />
              </div>
              <p className="text-sm text-foreground-muted mt-4">
                다양한 파일 타입의 미리보기와 업로드 진행률을 표시합니다.
              </p>
            </Card>
          </div>

          {/* NotificationBadge Demo */}
          <div className="mb-8">
            <h3 className="text-lg font-medium text-foreground-primary mb-4">알림 배지 (NotificationBadge)</h3>
            <Card className="p-6">
              <div className="flex flex-wrap items-center gap-6 mb-4">
                <NotificationBadge count={3} variant="danger">
                  <Button variant="secondary">메시지</Button>
                </NotificationBadge>
                
                <NotificationBadge count={12} variant="primary">
                  <Button variant="secondary">알림</Button>
                </NotificationBadge>
                
                <NotificationBadge count={105} maxCount={99} variant="warning">
                  <Button variant="secondary">활동</Button>
                </NotificationBadge>
                
                <NotificationBadge count={7} variant="success" pulse>
                  <Button variant="secondary">새 소식</Button>
                </NotificationBadge>
              </div>
              <p className="text-sm text-foreground-muted">
                다양한 스타일과 위치의 알림 배지를 표시합니다.
              </p>
            </Card>
          </div>

          {/* LoadingDots Demo */}
          <div className="mb-8">
            <h3 className="text-lg font-medium text-foreground-primary mb-4">로딩 점들 (LoadingDots)</h3>
            <Card className="p-6">
              <div className="flex flex-wrap items-center gap-8 mb-4">
                <div className="text-center">
                  <LoadingDots size="sm" color="primary" />
                  <p className="text-xs text-foreground-muted mt-2">Small</p>
                </div>
                <div className="text-center">
                  <LoadingDots size="md" color="secondary" />
                  <p className="text-xs text-foreground-muted mt-2">Medium</p>
                </div>
                <div className="text-center">
                  <LoadingDots size="lg" color="muted" />
                  <p className="text-xs text-foreground-muted mt-2">Large</p>
                </div>
              </div>
              <div className="flex items-center gap-2 p-3 bg-background-tertiary rounded">
                <span className="text-sm text-foreground-muted">Alice가 입력 중</span>
                <LoadingDots size="sm" color="muted" />
              </div>
              <p className="text-sm text-foreground-muted mt-4">
                타이핑 인디케이터나 로딩 상태에 사용할 수 있는 애니메이션 점들입니다.
              </p>
            </Card>
          </div>

          {/* ContextMenu Demo */}
          <div className="mb-8">
            <h3 className="text-lg font-medium text-foreground-primary mb-4">컨텍스트 메뉴 (ContextMenu)</h3>
            <Card className="p-6">
              <div className="relative">
                <Button
                  variant="secondary"
                  onContextMenu={(e) => {
                    e.preventDefault();
                    setContextMenuPosition({ x: e.clientX, y: e.clientY });
                    setShowContextMenu(true);
                  }}
                  className="mb-4"
                >
                  우클릭하여 컨텍스트 메뉴 열기
                </Button>
                {showContextMenu && (
                  <ContextMenu
                    items={contextMenuItems}
                    position={contextMenuPosition}
                    onClose={() => setShowContextMenu(false)}
                  />
                )}
              </div>
              <p className="text-sm text-foreground-muted">
                우클릭으로 나타나는 컨텍스트 메뉴입니다.
              </p>
            </Card>
          </div>

          {/* MessageActions Demo */}
          <div className="mb-8">
            <h3 className="text-lg font-medium text-foreground-primary mb-4">메시지 액션 (MessageActions)</h3>
            <Card className="p-6">
              <div className="group p-4 bg-background-tertiary rounded-lg hover:bg-background-secondary transition-colors">
                <div className="flex items-start justify-between">
                  <div>
                    <p className="text-foreground-primary">샘플 메시지입니다!</p>
                    <p className="text-xs text-foreground-muted mt-1">Alice • 방금 전</p>
                  </div>
                  <MessageActions
                    messageId="msg-123"
                    onReply={() => console.log('Reply')}
                    onEdit={() => console.log('Edit')}
                    onDelete={() => console.log('Delete')}
                    onReport={() => console.log('Report')}
                    onReaction={() => console.log('Reaction')}
                    isOwn={true}
                  />
                </div>
              </div>
              <p className="text-sm text-foreground-muted mt-4">
                메시지 위에 마우스를 올리면 액션 버튼이 나타납니다.
              </p>
            </Card>
          </div>

          {/* ThreadView Demo */}
          <div className="mb-8">
            <h3 className="text-lg font-medium text-foreground-primary mb-4">스레드 뷰 (ThreadView)</h3>
            <Card className="p-6">
              <Button
                variant="secondary"
                onClick={() => setShowThreadView(true)}
                className="mb-4"
              >
                스레드 열기
              </Button>
              {showThreadView && (
                <ThreadView
                  parentMessage={{
                    id: 1,
                    roomId: 1,
                    user: { id: 1, nickname: 'Alice', avatarUrl: '' },
                    type: 'TEXT',
                    contentText: '이것은 스레드의 원본 메시지입니다.',
                    createdAt: new Date().toISOString()
                  }}
                  replies={[
                    {
                      id: 2,
                      roomId: 1,
                      user: { id: 2, nickname: 'Bob', avatarUrl: '' },
                      type: 'TEXT',
                      contentText: '좋은 아이디어네요!',
                      createdAt: new Date().toISOString(),
                      parentMessageId: 1
                    }
                  ]}
                  onReply={(content) => console.log('Reply:', content)}
                  onClose={() => setShowThreadView(false)}
                  currentUserId="current-user"
                />
              )}
              <p className="text-sm text-foreground-muted">
                메시지에 대한 답글을 스레드 형태로 표시합니다.
              </p>
            </Card>
          </div>
        </section>
      </div>

      {/* Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="모달 예제"
        size="md"
      >
        <div className="space-y-4">
          <p className="text-foreground-secondary">
            이것은 모달 컴포넌트의 예제입니다. ESC 키를 누르거나 배경을 클릭하여 닫을 수 있습니다.
          </p>
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setIsModalOpen(false)}>
              취소
            </Button>
            <Button onClick={() => setIsModalOpen(false)}>
              확인
            </Button>
          </div>
        </div>
      </Modal>

      {/* Toast Container */}
      <ToastContainer
        toasts={toasts}
        onRemoveToast={(id) => setToasts(prev => prev.filter(toast => toast.id !== id))}
        position="top-right"
      />
    </div>
  );
};

export default Components;
