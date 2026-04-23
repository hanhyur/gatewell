import type { ScanResponse } from '@/types/scan'

export async function scanUrl(url: string): Promise<ScanResponse> {
  const res = await fetch('/api/scan/url', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ url }),
  })
  if (!res.ok) {
    const error = await res.json().catch(() => ({ error: res.statusText }))
    throw new Error(error.error || `Scan failed: ${res.status}`)
  }
  return res.json()
}

export async function scanGitHub(repoUrl: string): Promise<ScanResponse> {
  const res = await fetch('/api/scan/github', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ repoUrl }),
  })
  if (!res.ok) {
    const error = await res.json().catch(() => ({ error: res.statusText }))
    throw new Error(error.error || `Scan failed: ${res.status}`)
  }
  return res.json()
}

export async function getScanResult(id: string): Promise<ScanResponse> {
  const res = await fetch(`/api/scan/results/${id}`)
  if (!res.ok) {
    const error = await res.json().catch(() => ({ error: res.statusText }))
    throw new Error(error.error || `Failed to load: ${res.status}`)
  }
  return res.json()
}

export async function getRemainingScans(): Promise<{ remaining: number; limit: number; used: number }> {
  const res = await fetch('/api/scan/remaining')
  return res.json()
}

export async function registerEmail(email: string): Promise<{ remaining: number }> {
  const res = await fetch('/api/scan/register-email', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email }),
  })
  if (!res.ok) {
    const error = await res.json().catch(() => ({ error: res.statusText }))
    throw new Error(error.error || 'Registration failed')
  }
  return res.json()
}
