# Banking UI Starter

Starter project for **Module 8 Lab A** of the MD282 Java Full-Stack Development course.

This is the React application you will complete during the lab. The project structure, styling, and mock backend are already set up. Your job is to fill in the React-specific code marked with `// TODO:` comments.

## Prerequisites

- Node.js 20 or higher (`node --version`)
- npm 10 or higher (`npm --version`)
- Visual Studio Code

## VS Code extensions

When you open this folder in VS Code, you will be prompted to install the recommended extensions. Click **Install All**. The recommended extensions are:

- **ESLint** (Microsoft)
- **Prettier - Code formatter** (Prettier)
- **ES7+ React/Redux/React-Native snippets** (dsznajder)
- **Error Lens** (Alexander)
- **Path Intellisense** (Christian Kohler)

The project also includes a `.vscode/settings.json` that turns on Format on Save and uses Prettier as the default formatter for `.tsx`, `.ts`, `.json`, and `.css` files. No additional configuration is needed.

## Getting started

```bash
npm install
npm run dev
```

The dev server runs at [http://localhost:5173](http://localhost:5173).

## What this is

When you first start the application, the page will show a loading message that never resolves. That is expected. The React code that loads accounts and renders them has not been written yet. You will write it as you work through the lab.

The lab handout walks you through every step. Look for `// TODO:` comments in:

- `src/components/AccountList.tsx`
- `src/components/TransferForm.tsx`
- `src/App.tsx`

## Mock backend

This project uses Mock Service Worker (MSW) to simulate the banking API. Requests to `/api/accounts` and `/api/transfers` are intercepted by MSW handlers in `src/mocks/handlers.ts` and answered with canned data. No real backend is needed for this lab.

In a later lab you will replace MSW with calls to a real Spring Boot Backend-for-Frontend.

## Project structure

```
banking-ui-starter/
├── public/
│   └── bank.svg                ← favicon
├── src/
│   ├── api/
│   │   ├── client.ts           ← fetch wrapper (provided)
│   │   └── types.ts            ← TypeScript types (provided)
│   ├── components/
│   │   ├── AccountList.tsx     ← TODO sections
│   │   ├── TransferForm.tsx    ← TODO sections
│   │   └── Header.tsx          ← provided
│   ├── mocks/
│   │   ├── handlers.ts         ← MSW handlers (provided)
│   │   └── browser.ts          ← MSW worker (provided)
│   ├── App.tsx                 ← TODO sections
│   ├── App.css                 ← provided
│   ├── main.tsx                ← provided
│   └── index.css               ← provided
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
```

## After running `npm install`

The MSW package will install a service worker file at `public/mockServiceWorker.js`. This file is needed for MSW to work. It is generated automatically by the `npm install` step (configured by the `msw` block in `package.json`). If you do not see this file, run:

```bash
npx msw init public/ --save
```

## Available scripts

- `npm run dev` -- start the dev server with hot module replacement
- `npm run build` -- create a production build in `dist/`
- `npm run preview` -- serve the production build locally

## Useful documentation

- [React docs](https://react.dev) -- official React documentation
- [Vite docs](https://vitejs.dev) -- the build tool
- [MSW docs](https://mswjs.io) -- mock service worker
- [TypeScript docs](https://www.typescriptlang.org) -- TypeScript reference
