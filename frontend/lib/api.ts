import type { AssessmentRequest, AssessmentResponse, DashboardSummary, CompareResponse } from '@/types/api'

const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!res.ok) {
    const error = await res.json().catch(() => ({ error: res.statusText }))
    throw new Error(error.error || `Request failed: ${res.status}`)
  }
  return res.json()
}

export async function createAssessment(data: AssessmentRequest): Promise<AssessmentResponse> {
  return request('/assessments', { method: 'POST', body: JSON.stringify(data) })
}

export async function getAssessment(id: string): Promise<AssessmentResponse> {
  return request(`/assessments/${id}`)
}

export async function listAssessments(params?: {
  decision?: string
  severity?: string
}): Promise<AssessmentResponse[]> {
  const query = new URLSearchParams()
  if (params?.decision) query.set('decision', params.decision)
  if (params?.severity) query.set('severity', params.severity)
  const qs = query.toString()
  return request(`/assessments${qs ? `?${qs}` : ''}`)
}

export async function getDashboardSummary(): Promise<DashboardSummary> {
  return request('/assessments/summary')
}

export async function compareAssessments(id: string, otherId: string): Promise<CompareResponse> {
  return request(`/assessments/${id}/compare/${otherId}`)
}

export async function reassess(id: string, evidences: string[]): Promise<AssessmentResponse> {
  return request(`/assessments/${id}/reassess`, {
    method: 'POST',
    body: JSON.stringify({ evidences }),
  })
}
