import React, { useEffect, useRef } from 'react';
import { cn } from '../../utils/cn';

export interface ContextMenuItem {
  id: string;
  label: string;
  icon?: React.ReactNode;
  onClick: () => void;
  disabled?: boolean;
  destructive?: boolean;
  separator?: boolean;
}

export interface ContextMenuProps {
  items: ContextMenuItem[];
  position: { x: number; y: number };
  onClose: () => void;
  className?: string;
}

const ContextMenu: React.FC<ContextMenuProps> = ({
  items,
  position,
  onClose,
  className
}) => {
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        onClose();
      }
    };

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };

    const handleScroll = () => {
      onClose();
    };

    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleEscape);
    document.addEventListener('scroll', handleScroll, true);

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
      document.removeEventListener('scroll', handleScroll, true);
    };
  }, [onClose]);

  useEffect(() => {
    // Position the menu to stay within viewport
    if (menuRef.current) {
      const menu = menuRef.current;
      const rect = menu.getBoundingClientRect();
      const viewportWidth = window.innerWidth;
      const viewportHeight = window.innerHeight;

      let adjustedX = position.x;
      let adjustedY = position.y;

      // Adjust horizontal position if menu would go off-screen
      if (position.x + rect.width > viewportWidth) {
        adjustedX = position.x - rect.width;
      }

      // Adjust vertical position if menu would go off-screen
      if (position.y + rect.height > viewportHeight) {
        adjustedY = position.y - rect.height;
      }

      // Ensure menu doesn't go above or to the left of viewport
      adjustedX = Math.max(8, adjustedX);
      adjustedY = Math.max(8, adjustedY);

      menu.style.left = `${adjustedX}px`;
      menu.style.top = `${adjustedY}px`;
    }
  }, [position]);

  const handleItemClick = (item: ContextMenuItem) => {
    if (!item.disabled) {
      item.onClick();
      onClose();
    }
  };

  return (
    <>
      {/* Backdrop */}
      <div className="fixed inset-0 z-50" onClick={onClose} />
      
      {/* Menu */}
      <div
        ref={menuRef}
        className={cn(
          "fixed z-50 min-w-48 bg-background-secondary border border-border-default rounded-lg shadow-lg overflow-hidden",
          className
        )}
        style={{ left: position.x, top: position.y }}
      >
        <div className="py-1">
          {items.map((item, index) => (
            <React.Fragment key={item.id}>
              {item.separator && index > 0 && (
                <div className="border-t border-border-default my-1" />
              )}
              
              <button
                onClick={() => handleItemClick(item)}
                disabled={item.disabled}
                className={cn(
                  "w-full flex items-center gap-3 px-3 py-2 text-sm text-left transition-colors",
                  item.disabled
                    ? "text-foreground-muted cursor-not-allowed opacity-50"
                    : item.destructive
                      ? "text-semantic-error hover:bg-semantic-error/10"
                      : "text-foreground-primary hover:bg-background-tertiary"
                )}
              >
                {item.icon && (
                  <span className="flex-shrink-0 w-4 h-4 flex items-center justify-center">
                    {item.icon}
                  </span>
                )}
                <span className="flex-1">{item.label}</span>
              </button>
            </React.Fragment>
          ))}
        </div>
      </div>
    </>
  );
};

export default ContextMenu;
