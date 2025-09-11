import React from 'react';
import { cn } from '../../utils/cn';
import { Progress } from './Progress';

export interface FilePreviewFile {
  name: string;
  size: number;
  type: string;
  url?: string;
}

export interface FilePreviewProps {
  file: File | FilePreviewFile;
  onRemove?: () => void;
  showProgress?: boolean;
  uploadProgress?: number;
  className?: string;
  compact?: boolean;
}

const FilePreview: React.FC<FilePreviewProps> = ({
  file,
  onRemove,
  showProgress = false,
  uploadProgress = 0,
  className,
  compact = false
}) => {
  const fileName = file.name;
  const fileSize = file.size;
  const fileType = file.type;
  const fileUrl = file instanceof File ? URL.createObjectURL(file) : file.url;

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const getFileIcon = (type: string) => {
    if (type.startsWith('image/')) {
      return (
        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
      );
    } else if (type.startsWith('video/')) {
      return (
        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
        </svg>
      );
    } else if (type.startsWith('audio/')) {
      return (
        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" />
        </svg>
      );
    } else if (type === 'application/pdf') {
      return (
        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
        </svg>
      );
    } else {
      return (
        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
      );
    }
  };

  if (compact) {
    return (
      <div className={cn(
        "flex items-center gap-2 p-2 bg-background-tertiary rounded border border-border-default",
        className
      )}>
        <div className="text-foreground-muted">
          {getFileIcon(fileType)}
        </div>
        <div className="flex-1 min-w-0">
          <div className="text-sm font-medium text-foreground-primary truncate">
            {fileName}
          </div>
          <div className="text-xs text-foreground-muted">
            {formatFileSize(fileSize)}
          </div>
        </div>
        {onRemove && (
          <button
            onClick={onRemove}
            className="text-foreground-muted hover:text-semantic-error transition-colors"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        )}
      </div>
    );
  }

  return (
    <div className={cn(
      "bg-background-secondary border border-border-default rounded-lg overflow-hidden",
      className
    )}>
      {/* Preview Area */}
      <div className="relative">
        {fileType.startsWith('image/') && fileUrl ? (
          <div className="aspect-video bg-background-tertiary flex items-center justify-center">
            <img
              src={fileUrl}
              alt={fileName}
              className="max-h-full max-w-full object-contain"
            />
          </div>
        ) : fileType.startsWith('video/') && fileUrl ? (
          <div className="aspect-video bg-background-tertiary">
            <video
              src={fileUrl}
              className="w-full h-full object-contain"
              controls
              preload="metadata"
            />
          </div>
        ) : (
          <div className="aspect-video bg-background-tertiary flex flex-col items-center justify-center text-foreground-muted">
            {getFileIcon(fileType)}
            <span className="text-sm mt-2">미리보기 없음</span>
          </div>
        )}

        {/* Remove Button */}
        {onRemove && (
          <button
            onClick={onRemove}
            className="absolute top-2 right-2 p-1 bg-background-primary/80 hover:bg-background-primary rounded-full text-foreground-muted hover:text-semantic-error transition-colors"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        )}
      </div>

      {/* File Info */}
      <div className="p-3">
        <div className="flex items-start justify-between gap-2">
          <div className="flex-1 min-w-0">
            <div className="text-sm font-medium text-foreground-primary truncate">
              {fileName}
            </div>
            <div className="text-xs text-foreground-muted mt-1">
              {formatFileSize(fileSize)} • {fileType}
            </div>
          </div>
        </div>

        {/* Upload Progress */}
        {showProgress && (
          <div className="mt-3">
            <div className="flex items-center justify-between text-xs text-foreground-muted mb-1">
              <span>업로드 중...</span>
              <span>{uploadProgress}%</span>
            </div>
            <Progress 
              value={uploadProgress} 
              max={100} 
              variant={uploadProgress === 100 ? "success" : "default"}
              size="sm"
            />
          </div>
        )}
      </div>
    </div>
  );
};

export default FilePreview;
