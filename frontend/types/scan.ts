export interface ScanFindingResponse {
  severity: string
  category: string
  code: string
  title: string
  detail: string
  evidence: string
}

export interface ScanSummary {
  total: number
  critical: number
  high: number
  medium: number
  low: number
  info: number
  categories: string[]
}

export interface ScanResponse {
  id?: string
  scanType: string
  target: string
  reachable: boolean
  decision: string
  summary: ScanSummary
  findings: ScanFindingResponse[]
  createdAt?: string
  remainingScans?: number
}

export const SCAN_SEVERITY_COLORS: Record<string, string> = {
  CRITICAL: '#dc2626',
  HIGH: '#ef4444',
  MEDIUM: '#f59e0b',
  LOW: '#3b82f6',
  INFO: '#6b7280',
}

export const SCAN_DECISION_CONFIG: Record<string, { color: string; bg: string; label: string }> = {
  BLOCK: { color: '#ef4444', bg: '#fef2f2', label: 'Unsafe — Do Not Launch' },
  CAUTION: { color: '#f59e0b', bg: '#fffbeb', label: 'Issues Found — Fix Before Launch' },
  ALLOW: { color: '#22c55e', bg: '#f0fdf4', label: 'Looks Safe' },
}
