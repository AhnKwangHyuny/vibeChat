// import React from 'react';
// import { useForm } from 'react-hook-form';
// import { zodResolver } from '@hookform/resolvers/zod';
// import * as z from 'zod';
// import { Dialog, Transition } from '@headlessui/react';
// import { Fragment, useState } from 'react';
// import { createGuestUser } from '../../services/api/auth';
// import { useDispatch } from 'react-redux';
// import { setUser } from '../../store/userSlice';
// import { toast } from 'react-toastify';

// const nicknameSchema = z.object({
//   nickname: z.string().min(2, "Nickname must be at least 2 characters").max(32, "Nickname must be at most 32 characters"),
// });

// type NicknameFormInputs = z.infer<typeof nicknameSchema>;

// interface NicknameModalProps {
//   isOpen: boolean;
//   onClose: () => void;
// }

// export default function NicknameModal({ isOpen, onClose }: NicknameModalProps) {
//   const { register, handleSubmit, formState: { errors } } = useForm<NicknameFormInputs>({
//     resolver: zodResolver(nicknameSchema),
//   });
//   const dispatch = useDispatch();

//   const onSubmit = async (data: NicknameFormInputs) => {
//     try {
//       const user = await createGuestUser(data);
//       dispatch(setUser({ id: user.userId.toString(), nickname: user.nickname }));
//       toast.success(`Welcome, ${user.nickname}!`);
//       onClose();
//     } catch (error: unknown) {
//       if (error instanceof Error && (error as any).response) {
//         toast.error((error as any).response?.data?.message || "Failed to create guest user.");
//       } else {
//         toast.error("An unknown error occurred while creating guest user.");
//       }
//     }
//   };

//   return (
//     <Transition appear show={isOpen} as={Fragment}>
//       <Dialog as="div" className="relative z-10" onClose={onClose}>
//         <Transition.Child
//           as={Fragment}
//           enter="ease-out duration-300"
//           enterFrom="opacity-0"
//           enterTo="opacity-100"
//           leave="ease-in duration-200"
//           leaveFrom="opacity-100"
//           leaveTo="opacity-0"
//         >
//           <div className="fixed inset-0 bg-black bg-opacity-25" />
//         </Transition.Child>

//         <div className="fixed inset-0 overflow-y-auto">
//           <div className="flex min-h-full items-center justify-center p-4 text-center">
//             <Transition.Child
//               as={Fragment}
//               enter="ease-out duration-300"
//               enterFrom="opacity-0 scale-95"
//               enterTo="opacity-100 scale-100"
//               leave="ease-in duration-200"
//               leaveFrom="opacity-100 scale-100"
//               leaveTo="opacity-0 scale-95"
//             >
//               <Dialog.Panel className="w-full max-w-md transform overflow-hidden rounded-2xl bg-white p-6 text-left align-middle shadow-xl transition-all">
//                 <Dialog.Title
//                   as="h3"
//                   className="text-lg font-medium leading-6 text-gray-900"
//                 >
//                   Enter Your Nickname
//                 </Dialog.Title>
//                 <div className="mt-2">
//                   <p className="text-sm text-gray-500">
//                     Please enter a nickname to start chatting.
//                   </p>
//                   <form onSubmit={handleSubmit(onSubmit)} className="mt-4">
//                     <input
//                       type="text"
//                       {...register("nickname")}
//                       className="w-full p-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
//                       placeholder="Nickname"
//                     />
//                     {errors.nickname && (
//                       <p className="text-red-500 text-sm mt-1">{errors.nickname.message}</p>
//                     )}
//                     <div className="mt-4 flex justify-end">
//                       <button
//                         type="submit"
//                         className="inline-flex justify-center rounded-md border border-transparent bg-blue-100 px-4 py-2 text-sm font-medium text-blue-900 hover:bg-blue-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2"
//                       >
//                         Start Chatting
//                       </button>
//                     </div>
//                   </form>
//                 </div>
//               </Dialog.Panel>
//             </Transition.Child>
//           </div>
//         </div>
//       </Dialog>
//     </Transition>
//   );
// }

// 임시로 빈 컴포넌트로 대체
import { useState } from 'react';
import { Modal } from '../../components/ui/Modal';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import { cn } from '../../utils/cn';
import { useSupabaseAuth } from '../../hooks/useSupabaseAuth';
import { useSessionAuth } from '../../hooks/useSessionAuth';
import { toast } from 'react-toastify';

interface LoginModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function NicknameModal({ isOpen, onClose }: LoginModalProps) {
  const [tab, setTab] = useState<'guest' | 'google'>('guest');
  const [nickname, setNickname] = useState('');
  const { signInWithGoogle } = useSupabaseAuth();
  const { signInAsGuest, loading: guestLoading } = useSessionAuth();
  const [isGoogleProcessing, setIsGoogleProcessing] = useState(false);

  const handleGuest = async () => {
    const result = await signInAsGuest(nickname);
    if (result === true) {
      onClose();
    } else if (result && typeof result === 'object' && 'suggested' in result) {
      setNickname(result.suggested);
    }
  };

  const handleGoogle = async () => {
    setIsGoogleProcessing(true);
    try {
      const success = await signInWithGoogle();
      if (success) {
        toast.info('Google 로그인 페이지로 이동합니다...');
      } else {
        toast.error('Google 로그인에 실패했습니다. 다시 시도해 주세요.');
        setIsGoogleProcessing(false);
      }
    } catch (error) {
      console.error('Google login error:', error);
      toast.error('Google 로그인 중 오류가 발생했습니다.');
      setIsGoogleProcessing(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="로그인" size="sm">
      <div className="w-full max-w-[420px] mx-auto">
        <p className="text-sm text-foreground-muted mb-4">
          원하는 방법으로 로그인하세요. 게스트는 닉네임만 필요합니다.
        </p>
        <div className="flex w-full rounded-full bg-background-tertiary p-1 mb-5 border border-border-default">
          <button
            className={cn(
              'flex-1 py-2 rounded-full text-sm text-foreground-secondary hover:text-foreground-primary transition-colors',
              tab === 'guest' &&
                'bg-background-primary text-foreground-primary shadow-inner'
            )}
            onClick={() => setTab('guest')}
          >
            게스트
          </button>
          <button
            className={cn(
              'flex-1 py-2 rounded-full text-sm text-foreground-secondary hover:text-foreground-primary transition-colors',
              tab === 'google' &&
                'bg-background-primary text-foreground-primary shadow-inner'
            )}
            onClick={() => setTab('google')}
          >
            Google
          </button>
        </div>

        {tab === 'guest' ? (
          <div className="space-y-4">
            <label className="text-sm text-foreground-secondary">닉네임</label>
            <Input
              value={nickname}
              onChange={e => setNickname(e.target.value)}
              placeholder="닉네임을 입력하세요"
              className="w-full"
            />
            <Button
              onClick={handleGuest}
              disabled={guestLoading}
              className="w-full"
              variant="primary"
            >
              {guestLoading ? '처리 중...' : '게스트로 시작'}
            </Button>
          </div>
        ) : (
          <div className="space-y-4">
            <Button
              onClick={handleGoogle}
              disabled={isGoogleProcessing}
              variant="secondary"
              className="w-full"
            >
              {isGoogleProcessing ? '처리 중...' : 'Google로 계속'}
            </Button>
            <p className="text-xs text-foreground-muted text-center">
              최초 로그인 사용자는 자동으로 계정이 생성됩니다.
            </p>
          </div>
        )}
      </div>
    </Modal>
  );
}

