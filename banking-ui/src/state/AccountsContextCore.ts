import { createContext } from 'react';
import type { ContextValue } from './AccountsContext';

export const AccountsContext = createContext<ContextValue | undefined>(undefined);
