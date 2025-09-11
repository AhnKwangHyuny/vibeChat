import React, { useState, useRef, useEffect } from 'react';
import { cn } from '../../utils/cn';
import { Button } from './Button';

export interface SearchBarProps {
  placeholder: string;
  onSearch: (query: string) => void;
  suggestions?: string[];
  showFilters?: boolean;
  value?: string;
  className?: string;
  onFilterClick?: () => void;
  filters?: Array<{
    id: string;
    label: string;
    active: boolean;
  }>;
}

const SearchBar: React.FC<SearchBarProps> = ({
  placeholder,
  onSearch,
  suggestions = [],
  showFilters = false,
  value = '',
  className,
  onFilterClick,
  filters = []
}) => {
  const [searchValue, setSearchValue] = useState(value);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [highlightedIndex, setHighlightedIndex] = useState(-1);
  const inputRef = useRef<HTMLInputElement>(null);
  const suggestionsRef = useRef<HTMLDivElement>(null);

  const filteredSuggestions = suggestions.filter(suggestion =>
    suggestion.toLowerCase().includes(searchValue.toLowerCase())
  );

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        inputRef.current &&
        !inputRef.current.contains(event.target as Node) &&
        suggestionsRef.current &&
        !suggestionsRef.current.contains(event.target as Node)
      ) {
        setShowSuggestions(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSearch = (query: string = searchValue) => {
    onSearch(query);
    setShowSuggestions(false);
    setHighlightedIndex(-1);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      if (highlightedIndex >= 0 && filteredSuggestions[highlightedIndex]) {
        handleSearch(filteredSuggestions[highlightedIndex]);
        setSearchValue(filteredSuggestions[highlightedIndex]);
      } else {
        handleSearch();
      }
    } else if (e.key === 'ArrowDown') {
      e.preventDefault();
      setHighlightedIndex(prev => 
        prev < filteredSuggestions.length - 1 ? prev + 1 : prev
      );
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setHighlightedIndex(prev => prev > 0 ? prev - 1 : -1);
    } else if (e.key === 'Escape') {
      setShowSuggestions(false);
      setHighlightedIndex(-1);
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newValue = e.target.value;
    setSearchValue(newValue);
    setShowSuggestions(newValue.length > 0 && filteredSuggestions.length > 0);
    setHighlightedIndex(-1);
  };

  const handleSuggestionClick = (suggestion: string) => {
    setSearchValue(suggestion);
    handleSearch(suggestion);
  };

  const clearSearch = () => {
    setSearchValue('');
    setShowSuggestions(false);
    onSearch('');
    inputRef.current?.focus();
  };

  return (
    <div className={cn("relative", className)}>
      {/* Main Search Bar */}
      <div className="relative">
        <div className="relative flex items-center">
          {/* Search Icon */}
          <div className="absolute left-3 pointer-events-none">
            <svg className="w-4 h-4 text-foreground-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>

          {/* Input */}
          <input
            ref={inputRef}
            type="text"
            value={searchValue}
            onChange={handleInputChange}
            onKeyDown={handleKeyDown}
            onFocus={() => setShowSuggestions(searchValue.length > 0 && filteredSuggestions.length > 0)}
            placeholder={placeholder}
            className="w-full pl-10 pr-20 py-2 bg-background-secondary border border-border-default rounded-lg text-foreground-primary placeholder-foreground-muted focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500 transition-colors"
          />

          {/* Clear Button */}
          {searchValue && (
            <button
              onClick={clearSearch}
              className="absolute right-12 p-1 text-foreground-muted hover:text-foreground-primary transition-colors"
              title="Clear search"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          )}

          {/* Search Button */}
          <Button
            onClick={() => handleSearch()}
            variant="primary"
            size="sm"
            className="absolute right-1 top-1 bottom-1"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </Button>
        </div>

        {/* Suggestions Dropdown */}
        {showSuggestions && filteredSuggestions.length > 0 && (
          <div
            ref={suggestionsRef}
            className="absolute top-full left-0 right-0 mt-1 bg-background-secondary border border-border-default rounded-lg shadow-lg z-50 max-h-60 overflow-y-auto"
          >
            <div className="py-1">
              {filteredSuggestions.map((suggestion, index) => (
                <button
                  key={suggestion}
                  onClick={() => handleSuggestionClick(suggestion)}
                  className={cn(
                    "w-full text-left px-3 py-2 text-sm transition-colors",
                    index === highlightedIndex
                      ? "bg-primary-500/20 text-primary-600 dark:text-primary-400"
                      : "text-foreground-primary hover:bg-background-tertiary"
                  )}
                >
                  <div className="flex items-center gap-2">
                    <svg className="w-3 h-3 text-foreground-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                    </svg>
                    <span>{suggestion}</span>
                  </div>
                </button>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Filters */}
      {showFilters && filters.length > 0 && (
        <div className="flex items-center gap-2 mt-3">
          <span className="text-xs text-foreground-muted">필터:</span>
          <div className="flex gap-1 flex-wrap">
            {filters.map((filter) => (
              <button
                key={filter.id}
                onClick={onFilterClick}
                className={cn(
                  "px-2 py-1 text-xs rounded-md transition-colors",
                  filter.active
                    ? "bg-primary-500 text-foreground-primary"
                    : "bg-background-tertiary text-foreground-muted hover:text-foreground-primary hover:bg-background-tertiary"
                )}
              >
                {filter.label}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default SearchBar;
