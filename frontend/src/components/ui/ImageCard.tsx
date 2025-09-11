import React from 'react';
import { cn } from '../../utils/cn';

export interface ImageCardProps extends React.HTMLAttributes<HTMLDivElement> {
  src: string;
  alt: string;
  title?: string;
  description?: string;
  aspectRatio?: 'square' | 'video' | 'wide' | 'portrait';
  overlay?: boolean;
  children?: React.ReactNode;
  onImageClick?: () => void;
}

const ImageCard: React.FC<ImageCardProps> = ({
  src,
  alt,
  title,
  description,
  aspectRatio = 'video',
  overlay = false,
  children,
  onImageClick,
  className,
  ...props
}) => {
  const aspectRatioClasses = {
    square: 'aspect-square',
    video: 'aspect-video',
    wide: 'aspect-[16/9]',
    portrait: 'aspect-[3/4]'
  };

  return (
    <div
      className={cn(
        'group relative overflow-hidden rounded-lg border border-border-default bg-background-secondary transition-all duration-200 hover:border-border-focus hover:shadow-lg',
        className
      )}
      {...props}
    >
      {/* Image Container */}
      <div className={cn('relative overflow-hidden', aspectRatioClasses[aspectRatio])}>
        <img
          src={src}
          alt={alt}
          className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
          loading="lazy"
        />
        
        {/* Overlay */}
        {overlay && (
          <div className="absolute inset-0 bg-gradient-to-t from-background-primary/80 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
        )}

        {/* Image Click Handler */}
        {onImageClick && (
          <button
            onClick={onImageClick}
            className="absolute inset-0 w-full h-full bg-transparent hover:bg-background-primary/10 transition-colors duration-200"
            aria-label={`View ${alt}`}
          />
        )}

        {/* Children Overlay */}
        {children && (
          <div className="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-300">
            {children}
          </div>
        )}
      </div>

      {/* Content */}
      {(title || description) && (
        <div className="p-4 space-y-2">
          {title && (
            <h3 className="text-lg font-medium text-foreground-primary line-clamp-2">
              {title}
            </h3>
          )}
          {description && (
            <p className="text-sm text-foreground-muted line-clamp-3">
              {description}
            </p>
          )}
        </div>
      )}
    </div>
  );
};

export { ImageCard };
