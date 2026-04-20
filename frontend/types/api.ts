export interface FindingResponse {
  severity: string
  category: string
  code: string
  message: string
}

export interface FindingsSummary {
  total: number
  high: number
  medium: number
  low: number
  categories: string[]
}

export interface AssessmentResponse {
  id: string
  productName: string
  summary: string
  evidences: string[]
  capabilities: string[]
  findings: FindingResponse[]
  findingsSummary: FindingsSummary
  severity: string
  launchDecision: string
  recommendation: string
  ruleVersion: string
  createdAt: string
}

export interface AssessmentRequest {
  productName: string
  summary: string
  evidences: string[]
  capabilities: string[]
}

export interface DashboardSummary {
  totalAssessments: number
  byDecision: Record<string, number>
  bySeverity: Record<string, number>
  topCategories: Record<string, number>
}

export interface CompareResponse {
  before: { id: string; severity: string; launchDecision: string; findingsCount: number }
  after: { id: string; severity: string; launchDecision: string; findingsCount: number }
  decisionChanged: boolean
  severityChanged: boolean
  resolvedFindings: string[]
  newFindings: string[]
}

export const CAPABILITIES = [
  { value: 'CODE_EXECUTION', label: 'Code Execution', description: 'Can execute arbitrary code' },
  { value: 'WEB_BROWSING', label: 'Web Browsing', description: 'Can browse the internet' },
  { value: 'FILE_ACCESS', label: 'File Access', description: 'Can read/write files' },
  { value: 'DATABASE_ACCESS', label: 'Database Access', description: 'Can query databases directly' },
  { value: 'EXTERNAL_API_CALL', label: 'External API Calls', description: 'Can call third-party APIs' },
  { value: 'USER_DATA_PROCESSING', label: 'User Data Processing', description: 'Processes personal user data' },
] as const

export const SEVERITY_COLORS: Record<string, string> = {
  HIGH: '#ef4444',
  MEDIUM: '#f59e0b',
  LOW: '#3b82f6',
  NONE: '#22c55e',
}

export const DECISION_CONFIG: Record<string, { color: string; bg: string; label: string }> = {
  BLOCK: { color: '#ef4444', bg: '#fef2f2', label: 'Launch Blocked' },
  CAUTION: { color: '#f59e0b', bg: '#fffbeb', label: 'Proceed with Caution' },
  ALLOW: { color: '#22c55e', bg: '#f0fdf4', label: 'Cleared for Launch' },
}
