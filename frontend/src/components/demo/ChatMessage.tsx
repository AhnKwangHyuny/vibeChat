import React from 'react';
import { Avatar } from '../ui/Avatar';
import { Badge } from '../ui/Badge';
import { cn } from '../../utils/cn';

export interface ChatMessageProps {
  id: string;
  user: {
    id: string;
    nickname: string;
    avatarUrl?: string;
  };
  content: string;
  type: 'TEXT' | 'IMAGE' | 'GIF' | 'VIDEO';
  mediaUrl?: string;
  mediaThumbUrl?: string;
  durationSec?: number;
  createdAt: string;
  isOwn?: boolean;
  isPending?: boolean;
}

const ChatMessage: React.FC<ChatMessageProps> = ({
  user,
  content,
  type,
  mediaUrl,
  mediaThumbUrl,
  durationSec,
  createdAt,
  isOwn = false,
  isPending = false
}) => {
  const formatTime = (dateString: string) => {
    return new Date(dateString).toLocaleTimeString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const renderMedia = () => {
    if (type === 'TEXT') return null;

    const mediaProps = {
      className: 'rounded-lg max-w-xs max-h-64 object-cover',
      alt: content || 'Media content'
    };

    switch (type) {
      case 'IMAGE':
        return (
          <img
            {...mediaProps}
            src={mediaUrl}
            loading="lazy"
          />
        );
      case 'GIF':
        return (
          <div className="relative">
            <img
              {...mediaProps}
              src={mediaUrl}
              loading="lazy"
            />
            <Badge variant="info" size="sm" className="absolute top-2 left-2">
              GIF
            </Badge>
          </div>
        );
      case 'VIDEO':
        return (
          <div className="relative">
            <video
              {...mediaProps}
              src={mediaUrl}
              poster={mediaThumbUrl}
              controls
              preload="metadata"
            />
            {durationSec && (
              <Badge variant="default" size="sm" className="absolute bottom-2 right-2">
                {Math.floor(durationSec / 60)}:{(durationSec % 60).toString().padStart(2, '0')}
              </Badge>
            )}
          </div>
        );
      default:
        return null;
    }
  };

  return (
    <div className={cn(
      // 화면 좌우 벽에 더 가깝게: 컨테이너 내부 좌우 패딩 최소화
      'flex gap-2 px-1 py-2 sm:gap-3 sm:px-3 sm:py-3 hover:bg-background-chat/30 transition-colors',
      isOwn && 'flex-row-reverse'
    )}>
      <Avatar
        src={user.avatarUrl}
        fallback={user.nickname}
        size="md"
        online={!isPending}
      />
      
      <div className={cn(
        // 가장자리 근접 배치를 위해 최대폭 상향
        'flex flex-col gap-2 max-w-[88%] sm:max-w-[80%]',
        isOwn && 'items-end'
      )}>
        <div className="flex items-center gap-2">
          <span className="text-sm font-medium text-foreground-primary">
            {user.nickname}
          </span>
          <span className="text-xs text-foreground-muted">
            {formatTime(createdAt)}
          </span>
          {isPending && (
            <Badge variant="warning" size="sm">
              전송 중...
            </Badge>
          )}
        </div>
        
        <div className={cn(
          // 말풍선 둥근 정도 축소, 반대편 마진 축소로 가장자리 근접
          'rounded-md px-3 py-2 bg-background-chat-message border border-border-default shadow-sm',
          isOwn ? 'bg-foreground-primary text-background-primary border-foreground-primary ml-2 sm:ml-4' : 'mr-2 sm:mr-4'
        )}>
          {type === 'TEXT' ? (
            <p className="text-sm whitespace-pre-wrap break-words">
              {content}
            </p>
          ) : (
            <div className="space-y-2">
              {content && (
                <p className="text-sm whitespace-pre-wrap break-words">
                  {content}
                </p>
              )}
              {renderMedia()}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export { ChatMessage };
