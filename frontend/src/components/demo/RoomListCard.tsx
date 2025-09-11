import React from 'react';
import { cn } from '../../utils/cn';
import { Card } from '../ui/Card';
import { Badge } from '../ui/Badge';
import { Button } from '../ui/Button';
import NotificationBadge from '../ui/NotificationBadge';
import LoadingDots from '../ui/LoadingDots';

export interface RoomListItem {
  id: number;
  title: string;
  description?: string;
  tags: string[];
  participantsCount: number;
  isPrivate: boolean;
  lastMessageAt: string;
  lastMessage?: {
    user: string;
    content: string;
  };
  unreadCount: number;
  isMuted: boolean;
  isTyping?: boolean;
  typingUsers?: string[];
}

export interface RoomListCardProps {
  room: RoomListItem;
  onJoin: () => void;
  onLeave?: () => void;
  onMute?: () => void;
  onUnmute?: () => void;
  className?: string;
}

const RoomListCard: React.FC<RoomListCardProps> = ({
  room,
  onJoin,
  onLeave,
  onMute,
  onUnmute,
  className
}) => {
  const formatLastMessageTime = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffInMinutes = Math.floor((now.getTime() - date.getTime()) / (1000 * 60));

    if (diffInMinutes < 1) return '방금 전';
    if (diffInMinutes < 60) return `${diffInMinutes}분 전`;
    
    const diffInHours = Math.floor(diffInMinutes / 60);
    if (diffInHours < 24) return `${diffInHours}시간 전`;
    
    const diffInDays = Math.floor(diffInHours / 24);
    if (diffInDays < 7) return `${diffInDays}일 전`;
    
    return date.toLocaleDateString('ko-KR');
  };

  return (
    <NotificationBadge
      count={room.unreadCount}
      variant="danger"
      position="top-right"
      showZero={false}
    >
      <Card className={cn(
        "transition-all duration-200 hover:shadow-md cursor-pointer h-full flex flex-col",
        room.unreadCount > 0 && "ring-2 ring-primary-500/20",
        room.isMuted && "opacity-60",
        className
      )}>
        <div className="p-4 flex-1 flex flex-col" onClick={onJoin}>
          {/* Header */}
          <div className="flex items-start justify-between mb-3">
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 mb-1">
                <h3 className={cn(
                  "font-semibold truncate",
                  room.unreadCount > 0 ? "text-foreground-primary" : "text-foreground-secondary"
                )}>
                  {room.title}
                </h3>
                {room.isPrivate && (
                  <svg className="w-4 h-4 text-foreground-muted flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                  </svg>
                )}
                {room.isMuted && (
                  <svg className="w-4 h-4 text-foreground-muted flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z" />
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2" />
                  </svg>
                )}
              </div>
              
              <div className="flex items-center gap-2 text-sm text-foreground-muted">
                <span>{room.participantsCount}명</span>
                <span>•</span>
                <span>{formatLastMessageTime(room.lastMessageAt)}</span>
              </div>
            </div>
          </div>

          {/* Last Message */}
          {room.lastMessage && (
            <div className="mb-3">
              <p className={cn(
                "text-sm truncate",
                room.unreadCount > 0 ? "text-foreground-primary" : "text-foreground-muted"
              )}>
                <span className="font-medium">{room.lastMessage.user}:</span>{' '}
                {room.lastMessage.content}
              </p>
            </div>
          )}

          {/* Typing Indicator */}
          {room.isTyping && room.typingUsers && room.typingUsers.length > 0 && (
            <div className="flex items-center gap-2 mb-3">
              <LoadingDots size="sm" color="muted" />
              <span className="text-xs text-foreground-muted">
                {room.typingUsers.length === 1 
                  ? `${room.typingUsers[0]}님이 입력 중`
                  : `${room.typingUsers.length}명이 입력 중`
                }
              </span>
            </div>
          )}

          {/* Tags */}
          {room.tags.length > 0 && (
            <div className="flex flex-wrap gap-1 mb-3">
              {room.tags.slice(0, 3).map((tag, index) => (
                <Badge key={index} variant="secondary" size="sm">
                  #{tag}
                </Badge>
              ))}
              {room.tags.length > 3 && (
                <Badge variant="secondary" size="sm">
                  +{room.tags.length - 3}
                </Badge>
              )}
            </div>
          )}

          {/* Description */}
          <div className="flex-1 mb-3">
            {room.description && (
              <p className="text-sm text-foreground-muted line-clamp-2">
                {room.description}
              </p>
            )}
          </div>
        </div>

        {/* Actions */}
        <div className="px-4 pb-4 flex items-center justify-between">
          <div className="flex gap-2">
            <Button
              onClick={(e) => {
                e.stopPropagation();
                onJoin();
              }}
              variant="primary"
              size="sm"
            >
              입장
            </Button>
            {onLeave && (
              <Button
                onClick={(e) => {
                  e.stopPropagation();
                  onLeave();
                }}
                variant="secondary"
                size="sm"
              >
                나가기
              </Button>
            )}
          </div>

          <div className="flex gap-1">
            {room.isMuted ? (
              <Button
                onClick={(e) => {
                  e.stopPropagation();
                  onUnmute?.();
                }}
                variant="ghost"
                size="sm"
                className="p-2"
                title="알림 켜기"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z" />
                </svg>
              </Button>
            ) : (
              <Button
                onClick={(e) => {
                  e.stopPropagation();
                  onMute?.();
                }}
                variant="ghost"
                size="sm"
                className="p-2"
                title="알림 끄기"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2" />
                </svg>
              </Button>
            )}
          </div>
        </div>
      </Card>
    </NotificationBadge>
  );
};

export default RoomListCard;
