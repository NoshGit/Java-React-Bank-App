import type { TransactionType } from '../api/types';

const SIGN_BY_TYPE: Record<TransactionType, 1 | -1> = {
  DEPOSIT: 1,
  TRANSFER_IN: 1,
  WITHDRAWAL: -1,
  TRANSFER_OUT: -1,
  PAYMENT: -1,
};

export function transactionSign(type: TransactionType): 1 | -1 {
  return SIGN_BY_TYPE[type] ?? 1;
}
