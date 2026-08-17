type StatusBadgeProps = {
  status: string;
  tone?: 'success' | 'neutral' | 'warning' | 'danger';
};

const TONE_BY_STATUS: Record<string, 'success' | 'neutral' | 'warning' | 'danger'> = {
  ACTIVE: 'success',
  COMPLETED: 'success',
  INACTIVE: 'neutral',
  FAILED: 'danger',
};

const BOOTSTRAP_TONE: Record<'success' | 'neutral' | 'warning' | 'danger', string> = {
  success: 'text-bg-success',
  neutral: 'text-bg-secondary',
  warning: 'text-bg-warning',
  danger: 'text-bg-danger',
};

export function StatusBadge({ status, tone }: StatusBadgeProps) {
  const resolvedTone = tone ?? TONE_BY_STATUS[status] ?? 'neutral';
  return <span className={`badge ${BOOTSTRAP_TONE[resolvedTone]}`}>{status.replace(/_/g, ' ')}</span>;
}
