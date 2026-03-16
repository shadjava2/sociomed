// src/components/SkeletonLayout.tsx — structure header + sidebar + zone contenu (auth / chargement route)
import React from 'react';
import { Skeleton, SkeletonText } from './ui/Skeleton';

export const SkeletonLayout: React.FC = () => (
  <div className="min-h-screen bg-slate-50 flex">
    {/* Header skeleton */}
    <header className="sticky top-0 z-40 h-14 bg-slate-200 animate-pulse" />

    {/* Sidebar skeleton (desktop) */}
    <aside className="hidden md:block w-64 border-r border-slate-200 bg-white p-4">
      <div className="flex items-center gap-3 mb-6">
        <Skeleton className="h-12 w-12 rounded-full" />
        <Skeleton className="h-4 w-24" />
      </div>
      <div className="space-y-4">
        <SkeletonText lines={2} className="mb-4" />
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-10 w-full" />
      </div>
    </aside>

    {/* Main content skeleton */}
    <main className="flex-1 p-6 md:ml-0">
      <div className="max-w-4xl space-y-4">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-4 w-full" />
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mt-6">
          <Skeleton className="h-24 rounded-xl" />
          <Skeleton className="h-24 rounded-xl" />
          <Skeleton className="h-24 rounded-xl" />
        </div>
      </div>
    </main>
  </div>
);
