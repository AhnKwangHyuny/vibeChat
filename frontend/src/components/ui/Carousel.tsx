import React, { useState, useEffect } from 'react';
import { cn } from '../../utils/cn';

export interface CarouselProps {
  children: React.ReactNode[];
  autoPlay?: boolean;
  interval?: number;
  showDots?: boolean;
  showArrows?: boolean;
  className?: string;
}

const Carousel: React.FC<CarouselProps> = ({
  children,
  autoPlay = false,
  interval = 3000,
  showDots = true,
  showArrows = true,
  className
}) => {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isTransitioning, setIsTransitioning] = useState(false);

  const goToSlide = (index: number) => {
    if (isTransitioning) return;
    setIsTransitioning(true);
    setCurrentIndex(index);
    setTimeout(() => setIsTransitioning(false), 300);
  };

  const goToPrevious = () => {
    const newIndex = currentIndex === 0 ? children.length - 1 : currentIndex - 1;
    goToSlide(newIndex);
  };

  const goToNext = () => {
    const newIndex = currentIndex === children.length - 1 ? 0 : currentIndex + 1;
    goToSlide(newIndex);
  };

  // Auto play effect
  useEffect(() => {
    if (!autoPlay || children.length <= 1) return;

    const timer = setInterval(() => {
      goToNext();
    }, interval);

    return () => clearInterval(timer);
  }, [autoPlay, interval, currentIndex, children.length]);

  if (!children || children.length === 0) return null;

  return (
    <div className={cn('relative w-full overflow-hidden', className)}>
      {/* Carousel Container */}
      <div className="relative h-full">
        <div
          className="flex transition-transform duration-300 ease-in-out"
          style={{ transform: `translateX(-${currentIndex * 100}%)` }}
        >
          {children.map((child, index) => (
            <div key={index} className="w-full flex-shrink-0">
              {child}
            </div>
          ))}
        </div>
      </div>

      {/* Navigation Arrows */}
      {showArrows && children.length > 1 && (
        <>
          <button
            onClick={goToPrevious}
            className="absolute left-4 top-1/2 -translate-y-1/2 z-10 bg-background-secondary/80 hover:bg-background-secondary text-foreground-primary p-2 rounded-full border border-border-default transition-all duration-200 hover:scale-110"
            disabled={isTransitioning}
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <button
            onClick={goToNext}
            className="absolute right-4 top-1/2 -translate-y-1/2 z-10 bg-background-secondary/80 hover:bg-background-secondary text-foreground-primary p-2 rounded-full border border-border-default transition-all duration-200 hover:scale-110"
            disabled={isTransitioning}
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
            </svg>
          </button>
        </>
      )}

      {/* Dots Indicator */}
      {showDots && children.length > 1 && (
        <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex space-x-2">
          {children.map((_, index) => (
            <button
              key={index}
              onClick={() => goToSlide(index)}
              className={cn(
                'w-2 h-2 rounded-full transition-all duration-200',
                index === currentIndex
                  ? 'bg-foreground-primary scale-125'
                  : 'bg-foreground-muted hover:bg-foreground-secondary'
              )}
            />
          ))}
        </div>
      )}
    </div>
  );
};

export { Carousel };
