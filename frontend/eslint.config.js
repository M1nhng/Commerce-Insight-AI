// @ts-check
// Flat config for ESLint 9 (Sprint 13A). Minimal, deterministic — recommended
// rules only, NO type-checked rules (keeps CI fast and avoids a project-service
// setup). `tsc --noEmit` remains the source of truth for type correctness.
import js from '@eslint/js'
import tseslint from 'typescript-eslint'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import globals from 'globals'

export default tseslint.config(
  {
    ignores: ['dist/**', 'coverage/**', 'playwright-report/**', 'test-results/**', 'node_modules/**'],
  },

  js.configs.recommended,
  ...tseslint.configs.recommended,

  // App + test source
  {
    files: ['src/**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2022,
      globals: { ...globals.browser, ...globals.es2021 },
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      // HMR-only ergonomics, not correctness — advisory.
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
      // tsc (noUnusedLocals/Parameters) already enforces this; allow `_`-prefix escape.
      '@typescript-eslint/no-unused-vars': [
        'error',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_', caughtErrors: 'none' },
      ],
      // `tsc --strict` (noImplicitAny) already forbids *implicit* any. Explicit
      // `any` is a deliberate escape hatch used in ~13 pre-existing spots
      // (Recharts callbacks, form adapters). Sprint 13A does not do a style
      // refactor — keep it advisory rather than blocking CI.
      '@typescript-eslint/no-explicit-any': 'warn',
      // Filename / input sanitisers intentionally match control-char ranges.
      'no-control-regex': 'off',
      // shadcn/ui generates `interface FooProps extends React.X {}` wrappers —
      // an established pattern, not worth a project-wide rewrite.
      '@typescript-eslint/no-empty-object-type': ['error', { allowInterfaces: 'with-single-extends' }],
    },
  },

  // Vitest unit tests + Vitest setup
  {
    files: ['src/**/*.{test,spec}.{ts,tsx}', 'src/test/**/*.{ts,tsx}'],
    languageOptions: { globals: { ...globals.node } },
    rules: {
      '@typescript-eslint/no-explicit-any': 'off',
    },
  },

  // Playwright E2E + Node-side tooling configs
  {
    files: ['e2e/**/*.ts', '*.config.{ts,js}', 'vite.config.ts', 'vitest.config.ts'],
    languageOptions: {
      ecmaVersion: 2022,
      globals: { ...globals.node },
    },
    rules: {
      '@typescript-eslint/no-explicit-any': 'off',
    },
  },
)
