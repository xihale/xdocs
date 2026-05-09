import React, { useEffect } from "react";
import { X, AlertTriangle, CheckCircle2, Info } from "lucide-react";

interface ModalProps {
  open: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
  footer?: React.ReactNode;
  type?: "danger" | "info" | "success";
}

export function Modal({ open, onClose, title, children, footer, type = "info" }: ModalProps) {
  useEffect(() => {
    if (open) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "unset";
    }
    return () => {
      document.body.style.overflow = "unset";
    };
  }, [open]);

  if (!open) return null;

  const Icon = type === "danger" ? AlertTriangle : type === "success" ? CheckCircle2 : Info;
  const iconColor = type === "danger" ? "text-error" : type === "success" ? "text-green-500" : "text-primary";

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 sm:p-6">
      {/* Backdrop */}
      <div 
        className="absolute inset-0 bg-black/40 backdrop-blur-sm transition-opacity" 
        onClick={onClose}
      />

      {/* Dialog */}
      <div className="relative w-full max-w-md bg-surface-container-lowest overflow-hidden transform transition-all animate-in zoom-in-95 duration-200">
        <div className="flex items-center justify-between px-6 py-4 border-b border-outline-variant">
          <div className="flex items-center gap-3">
            <Icon className={`w-5 h-5 ${iconColor}`} />
            <h3 className="text-lg font-semibold text-on-surface">{title}</h3>
          </div>
          <button 
            onClick={onClose}
            className="p-1.5 hover:bg-surface-container rounded-full text-on-surface-variant transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="px-6 py-6 text-sm text-on-surface-variant leading-relaxed">
          {children}
        </div>

        {footer && (
          <div className="px-6 py-4 bg-surface-container-low border-t border-outline-variant flex justify-end gap-3">
            {footer}
          </div>
        )}
      </div>
    </div>
  );
}

interface ConfirmModalProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  loading?: boolean;
  danger?: boolean;
}

export function ConfirmModal({
  open,
  onClose,
  onConfirm,
  title,
  message,
  confirmText = "确认",
  cancelText = "取消",
  loading = false,
  danger = false,
}: ConfirmModalProps) {
  return (
    <Modal
      open={open}
      onClose={onClose}
      title={title}
      type={danger ? "danger" : "info"}
      footer={
        <>
          <button
            onClick={onClose}
            disabled={loading}
            className="px-4 py-2 text-sm font-medium text-on-surface-variant hover:bg-surface-container transition-colors"
          >
            {cancelText}
          </button>
          <button
            onClick={onConfirm}
            disabled={loading}
            className={`px-4 py-2 text-sm font-medium transition-colors ${
              danger
                ? "bg-error text-on-error hover:opacity-90"
                : "bg-primary text-on-primary hover:opacity-90"
            } disabled:opacity-50`}
          >
            {loading ? "处理中..." : confirmText}
          </button>
        </>
      }
    >
      {message}
    </Modal>
  );
}
