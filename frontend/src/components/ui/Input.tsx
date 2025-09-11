import React from 'react';
import { cn } from '../../utils/cn';

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  variant?: 'textbox' | 'search';
  error?: boolean;
}

const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, variant = 'textbox', error, ...props }, ref) => {
    const baseClasses = 'w-full transition-all duration-100 ease-out focus:outline-none';
    
    const variantClasses = {
      textbox: 'bg-background-tertiary border border-border-focus rounded-md px-4 py-3 text-foreground-primary text-sm placeholder:text-foreground-muted focus:ring-2 focus:ring-border-focus focus:border-border-focus',
      search: 'bg-background-tertiary border border-border-default rounded-md px-4 py-2 text-foreground-primary text-sm placeholder:text-foreground-muted focus:border-border-focus focus:ring-1 focus:ring-border-focus'
    };

    const errorClasses = error ? 'border-semantic-error focus:border-semantic-error focus:ring-semantic-error' : '';

    return (
      <input
        className={cn(
          baseClasses,
          variantClasses[variant],
          errorClasses,
          className
        )}
        ref={ref}
        {...props}
      />
    );
  }
);

Input.displayName = 'Input';

export { Input };
