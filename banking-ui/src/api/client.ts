import type {
  Account,
  AmountRequest,
  PaymentRequest,
  StatusUpdateRequest,
  Transaction,
  TransactionReportLine,
  TransferRequest,
  TransferResult,
  User,
} from './types';

/** Carries the real HTTP status from bankbff/bankapi so callers can branch on it
 *  (403 ownership, 422 business rule, 502 external service, 404 not found). */
export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

function defaultMessageForStatus(status: number): string {
  switch (status) {
    case 400:
      return 'That request was invalid. Please check the values you entered.';
    case 401:
      return 'Your session has expired. Please sign in again.';
    case 403:
      return 'You are not authorized to perform this action.';
    case 404:
      return 'The requested account or customer could not be found.';
    case 422:
      return 'This could not be completed — check the account is active and has sufficient funds.';
    case 502:
      return 'The payment service is temporarily unavailable. Please try again shortly.';
    default:
      return `Something went wrong (${status}). Please try again.`;
  }
}

async function safeReadErrorMessage(response: Response): Promise<string | null> {
  try {
    const body = await response.json();
    if (body && typeof body.message === 'string') {
      return body.message;
    }
    return null;
  } catch {
    return null;
  }
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const message = await safeReadErrorMessage(response);
    throw new ApiError(response.status, message ?? defaultMessageForStatus(response.status));
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

function jsonHeaders(): HeadersInit {
  return { Accept: 'application/json', 'Content-Type': 'application/json' };
}

export async function getCurrentUser(): Promise<User | null> {
  const response = await fetch('/api/me', { headers: { Accept: 'application/json' } });
  if (response.status === 401) {
    return null;
  }
  return handleResponse<User>(response);
}

export async function getMyAccounts(): Promise<Account[]> {
  const response = await fetch('/api/accounts', { headers: { Accept: 'application/json' } });
  return handleResponse<Account[]>(response);
}

export async function getAccountsForCustomer(customerNumber: string): Promise<Account[]> {
  const response = await fetch(`/api/customers/${encodeURIComponent(customerNumber)}/accounts`, {
    headers: { Accept: 'application/json' },
  });
  return handleResponse<Account[]>(response);
}

export async function getTransactions(accountId: number): Promise<Transaction[]> {
  const response = await fetch(`/api/accounts/${accountId}/transactions`, {
    headers: { Accept: 'application/json' },
  });
  return handleResponse<Transaction[]>(response);
}

export async function postTransfer(accountId: number, request: TransferRequest): Promise<TransferResult> {
  const response = await fetch(`/api/accounts/${accountId}/transfers`, {
    method: 'POST',
    headers: jsonHeaders(),
    body: JSON.stringify(request),
  });
  return handleResponse<TransferResult>(response);
}

export async function postPayment(accountId: number, request: PaymentRequest): Promise<TransferResult> {
  const response = await fetch(`/api/accounts/${accountId}/payments`, {
    method: 'POST',
    headers: jsonHeaders(),
    body: JSON.stringify(request),
  });
  return handleResponse<TransferResult>(response);
}

export async function postDeposit(accountId: number, request: AmountRequest): Promise<TransferResult> {
  const response = await fetch(`/api/accounts/${accountId}/deposits`, {
    method: 'POST',
    headers: jsonHeaders(),
    body: JSON.stringify(request),
  });
  return handleResponse<TransferResult>(response);
}

export async function postWithdrawal(accountId: number, request: AmountRequest): Promise<TransferResult> {
  const response = await fetch(`/api/accounts/${accountId}/withdrawals`, {
    method: 'POST',
    headers: jsonHeaders(),
    body: JSON.stringify(request),
  });
  return handleResponse<TransferResult>(response);
}

export async function putAccountStatus(accountId: number, request: StatusUpdateRequest): Promise<Account> {
  const response = await fetch(`/api/accounts/${accountId}/status`, {
    method: 'PUT',
    headers: jsonHeaders(),
    body: JSON.stringify(request),
  });
  return handleResponse<Account>(response);
}

export async function getTransactionReport(): Promise<TransactionReportLine[]> {
  const response = await fetch('/api/reports/transactions', {
    headers: { Accept: 'application/json' },
  });
  return handleResponse<TransactionReportLine[]>(response);
}

export async function logout(): Promise<void> {
  const response = await fetch('/logout', {
    method: 'POST',
    headers: { Accept: 'application/json' },
  });
  if (!response.ok && response.status !== 302) {
    throw new ApiError(response.status, 'Logout failed.');
  }
}
