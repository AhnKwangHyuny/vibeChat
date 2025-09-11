import React from 'react';
import { cn } from '../../utils/cn';

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'primary' | 'secondary';
  children: React.ReactNode;
}

const Card = React.forwardRef<HTMLDivElement, CardProps>(
  ({ className, variant = 'primary', children, ...props }, ref) => {
    const variantClasses = {
      primary: 'bg-background-secondary border-0 rounded-lg p-8 shadow-none',
      secondary: 'bg-transparent border-2 border-border-default rounded-full px-4 py-0 shadow-none'
    };

    return (
      <div
        className={cn(
          variantClasses[variant],
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

Card.displayName = 'Card';

export { Card };
