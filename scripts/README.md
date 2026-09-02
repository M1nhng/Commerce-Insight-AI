# Scripts — Commerce Insight AI

> Developer utility scripts for local environment setup and maintenance.

---

## Available Scripts

| Script | Description |
|--------|-------------|
| `setup.sh` | One-command local environment setup |
| `migrate.sh` | Run Flyway database migrations |
| `seed.sh` | Seed the database with sample data |
| `demo-up.sh` | Start the full demo stack (`demo` profile + deterministic demo dataset) and wait for health |
| `demo-reset.sh` | Wipe the demo database/volume and rebuild a clean seeded demo stack |
| `security-check.mjs` | Static secret / leak scan (`node scripts/security-check.mjs`) |

---

## Usage

```bash
# Make scripts executable (Unix/Mac)
chmod +x scripts/*.sh

# Run full setup
./scripts/setup.sh

# Run migrations only
./scripts/migrate.sh

# Seed database
./scripts/seed.sh
```

---

> **Windows Users**: Run scripts via Git Bash, WSL, or adapt commands manually.
