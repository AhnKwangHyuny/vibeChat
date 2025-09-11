import React, { useState } from 'react';
import { cn } from '../../utils/cn';
import { Avatar } from '../ui/Avatar';

export interface User {
  id: string;
  nickname: string;
  status: 'online' | 'away' | 'offline';
  avatarUrl?: string;
  lastSeen?: string;
}

export interface UserListProps {
  users: User[];
  onUserClick?: (userId: string) => void;
  showStatus?: boolean;
  className?: string;
  title?: string;
}

const UserList: React.FC<UserListProps> = ({
  users,
  onUserClick,
  showStatus = true,
  className,
  title = "온라인 사용자"
}) => {
  const [filter, setFilter] = useState<'all' | 'online' | 'away' | 'offline'>('all');

  const filteredUsers = users.filter(user => {
    if (filter === 'all') return true;
    return user.status === filter;
  });

  const onlineUsers = users.filter(user => user.status === 'online');
  const awayUsers = users.filter(user => user.status === 'away');
  const offlineUsers = users.filter(user => user.status === 'offline');

  const getStatusColor = (status: User['status']) => {
    switch (status) {
      case 'online':
        return 'bg-semantic-success';
      case 'away':
        return 'bg-semantic-warning';
      case 'offline':
        return 'bg-semantic-neutral';
      default:
        return 'bg-semantic-neutral';
    }
  };

  const getStatusText = (status: User['status']) => {
    switch (status) {
      case 'online':
        return '온라인';
      case 'away':
        return '자리비움';
      case 'offline':
        return '오프라인';
      default:
        return '알 수 없음';
    }
  };

  return (
    <div className={cn("bg-background-secondary border border-border-default rounded-lg", className)}>
      {/* Header */}
      <div className="p-4 border-b border-border-default">
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-sm font-medium text-foreground-primary">{title}</h3>
          <span className="text-xs text-foreground-muted">
            {users.length}명
          </span>
        </div>

        {/* Status Counts */}
        <div className="flex items-center gap-4 text-xs">
          <div className="flex items-center gap-1">
            <div className="w-2 h-2 rounded-full bg-semantic-success"></div>
            <span className="text-foreground-muted">{onlineUsers.length}</span>
          </div>
          <div className="flex items-center gap-1">
            <div className="w-2 h-2 rounded-full bg-semantic-warning"></div>
            <span className="text-foreground-muted">{awayUsers.length}</span>
          </div>
          <div className="flex items-center gap-1">
            <div className="w-2 h-2 rounded-full bg-semantic-neutral"></div>
            <span className="text-foreground-muted">{offlineUsers.length}</span>
          </div>
        </div>

        {/* Filter Buttons */}
        <div className="flex gap-1 mt-3">
          {(['all', 'online', 'away', 'offline'] as const).map((status) => (
            <button
              key={status}
              onClick={() => setFilter(status)}
              className={cn(
                "px-2 py-1 text-xs rounded transition-colors",
                filter === status
                  ? "bg-primary-500 text-foreground-primary"
                  : "text-foreground-muted hover:text-foreground-primary hover:bg-background-tertiary"
              )}
            >
              {status === 'all' ? '전체' : getStatusText(status)}
            </button>
          ))}
        </div>
      </div>

      {/* User List */}
      <div className="max-h-64 overflow-y-auto">
        {filteredUsers.length === 0 ? (
          <div className="p-4 text-center text-foreground-muted text-sm">
            {filter === 'all' ? '사용자가 없습니다' : `${getStatusText(filter)} 사용자가 없습니다`}
          </div>
        ) : (
          <div className="p-2">
            {filteredUsers.map((user) => (
              <button
                key={user.id}
                onClick={() => onUserClick?.(user.id)}
                className="w-full flex items-center gap-3 p-2 rounded-lg hover:bg-background-tertiary transition-colors text-left"
              >
                <div className="relative">
                  <Avatar
                    src={user.avatarUrl}
                    fallback={user.nickname.charAt(0).toUpperCase()}
                    size="sm"
                  />
                  {showStatus && (
                    <div className={cn(
                      "absolute -bottom-0.5 -right-0.5 w-3 h-3 rounded-full border-2 border-background-secondary",
                      getStatusColor(user.status)
                    )} />
                  )}
                </div>

                <div className="flex-1 min-w-0">
                  <div className="text-sm font-medium text-foreground-primary truncate">
                    {user.nickname}
                  </div>
                  <div className="text-xs text-foreground-muted">
                    {user.status === 'offline' && user.lastSeen
                      ? `${user.lastSeen}에 마지막 접속`
                      : getStatusText(user.status)
                    }
                  </div>
                </div>

                {onUserClick && (
                  <div className="text-foreground-muted">
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                    </svg>
                  </div>
                )}
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default UserList;
