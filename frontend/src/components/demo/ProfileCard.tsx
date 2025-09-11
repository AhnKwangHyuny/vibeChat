import React, { useState } from 'react';
import { cn } from '../../utils/cn';
import { Avatar } from '../ui/Avatar';
import { Button } from '../ui/Button';
import { Badge } from '../ui/Badge';
import { Input } from '../ui/Input';
import { Card } from '../ui/Card';

export interface ProfileCardProps {
  user: {
    id: string;
    nickname: string;
    email?: string;
    avatarUrl?: string;
    bio?: string;
    joinedAt: string;
    status: 'online' | 'away' | 'offline';
    badges: string[];
    stats: {
      totalMessages: number;
      roomsJoined: number;
      friendsCount: number;
    };
  };
  onUpdateProfile?: (data: { nickname: string; bio: string }) => void;
  onChangeStatus?: (status: 'online' | 'away' | 'offline') => void;
  className?: string;
  editable?: boolean;
}

const ProfileCard: React.FC<ProfileCardProps> = ({
  user,
  onUpdateProfile,
  onChangeStatus,
  className,
  editable = true
}) => {
  const [isEditing, setIsEditing] = useState(false);
  const [editedNickname, setEditedNickname] = useState(user.nickname);
  const [editedBio, setEditedBio] = useState(user.bio || '');

  const handleSave = () => {
    onUpdateProfile?.({ nickname: editedNickname, bio: editedBio });
    setIsEditing(false);
  };

  const handleCancel = () => {
    setEditedNickname(user.nickname);
    setEditedBio(user.bio || '');
    setIsEditing(false);
  };

  const getStatusColor = (status: typeof user.status) => {
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

  const getStatusText = (status: typeof user.status) => {
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
    <Card className={cn("max-w-2xl mx-auto", className)}>
      {/* Header */}
      <div className="p-6 border-b border-border-default">
        <div className="flex flex-col sm:flex-row items-center sm:items-start gap-6">
          {/* Avatar */}
          <div className="relative">
            <Avatar
              src={user.avatarUrl}
              fallback={user.nickname.charAt(0).toUpperCase()}
              size="xl"
              className="w-24 h-24 sm:w-32 sm:h-32"
            />
            {/* Status Indicator */}
            <div className={cn(
              "absolute -bottom-1 -right-1 w-6 h-6 rounded-full border-4 border-background-secondary",
              getStatusColor(user.status)
            )} />
          </div>

          {/* User Info */}
          <div className="flex-1 text-center sm:text-left">
            {isEditing ? (
              <div className="space-y-4">
                <Input
                  value={editedNickname}
                  onChange={(e) => setEditedNickname(e.target.value)}
                  placeholder="닉네임"
                  className="text-lg font-semibold"
                />
                <textarea
                  value={editedBio}
                  onChange={(e) => setEditedBio(e.target.value)}
                  placeholder="자기소개를 입력하세요..."
                  className="w-full p-3 border border-border-default rounded-md bg-background-secondary text-foreground-primary placeholder-foreground-muted resize-none"
                  rows={3}
                />
                <div className="flex gap-2">
                  <Button onClick={handleSave} size="sm" variant="primary">
                    저장
                  </Button>
                  <Button onClick={handleCancel} size="sm" variant="secondary">
                    취소
                  </Button>
                </div>
              </div>
            ) : (
              <>
                <div className="flex items-center justify-center sm:justify-start gap-3 mb-2">
                  <h1 className="text-2xl font-bold text-foreground-primary">
                    {user.nickname}
                  </h1>
                  {editable && (
                    <Button
                      onClick={() => setIsEditing(true)}
                      variant="ghost"
                      size="sm"
                      className="p-1"
                    >
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                    </Button>
                  )}
                </div>

                {/* Status */}
                <div className="flex items-center justify-center sm:justify-start gap-2 mb-3">
                  <div className={cn("w-2 h-2 rounded-full", getStatusColor(user.status))} />
                  <span className="text-sm text-foreground-muted">
                    {getStatusText(user.status)}
                  </span>
                  {editable && onChangeStatus && (
                    <select
                      value={user.status}
                      onChange={(e) => onChangeStatus(e.target.value as any)}
                      className="ml-2 text-xs bg-background-secondary border border-border-default rounded px-2 py-1"
                    >
                      <option value="online">온라인</option>
                      <option value="away">자리비움</option>
                      <option value="offline">오프라인</option>
                    </select>
                  )}
                </div>

                {user.email && (
                  <p className="text-foreground-muted mb-3">{user.email}</p>
                )}

                {user.bio && (
                  <p className="text-foreground-secondary mb-4 max-w-md">
                    {user.bio}
                  </p>
                )}

                <p className="text-sm text-foreground-muted">
                  {new Date(user.joinedAt).toLocaleDateString('ko-KR')}에 가입
                </p>
              </>
            )}
          </div>
        </div>

        {/* Badges */}
        {user.badges.length > 0 && (
          <div className="flex flex-wrap gap-2 justify-center sm:justify-start mt-4">
            {user.badges.map((badge, index) => (
              <Badge key={index} variant="secondary" size="sm">
                {badge}
              </Badge>
            ))}
          </div>
        )}
      </div>

      {/* Stats */}
      <div className="p-6">
        <h3 className="text-lg font-semibold text-foreground-primary mb-4">활동 통계</h3>
        <div className="grid grid-cols-3 gap-4">
          <div className="text-center">
            <div className="text-2xl font-bold text-foreground-primary mb-1">
              {user.stats.totalMessages.toLocaleString()}
            </div>
            <div className="text-sm text-foreground-muted">총 메시지</div>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold text-foreground-primary mb-1">
              {user.stats.roomsJoined}
            </div>
            <div className="text-sm text-foreground-muted">참여 방</div>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold text-foreground-primary mb-1">
              {user.stats.friendsCount}
            </div>
            <div className="text-sm text-foreground-muted">친구</div>
          </div>
        </div>
      </div>
    </Card>
  );
};

export default ProfileCard;
