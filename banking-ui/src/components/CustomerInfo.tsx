import { useAccounts } from '../state/useAccounts';
import { maskAccountNumber } from '../utils/format';

type CustomerInfoProps = {
    lookedUpCustomer: string | null;
};

export default function CustomerInfo({ lookedUpCustomer }: CustomerInfoProps) {
    const { accounts } = useAccounts();
    return (
        <div className="customer-info-card card">
            <div className="d-flex align-items-center">
                <div className="customer-avatar">
                    {accounts[0].profileImageUrl ? (
                        <img src={accounts[0].profileImageUrl} alt={`${accounts[0].fullName} avatar`} />
                    ) : (
                        <div className="customer-avatar-fallback">{accounts[0].fullName?.charAt(0).toUpperCase()}</div>
                    )}
                </div>

                <div className="customer-meta ms-3">
                    <div className="customer-name">{accounts[0].fullName}</div>

                    <div className="customer-details">
                        <span className="customer-acc"><strong>Primary Account: </strong> {maskAccountNumber(accounts[0].accountNumber)}</span>
                        {lookedUpCustomer && (<span className="customer-id"><strong>Customer ID: </strong> {lookedUpCustomer}</span>)}
                    </div>
                </div>
            </div>
        </div>
    )
}