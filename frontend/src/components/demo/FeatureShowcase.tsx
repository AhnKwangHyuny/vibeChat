import React from 'react';
import { Card } from '../ui/Card';
import { Badge } from '../ui/Badge';
import { Button } from '../ui/Button';
import { cn } from '../../utils/cn';

export interface FeatureShowcaseProps {
  title: string;
  description: string;
  features: Array<{
    icon: React.ReactNode;
    title: string;
    description: string;
  }>;
  imageUrl?: string;
  tags?: string[];
  className?: string;
}

const FeatureShowcase: React.FC<FeatureShowcaseProps> = ({
  title,
  description,
  features,
  imageUrl,
  tags = [],
  className
}) => {
  return (
    <Card className={cn('relative overflow-hidden group', className)}>
      {/* Background Image */}
      {imageUrl && (
        <div className="absolute inset-0 opacity-10">
          <img
            src={imageUrl}
            alt=""
            className="w-full h-full object-cover"
          />
        </div>
      )}

      {/* Content */}
      <div className="relative z-10">
        {/* Header */}
        <div className="mb-6">
          <div className="flex items-start justify-between mb-4">
            <div>
              <h3 className="text-2xl font-semibold text-foreground-primary mb-2">
                {title}
              </h3>
              <p className="text-foreground-muted leading-relaxed">
                {description}
              </p>
            </div>
            {tags.length > 0 && (
              <div className="flex flex-wrap gap-2">
                {tags.map((tag, index) => (
                  <Badge key={index} variant="default" size="sm">
                    {tag}
                  </Badge>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Features Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
          {features.map((feature, index) => (
            <div key={index} className="flex items-start gap-4">
              <div className="flex-shrink-0 w-12 h-12 bg-background-tertiary rounded-lg flex items-center justify-center text-foreground-primary">
                {feature.icon}
              </div>
              <div>
                <h4 className="text-lg font-medium text-foreground-primary mb-2">
                  {feature.title}
                </h4>
                <p className="text-sm text-foreground-muted">
                  {feature.description}
                </p>
              </div>
            </div>
          ))}
        </div>

        {/* Action Button */}
        <div className="flex justify-end">
          <Button variant="primary">
            자세히 알아보기
          </Button>
        </div>
      </div>
    </Card>
  );
};

export { FeatureShowcase };
