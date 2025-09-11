import React from 'react';
import { cn } from '../../utils/cn';

export interface LoadingDotsProps {
  size?: 'sm' | 'md' | 'lg';
  color?: 'primary' | 'secondary' | 'muted' | 'white';
  className?: string;
}

const LoadingDots: React.FC<LoadingDotsProps> = ({
  size = 'md',
  color = 'primary',
  className
}) => {
  const getSizeStyles = () => {
    switch (size) {
      case 'sm':
        return 'w-1 h-1';
      case 'md':
        return 'w-1.5 h-1.5';
      case 'lg':
        return 'w-2 h-2';
      default:
        return 'w-1.5 h-1.5';
    }
  };

  const getColorStyles = () => {
    switch (color) {
      case 'primary':
        return 'bg-primary-500';
      case 'secondary':
        return 'bg-foreground-secondary';
      case 'muted':
        return 'bg-foreground-muted';
      case 'white':
        return 'bg-white';
      default:
        return 'bg-primary-500';
    }
  };

  const getGapStyles = () => {
    switch (size) {
      case 'sm':
        return 'gap-0.5';
      case 'md':
        return 'gap-1';
      case 'lg':
        return 'gap-1.5';
      default:
        return 'gap-1';
    }
  };

  return (
    <div className={cn("flex items-center", getGapStyles(), className)}>
      <div
        className={cn(
          "rounded-full animate-pulse",
          getSizeStyles(),
          getColorStyles()
        )}
        style={{
          animationDelay: '0ms',
          animationDuration: '1400ms'
        }}
      />
      <div
        className={cn(
          "rounded-full animate-pulse",
          getSizeStyles(),
          getColorStyles()
        )}
        style={{
          animationDelay: '160ms',
          animationDuration: '1400ms'
        }}
      />
      <div
        className={cn(
          "rounded-full animate-pulse",
          getSizeStyles(),
          getColorStyles()
        )}
        style={{
          animationDelay: '320ms',
          animationDuration: '1400ms'
        }}
      />
    </div>
  );
};

export default LoadingDots;
