import React from 'react';
import { Button } from '../ui/Button';
import { Badge } from '../ui/Badge';
import { cn } from '../../utils/cn';

export interface HeroProps {
  title: string;
  subtitle: string;
  description: string;
  primaryAction?: {
    label: string;
    onClick: () => void;
  };
  secondaryAction?: {
    label: string;
    onClick: () => void;
  };
  backgroundImage?: string;
  badges?: string[];
  stats?: Array<{
    value: string;
    label: string;
  }>;
  className?: string;
}

const Hero: React.FC<HeroProps> = ({
  title,
  subtitle,
  description,
  primaryAction,
  secondaryAction,
  backgroundImage,
  badges = [],
  stats = [],
  className
}) => {
  return (
    <section className={cn(
      'relative overflow-hidden bg-background-primary',
      className
    )}>
      {/* Background */}
      {backgroundImage && (
        <div className="absolute inset-0">
          <img
            src={backgroundImage}
            alt=""
            className="w-full h-full object-cover opacity-10"
          />
          <div className="absolute inset-0 bg-gradient-to-r from-background-primary via-background-primary/80 to-background-primary" />
        </div>
      )}

      <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-24 lg:py-32">
        <div className="text-center">
          {/* Badges */}
          {badges.length > 0 && (
            <div className="flex justify-center mb-6">
              <div className="flex flex-wrap gap-2">
                {badges.map((badge, index) => (
                  <Badge key={index} variant="default" size="md">
                    {badge}
                  </Badge>
                ))}
              </div>
            </div>
          )}

          {/* Subtitle */}
          <p className="text-sm sm:text-base md:text-lg text-foreground-secondary mb-3 sm:mb-4 font-medium px-4">
            {subtitle}
          </p>

          {/* Title */}
          <h1 className="text-2xl sm:text-3xl md:text-4xl lg:text-5xl xl:text-6xl font-bold text-foreground-primary mb-4 sm:mb-6 leading-tight px-4">
            {title}
          </h1>

          {/* Description */}
          <p className="text-sm sm:text-base md:text-lg lg:text-xl text-foreground-muted mb-6 sm:mb-8 max-w-3xl mx-auto leading-relaxed px-4">
            {description}
          </p>

          {/* Actions */}
          <div className="flex flex-col sm:flex-row gap-3 sm:gap-4 justify-center mb-8 sm:mb-12 px-4">
            {primaryAction && (
              <Button
                onClick={primaryAction.onClick}
                variant="primary"
                size="lg"
                className="px-6 sm:px-8 py-3 sm:py-4 text-sm sm:text-base lg:text-lg w-full sm:w-auto"
              >
                {primaryAction.label}
              </Button>
            )}
            {secondaryAction && (
              <Button
                onClick={secondaryAction.onClick}
                variant="secondary"
                size="lg"
                className="px-6 sm:px-8 py-3 sm:py-4 text-sm sm:text-base lg:text-lg w-full sm:w-auto"
              >
                {secondaryAction.label}
              </Button>
            )}
          </div>

          {/* Stats */}
          {stats.length > 0 && (
            <div className="grid grid-cols-3 gap-2 sm:gap-6 md:gap-8 max-w-2xl mx-auto px-4">
              {stats.map((stat, index) => (
                <div key={index} className="text-center">
                  <div className="text-lg sm:text-2xl md:text-3xl font-bold text-foreground-primary mb-1 sm:mb-2">
                    {stat.value}
                  </div>
                  <div className="text-xs sm:text-sm text-foreground-muted">
                    {stat.label}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Decorative Elements */}
      <div className="absolute top-0 left-0 w-full h-full pointer-events-none">
        <div className="absolute top-1/4 left-1/4 w-64 h-64 bg-primary-500/5 rounded-full blur-3xl" />
        <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-semantic-info/5 rounded-full blur-3xl" />
      </div>
    </section>
  );
};

export { Hero };
