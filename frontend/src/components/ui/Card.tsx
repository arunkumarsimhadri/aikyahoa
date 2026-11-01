import React from 'react';
import { clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export interface CardProps extends Omit<React.HTMLAttributes<HTMLDivElement>, 'title'> {
  title?: React.ReactNode;
  subtitle?: React.ReactNode;
  description?: React.ReactNode;
  action?: React.ReactNode;
  headerRight?: React.ReactNode;
  footer?: React.ReactNode;
  noPadding?: boolean;
  padding?: 'none' | 'sm' | 'md' | 'lg';
  hoverable?: boolean;
  accent?: 'none' | 'saffron' | 'indigo' | 'emerald' | 'rose' | 'amber';
}

const paddingMap = {
  none: 'p-0',
  sm: 'p-4',
  md: 'p-5 sm:p-6',
  lg: 'p-6 sm:p-8',
};

const accentMap = {
  none: '',
  saffron: 'border-t-4 border-t-orange-500',
  indigo: 'border-t-4 border-t-indigo-600',
  emerald: 'border-t-4 border-t-emerald-500',
  rose: 'border-t-4 border-t-rose-500',
  amber: 'border-t-4 border-t-amber-500',
};

export const Card: React.FC<CardProps> = ({
  title,
  subtitle,
  description,
  action,
  headerRight,
  footer,
  noPadding = false,
  padding = 'md',
  hoverable = false,
  accent = 'none',
  className,
  children,
  ...props
}) => {
  const subText = subtitle ?? description;
  const rightContent = action ?? headerRight;
  const hasHeader = Boolean(title || subText || rightContent);
  const resolvedPadding = noPadding ? 'none' : padding;

  return (
    <div
      className={twMerge(
        clsx(
          'bg-white rounded-2xl border border-slate-200/80 shadow-card overflow-hidden transition-all duration-200',
          hoverable && 'hover:shadow-soft hover:border-slate-300/90',
          accentMap[accent],
          className
        )
      )}
      {...props}
    >
      {hasHeader && (
        <div className="px-5 sm:px-6 py-4 border-b border-slate-100 flex flex-wrap items-center justify-between gap-3">
          <div>
            {title && (
              <h3 className="text-base font-semibold text-slate-900 leading-snug">
                {title}
              </h3>
            )}
            {subText && (
              <p className="mt-0.5 text-xs sm:text-sm text-slate-500">
                {subText}
              </p>
            )}
          </div>
          {rightContent && <div className="shrink-0">{rightContent}</div>}
        </div>
      )}

      <div className={clsx(paddingMap[resolvedPadding])}>{children}</div>

      {footer && (
        <div className="px-5 sm:px-6 py-3.5 bg-slate-50/70 border-t border-slate-100">
          {footer}
        </div>
      )}
    </div>
  );
};

export const CardHeader: React.FC<React.HTMLAttributes<HTMLDivElement>> = ({
  className,
  children,
  ...props
}) => (
  <div
    className={twMerge(
      clsx('px-5 sm:px-6 py-4 border-b border-slate-100 flex items-center justify-between gap-3', className)
    )}
    {...props}
  >
    {children}
  </div>
);

export const CardTitle: React.FC<React.HTMLAttributes<HTMLHeadingElement>> = ({
  className,
  children,
  ...props
}) => (
  <h3
    className={twMerge(clsx('text-base font-semibold text-slate-900 leading-snug', className))}
    {...props}
  >
    {children}
  </h3>
);

export const CardDescription: React.FC<React.HTMLAttributes<HTMLParagraphElement>> = ({
  className,
  children,
  ...props
}) => (
  <p className={twMerge(clsx('mt-0.5 text-xs sm:text-sm text-slate-500', className))} {...props}>
    {children}
  </p>
);

export const CardContent: React.FC<React.HTMLAttributes<HTMLDivElement>> = ({
  className,
  children,
  ...props
}) => (
  <div className={twMerge(clsx('p-5 sm:p-6', className))} {...props}>
    {children}
  </div>
);

export const CardFooter: React.FC<React.HTMLAttributes<HTMLDivElement>> = ({
  className,
  children,
  ...props
}) => (
  <div
    className={twMerge(
      clsx('px-5 sm:px-6 py-3.5 bg-slate-50/70 border-t border-slate-100 flex items-center', className)
    )}
    {...props}
  >
    {children}
  </div>
);

export default Card;
