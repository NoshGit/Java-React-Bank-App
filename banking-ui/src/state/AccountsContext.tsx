import React, { useReducer, useCallback } from 'react';
import type { ReactNode } from 'react';
import { ApiError, getAccountsForCustomer, getMyAccounts, getTransactions } from '../api/client';
import type { Account, Transaction } from '../api/types';
import { AccountsContext } from './AccountsContextCore';

type State = {
  accounts: Account[];
  accountsLoading: boolean;
  accountsError: string | null;
  lookupLoading: boolean;
  lookedUpCustomer: string | null;
  selectedAccount: Account | null;
  transactions: Transaction[];
  transactionsLoading: boolean;
  transactionsError: string | null;
};

type Actions =
  | { type: 'LOAD_ACCOUNTS_START' }
  | { type: 'LOAD_ACCOUNTS_SUCCESS'; payload: Account[] }
  | { type: 'LOAD_ACCOUNTS_ERROR'; payload: string }
  | { type: 'REFRESH_CUSTOMER_START' }
  | { type: 'REFRESH_CUSTOMER_END'; payload: Account[] }
  | { type: 'SET_LOOKED_UP_CUSTOMER'; payload: string | null }
  | { type: 'SET_SELECTED_ACCOUNT'; payload: Account | null }
  | { type: 'LOAD_TRANSACTIONS_START' }
  | { type: 'LOAD_TRANSACTIONS_SUCCESS'; payload: Transaction[] }
  | { type: 'LOAD_TRANSACTIONS_ERROR'; payload: string }
  | { type: 'UPDATE_ACCOUNT'; payload: Account };

const initialState: State = {
  accounts: [],
  accountsLoading: false,
  accountsError: null,
  lookupLoading: false,
  lookedUpCustomer: null,
  selectedAccount: null,
  transactions: [],
  transactionsLoading: false,
  transactionsError: null,
};

function reducer(state: State, action: Actions): State {
  switch (action.type) {
    case 'LOAD_ACCOUNTS_START':
      return { ...state, accountsLoading: true, accountsError: null };
    case 'LOAD_ACCOUNTS_SUCCESS':
      return { ...state, accountsLoading: false, accounts: action.payload };
    case 'LOAD_ACCOUNTS_ERROR':
      return { ...state, accountsLoading: false, accountsError: action.payload };
    case 'REFRESH_CUSTOMER_START':
      return { ...state, lookupLoading: true, accountsError: null };
    case 'REFRESH_CUSTOMER_END':
      return { ...state, lookupLoading: false, accounts: action.payload };
    case 'SET_LOOKED_UP_CUSTOMER':
      return { ...state, lookedUpCustomer: action.payload };
    case 'SET_SELECTED_ACCOUNT':
      return { ...state, selectedAccount: action.payload };
    case 'LOAD_TRANSACTIONS_START':
      return { ...state, transactionsLoading: true, transactionsError: null };
    case 'LOAD_TRANSACTIONS_SUCCESS':
      return { ...state, transactionsLoading: false, transactions: action.payload };
    case 'LOAD_TRANSACTIONS_ERROR':
      return { ...state, transactionsLoading: false, transactionsError: action.payload };
    case 'UPDATE_ACCOUNT':
      return {
        ...state,
        selectedAccount: action.payload,
        accounts: state.accounts.map((a) => (a.accountId === action.payload.accountId ? action.payload : a)),
      };
    default:
      return state;
  }
}

export type ContextValue = State & {
  loadMyAccounts: () => Promise<Account[]>;
  refreshCustomerAccounts: (customerNumber: string) => Promise<Account[]>;
  setLookedUpCustomer: (customer: string | null) => void;
  selectAccount: (account: Account | null) => void;
  loadTransactions: (accountId: number) => Promise<void>;
  updateAccount: (account: Account) => void;
  clearTransactions: () => void;
};

export function AccountsProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(reducer, initialState);

  const loadMyAccounts = useCallback(async (): Promise<Account[]> => {
    dispatch({ type: 'LOAD_ACCOUNTS_START' });
    try {
      const data = await getMyAccounts();
      dispatch({ type: 'LOAD_ACCOUNTS_SUCCESS', payload: data });
      return data;
    } catch (e) {
      const msg = e instanceof ApiError ? e.message : 'Could not load your accounts.';
      dispatch({ type: 'LOAD_ACCOUNTS_ERROR', payload: msg });
      return [];
    }
  }, []);

  const refreshCustomerAccounts = useCallback(async (customerNumber: string): Promise<Account[]> => {
    dispatch({ type: 'REFRESH_CUSTOMER_START' });
    try {
      const data = await getAccountsForCustomer(customerNumber);
      dispatch({ type: 'REFRESH_CUSTOMER_END', payload: data });
      return data;
    } finally {
      // lookupLoading toggled in REFRESH_CUSTOMER_END
    }
  }, []);

  const selectAccount = useCallback((account: Account | null) => {
    dispatch({ type: 'SET_SELECTED_ACCOUNT', payload: account });
  }, []);

  const setLookedUpCustomer = useCallback((customer: string | null) => {
    dispatch({ type: 'SET_LOOKED_UP_CUSTOMER', payload: customer });
  }, []);

  const loadTransactions = useCallback(async (accountId: number): Promise<void> => {
    dispatch({ type: 'LOAD_TRANSACTIONS_START' });
    try {
      const data = await getTransactions(accountId);
      dispatch({ type: 'LOAD_TRANSACTIONS_SUCCESS', payload: data });
    } catch (e) {
      const msg = e instanceof ApiError ? e.message : 'Could not load transactions.';
      dispatch({ type: 'LOAD_TRANSACTIONS_ERROR', payload: msg });
    }
  }, []);

  const updateAccount = useCallback((account: Account) => {
    dispatch({ type: 'UPDATE_ACCOUNT', payload: account });
  }, []);

  const clearTransactions = useCallback(() => {
    dispatch({ type: 'LOAD_TRANSACTIONS_SUCCESS', payload: [] });
  }, []);

  const value: ContextValue = {
    ...state,
    loadMyAccounts,
    refreshCustomerAccounts,
    setLookedUpCustomer,
    selectAccount,
    loadTransactions,
    updateAccount,
    clearTransactions,
  };

  return <AccountsContext.Provider value={value}>{children}</AccountsContext.Provider>;
}

