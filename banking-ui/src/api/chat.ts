/* ============================================================================
 * Support chat service
 * ----------------------------------------------------------------------------
 * Powers the customer-support FAQ assistant. It resolves an answer through
 * three tiers, in this order of preference, and always falls back so the widget
 * never hard-fails:
 *
 *   1. proxy  – POST to the bankbff endpoint (/api/support/chat). The Gen AI
 *               provider key lives on the SERVER. This is the recommended mode
 *               for a banking app: no secret ships to the browser.
 *   2. gemini – call Google Gemini's free tier directly from the browser using
 *               VITE_GEMINI_API_KEY. Convenient for local/training use ONLY —
 *               the key is visible in the built JS bundle, so never point this
 *               at a production/paid key.
 *   3. local  – a built-in keyword-matched FAQ knowledge base. No network, no
 *               key, no cost. Always available, and the fallback if a remote
 *               call fails.
 *
 * Mode is chosen automatically from env, or forced with VITE_CHAT_MODE.
 * ==========================================================================*/

export type ChatRole = 'user' | 'assistant';

export interface ChatMessage {
  role: ChatRole;
  content: string;
}

export interface ChatReply {
  content: string;
  /** Which tier produced the answer — handy for a small "source" hint in the UI. */
  source: 'local' | 'gemini' | 'proxy';
}

type ChatMode = 'local' | 'gemini' | 'proxy';

/* ----------------------------------------------------------------------------
 * Configuration (read from Vite env — see .env.example)
 * --------------------------------------------------------------------------*/

const ENV = import.meta.env as Record<string, string | undefined>;

const GEMINI_KEY = ENV.VITE_GEMINI_API_KEY?.trim() || '';
const GEMINI_MODEL = ENV.VITE_GEMINI_MODEL?.trim() || 'gemini-2.5-flash';
// Model IDs change over time; override with VITE_GEMINI_MODEL if this one is
// retired. Check https://ai.google.dev/gemini-api/docs/models for current IDs.

const PROXY_URL = ENV.VITE_CHAT_PROXY_URL?.trim() || '/api/support/chat';
const PROXY_ENABLED = (ENV.VITE_CHAT_PROXY_ENABLED?.trim() || '') === 'true';

function resolveMode(): ChatMode {
  const forced = ENV.VITE_CHAT_MODE?.trim() as ChatMode | undefined;
  if (forced === 'local' || forced === 'gemini' || forced === 'proxy') return forced;
  if (PROXY_ENABLED) return 'proxy';
  if (GEMINI_KEY) return 'gemini';
  return 'local';
}

/* ----------------------------------------------------------------------------
 * System prompt — keeps the model on-task and safe for a banking context.
 * --------------------------------------------------------------------------*/

const SYSTEM_PROMPT = `You are the Dynamic Bank Support Assistant, a friendly customer-support FAQ chatbot inside the Dynamic Bank web app.

Scope:
- Help customers and tellers understand how to use the app: signing in, dashboards, transfers, paying someone, deposits, withdrawals, transaction history, reports, account status, fees, and general banking questions.
- Keep answers short, clear, and step-by-step where useful. Use plain language.

Hard rules:
- You have NO access to any customer's real accounts, balances, transactions, passwords, OTPs, or personal data. Never claim to look these up or reveal them.
- Never ask for or accept full card numbers, PINs, passwords, OTPs, or CVV. If a user shares one, tell them not to and to keep it private.
- For anything account-specific, sensitive, or that you cannot answer, direct the user to the in-app actions or to contact Dynamic Bank support (support line and secure Message Centre inside the app).
- If a request is out of scope (not about banking or this app), politely say so and steer back to how you can help.
- Do not give financial, legal, or investment advice. Suggest speaking to a qualified advisor.

Style: warm, concise, professional. Prefer a couple of sentences or a short numbered list. Sentence case.`;

/* ----------------------------------------------------------------------------
 * Tier 3: local FAQ knowledge base (keyword-scored)
 * --------------------------------------------------------------------------*/

interface FaqEntry {
  /** Lowercase keywords/phrases; more matches => higher score. */
  keywords: string[];
  answer: string;
}

const FAQ_KB: FaqEntry[] = [
  {
    keywords: ['transfer', 'move money', 'between accounts', 'send to my account'],
    answer:
      'To move money between your own accounts:\n1. Open **Fund Transfer** from the sidebar.\n2. Pick the source and destination accounts.\n3. Enter the amount and confirm.\nBoth accounts must be active and the source needs sufficient funds.',
  },
  {
    keywords: ['pay', 'payment', 'pay someone', 'send money', 'payee', 'beneficiary'],
    answer:
      'To pay someone, open **Pay** from the sidebar, choose the account to pay from, enter the payee details and amount, then confirm. Payments are processed through the bank’s payment service and appear in your Transaction History once completed.',
  },
  {
    keywords: ['deposit', 'add money', 'cash in', 'pay in'],
    answer:
      'Deposits are handled at the branch by a teller. A teller opens **Deposit**, looks up the customer, selects the account, and enters the amount. As a customer you’ll see the deposit in your Transaction History and updated balance right after it posts.',
  },
  {
    keywords: ['withdraw', 'withdrawal', 'take out', 'cash out'],
    answer:
      'Withdrawals are processed by a teller from the **Withdrawal** screen. The account must be active and have enough available balance. The new balance and a matching entry appear in Transaction History once done.',
  },
  {
    keywords: ['history', 'transactions', 'statement', 'past activity', 'view transactions'],
    answer:
      'Open **Transaction History** from the sidebar to search and review past activity. You can filter and page through entries. For an official statement, use the Message Centre to request one.',
  },
  {
    keywords: ['report', 'reports', 'summary by type'],
    answer:
      'The **Reports** page (available to tellers) shows completed transactions grouped by type across customers. Use the KPI cards and table to review activity at a glance.',
  },
  {
    keywords: ['activate', 'deactivate', 'account status', 'active', 'inactive', 'freeze', 'block account'],
    answer:
      'Account activation/deactivation is done by a teller on the **Account Status** screen. If your account looks inactive and you didn’t request that, contact support so they can review and reactivate it.',
  },
  {
    keywords: ['sign in', 'login', 'log in', 'cannot log in', "can't log in", 'access account'],
    answer:
      'Use the Sign in button on the landing page. Sign-in is handled securely by the bank’s identity service. If you’re stuck, make sure cookies are enabled and try again, then contact support if it persists.',
  },
  {
    keywords: ['password', 'reset password', 'forgot password', 'change password'],
    answer:
      'For security, passwords are managed through the bank’s secure sign-in flow, not inside this chat. Use the “Forgot password” option on the sign-in screen, or contact support. Never share your password with anyone — including this assistant.',
  },
  {
    keywords: ['fee', 'fees', 'charges', 'cost', 'how much'],
    answer:
      'Standard app actions like transfers between your own accounts are typically free, but specific fees depend on your account type and the transaction. For exact fees, check your account terms or ask support through the Message Centre.',
  },
  {
    keywords: ['fraud', 'scam', 'unauthorized', 'stolen', 'lost card', 'suspicious', 'hacked'],
    answer:
      '⚠️ If you suspect fraud or an unauthorized transaction, act quickly:\n1. Contact Dynamic Bank support immediately via the emergency line.\n2. Ask to freeze the affected account/card.\n3. Review recent activity in Transaction History.\nNever share passwords, PINs, or OTPs with anyone.',
  },
  {
    keywords: ['contact', 'support', 'help', 'phone', 'call', 'human', 'agent', 'talk to someone'],
    answer:
      'You can reach a person through the in-app **Message Centre**, or the Dynamic Bank support line shown on your statements and the bank’s website. For urgent fraud issues, use the emergency line.',
  },
  {
    keywords: ['hours', 'open', 'timing', 'available', 'when'],
    answer:
      'The app is available 24/7 for transfers, payments, and history. Branch and phone-support hours vary by location — check the bank’s website or your branch details for exact times.',
  },
  {
    keywords: ['balance', 'how much money', 'my balance', 'available funds'],
    answer:
      'Your current balances are shown on the **Dashboard** and on each account card. I can’t look up your balance directly from here, but you’ll always see the latest figure once you’re signed in.',
  },
  {
    keywords: ['dark mode', 'theme', 'light mode', 'appearance'],
    answer:
      'You can switch between light and dark themes using the theme toggle in the top bar. Your choice is remembered on this device.',
  },
  {
    keywords: ['what can you do', 'help me', 'options', 'menu', 'start'],
    answer:
      'I can help with how-tos for transfers, payments, deposits, withdrawals, transaction history, reports, account status, sign-in, and general banking questions. What would you like to do?',
  },
];

const GREETING_KEYS = ['hi', 'hello', 'hey', 'good morning', 'good afternoon', 'good evening', 'yo'];
const THANKS_KEYS = ['thank', 'thanks', 'cheers', 'appreciate'];

function tokenize(text: string): string[] {
  return text.toLowerCase().replace(/[^a-z0-9\s']/g, ' ').split(/\s+/).filter(Boolean);
}

/** Returns the best FAQ answer for a question, or null if nothing matches well. */
export function answerLocally(question: string): string | null {
  const q = question.toLowerCase().trim();
  if (!q) return null;

  const words = new Set(tokenize(q));

  if (GREETING_KEYS.some((g) => q === g || q.startsWith(g + ' '))) {
    return 'Hi! I’m the Dynamic Bank support assistant. Ask me how to transfer money, pay someone, view your history, and more.';
  }
  if (THANKS_KEYS.some((t) => words.has(t))) {
    return 'You’re welcome! Is there anything else I can help you with?';
  }

  let best: { score: number; answer: string } | null = null;

  for (const entry of FAQ_KB) {
    let score = 0;
    for (const kw of entry.keywords) {
      if (kw.includes(' ')) {
        // multi-word phrase: strong signal if present verbatim
        if (q.includes(kw)) score += 3;
      } else if (words.has(kw)) {
        score += 1;
      }
    }
    if (score > 0 && (!best || score > best.score)) {
      best = { score, answer: entry.answer };
    }
  }

  return best ? best.answer : null;
}

const LOCAL_FALLBACK =
  'I’m not sure about that one. I can help with transfers, payments, deposits, withdrawals, transaction history, reports, account status, and sign-in. For anything account-specific, please use the in-app Message Centre or contact Dynamic Bank support.';

/* Suggested starter questions surfaced as quick-reply chips in the UI. */
export const SUGGESTED_QUESTIONS: string[] = [
  'How do I transfer money?',
  'How do I pay someone?',
  'Where can I see my transactions?',
  'I think there’s a fraudulent charge',
];

/* ----------------------------------------------------------------------------
 * Tier 2: Gemini free tier (direct from browser — dev/training only)
 * --------------------------------------------------------------------------*/

async function askGemini(messages: ChatMessage[]): Promise<string> {
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent?key=${encodeURIComponent(
    GEMINI_KEY,
  )}`;

  const body = {
    system_instruction: { parts: [{ text: SYSTEM_PROMPT }] },
    contents: messages.map((m) => ({
      role: m.role === 'assistant' ? 'model' : 'user',
      parts: [{ text: m.content }],
    })),
    generationConfig: { temperature: 0.4, maxOutputTokens: 400 },
  };

  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });

  if (!res.ok) {
    throw new Error(`Gemini request failed (${res.status})`);
  }

  const data = await res.json();
  const text: string | undefined = data?.candidates?.[0]?.content?.parts
    ?.map((p: { text?: string }) => p.text ?? '')
    .join('')
    .trim();

  if (!text) throw new Error('Gemini returned an empty response');
  return text;
}

/* ----------------------------------------------------------------------------
 * Tier 1: BFF proxy (key stays on the server — recommended)
 * --------------------------------------------------------------------------*/

async function askProxy(messages: ChatMessage[]): Promise<string> {
  const res = await fetch(PROXY_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ messages }),
  });

  if (!res.ok) {
    throw new Error(`Support proxy failed (${res.status})`);
  }

  const data = await res.json();
  const text: string | undefined = (data?.reply ?? data?.content)?.toString().trim();
  if (!text) throw new Error('Support proxy returned an empty response');
  return text;
}

/* ----------------------------------------------------------------------------
 * Public entry point
 * --------------------------------------------------------------------------*/

/**
 * Resolve a reply for the latest user turn. `messages` is the full conversation
 * (oldest first), ending with the user's newest message.
 */
export async function sendChatMessage(messages: ChatMessage[]): Promise<ChatReply> {
  const latest = messages[messages.length - 1]?.content ?? '';
  const mode = resolveMode();

  if (mode === 'local') {
    return { content: answerLocally(latest) ?? LOCAL_FALLBACK, source: 'local' };
  }

  try {
    const content = mode === 'proxy' ? await askProxy(messages) : await askGemini(messages);
    return { content, source: mode };
  } catch (err) {
    // Never leave the user stranded — fall back to the local knowledge base.
    console.warn(`Support chat "${mode}" mode failed, using local FAQ fallback:`, err);
    return { content: answerLocally(latest) ?? LOCAL_FALLBACK, source: 'local' };
  }
}

export const chatMode = resolveMode();
