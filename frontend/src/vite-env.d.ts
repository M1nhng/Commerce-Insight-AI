/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  readonly VITE_MCP_SERVER_URL: string
  readonly VITE_APP_NAME: string
  readonly VITE_FEATURE_AI_INSIGHTS: string
  readonly VITE_FEATURE_IMPORT_EXPORT: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
