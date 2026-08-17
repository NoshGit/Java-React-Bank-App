import { useEffect, useRef, useState, useCallback } from 'react';
import {
  sendChatMessage,
  SUGGESTED_QUESTIONS,
  type ChatMessage,
} from '../api/chat';
import './SupportChat.css';

interface DisplayMessage extends ChatMessage {
  id: string;
}

const WELCOME: DisplayMessage = {
  id: 'welcome',
  role: 'assistant',
  content:
    'Hi! I’m the Dynamic Bank support assistant. Ask me how to transfer money, pay someone, check your history, and more. I can’t see your account details, so please don’t share passwords, PINs, or OTPs.',
};

function uid(): string {
  return Math.random().toString(36).slice(2) + Date.now().toString(36);
}

/** Very small, safe inline formatter: **bold** and newlines only. */
function renderContent(text: string) {
  return text.split('\n').map((line, i) => {
    const parts = line.split(/(\*\*[^*]+\*\*)/g).map((seg, j) =>
      seg.startsWith('**') && seg.endsWith('**') ? (
        <strong key={j}>{seg.slice(2, -2)}</strong>
      ) : (
        <span key={j}>{seg}</span>
      ),
    );
    return (
      <span key={i} className="sc-line">
        {parts}
      </span>
    );
  });
}

export function SupportChat() {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<DisplayMessage[]>([WELCOME]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);

  const listRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Auto-scroll to the newest message.
  useEffect(() => {
    if (listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight;
    }
  }, [messages, sending, open]);

  // Focus the input when the panel opens.
  useEffect(() => {
    if (open) inputRef.current?.focus();
  }, [open]);

  // Close on Escape.
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setOpen(false);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open]);

  const submit = useCallback(
    async (raw: string) => {
      const text = raw.trim();
      if (!text || sending) return;

      const userMsg: DisplayMessage = { id: uid(), role: 'user', content: text };
      const history = [...messages, userMsg];
      setMessages(history);
      setInput('');
      setSending(true);

      try {
        const reply = await sendChatMessage(
          history.map(({ role, content }) => ({ role, content })),
        );
        setMessages((prev) => [
          ...prev,
          { id: uid(), role: 'assistant', content: reply.content },
        ]);
      } catch {
        setMessages((prev) => [
          ...prev,
          {
            id: uid(),
            role: 'assistant',
            content:
              'Sorry — something went wrong on my end. Please try again, or reach the team through the in-app Message Centre.',
          },
        ]);
      } finally {
        setSending(false);
      }
    },
    [messages, sending],
  );

  const onFormSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    submit(input);
  };

  return (
    <>
      {/* Launcher */}
      <button
        type="button"
        className={`sc-launcher${open ? ' is-open' : ''}`}
        onClick={() => setOpen((v) => !v)}
        aria-label={open ? 'Close support chat' : 'Open support chat'}
        aria-expanded={open}
      >
        {open ? (
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M6 6l12 12M18 6L6 18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
          </svg>
        ) : (
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path
              d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"
              stroke="currentColor"
              strokeWidth="1.8"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        )}
      </button>

      {/* Panel */}
      {open && (
        <section className="sc-panel card" role="dialog" aria-label="Dynamic Bank support assistant">
          <header className="sc-header">
            <span className="sc-avatar" aria-hidden="true">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path d="M12 2L2 7.5V9h20V7.5L12 2z" fill="currentColor" />
                <path d="M4 10v9h2v-9H4zM9 10v9h2v-9H9zM13 10v9h2v-9h-2zM18 10v9h2v-9h-2zM2 21h20v2H2z" fill="currentColor" />
              </svg>
            </span>
            <div className="sc-header-text">
              <span className="sc-title">Support Assistant</span>
              <span className="sc-subtitle">Dynamic Bank · FAQs &amp; help</span>
            </div>
            <button
              type="button"
              className="sc-close"
              onClick={() => setOpen(false)}
              aria-label="Close support chat"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path d="M6 6l12 12M18 6L6 18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              </svg>
            </button>
          </header>

          <div className="sc-messages" ref={listRef}>
            {messages.map((m) => (
              <div key={m.id} className={`sc-msg sc-msg-${m.role}`}>
                <div className="sc-bubble">{renderContent(m.content)}</div>
              </div>
            ))}

            {sending && (
              <div className="sc-msg sc-msg-assistant">
                <div className="sc-bubble sc-typing" aria-label="Assistant is typing">
                  <span />
                  <span />
                  <span />
                </div>
              </div>
            )}

            {/* Starter chips (only before the first user turn). */}
            {messages.length === 1 && !sending && (
              <div className="sc-suggestions">
                {SUGGESTED_QUESTIONS.map((q) => (
                  <button key={q} type="button" className="sc-chip" onClick={() => submit(q)}>
                    {q}
                  </button>
                ))}
              </div>
            )}
          </div>

          <form className="sc-input-row" onSubmit={onFormSubmit}>
            <input
              ref={inputRef}
              type="text"
              className="form-control sc-input"
              placeholder="Ask a question…"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              disabled={sending}
              aria-label="Type your message"
              autoComplete="off"
            />
            <button
              type="submit"
              className="sc-send"
              disabled={sending || !input.trim()}
              aria-label="Send message"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </button>
          </form>

          <p className="sc-disclaimer">
            Automated assistant. Don’t share passwords, PINs, or OTPs. For account-specific help, use the Message Centre.
          </p>
        </section>
      )}
    </>
  );
}
