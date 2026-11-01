import React, { useEffect } from 'react';
import { clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { X } from 'lucide-react';

export type ModalSize = 'sm' | 'md' | 'lg' | 'xl' | '2xl' | 'full';

export interface ModalProps {
  isOpen?: boolean;
  open?: boolean;
  onClose: () => void;
  title?: React.ReactNode;
  subtitle?: React.ReactNode;
  description?: React.ReactNode;
  size?: ModalSize;
  maxWidth?: string;
  children: React.ReactNode;
  footer?: React.ReactNode;
  className?: string;
  closeOnOverlayClick?: boolean;
}

const sizeClasses: Record<ModalSize, string> = {
  sm: 'max-w-md',
  md: 'max-w-lg',
  lg: 'max-w-2xl',
  xl: 'max-w-3xl',
  '2xl': 'max-w-4xl',
  full: 'max-w-6xl',
};

export const Modal: React.FC<ModalProps> = ({
  isOpen,
  open,
  onClose,
  title,
  subtitle,
  description,
  size = 'md',
  maxWidth,
  children,
  footer,
  className,
  closeOnOverlayClick = true,
}) => {
  const visible = Boolean(isOpen ?? open);
  const subText = subtitle ?? description;

  useEffect(() => {
    if (!visible) return;
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [visible, onClose]);

  if (!visible) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 overflow-y-auto"
      role="dialog"
      aria-modal="true"
    >
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs transition-opacity animate-fade-in"
        onClick={closeOnOverlayClick ? onClose : undefined}
        aria-hidden="true"
      />

      {/* Dialog Panel */}
      <div
        className={twMerge(
          clsx(
            'relative z-10 w-full bg-white rounded-2xl shadow-2xl border border-slate-200/90 overflow-hidden my-auto animate-slide-up',
            maxWidth || sizeClasses[size] || sizeClasses.md,
            className
          )
        )}
      >
        {/* Top decorative saffron-indigo accent bar */}
        <div className="h-1 w-full bg-gradient-to-r from-orange-500 via-indigo-600 to-emerald-500" />

        {(title || subText) && (
          <div className="flex items-start justify-between gap-4 px-6 py-4 border-b border-slate-100 bg-slate-50/50">
            <div>
              {title && (
                <h3 className="text-lg font-semibold text-slate-900 leading-snug">
                  {title}
                </h3>
              )}
              {subText && (
                <p className="mt-0.5 text-xs sm:text-sm text-slate-500">
                  {subText}
                </p>
              )}
            </div>
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors focus:outline-none focus:ring-2 focus:ring-indigo-500"
              aria-label="Close modal"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        )}

        {!title && !subText && (
          <button
            type="button"
            onClick={onClose}
            className="absolute top-3.5 right-3.5 z-10 rounded-lg p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
            aria-label="Close modal"
          >
            <X className="w-5 h-5" />
          </button>
        )}

        <div className="px-6 py-5 max-h-[78vh] overflow-y-auto">{children}</div>

        {footer && (
          <div className="px-6 py-3.5 bg-slate-50 border-t border-slate-100 flex items-center justify-end gap-2.5">
            {footer}
          </div>
        )}
      </div>
    </div>
  );
};

export default Modal;
