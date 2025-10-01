import { useCallback } from 'react';

interface FileUploadResult {
  type: 'IMAGE' | 'VIDEO' | 'GIF';
  mediaUrl: string;
  mediaThumbUrl?: string;
  mediaDurationSec?: number;
}

interface UseFileUploadReturn {
  handleFileUpload: (file: File) => Promise<FileUploadResult | null>;
  isUploading: boolean;
}

export function useFileUpload(): UseFileUploadReturn {
  // 파일 업로드 처리 (현재는 임시 URL 생성, 추후 실제 업로드 API 연동)
  const handleFileUpload = useCallback(async (file: File): Promise<FileUploadResult | null> => {
    try {
      console.log('[FILE_UPLOAD] 파일 업로드 시작:', {
        fileName: file.name,
        fileType: file.type,
        fileSize: file.size
      });

      // 파일 타입 검증
      if (!file.type.startsWith('image/') && !file.type.startsWith('video/')) {
        throw new Error('지원하지 않는 파일 형식입니다.');
      }

      // 파일 크기 검증 (10MB 제한)
      const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
      if (file.size > MAX_FILE_SIZE) {
        throw new Error('파일 크기는 10MB를 초과할 수 없습니다.');
      }

      // 임시 URL 생성 (실제 구현 시 서버 업로드 필요)
      const mockUrl = URL.createObjectURL(file);

      let result: FileUploadResult;

      if (file.type.startsWith('image/')) {
        // 이미지 파일 처리
        if (file.type === 'image/gif') {
          result = {
            type: 'GIF',
            mediaUrl: mockUrl,
            mediaThumbUrl: mockUrl, // GIF는 썸네일과 원본이 동일
          };
        } else {
          result = {
            type: 'IMAGE',
            mediaUrl: mockUrl,
            mediaThumbUrl: mockUrl, // 이미지는 썸네일과 원본이 동일
          };
        }
      } else if (file.type.startsWith('video/')) {
        // 비디오 파일 처리
        result = {
          type: 'VIDEO',
          mediaUrl: mockUrl,
          mediaThumbUrl: undefined, // 비디오 썸네일은 별도 생성 필요
          mediaDurationSec: 30, // 임시 값, 실제로는 비디오 메타데이터에서 추출
        };

        // 비디오 길이 제한 검증 (10초)
        // TODO: 실제 구현에서는 비디오 메타데이터를 읽어서 길이 확인
        console.log('[FILE_UPLOAD] 비디오 파일 처리 완료 (길이 검증 필요)');
      } else {
        throw new Error('지원하지 않는 파일 형식입니다.');
      }

      console.log('[FILE_UPLOAD] 파일 업로드 완료:', result);
      return result;

    } catch (error) {
      console.error('[FILE_UPLOAD] 파일 업로드 실패:', error);
      return null;
    }
  }, []);

  return {
    handleFileUpload,
    isUploading: false, // 현재는 임시 구현이므로 항상 false
  };
}