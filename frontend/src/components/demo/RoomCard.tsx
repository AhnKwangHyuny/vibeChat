import React from 'react';
import { Badge } from '../ui/Badge';
import { Button } from '../ui/Button';
import { cn } from '../../utils/cn';

export interface RoomCardProps {
  id: string;
  title: string;
  description?: string;
  tags: string[];
  isPrivate: boolean;
  participantsCount: number;
  lastMessageAt?: string;
  onJoin?: (roomId: string) => void;
  className?: string;
}

const RoomCard: React.FC<RoomCardProps> = ({
  id,
  title,
  description,
  tags,
  isPrivate,
  participantsCount,
  lastMessageAt,
  onJoin,
  className
}) => {
  const formatLastMessage = (dateString?: string) => {
    if (!dateString) return '메시지 없음';
    
    const date = new Date(dateString);
    const now = new Date();
    const diffInHours = (now.getTime() - date.getTime()) / (1000 * 60 * 60);
    
    if (diffInHours < 1) {
      return '방금 전';
    } else if (diffInHours < 24) {
      return `${Math.floor(diffInHours)}시간 전`;
    } else {
      return `${Math.floor(diffInHours / 24)}일 전`;
    }
  };

  return (
    <div className={cn(
      'bg-background-secondary rounded-lg border border-border-default p-6 hover:border-border-focus transition-all duration-200 group',
      className
    )}>
      <div className="flex items-start justify-between mb-4">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-2">
            <h3 className="text-lg font-medium text-foreground-primary truncate">
              {title}
            </h3>
            {isPrivate && (
              <Badge variant="warning" size="sm">
                비공개
              </Badge>
            )}
          </div>
          
          {description && (
            <p className="text-sm text-foreground-muted mb-3 line-clamp-2">
              {description}
            </p>
          )}
        </div>
      </div>

      <div className="space-y-3">
        {/* Tags */}
        <div className="flex flex-wrap gap-2">
          {tags.map((tag, index) => (
            <Badge key={index} variant="default" size="sm">
              #{tag}
            </Badge>
          ))}
        </div>

        {/* Stats */}
        <div className="flex items-center justify-between text-sm text-foreground-muted">
          <div className="flex items-center gap-4">
            <span className="flex items-center gap-1">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
              {participantsCount}명 참여
            </span>
            <span>
              {formatLastMessage(lastMessageAt)}
            </span>
          </div>
        </div>

        {/* Join Button */}
        <Button
          onClick={() => onJoin?.(id)}
          className="w-full group-hover:bg-foreground-primary group-hover:text-background-primary"
          variant="secondary"
        >
          참여하기
        </Button>
      </div>
    </div>
  );
};

export { RoomCard };
