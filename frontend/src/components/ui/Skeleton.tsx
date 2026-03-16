// src/components/ui/Skeleton.tsx
import React from 'react';

const baseClass = 'animate-pulse bg-slate-200 rounded';

export const Skeleton: React.FC<{ className?: string }> = ({ className = '' }) => (
  <div className={`${baseClass} ${className}`} aria-hidden />
);

export const SkeletonText: React.FC<{ lines?: number; className?: string }> = ({
  lines = 1,
  className = '',
}) => (
  <div className={`space-y-2 ${className}`} aria-hidden>
    {Array.from({ length: lines }).map((_, i) => (
      <div
        key={i}
        className={`${baseClass} h-4 ${i === lines - 1 && lines > 1 ? 'w-3/4' : 'w-full'}`}
      />
    ))}
  </div>
);

export const SkeletonAvatar: React.FC<{ size?: number; className?: string }> = ({
  size = 10,
  className = '',
}) => (
  <div
    className={`${baseClass} ${className}`}
    style={{ width: size * 4, height: size * 4 }}
    aria-hidden
  />
);

export const SkeletonCard: React.FC<{ className?: string }> = ({ className = '' }) => (
  <div className={`${baseClass} h-24 ${className}`} aria-hidden />
);

export const SkeletonTableRow: React.FC<{ cols: number; className?: string }> = ({
  cols,
  className = '',
}) => (
  <tr className={className}>
    {Array.from({ length: cols }).map((_, i) => (
      <td key={i} className="p-3">
        <div className={`${baseClass} h-5 w-full`} />
      </td>
    ))}
  </tr>
);
