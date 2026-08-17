type AlertProps = {
  tone: 'success' | 'error' | 'info';
  children: React.ReactNode;
};

const BOOTSTRAP_TONE: Record<AlertProps['tone'], string> = {
  success: 'alert-success',
  error: 'alert-danger',
  info: 'alert-info',
};

export function Alert({ tone, children }: AlertProps) {
  return (
    <div className={`alert ${BOOTSTRAP_TONE[tone]} mt-3 mb-0`} role={tone === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  );
}
