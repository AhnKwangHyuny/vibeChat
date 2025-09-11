import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';

// Import components from our component library
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { Input } from '../components/ui/Input';
import { Badge } from '../components/ui/Badge';
import { Spinner } from '../components/ui/Spinner';
import { Modal } from '../components/ui/Modal';
import { TagInput } from '../components/demo/TagInput';
import { Navbar } from '../components/layout/Navbar';
import { cn } from '../utils/cn';

const createRoomSchema = z.object({
  title: z.string().min(1, "Title is required").max(80, "Title cannot exceed 80 characters"),
  description: z.string().max(255, "Description cannot exceed 255 characters").optional(),
  isPrivate: z.boolean(),
  tags: z.array(z.string().min(1, "Tag cannot be empty")).min(1, "At least one tag is required").max(5, "Cannot exceed 5 tags"),
});

type CreateRoomFormInputs = z.infer<typeof createRoomSchema>;

export default function CreateRoom() {
  const navigate = useNavigate();
  const { register, handleSubmit, setValue, watch, formState: { errors } } = useForm<CreateRoomFormInputs>({
    resolver: zodResolver(createRoomSchema),
    defaultValues: { isPrivate: false, tags: [] },
  });

  const [isCreating, setIsCreating] = useState(false);
  const [showPreview, setShowPreview] = useState(false);
  const [isLoggedIn, setIsLoggedIn] = useState(true);
  const [user, setUser] = useState({ id: '1', nickname: 'John Doe', avatarUrl: '' });

  // Mock autocomplete data
  const mockAutocompleteResults = [
    { name: "react", popularity: 95 },
    { name: "typescript", popularity: 88 },
    { name: "javascript", popularity: 92 },
    { name: "design", popularity: 75 },
    { name: "ui", popularity: 80 },
    { name: "programming", popularity: 85 },
    { name: "web", popularity: 90 },
    { name: "development", popularity: 87 }
  ];

  const onSubmit = async (data: CreateRoomFormInputs) => {
    setIsCreating(true);
    
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 2000));
    
    const newRoom = {
      id: Date.now(),
      ...data,
      participantsCount: 1,
      createdAt: new Date().toISOString()
    };
    
    toast.success(`Room '${newRoom.title}' created successfully!`);
    navigate(`/rooms/${newRoom.id}`);
  };

  const handleLogin = () => {
    setIsLoggedIn(true);
    setUser({ id: '1', nickname: 'John Doe', avatarUrl: '' });
    toast.success("Successfully logged in!");
  };

  const handleLogout = () => {
    setIsLoggedIn(false);
    setUser({ id: '1', nickname: 'Guest User', avatarUrl: '' });
    toast.success("Successfully logged out!");
  };

  const currentTags = watch('tags') || [];
  const formData = watch();

  return (
    <div className="min-h-screen bg-background-primary">
      <Navbar 
        user={isLoggedIn ? user : undefined}
        onLogin={handleLogin}
        onLogout={handleLogout}
      />

      <main className="container mx-auto px-4 py-8">
        <div className="max-w-2xl mx-auto">
          {/* Header */}
          <div className="text-center mb-8">
            <h1 className="text-3xl font-bold text-foreground-primary mb-4">
              새 채팅방 만들기
            </h1>
            <p className="text-lg text-foreground-muted">
              대화를 시작하고 같은 관심사를 가진 사람들과 연결하세요
            </p>
          </div>

          {/* Form */}
          <Card className="p-8">
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
              {/* Room Title */}
              <div>
                <label htmlFor="title" className="block text-sm font-medium text-foreground-primary mb-2">
                  방 제목 *
                </label>
                <Input
                  id="title"
                  type="text"
                  placeholder="방 제목을 입력하세요..."
                  {...register("title")}
                  error={errors.title?.message}
                />
                <p className="text-xs text-foreground-muted mt-1">
                  방에 대한 명확하고 설명적인 제목을 선택하세요
                </p>
              </div>

              {/* Room Description */}
              <div>
                <label htmlFor="description" className="block text-sm font-medium text-foreground-primary mb-2">
                  설명 (선택사항)
                </label>
                <Input
                  id="description"
                  type="text"
                  placeholder="이 방이 무엇에 관한 것인지 설명하세요..."
                  {...register("description")}
                  error={errors.description?.message}
                />
                <p className="text-xs text-foreground-muted mt-1">
                  사람들이 방의 목적을 이해할 수 있도록 도와주세요
                </p>
              </div>

              {/* Privacy Setting */}
              <div>
                <label className="block text-sm font-medium text-foreground-primary mb-3">
                  방 공개 설정
                </label>
                <div className="space-y-3">
                  <label className="flex items-center p-4 border border-border-default rounded-lg cursor-pointer hover:bg-background-tertiary/50 transition-colors">
                    <input
                      type="radio"
                      value="false"
                      {...register("isPrivate")}
                      className="mr-3"
                    />
                    <div>
                      <div className="font-medium text-foreground-primary">공개 방</div>
                      <div className="text-sm text-foreground-muted">
                        누구나 이 방을 찾아서 참여할 수 있습니다
                      </div>
                    </div>
                  </label>
                  
                  <label className="flex items-center p-4 border border-border-default rounded-lg cursor-pointer hover:bg-background-tertiary/50 transition-colors">
                    <input
                      type="radio"
                      value="true"
                      {...register("isPrivate")}
                      className="mr-3"
                    />
                    <div>
                      <div className="font-medium text-foreground-primary">비공개 방</div>
                      <div className="text-sm text-foreground-muted">
                        초대 코드가 있는 사람만 참여할 수 있습니다
                      </div>
                    </div>
                  </label>
                </div>
              </div>

              {/* Tags */}
              <div>
                <label className="block text-sm font-medium text-foreground-primary mb-2">
                  태그 *
                </label>
                <TagInput
                  tags={currentTags}
                  onTagsChange={(tags) => setValue('tags', tags)}
                  placeholder="사람들이 방을 찾을 수 있도록 태그를 추가하세요..."
                  maxTags={5}
                />
                {errors.tags && (
                  <p className="text-sm text-semantic-error mt-2">{errors.tags.message}</p>
                )}
                <p className="text-xs text-foreground-muted mt-1">
                  Add 1-5 tags that describe your room's topic
                </p>
              </div>

              {/* Preview Section */}
              <div className="border-t border-border-default pt-6">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-lg font-medium text-foreground-primary">Room Preview</h3>
                  <Button
                    type="button"
                    variant="secondary"
                    size="sm"
                    onClick={() => setShowPreview(!showPreview)}
                  >
                    {showPreview ? 'Hide' : 'Show'} Preview
                  </Button>
                </div>
                
                {showPreview && (
                  <Card className="p-4 bg-background-tertiary/30">
                    <div className="space-y-3">
                      <div>
                        <h4 className="font-semibold text-foreground-primary">
                          {formData.title || 'Room Title'}
                        </h4>
                        {formData.description && (
                          <p className="text-sm text-foreground-muted">
                            {formData.description}
                          </p>
                        )}
                      </div>
                      
                      <div className="flex flex-wrap gap-2">
                        {currentTags.map((tag) => (
                          <Badge key={tag} variant="secondary" size="sm">
                            #{tag}
                          </Badge>
                        ))}
                      </div>
                      
                      <div className="flex items-center gap-4 text-sm text-foreground-muted">
                        <span className="flex items-center gap-1">
                          <div className="w-2 h-2 bg-semantic-success rounded-full"></div>
                          1 online
                        </span>
                        <span>
                          {formData.isPrivate ? 'Private Room' : 'Public Room'}
                        </span>
                      </div>
                    </div>
                  </Card>
                )}
              </div>

              {/* Action Buttons */}
              <div className="flex items-center justify-between pt-6">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => navigate('/')}
                >
                  Cancel
                </Button>
                
                <Button
                  type="submit"
                  variant="primary"
                  disabled={isCreating}
                  className="px-8"
                >
                  {isCreating ? (
                    <>
                      <Spinner size="sm" className="mr-2" />
                      방 생성 중...
                    </>
                  ) : (
                    '방 만들기'
                  )}
                </Button>
              </div>
            </form>
          </Card>

          {/* Tips Section */}
          <Card className="p-6 mt-8 bg-semantic-info/5 border-semantic-info/20">
            <div className="flex items-start gap-3">
              <div className="w-6 h-6 bg-semantic-info/20 rounded-full flex items-center justify-center flex-shrink-0 mt-0.5">
                <svg className="w-3 h-3 text-semantic-info" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <div>
                <h4 className="font-medium text-foreground-primary mb-2">좋은 방을 만드는 팁</h4>
                <ul className="text-sm text-foreground-muted space-y-1">
                  <li>• 방의 목적을 명확히 설명하는 구체적인 제목을 선택하세요</li>
                  <li>• 사람들이 방을 발견할 수 있도록 관련 태그를 사용하세요</li>
                  <li>• 기대치를 설정하는 도움이 되는 설명을 작성하세요</li>
                  <li>• 더 작고 집중된 그룹을 원한다면 비공개로 만드는 것을 고려하세요</li>
                </ul>
              </div>
            </div>
          </Card>
        </div>
      </main>
    </div>
  );
}