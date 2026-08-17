type SparklineProps = {
  values: number[];
  width?: number;
  height?: number;
  tone?: 'positive' | 'negative' | 'neutral';
};

/** Lightweight inline SVG sparkline -- no charting dependency. Renders a smooth line with a
 *  soft gradient fill, normalized to the data's own min/max so small accounts and large
 *  accounts both read clearly. */
export function Sparkline({ values, width = 120, height = 36, tone = 'neutral' }: SparklineProps) {
  if (values.length < 2) {
    return <svg width={width} height={height} aria-hidden="true" />;
  }

  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min || 1;
  const stepX = width / (values.length - 1);

  const points = values.map((v, i) => {
    const x = i * stepX;
    const y = height - ((v - min) / range) * (height - 4) - 2;
    return [x, y];
  });

  const linePath = points
    .map(([x, y], i) => `${i === 0 ? 'M' : 'L'} ${x.toFixed(1)} ${y.toFixed(1)}`)
    .join(' ');

  const areaPath = `${linePath} L ${width} ${height} L 0 ${height} Z`;

  const colorVar =
    tone === 'positive' ? 'var(--success-text)' : tone === 'negative' ? 'var(--danger-text)' : 'var(--text-tertiary)';
  const gradientId = `sparkline-fill-${tone}`;

  return (
    <svg width={width} height={height} viewBox={`0 0 ${width} ${height}`} aria-hidden="true">
      <defs>
        <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={colorVar} stopOpacity="0.22" />
          <stop offset="100%" stopColor={colorVar} stopOpacity="0" />
        </linearGradient>
      </defs>
      <path d={areaPath} fill={`url(#${gradientId})`} stroke="none" />
      <path d={linePath} fill="none" stroke={colorVar} strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" />
      <circle cx={points[points.length - 1][0]} cy={points[points.length - 1][1]} r="2.5" fill={colorVar} />
    </svg>
  );
}
