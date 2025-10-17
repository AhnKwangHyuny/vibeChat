/**
 * SettingsPanel Component
 *
 * 책임: 환경설정 및 계정 관리
 * - 알림 설정 토글
 * - 계정 삭제 (Danger Zone)
 * - 삭제 확인 Modal
 */

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { Card } from '../../../components/ui/Card';
import { Button } from '../../../components/ui/Button';
import { Modal } from '../../../components/ui/Modal';

export function SettingsPanel() {
  const navigate = useNavigate();
  const [showDeleteModal, setShowDeleteModal] = useState(false);

  const handleDeleteAccount = () => {
    toast.success('계정이 삭제되었습니다!');
    navigate('/');
  };

  return (
    <>
      {/* Preferences */}
      <Card>
        <div className="p-6">
          <h3 className="text-lg font-semibold text-foreground-primary mb-4">
            환경설정
          </h3>
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-sm text-foreground-secondary">알림 받기</span>
              <label className="relative inline-flex items-center cursor-pointer">
                <input type="checkbox" className="sr-only peer" defaultChecked />
                <div className="w-11 h-6 bg-background-tertiary peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-500"></div>
              </label>
            </div>

            <div className="flex items-center justify-between">
              <span className="text-sm text-foreground-secondary">소리 알림</span>
              <label className="relative inline-flex items-center cursor-pointer">
                <input type="checkbox" className="sr-only peer" />
                <div className="w-11 h-6 bg-background-tertiary peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-500"></div>
              </label>
            </div>

            <div className="flex items-center justify-between">
              <span className="text-sm text-foreground-secondary">온라인 상태 표시</span>
              <label className="relative inline-flex items-center cursor-pointer">
                <input type="checkbox" className="sr-only peer" defaultChecked />
                <div className="w-11 h-6 bg-background-tertiary peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-500"></div>
              </label>
            </div>
          </div>
        </div>
      </Card>

      {/* Danger Zone */}
      <Card className="border-semantic-error">
        <div className="p-6">
          <h3 className="text-lg font-semibold text-semantic-error mb-4">
            위험 구역
          </h3>
          <div className="space-y-3">
            <Button
              onClick={() => setShowDeleteModal(true)}
              variant="danger"
              size="sm"
              className="w-full"
            >
              계정 삭제
            </Button>
          </div>
          <p className="text-xs text-foreground-muted mt-2">
            계정을 삭제하면 모든 데이터가 영구적으로 삭제됩니다.
          </p>
        </div>
      </Card>

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={showDeleteModal}
        onClose={() => setShowDeleteModal(false)}
        title="계정 삭제"
        size="md"
      >
        <div className="space-y-4">
          <p className="text-foreground-secondary">
            정말로 계정을 삭제하시겠습니까? 이 작업은 취소할 수 없습니다.
          </p>
          <div className="flex justify-end gap-2">
            <Button
              variant="secondary"
              onClick={() => setShowDeleteModal(false)}
            >
              취소
            </Button>
            <Button variant="danger" onClick={handleDeleteAccount}>
              계정 삭제
            </Button>
          </div>
        </div>
      </Modal>
    </>
  );
}
