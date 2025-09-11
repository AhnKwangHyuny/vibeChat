import React, { useState, useEffect, useRef } from 'react';
import { cn } from '../../utils/cn';
import { Button } from '../ui/Button';
import { ChatMessage } from './ChatMessage';
import { MessageInput } from './MessageInput';

export interface Message {
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

export interface ThreadViewProps {
  parentMessage: Message;
  replies: Message[];
  onReply: (content: string) => void;
  onClose: () => void;
  currentUserId: string;
  className?: string;
}

const ThreadView: React.FC<ThreadViewProps> = ({
  parentMessage,
  replies,
  onReply,
  onClose,
  currentUserId,
  className
}) => {
  const [replyContent, setReplyContent] = useState('');
  const repliesEndRef = useRef<HTMLDivElement>(null);

  // Auto scroll to bottom when new replies are added
  useEffect(() => {
    if (repliesEndRef.current) {
      repliesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [replies]);

  const handleSendReply = () => {
    if (!replyContent.trim()) return;
    onReply(replyContent);
    setReplyContent('');
  };

  const handleFileUpload = (file: File) => {
    // Handle file upload for replies
    console.log('File upload in thread:', file);
  };

  return (
    <div className={cn(
      "fixed inset-y-0 right-0 w-96 bg-background-primary border-l border-border-default shadow-xl z-50 flex flex-col",
      className
    )}>
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b border-border-default bg-background-secondary">
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2">
            <svg className="w-5 h-5 text-foreground-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6" />
            </svg>
            <h3 className="text-sm font-medium text-foreground-primary">스레드</h3>
          </div>
          <div className="text-xs text-foreground-muted">
            {replies.length}개의 답글
          </div>
        </div>
        
        <Button
          variant="ghost"
          size="sm"
          onClick={onClose}
          className="p-1"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </Button>
      </div>

      {/* Parent Message */}
      <div className="p-4 border-b border-border-default bg-background-secondary/50">
        <div className="text-xs text-foreground-muted mb-2 flex items-center gap-1">
          <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 8h10M7 12h4m1 8l-4-4H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-3l-4 4z" />
          </svg>
          원본 메시지
        </div>
        <ChatMessage
          message={parentMessage}
          isOwn={parentMessage.user.id.toString() === currentUserId}
          isPending={false}
          showThread={false}
        />
      </div>

      {/* Replies */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3">
        {replies.length === 0 ? (
          <div className="text-center py-8">
            <div className="w-12 h-12 mx-auto mb-3 bg-background-tertiary rounded-full flex items-center justify-center">
              <svg className="w-6 h-6 text-foreground-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6" />
              </svg>
            </div>
            <p className="text-sm text-foreground-muted mb-2">아직 답글이 없습니다</p>
            <p className="text-xs text-foreground-muted">첫 번째 답글을 작성해보세요!</p>
          </div>
        ) : (
          <>
            {replies.map((reply) => (
              <div key={reply.id || reply.clientTempId} className="relative">
                {/* Thread indicator line */}
                <div className="absolute left-4 top-0 bottom-0 w-px bg-border-default"></div>
                
                <div className="pl-8">
                  <ChatMessage
                    message={reply}
                    isOwn={reply.user.id.toString() === currentUserId}
                    isPending={!!reply.clientTempId}
                    showThread={false}
                    compact={true}
                  />
                </div>
              </div>
            ))}
            <div ref={repliesEndRef} />
          </>
        )}
      </div>

      {/* Reply Input */}
      <div className="border-t border-border-default bg-background-secondary p-4">
        <div className="text-xs text-foreground-muted mb-2 flex items-center gap-1">
          <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6" />
          </svg>
          스레드에 답글 작성
        </div>
        
        <MessageInput
          onSendMessage={handleSendReply}
          onFileUpload={handleFileUpload}
          placeholder={`${parentMessage.user.nickname}님에게 답글...`}
          disabled={false}
          compact={true}
        />
      </div>
    </div>
  );
};

export default ThreadView;
