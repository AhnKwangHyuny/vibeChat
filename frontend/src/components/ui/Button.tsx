import React from 'react';
import { cn } from '../../utils/cn';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  children: React.ReactNode;
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = 'primary', size = 'md', children, ...props }, ref) => {
    const baseClasses = 'inline-flex items-center justify-center font-medium transition-all duration-100 ease-out focus:outline-none focus:ring-2 focus:ring-border-focus disabled:opacity-50 disabled:cursor-not-allowed h-[42px]';
    
    const variantClasses = {
      primary: 'bg-foreground-primary text-background-primary border-0 rounded-md hover:opacity-90 active:scale-95',
      secondary: 'bg-transparent text-foreground-muted border border-border-default rounded-md hover:bg-background-tertiary hover:text-foreground-secondary',
      ghost: 'bg-transparent text-foreground-primary border-0 rounded-none hover:opacity-80',
      danger: 'bg-semantic-error text-white border-0 rounded-md hover:opacity-90 active:scale-95'
    };
    
    const sizeClasses = {
      sm: 'px-3 py-2 text-xs',
      md: 'px-6 py-3 text-sm',
      lg: 'px-8 py-4 text-base'
    };

    return (
      <button
        type={props.type ?? 'button'}
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
      </button>
    );
  }
);

Button.displayName = 'Button';

export { Button };
