import React from 'react';
import { cn } from '../../utils/cn';

export interface NotificationBadgeProps {
  count: number;
  maxCount?: number;
  variant?: 'primary' | 'danger' | 'warning' | 'success';
  position?: 'top-right' | 'top-left' | 'bottom-right' | 'bottom-left';
  size?: 'sm' | 'md' | 'lg';
  className?: string;
  children?: React.ReactNode;
  showZero?: boolean;
  pulse?: boolean;
}

const NotificationBadge: React.FC<NotificationBadgeProps> = ({
  count,
  maxCount = 99,
  variant = 'danger',
  position = 'top-right',
  size = 'md',
  className,
  children,
  showZero = false,
  pulse = false
}) => {
  const displayCount = count > maxCount ? `${maxCount}+` : count.toString();
  const shouldShow = count > 0 || (showZero && count === 0);

  const getVariantStyles = () => {
    switch (variant) {
      case 'primary':
        return 'bg-primary-500 text-foreground-primary';
      case 'danger':
        return 'bg-semantic-error text-white';
      case 'warning':
        return 'bg-semantic-warning text-white';
      case 'success':
        return 'bg-semantic-success text-white';
      default:
        return 'bg-semantic-error text-white';
    }
  };

  const getSizeStyles = () => {
    switch (size) {
      case 'sm':
        return 'h-5 min-w-5 text-xs px-1.5';
      case 'md':
        return 'h-6 min-w-6 text-xs px-2';
      case 'lg':
        return 'h-7 min-w-7 text-sm px-2.5';
      default:
        return 'h-6 min-w-6 text-xs px-2';
    }
  };

  const getPositionStyles = () => {
    switch (position) {
      case 'top-right':
        return '-top-1 -right-1';
      case 'top-left':
        return '-top-1 -left-1';
      case 'bottom-right':
        return '-bottom-1 -right-1';
      case 'bottom-left':
        return '-bottom-1 -left-1';
      default:
        return '-top-1 -right-1';
    }
  };

  if (children) {
    return (
      <div className={cn("relative inline-block", className)}>
        {children}
        {shouldShow && (
          <span
            className={cn(
              "absolute flex items-center justify-center rounded-full font-medium border-2 border-background-primary transform transition-all duration-200",
              getVariantStyles(),
              getSizeStyles(),
              getPositionStyles(),
              pulse && "animate-pulse",
              "z-10"
            )}
          >
            {displayCount}
          </span>
        )}
      </div>
    );
  }

  if (!shouldShow) return null;

  return (
    <span
      className={cn(
        "inline-flex items-center justify-center rounded-full font-medium",
        getVariantStyles(),
        getSizeStyles(),
        pulse && "animate-pulse",
        className
      )}
    >
      {displayCount}
    </span>
  );
};

export default NotificationBadge;
