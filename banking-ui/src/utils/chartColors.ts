import type { TransactionType } from '../api/types';

/** Brand-consistent categorical palette (drawn from the app's own design tokens) so charts
 *  read as part of this bank's identity rather than a generic charting-library default. */
export const CHART_COLOR_BY_TYPE: Record<TransactionType, string> = {
  DEPOSIT: '#0f7a4f',
  TRANSFER_IN: '#b98a2e',
  TRANSFER_OUT: '#21395a',
  WITHDRAWAL: '#b3261e',
  PAYMENT: '#3a6ea5',
};

export const TYPE_LABEL: Record<TransactionType, string> = {
  DEPOSIT: 'Deposits',
  WITHDRAWAL: 'Withdrawals',
  TRANSFER_IN: 'Transfers in',
  TRANSFER_OUT: 'Transfers out',
  PAYMENT: 'Payments',
};
