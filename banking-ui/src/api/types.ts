export type AccountType = 'CHECKING' | 'SAVINGS';
export type AccountStatus = 'ACTIVE' | 'INACTIVE';
export type TransactionType = 'DEPOSIT' | 'WITHDRAWAL' | 'TRANSFER_IN' | 'TRANSFER_OUT' | 'PAYMENT';
export type TransactionStatus = 'COMPLETED' | 'FAILED';
export type Role = 'teller' | 'account_holder' | 'auditor';

export type Account = {
  accountId: number;
  accountNumber: string;
  customerNumber: string;
  accountType: AccountType;
  accountStatus: AccountStatus;
  balance: number;
  openedDate: string;
  fullName: string;
  profileImageUrl: string;
};

export type Transaction = {
  txnId: string;
  accountId: number;
  txnType: TransactionType;
  amount: number;
  status: TransactionStatus;
  txnDate: string;
  description: string | null;
};

export type TransferRequest = {
  toAccountId: number;
  amount: number;
};

export type PaymentRequest = {
  amount: number;
  reference: string;
};

export type AmountRequest = {
  amount: number;
};

export type StatusUpdateRequest = {
  status: AccountStatus;
};

export type TransferResult = {
  transferId: string | null;
  fromAccountId: number | null;
  toAccountId: number | null;
  amount: number;
  status: string;
};

export type TransactionReportLine = {
  txnType: string;
  count: number;
  totalAmount: number;
};

export type User = {
  subject: string;
  preferredUsername: string;
  fullName: string;
  roles: Role[];
};
