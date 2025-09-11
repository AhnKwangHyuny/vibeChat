import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import NicknameModal from '../NicknameModal';

vi.mock('../../../services/api/auth', () => ({
  createGuestUser: vi.fn(),
  getMe: vi.fn(),
}));

import { createGuestUser, getMe } from '../../../services/api/auth';

describe('게스트 로그인 모달', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('모달_열리면_닉네임_입력과_버튼이_보인다', () => {
    render(<NicknameModal isOpen={true} onClose={vi.fn()} />);
    expect(screen.getByText('로그인')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('닉네임을 입력하세요')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '게스트로 시작' })).toBeInTheDocument();
  });

  it('닉네임_미입력시_에러토스트_표시후_API_호출하지_않음', async () => {
    render(<NicknameModal isOpen={true} onClose={vi.fn()} />);
    fireEvent.click(screen.getByRole('button', { name: '게스트로 시작' }));
    await waitFor(() => {
      expect(createGuestUser).not.toHaveBeenCalled();
    });
  });

  it('정상_닉네임_제출시_createGuestUser와_getMe_호출', async () => {
    (createGuestUser as unknown as ReturnType<typeof vi.fn>).mockResolvedValue({ userId: 1, nickname: 'aa' });
    (getMe as unknown as ReturnType<typeof vi.fn>).mockResolvedValue({ userId: 1, nickname: 'aa', provider: 'GUEST' });

    const onClose = vi.fn();
    render(<NicknameModal isOpen={true} onClose={onClose} />);

    fireEvent.change(screen.getByPlaceholderText('닉네임을 입력하세요'), { target: { value: 'aa' } });
    fireEvent.click(screen.getByRole('button', { name: '게스트로 시작' }));

    await waitFor(() => {
      expect(createGuestUser).toHaveBeenCalledWith({ nickname: 'aa' });
      expect(getMe).toHaveBeenCalled();
      expect(onClose).toHaveBeenCalled();
    });
  });

  it('닉네임_충돌시_서버_제안값으로_Input_자동_대체', async () => {
    (createGuestUser as unknown as ReturnType<typeof vi.fn>).mockRejectedValue({
      response: { data: { suggestedNickname: 'nick123' } }
    });
    const onClose = vi.fn();
    render(<NicknameModal isOpen={true} onClose={onClose} />);
    fireEvent.change(screen.getByPlaceholderText('닉네임을 입력하세요'), { target: { value: 'dup' } });
    fireEvent.click(screen.getByRole('button', { name: '게스트로 시작' }));

    await waitFor(() => {
      expect(createGuestUser).toHaveBeenCalled();
      expect(screen.getByDisplayValue('nick123')).toBeInTheDocument();
      expect(onClose).not.toHaveBeenCalled();
    });
  });
});


