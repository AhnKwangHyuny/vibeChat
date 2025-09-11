import React, { useState } from 'react';
import { cn } from '../../utils/cn';

export interface MessageReaction {
  emoji: string;
  count: number;
  users: string[];
}

export interface MessageReactionsProps {
  reactions: MessageReaction[];
  onReactionAdd: (emoji: string) => void;
  onReactionRemove: (emoji: string) => void;
  currentUserId: string;
  className?: string;
}

const MessageReactions: React.FC<MessageReactionsProps> = ({
  reactions,
  onReactionAdd,
  onReactionRemove,
  currentUserId,
  className
}) => {
  const [hoveredReaction, setHoveredReaction] = useState<string | null>(null);

  const handleReactionClick = (emoji: string) => {
    const reaction = reactions.find(r => r.emoji === emoji);
    if (reaction && reaction.users.includes(currentUserId)) {
      onReactionRemove(emoji);
    } else {
      onReactionAdd(emoji);
    }
  };

  if (reactions.length === 0) return null;

  return (
    <div className={cn("flex flex-wrap gap-1 mt-2", className)}>
      {reactions.map((reaction) => {
        const hasUserReacted = reaction.users.includes(currentUserId);
        
        return (
          <div
            key={reaction.emoji}
            className="relative"
            onMouseEnter={() => setHoveredReaction(reaction.emoji)}
            onMouseLeave={() => setHoveredReaction(null)}
          >
            <button
              onClick={() => handleReactionClick(reaction.emoji)}
              className={cn(
                "inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs transition-all duration-200",
                hasUserReacted
                  ? "bg-primary-500/20 border border-primary-500/30 text-primary-600 dark:text-primary-400"
                  : "bg-background-tertiary border border-border-default text-foreground-muted hover:text-foreground-primary hover:border-border-hover"
              )}
            >
              <span className="text-sm">{reaction.emoji}</span>
              <span className="font-medium">{reaction.count}</span>
            </button>

            {/* Tooltip showing who reacted */}
            {hoveredReaction === reaction.emoji && (
              <div className="absolute bottom-full left-1/2 transform -translate-x-1/2 mb-2 px-2 py-1 bg-background-primary border border-border-default rounded shadow-lg text-xs text-foreground-primary whitespace-nowrap z-10">
                {reaction.users.length === 1 
                  ? `${reaction.users[0]}님이 반응했습니다`
                  : reaction.users.length <= 3
                    ? `${reaction.users.join(', ')}님이 반응했습니다`
                    : `${reaction.users.slice(0, 2).join(', ')} 외 ${reaction.users.length - 2}명이 반응했습니다`
                }
                <div className="absolute top-full left-1/2 transform -translate-x-1/2 w-0 h-0 border-l-2 border-r-2 border-t-2 border-transparent border-t-border-default"></div>
              </div>
            )}
          </div>
        );
      })}

      {/* Add Reaction Button */}
      <button
        onClick={() => onReactionAdd('👍')} // Default reaction
        className="inline-flex items-center justify-center w-8 h-8 rounded-full text-xs transition-colors bg-background-tertiary border border-border-default text-foreground-muted hover:text-foreground-primary hover:border-border-hover"
        title="반응 추가"
      >
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
        </svg>
      </button>
    </div>
  );
};

export default MessageReactions;
