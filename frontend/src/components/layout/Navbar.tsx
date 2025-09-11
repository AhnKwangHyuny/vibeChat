import React, { useState } from 'react';
import { Button } from '../ui/Button';
import { Avatar } from '../ui/Avatar';
import { cn } from '../../utils/cn';
import { useDarkMode } from '../../hooks/useDarkMode';
import NotificationBadge from '../ui/NotificationBadge';

export interface NavbarProps {
  user?: {
    id: string;
    nickname: string;
    avatarUrl?: string;
  };
  onLogin?: () => void;
  onLogout?: () => void;
  onProfileClick?: () => void;
  className?: string;
}

const Navbar: React.FC<NavbarProps> = ({
  user,
  onLogin,
  onLogout,
  onProfileClick,
  className
}) => {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const { isDarkMode, toggleDarkMode } = useDarkMode();

  return (
    <nav className={cn(
      'sticky top-0 z-50 bg-background-secondary/80 backdrop-blur-sm border-b border-border-default',
      className
    )}>
      <div className="w-full px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* Logo */}
          <div className="flex items-center">
            <a href="/" className="flex items-center space-x-2">
              <div className="w-8 h-8 bg-gradient-to-br from-primary-500 to-primary-700 rounded-lg flex items-center justify-center">
                <svg className="w-5 h-5 text-foreground-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                </svg>
              </div>
              <span className="text-xl font-semibold text-foreground-primary">
                VibeChat
              </span>
            </a>
          </div>

          {/* Desktop Navigation */}
          <div className="hidden lg:flex items-center space-x-8">
            <a
              href="/"
              className="text-foreground-secondary hover:text-foreground-primary transition-colors duration-200"
            >
              홈
            </a>
            <a
              href="/components"
              className="text-foreground-secondary hover:text-foreground-primary transition-colors duration-200"
            >
              컴포넌트
            </a>
            <a
              href="/create"
              className="text-foreground-secondary hover:text-foreground-primary transition-colors duration-200"
            >
              방 만들기
            </a>
            <a
              href="/roomList"
              className="text-foreground-secondary hover:text-foreground-primary transition-colors duration-200"
            >
              내가 들어간 방
            </a>
            <a
              href="/profile"
              className="text-foreground-secondary hover:text-foreground-primary transition-colors duration-200"
            >
              프로필
            </a>
          </div>

          {/* User Section */}
          <div className="flex items-center space-x-4">
            {/* Dark Mode Toggle */}
            <button
              onClick={toggleDarkMode}
              className="p-2 text-foreground-secondary hover:text-foreground-primary transition-colors rounded-lg hover:bg-background-tertiary"
              aria-label="Toggle dark mode"
            >
              {isDarkMode ? (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
                </svg>
              ) : (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
                </svg>
              )}
            </button>

            {user ? (
              <div className="flex items-center space-x-3">
                <span className="text-sm text-foreground-secondary hidden sm:block">
                  안녕하세요, {user.nickname}님
                </span>
                <div className="relative group">
                  <button
                    onClick={onProfileClick}
                    className="flex items-center space-x-2 hover:opacity-80 transition-opacity"
                  >
                    <Avatar
                      src={user.avatarUrl}
                      fallback={user.nickname}
                      size="md"
                    />
                    <svg className="w-4 h-4 text-foreground-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                    </svg>
                  </button>
                  
                  {/* Dropdown Menu */}
                  <div className="absolute right-0 mt-2 w-48 bg-background-secondary border border-border-default rounded-lg shadow-lg opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200">
                    <div className="py-1">
                      <button
                        onClick={onProfileClick}
                        className="w-full text-left px-4 py-2 text-sm text-foreground-primary hover:bg-background-tertiary transition-colors"
                      >
                        프로필 설정
                      </button>
                      <button
                        onClick={onLogout}
                        className="w-full text-left px-4 py-2 text-sm text-foreground-primary hover:bg-background-tertiary transition-colors"
                      >
                        로그아웃
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            ) : (
              <Button
                onClick={onLogin}
                variant="primary"
                size="sm"
              >
                로그인
              </Button>
            )}

            {/* Mobile Menu Button */}
            <button
              onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
              className="lg:hidden p-2 text-foreground-secondary hover:text-foreground-primary transition-colors"
            >
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                {isMobileMenuOpen ? (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                ) : (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                )}
              </svg>
            </button>
          </div>
        </div>

        {/* Mobile Menu */}
        {isMobileMenuOpen && (
          <div className="lg:hidden border-t border-border-default py-4">
            <div className="flex flex-col space-y-4">
              <a
                href="/"
                className="text-foreground-secondary hover:text-foreground-primary transition-colors duration-200"
                onClick={() => setIsMobileMenuOpen(false)}
              >
                홈
              </a>
              <a
                href="/components"
                className="text-foreground-secondary hover:text-foreground-primary transition-colors duration-200"
                onClick={() => setIsMobileMenuOpen(false)}
              >
                컴포넌트
              </a>
              <a
                href="/create"
                className="text-foreground-secondary hover:text-foreground-primary transition-colors duration-200"
                onClick={() => setIsMobileMenuOpen(false)}
              >
                방 만들기
              </a>
              <a
                href="/roomList"
                className="text-foreground-secondary hover:text-foreground-primary transition-colors duration-200"
                onClick={() => setIsMobileMenuOpen(false)}
              >
                내가 들어간 방
              </a>
              <a
                href="/profile"
                className="text-foreground-secondary hover:text-foreground-primary transition-colors duration-200"
                onClick={() => setIsMobileMenuOpen(false)}
              >
                프로필
              </a>
              {user && (
                <div className="pt-4 border-t border-border-default">
                  <button
                    onClick={() => {
                      onProfileClick?.();
                      setIsMobileMenuOpen(false);
                    }}
                    className="w-full text-left text-foreground-secondary hover:text-foreground-primary transition-colors duration-200"
                  >
                    프로필 설정
                  </button>
                  <button
                    onClick={() => {
                      onLogout?.();
                      setIsMobileMenuOpen(false);
                    }}
                    className="w-full text-left text-foreground-secondary hover:text-foreground-primary transition-colors duration-200 mt-2"
                  >
                    로그아웃
                  </button>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </nav>
  );
};

export { Navbar };
