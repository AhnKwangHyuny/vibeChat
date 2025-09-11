import React, { useState } from 'react';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { cn } from '../../utils/cn';

export interface MessageInputProps {
  onSendMessage: (content: string) => void;
  onFileUpload?: (file: File) => void;
  onEmojiClick?: () => void;
  placeholder?: string;
  disabled?: boolean;
  isTyping?: boolean;
  onTypingChange?: (typing: boolean) => void;
  className?: string;
  compact?: boolean;
}

const MessageInput: React.FC<MessageInputProps> = ({
  onSendMessage,
  onFileUpload,
  onEmojiClick,
  placeholder = "메시지를 입력하세요...",
  disabled = false,
  isTyping = false,
  onTypingChange,
  className,
  compact = false
}) => {
  const [message, setMessage] = useState('');
  const [isComposing, setIsComposing] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (message.trim() && !disabled) {
      onSendMessage(message.trim());
      setMessage('');
      onTypingChange?.(false);
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setMessage(value);
    
    if (value.trim() && !isComposing) {
      onTypingChange?.(true);
    } else if (!value.trim()) {
      onTypingChange?.(false);
    }
  };

  const handleCompositionStart = () => {
    setIsComposing(true);
  };

  const handleCompositionEnd = () => {
    setIsComposing(false);
    if (message.trim()) {
      onTypingChange?.(true);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file && onFileUpload) {
      onFileUpload(file);
    }
    // Reset input
    e.target.value = '';
  };

  return (
    <div className={cn(
      'bg-background-secondary border-t border-border-default',
      compact ? 'px-2 py-2' : 'px-2 sm:px-4 py-3',
      className
    )}>
      <form onSubmit={handleSubmit} className="flex items-center gap-1.5 sm:gap-2">
        {/* File Upload Button */}
        <div className="relative order-1 sm:order-none ml-0">
          <input
            type="file"
            accept="image/*,video/*,.gif"
            onChange={handleFileChange}
            className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
            disabled={disabled}
          />
          <Button
            type="button"
            variant="ghost"
            size="sm"
            disabled={disabled}
            className="p-1.5 sm:p-2 text-foreground-muted hover:text-foreground-primary"
            title="파일 첨부"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13" />
            </svg>
          </Button>
        </div>

        {/* Emoji Button */}
        {onEmojiClick && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={onEmojiClick}
            disabled={disabled}
            className="p-1.5 sm:p-2 text-foreground-muted hover:text-foreground-primary order-2"
            title="이모지"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.828 14.828a4 4 0 01-5.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </Button>
        )}

        {/* Message Input */}
        <div className="flex-1 relative order-3 sm:order-none">
          <Input
            value={message}
            onChange={handleInputChange}
            onCompositionStart={handleCompositionStart}
            onCompositionEnd={handleCompositionEnd}
            placeholder={placeholder}
            disabled={disabled}
            className="w-full h-[42px] rounded-md bg-background-tertiary border-border-focus px-5 sm:px-6 text-sm"
            onKeyDown={(e) => {
              // Prevent submitting while composing (IME), e.g., Korean/Japanese/Chinese
              const isNativeComposing = (e as unknown as any).nativeEvent?.isComposing;
              if (isNativeComposing || isComposing) {
                return;
              }
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                handleSubmit(e);
              }
            }}
          />
        </div>

        {/* Send Button */}
        <Button
          type="submit"
          disabled={!message.trim() || disabled}
          size={"sm"}
          className="px-3 sm:px-5 h-[42px] ml-auto order-4 sm:order-none mr-0"
        >
          <span className="hidden sm:inline">전송</span>
          <span className="sm:hidden">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
            </svg>
          </span>
        </Button>
      </form>

      {/* Typing Indicator */}
      {isTyping && (
        <div className="mt-2 text-xs text-foreground-muted animate-pulse">
          입력 중...
        </div>
      )}
    </div>
  );
};

export { MessageInput };
