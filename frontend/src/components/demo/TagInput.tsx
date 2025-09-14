import React, { useState, useRef, useEffect, useMemo } from 'react';
import { Badge } from '../ui/Badge';
import { cn } from '../../utils/cn';

export interface TagInputProps {
  tags: string[];
  onTagsChange: (tags: string[]) => void;
  placeholder?: string;
  maxTags?: number;
  suggestions?: string[];
  onSuggestionSelect?: (tag: string) => void;
  className?: string;
}

const TagInput: React.FC<TagInputProps> = ({
  tags,
  onTagsChange,
  placeholder = "태그를 입력하세요...",
  maxTags = 5,
  suggestions = [],
  onSuggestionSelect,
  className
}) => {
  const [inputValue, setInputValue] = useState('');
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [isComposing, setIsComposing] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  // useMemo를 사용하여 필터링 로직 최적화
  const filteredSuggestions = useMemo(() => {
    if (!inputValue.trim()) return [];
    
    return suggestions.filter(
      suggestion => 
        suggestion.toLowerCase().includes(inputValue.toLowerCase()) &&
        !tags.includes(suggestion)
    );
  }, [inputValue, suggestions, tags]);

  // 필터링된 제안이 변경될 때만 showSuggestions 업데이트
  useEffect(() => {
    setShowSuggestions(filteredSuggestions.length > 0 && inputValue.trim() !== '');
  }, [filteredSuggestions, inputValue]);

  const addTag = (tag: string) => {
    const trimmedTag = tag.trim();
    if (trimmedTag && !tags.includes(trimmedTag) && tags.length < maxTags) {
      onTagsChange([...tags, trimmedTag]);
      setInputValue('');
    }
  };

  const removeTag = (tagToRemove: string) => {
    onTagsChange(tags.filter(tag => tag !== tagToRemove));
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    // 한글 입력 중일 때는 태그 추가하지 않음
    if (isComposing) return;
    
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault();
      addTag(inputValue);
    } else if (e.key === 'Backspace' && !inputValue && tags.length > 0) {
      removeTag(tags[tags.length - 1]);
    } else if (e.key === 'Escape') {
      setShowSuggestions(false);
    }
  };

  const handleCompositionStart = () => {
    setIsComposing(true);
  };

  const handleCompositionEnd = () => {
    setIsComposing(false);
  };

  const handleSuggestionClick = (suggestion: string) => {
    addTag(suggestion);
    onSuggestionSelect?.(suggestion);
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setInputValue(e.target.value);
  };

  const handleBlur = () => {
    // Delay hiding suggestions to allow clicking on them
    setTimeout(() => {
      if (inputValue.trim() === '') {
        setShowSuggestions(false);
      }
    }, 150);
  };

  return (
    <div className={cn('relative', className)}>
      <div className="min-h-[44px] border border-border-default rounded-base bg-background-tertiary p-2 flex flex-wrap gap-2 focus-within:border-border-focus focus-within:ring-2 focus-within:ring-border-focus">
        {/* Tags */}
        {tags.map((tag, index) => (
          <Badge
            key={index}
            variant="default"
            size="sm"
            className="flex items-center gap-1 pr-1"
          >
            {tag}
            <button
              onClick={() => removeTag(tag)}
              className="ml-1 hover:text-foreground-primary transition-colors"
            >
              <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </Badge>
        ))}

        {/* Input */}
        {tags.length < maxTags && (
          <input
            ref={inputRef}
            type="text"
            value={inputValue}
            onChange={handleInputChange}
            onKeyDown={handleKeyDown}
            onCompositionStart={handleCompositionStart}
            onCompositionEnd={handleCompositionEnd}
            onBlur={handleBlur}
            onFocus={() => {
              if (inputValue.trim() && filteredSuggestions.length > 0) {
                setShowSuggestions(true);
              }
            }}
            placeholder={tags.length === 0 ? placeholder : ''}
            className="flex-1 min-w-[120px] bg-transparent text-foreground-primary placeholder:text-foreground-muted focus:outline-none text-sm"
          />
        )}
      </div>

      {/* Suggestions */}
      {showSuggestions && filteredSuggestions.length > 0 && (
        <div className="absolute top-full left-0 right-0 mt-1 bg-background-secondary border border-border-default rounded-base shadow-lg z-10 max-h-48 overflow-y-auto">
          {filteredSuggestions.map((suggestion, index) => (
            <button
              key={index}
              onClick={() => handleSuggestionClick(suggestion)}
              className="w-full px-3 py-2 text-left text-sm text-foreground-primary hover:bg-background-tertiary transition-colors first:rounded-t-base last:rounded-b-base"
            >
              {suggestion}
            </button>
          ))}
        </div>
      )}

      {/* Tag Count */}
      <div className="mt-1 text-xs text-foreground-muted">
        {tags.length}/{maxTags} 태그
      </div>
    </div>
  );
};

export { TagInput };
