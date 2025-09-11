import React from 'react';

interface SkeletonProps {
  width?: string;
  height?: string;
  className?: string;
}

const Skeleton: React.FC<SkeletonProps> = ({ width = '100%', height = '1em', className = '' }) => {
  return (
    <div
      data-testid="skeleton"
      className={`bg-gray-300 animate-pulse rounded ${className}`}
      style={{ width, height }}
    />
  );
};

export default Skeleton;
