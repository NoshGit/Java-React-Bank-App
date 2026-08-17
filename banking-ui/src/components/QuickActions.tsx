import { IconDeposit, IconPay, IconTransfer, IconWithdraw } from './icons/Icons';

export type QuickAction = 'deposit' | 'withdrawal' | 'transfer' | 'pay';

type QuickActionsProps = {
  isTeller: boolean;
  onAction: (action: QuickAction) => void;
};

export function QuickActions({ isTeller, onAction }: QuickActionsProps) {
  return (
    <div className="quick-actions">
      {isTeller ? (
        <>
          <button type="button" className="quick-action-btn card" onClick={() => onAction('deposit')}>
            <IconDeposit />
            <span>Deposit</span>
          </button>
          <button type="button" className="quick-action-btn card" onClick={() => onAction('withdrawal')}>
            <IconWithdraw />
            <span>Withdraw</span>
          </button>
          <button type="button" className="quick-action-btn card" onClick={() => onAction('transfer')}>
            <IconTransfer />
            <span>Transfer money</span>
          </button>
          <button type="button" className="quick-action-btn card" onClick={() => onAction('pay')}>
            <IconPay />
            <span>Make a payment</span>
          </button>
        </>
      ) : (
        <>
          <button type="button" className="quick-action-btn card" onClick={() => onAction('transfer')}>
            <IconTransfer />
            <span>Transfer money</span>
          </button>
          <button type="button" className="quick-action-btn card" onClick={() => onAction('pay')}>
            <IconPay />
            <span>Make a payment</span>
          </button>
        </>
      )}
    </div>
  );
}
