import React, { useState, useRef, useEffect } from 'react';
import { cn } from '../../utils/cn';

export interface MessageActionsProps {
  messageId: string;
  onReply: () => void;
  onEdit: () => void;
  onDelete: () => void;
  onReport: () => void;
  onReaction: () => void;
  isOwn: boolean;
  className?: string;
  active?: boolean;
}

const MessageActions: React.FC<MessageActionsProps> = ({
  messageId,
  onReply,
  onEdit,
  onDelete,
  onReport,
  onReaction,
  isOwn,
  className,
  active = false
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleAction = (action: () => void) => {
    action();
    setIsOpen(false);
  };

  return (
    <div className={cn("relative", className)} ref={menuRef}>
      {/* Trigger Button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className={cn(
          'p-2 rounded-full bg-background-primary border border-border-default shadow-sm text-foreground-muted hover:text-foreground-primary hover:bg-background-secondary transition-all duration-200',
          active ? 'opacity-100' : 'opacity-0'
        )}
        title="메시지 옵션"
      >
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z" />
        </svg>
      </button>

      {/* Quick Reactions (Always Visible on Hover) */}
      <div className={cn('absolute top-0 right-8 flex items-center gap-1 bg-background-primary border border-border-default rounded-lg px-2 py-1 shadow-lg transition-all duration-200', active ? 'opacity-100' : 'opacity-0') }>
        {['👍', '❤️', '😂', '😮', '😢', '😡'].map((emoji) => (
          <button
            key={emoji}
            onClick={() => onReaction()}
            className="p-1 rounded hover:bg-background-tertiary transition-colors text-sm"
            title={`${emoji} 반응하기`}
          >
            {emoji}
          </button>
        ))}
      </div>

      {/* Dropdown Menu */}
      {isOpen && (
        <div className="absolute top-full right-0 mt-1 w-48 bg-background-secondary border border-border-default rounded-lg shadow-lg z-50 overflow-hidden">
          <div className="py-1">
            {/* Reply */}
            <button
              onClick={() => handleAction(onReply)}
              className="w-full flex items-center gap-3 px-3 py-2 text-sm text-foreground-primary hover:bg-background-tertiary transition-colors"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6" />
              </svg>
              답글
            </button>

            {/* Reaction */}
            <button
              onClick={() => handleAction(onReaction)}
              className="w-full flex items-center gap-3 px-3 py-2 text-sm text-foreground-primary hover:bg-background-tertiary transition-colors"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.828 14.828a4 4 0 01-5.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              반응하기
            </button>

            <div className="border-t border-border-default my-1"></div>

            {/* Owner Actions */}
            {isOwn && (
              <>
                <button
                  onClick={() => handleAction(onEdit)}
                  className="w-full flex items-center gap-3 px-3 py-2 text-sm text-foreground-primary hover:bg-background-tertiary transition-colors"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                  </svg>
                  수정
                </button>

                <button
                  onClick={() => handleAction(onDelete)}
                  className="w-full flex items-center gap-3 px-3 py-2 text-sm text-semantic-error hover:bg-semantic-error/10 transition-colors"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                  삭제
                </button>

                <div className="border-t border-border-default my-1"></div>
              </>
            )}

            {/* Report (for non-own messages) */}
            {!isOwn && (
              <button
                onClick={() => handleAction(onReport)}
                className="w-full flex items-center gap-3 px-3 py-2 text-sm text-semantic-warning hover:bg-semantic-warning/10 transition-colors"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.732-.833-2.464 0L4.35 16.5c-.77.833.192 2.5 1.732 2.5z" />
                </svg>
                신고
              </button>
            )}

            {/* Copy Message ID (for debugging) */}
            <button
              onClick={() => {
                navigator.clipboard.writeText(messageId);
                handleAction(() => {});
              }}
              className="w-full flex items-center gap-3 px-3 py-2 text-sm text-foreground-muted hover:bg-background-tertiary transition-colors"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
              </svg>
              메시지 ID 복사
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default MessageActions;
