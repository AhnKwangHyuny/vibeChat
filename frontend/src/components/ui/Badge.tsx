import React from 'react';
import { cn } from '../../utils/cn';

export interface BadgeProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info';
  size?: 'sm' | 'md';
  children: React.ReactNode;
}

const Badge = React.forwardRef<HTMLDivElement, BadgeProps>(
  ({ className, variant = 'default', size = 'md', children, ...props }, ref) => {
    const baseClasses = 'inline-flex items-center font-medium rounded-full';
    
    const variantClasses = {
      default: 'bg-background-tertiary text-foreground-secondary border border-border-default',
      primary: 'bg-accent-primary/10 text-accent-primary border border-accent-primary/20',
      secondary: 'bg-background-secondary text-foreground-tertiary border border-border-subtle',
      success: 'bg-semantic-success/10 text-semantic-success border border-semantic-success/20',
      warning: 'bg-semantic-warning/10 text-semantic-warning border border-semantic-warning/20',
      error: 'bg-semantic-error/10 text-semantic-error border border-semantic-error/20',
      info: 'bg-semantic-info/10 text-semantic-info border border-semantic-info/20'
    };
    
    const sizeClasses = {
      sm: 'px-2 py-1 text-xs',
      md: 'px-3 py-1.5 text-sm'
    };

    return (
      <div
        className={cn(
          baseClasses,
          variantClasses[variant],
          sizeClasses[size],
          className
        )}
        ref={ref}
        {...props}
      >
        {children}
      </div>
    );
  }
);

Badge.displayName = 'Badge';

export { Badge };
