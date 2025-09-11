import React from 'react';
import { cn } from '../../utils/cn';

export interface AvatarProps extends React.ImgHTMLAttributes<HTMLImageElement> {
  size?: 'sm' | 'md' | 'lg' | 'xl';
  fallback?: string;
  online?: boolean;
}

const Avatar = React.forwardRef<HTMLImageElement, AvatarProps>(
  ({ className, size = 'md', fallback, online, ...props }, ref) => {
    const sizeClasses = {
      sm: 'w-6 h-6',
      md: 'w-8 h-8',
      lg: 'w-12 h-12',
      xl: 'w-16 h-16'
    };

    const [imageError, setImageError] = React.useState(false);

    const handleImageError = () => {
      setImageError(true);
    };

    if (imageError || !props.src) {
      return (
        <div className={cn(
          'rounded-full bg-background-tertiary flex items-center justify-center text-foreground-muted font-medium',
          sizeClasses[size],
          className
        )}>
          {fallback ? fallback.charAt(0).toUpperCase() : '?'}
        </div>
      );
    }

    return (
      <div className="relative">
        <img
          className={cn(
            'rounded-full object-cover',
            sizeClasses[size],
            className
          )}
          ref={ref}
          onError={handleImageError}
          {...props}
        />
        {online && (
          <div className="absolute -bottom-0.5 -right-0.5 w-3 h-3 bg-semantic-success rounded-full border-2 border-background-primary" />
        )}
      </div>
    );
  }
);

Avatar.displayName = 'Avatar';

export { Avatar };
