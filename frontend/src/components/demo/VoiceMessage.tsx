import React, { useState, useRef, useEffect } from 'react';
import { cn } from '../../utils/cn';

export interface VoiceMessageProps {
  audioUrl: string;
  duration: number;
  isPlaying: boolean;
  onPlay: () => void;
  onPause: () => void;
  onSeek?: (time: number) => void;
  waveform?: number[];
  className?: string;
}

const VoiceMessage: React.FC<VoiceMessageProps> = ({
  audioUrl,
  duration,
  isPlaying,
  onPlay,
  onPause,
  onSeek,
  waveform = [],
  className
}) => {
  const [currentTime, setCurrentTime] = useState(0);
  const [progress, setProgress] = useState(0);
  const audioRef = useRef<HTMLAudioElement>(null);
  const progressBarRef = useRef<HTMLDivElement>(null);

  // Generate mock waveform if none provided
  const mockWaveform = waveform.length > 0 ? waveform : 
    Array.from({ length: 40 }, () => Math.random() * 0.8 + 0.2);

  useEffect(() => {
    const audio = audioRef.current;
    if (!audio) return;

    const updateTime = () => {
      const currentTime = audio.currentTime;
      const duration = audio.duration || 1;
      setCurrentTime(currentTime);
      setProgress((currentTime / duration) * 100);
    };

    audio.addEventListener('timeupdate', updateTime);
    audio.addEventListener('loadedmetadata', updateTime);

    return () => {
      audio.removeEventListener('timeupdate', updateTime);
      audio.removeEventListener('loadedmetadata', updateTime);
    };
  }, []);

  const formatTime = (time: number) => {
    const minutes = Math.floor(time / 60);
    const seconds = Math.floor(time % 60);
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  };

  const handleProgressClick = (e: React.MouseEvent) => {
    if (!progressBarRef.current || !audioRef.current) return;

    const rect = progressBarRef.current.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const newProgress = (clickX / rect.width) * 100;
    const newTime = (newProgress / 100) * duration;

    audioRef.current.currentTime = newTime;
    setCurrentTime(newTime);
    setProgress(newProgress);
    
    if (onSeek) {
      onSeek(newTime);
    }
  };

  const handlePlayPause = () => {
    if (isPlaying) {
      onPause();
      audioRef.current?.pause();
    } else {
      onPlay();
      audioRef.current?.play();
    }
  };

  return (
    <div className={cn(
      "flex items-center gap-3 p-3 bg-background-secondary rounded-lg border border-border-default max-w-xs",
      className
    )}>
      <audio ref={audioRef} src={audioUrl} preload="metadata" />

      {/* Play/Pause Button */}
      <button
        onClick={handlePlayPause}
        className="flex-shrink-0 w-10 h-10 rounded-full bg-primary-500 text-foreground-primary hover:bg-primary-600 transition-colors flex items-center justify-center"
      >
        {isPlaying ? (
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 9v6m4-6v6" />
          </svg>
        ) : (
          <svg className="w-5 h-5 ml-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
          </svg>
        )}
      </button>

      {/* Waveform and Progress */}
      <div className="flex-1">
        {/* Waveform */}
        <div 
          ref={progressBarRef}
          className="flex items-end justify-between h-8 gap-0.5 cursor-pointer mb-1"
          onClick={handleProgressClick}
        >
          {mockWaveform.map((height, index) => (
            <div
              key={index}
              className={cn(
                "flex-1 rounded-full transition-colors",
                (index / mockWaveform.length) * 100 <= progress
                  ? "bg-primary-500"
                  : "bg-foreground-muted/30"
              )}
              style={{ 
                height: `${height * 100}%`,
                minHeight: '2px'
              }}
            />
          ))}
        </div>

        {/* Time Display */}
        <div className="flex justify-between items-center text-xs text-foreground-muted">
          <span>{formatTime(currentTime)}</span>
          <span>{formatTime(duration)}</span>
        </div>
      </div>

      {/* Download Button */}
      <button
        onClick={() => {
          const a = document.createElement('a');
          a.href = audioUrl;
          a.download = 'voice-message.mp3';
          a.click();
        }}
        className="flex-shrink-0 p-2 text-foreground-muted hover:text-foreground-primary transition-colors"
        title="음성 메시지 다운로드"
      >
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
      </button>
    </div>
  );
};

export default VoiceMessage;
