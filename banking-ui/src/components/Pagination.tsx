import { IconChevronLeft, IconChevronRight } from './icons/Icons';

type PaginationProps = {
  page: number;
  pageCount: number;
  onPageChange: (page: number) => void;
  totalItems: number;
  pageSize: number;
};

export function Pagination({ page, pageCount, onPageChange, totalItems, pageSize }: PaginationProps) {
  if (pageCount <= 1) return null;

  const start = (page - 1) * pageSize + 1;
  const end = Math.min(page * pageSize, totalItems);

  return (
    <nav
      aria-label="Pagination"
      className="d-flex align-items-center justify-content-between flex-wrap gap-3 mt-4 pt-3 border-top"
    >
      <span className="small text-body-secondary">
        {start}–{end} of {totalItems}
      </span>
      <ul className="pagination mb-0">
        <li className={`page-item${page === 1 ? ' disabled' : ''}`}>
          <button type="button" className="page-link" onClick={() => onPageChange(page - 1)} aria-label="Previous page">
            <IconChevronLeft />
          </button>
        </li>
        <li className="page-item disabled">
          <span className="page-link">
            Page {page} of {pageCount}
          </span>
        </li>
        <li className={`page-item${page === pageCount ? ' disabled' : ''}`}>
          <button type="button" className="page-link" onClick={() => onPageChange(page + 1)} aria-label="Next page">
            <IconChevronRight />
          </button>
        </li>
      </ul>
    </nav>
  );
}
