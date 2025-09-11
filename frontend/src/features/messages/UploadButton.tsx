import React, { useRef, useState } from 'react';
import { uploadFile } from '../../services/api/upload';
import { toast } from 'react-toastify';

interface UploadButtonProps {
  onMediaSelected: (media: { type: 'TEXT' | 'IMAGE' | 'GIF' | 'VIDEO'; url: string; thumbUrl?: string; durationSec?: number; }) => void;
}

const MAX_FILE_SIZE_MB = 20; // Max file size in MB
const ALLOWED_FILE_TYPES = [
  'image/jpeg', 'image/png', 'image/webp', 'image/gif',
  'video/mp4'
];

export default function UploadButton({ onMediaSelected }: UploadButtonProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    // Client-side validation
    if (!ALLOWED_FILE_TYPES.includes(file.type)) {
      toast.error(`Unsupported file type: ${file.type}. Allowed types are: ${ALLOWED_FILE_TYPES.join(', ')}`);
      if (fileInputRef.current) fileInputRef.current.value = '';
      setSelectedFile(null);
      setPreview(null);
      return;
    }

    if (file.size > MAX_FILE_SIZE_MB * 1024 * 1024) {
      toast.error(`File size exceeds limit of ${MAX_FILE_SIZE_MB}MB.`);
      if (fileInputRef.current) fileInputRef.current.value = '';
      setSelectedFile(null);
      setPreview(null);
      return;
    }

    setSelectedFile(file);
    setPreview(URL.createObjectURL(file));
  };

  const handleUpload = async () => {
    if (!selectedFile) return;

    try {
      const response = await uploadFile(selectedFile);
      onMediaSelected({
        type: response.type,
        url: response.url,
        thumbUrl: response.thumbUrl,
        durationSec: response.durationSec,
      });
      setPreview(null);
      setSelectedFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = ''; // Clear file input
      }
    } catch (error: unknown) {
      if (error instanceof Error && (error as any).response) {
        toast.error((error as any).response?.data?.message || "Failed to upload file.");
      } else {
        toast.error("An unknown error occurred during upload.");
      }
    }
  };

  return (
    <div className="flex items-center space-x-2">
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFileChange}
        className="hidden"
        accept={ALLOWED_FILE_TYPES.join(',')}
      />
      <button
        type="button"
        onClick={() => fileInputRef.current?.click()}
        className="bg-gray-200 text-gray-700 px-4 py-2 rounded-lg hover:bg-gray-300"
      >
        Attach
      </button>

      {preview && (
        <div className="flex items-center space-x-2">
          {selectedFile?.type.startsWith('image') ? (
            <img src={preview} alt="Preview" className="h-12 w-12 object-cover rounded" />
          ) : (
            <video src={preview} controls className="h-12 w-12 object-cover rounded" />
          )}
          <button
            type="button"
            onClick={handleUpload}
            className="bg-blue-500 text-white px-4 py-2 rounded-lg hover:bg-blue-600"
          >
            Upload & Send
          </button>
          <button
            type="button"
            onClick={() => { setPreview(null); setSelectedFile(null); if (fileInputRef.current) fileInputRef.current.value = ''; }}
            className="bg-red-500 text-white px-4 py-2 rounded-lg hover:bg-red-600"
          >
            Cancel
          </button>
        </div>
      )}
    </div>
  );
}