# Docker — Commerce Insight AI

> Docker configuration files for all services.

---

## Structure

```
docker/
├── postgres/
│   └── init.sql       # PostgreSQL initialization script
├── nginx/
│   └── nginx.conf     # Nginx reverse proxy configuration
└── README.md
```

---

## Services

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| `postgres` | postgres:16-alpine | 5432 | Primary database |
| `pgadmin` | dpage/pgadmin4 | 5050 | DB admin UI (dev only) |
| `backend` | custom | 8080 | Spring Boot API |
| `frontend` | custom (nginx) | 5173 | React SPA |
| `mcp-server` | custom | 3001 | MCP Protocol server |

---

## Usage

```bash
# Start only infrastructure (DB)
docker-compose up -d postgres

# Start all services
docker-compose up -d

# Start with dev tools (pgadmin)
docker-compose --profile dev-tools up -d

# View logs
docker-compose logs -f backend
```

---

## Data Persistence

PostgreSQL data is persisted in the `postgres_data` Docker volume.
To reset the database: `docker-compose down -v`
