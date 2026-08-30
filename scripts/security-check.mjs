#!/usr/bin/env node
/**
 * scripts/security-check.mjs — reproducible static security scan (Sprint 12C).
 *
 * Zero dependencies. Walks the tracked source of backend/, frontend/, mcp-server/
 * and flags: committed secrets, frontend info leaks, backend info leaks, and
 * MCP boundary violations (DB / LLM imports, key echoing).
 *
 *   node scripts/security-check.mjs           # human report
 *   node scripts/security-check.mjs --json    # machine report
 *
 * Exit code 0 = no ERROR findings (WARN allowed), 1 = at least one ERROR.
 */
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join, relative, sep, extname } from 'node:path'

const ROOT = join(import.meta.dirname, '..')
const JSON_OUT = process.argv.includes('--json')

const SCAN_DIRS = ['backend/src', 'frontend/src', 'frontend/e2e', 'mcp-server/src']
const SKIP_DIR = new Set(['node_modules', 'dist', 'target', 'build', '.git', 'coverage'])
const TEXT_EXT = new Set(['.ts', '.tsx', '.js', '.jsx', '.mjs', '.cjs', '.java', '.yml', '.yaml', '.properties', '.sql', '.json', '.env', '.md'])

/** Paths (relative, posix) that are allowed to contain otherwise-flagged text. */
const ALLOW = [
  // Sprint 12A documented dev defaults live only here.
  'backend/src/main/resources/application.yml',
  // Sprint 12C test-only, clearly-labelled e2e values.
  'backend/src/main/resources/application-e2e.yml',
  'backend/src/main/resources/db/e2e/R__seed_e2e_users.sql',
  'backend/src/test/resources/application-test.yml',
  // The approved token abstraction + the store that owns persistence.
  'frontend/src/lib/authTokens.ts',
  'frontend/src/store/auth.store.ts',
  // This scanner and the security docs describe the patterns themselves.
  'scripts/security-check.mjs',
]

const findings = []
function add(level, category, file, line, msg, snippet) {
  findings.push({ level, category, file, line, msg, snippet: snippet?.trim().slice(0, 160) })
}

function walk(dir) {
  let entries
  try {
    entries = readdirSync(dir)
  } catch {
    return
  }
  for (const name of entries) {
    if (SKIP_DIR.has(name)) continue
    const full = join(dir, name)
    const st = statSync(full)
    if (st.isDirectory()) walk(full)
    else if (TEXT_EXT.has(extname(name)) || name.startsWith('.env')) scanFile(full)
  }
}

const isAllowed = (rel) => ALLOW.includes(rel)

// ── Rule sets ──────────────────────────────────────────────────────────────
// Never acceptable, anywhere (including test code).
const SECRET_RULES = [
  { re: /-----BEGIN (?:RSA |EC |OPENSSH |PGP )?PRIVATE KEY-----/, msg: 'Committed private key' },
  { re: /\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{5,}/, msg: 'Hardcoded JWT' },
  { re: /Authorization:\s*Bearer\s+[A-Za-z0-9._-]{12,}/i, msg: 'Hardcoded Authorization: Bearer header' },
  { re: /jdbc:postgresql:\/\/[^ "'\n]*:[^ "'\n@]+@/, msg: 'JDBC URL with inline credentials' },
]
// Credential literals: flagged only in NON-test main/source code. Test fixtures
// legitimately define throwaway passwords.
const SECRET_RULES_NON_TEST = [
  { re: /(password|secret|api[_-]?key)\s*[:=]\s*["'][^"'${}\s]{12,}["']/i, msg: 'Possible hardcoded credential literal in non-test code' },
]

const FRONTEND_RULES = [
  { re: /\bconsole\.(log|warn|error|debug|info)\s*\(/, level: 'ERROR', msg: 'console.* in shipped frontend code' },
  { re: /dangerouslySetInnerHTML/, level: 'ERROR', msg: 'dangerouslySetInnerHTML' },
  { re: /\.innerHTML\s*=/, level: 'ERROR', msg: 'innerHTML assignment' },
  { re: /document\.cookie/, level: 'ERROR', msg: 'document.cookie access' },
  { re: /localStorage\.(getItem|setItem|removeItem)\(\s*['"](access_token|refresh_token)['"]/, level: 'ERROR', msg: 'raw token localStorage access outside authTokens.ts' },
]

const BACKEND_RULES = [
  { re: /\.printStackTrace\s*\(/, level: 'ERROR', msg: 'printStackTrace (leaks stack trace)' },
  { re: /include-stacktrace:\s*always/, level: 'ERROR', msg: 'server.error.include-stacktrace: always' },
  { re: /include-message:\s*always/, level: 'WARN', msg: 'server.error.include-message: always' },
]

const MCP_RULES = [
  { re: /from\s+['"](pg|postgres|typeorm|knex|sequelize|mysql2?|better-sqlite3|mongoose|drizzle-orm)['"]/, level: 'ERROR', msg: 'MCP importing a database driver/ORM' },
  { re: /from\s+['"](openai|@anthropic-ai\/sdk|@google\/generative-ai|langchain|@langchain\/[a-z]+|cohere-ai)['"]/, level: 'ERROR', msg: 'MCP importing an LLM SDK' },
  { re: /(content|text|message)\s*:\s*[^,\n]*mcpApiKey/, level: 'ERROR', msg: 'MCP tool output may echo the API key' },
]

function scanFile(full) {
  const rel = relative(ROOT, full).split(sep).join('/')
  let text
  try {
    text = readFileSync(full, 'utf8')
  } catch {
    return
  }
  const lines = text.split(/\r?\n/)
  const inFrontend = rel.startsWith('frontend/src/')
  const inFrontendTest =
    /\.(test|spec)\.(ts|tsx)$/.test(rel) || rel.startsWith('frontend/src/test/')
  const inBackend = rel.startsWith('backend/src/')
  const inMcp = rel.startsWith('mcp-server/src/')
  const inMcpTest = /\.test\.ts$/.test(rel)
  const isTestFile =
    /(^|\/)src\/test\//.test(rel) ||
    /\/test\//.test(rel) ||
    /(Test|Tests|IT)\.java$/.test(rel) ||
    /\.(test|spec)\.(ts|tsx|js)$/.test(rel) ||
    rel.startsWith('frontend/e2e/')

  lines.forEach((ln, i) => {
    const n = i + 1
    if (!isAllowed(rel)) {
      for (const r of SECRET_RULES) {
        if (r.re.test(ln)) add('ERROR', 'secret', rel, n, r.msg, ln)
      }
      if (!isTestFile) {
        for (const r of SECRET_RULES_NON_TEST) {
          if (r.re.test(ln)) add('ERROR', 'secret', rel, n, r.msg, ln)
        }
      }
    }
    if (inFrontend && !inFrontendTest) {
      for (const r of FRONTEND_RULES) {
        if (r.re.test(ln) && !isAllowed(rel)) add(r.level, 'frontend-leak', rel, n, r.msg, ln)
      }
    }
    if (inBackend) {
      for (const r of BACKEND_RULES) if (r.re.test(ln)) add(r.level, 'backend-leak', rel, n, r.msg, ln)
    }
    if (inMcp && !inMcpTest) {
      for (const r of MCP_RULES) if (r.re.test(ln)) add(r.level, 'mcp-leak', rel, n, r.msg, ln)
    }
  })
}

for (const d of SCAN_DIRS) walk(join(ROOT, d))

const errors = findings.filter((f) => f.level === 'ERROR')
const warns = findings.filter((f) => f.level === 'WARN')

if (JSON_OUT) {
  console.log(JSON.stringify({ errors, warns }, null, 2))
} else {
  const fmt = (f) => `  [${f.level}] ${f.category}  ${f.file}:${f.line}\n        ${f.msg}\n        > ${f.snippet}`
  console.log('── Sprint 12C static security scan ─────────────────────────────')
  console.log(`scanned: ${SCAN_DIRS.join(', ')}`)
  console.log(`ERROR findings: ${errors.length}`)
  console.log(`WARN  findings: ${warns.length}\n`)
  if (errors.length) console.log('ERRORS:\n' + errors.map(fmt).join('\n\n') + '\n')
  if (warns.length) console.log('WARNINGS:\n' + warns.map(fmt).join('\n\n') + '\n')
  if (!errors.length && !warns.length) console.log('✓ clean — no findings')
  else if (!errors.length) console.log('✓ no ERROR findings (warnings are advisory)')
  else console.log('✗ ERROR findings present — see above')
}

process.exit(errors.length ? 1 : 0)
