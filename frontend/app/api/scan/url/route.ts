import { BACKEND_URL } from '@/lib/server-config'
import { NextRequest, NextResponse } from 'next/server'

export async function POST(request: NextRequest) {
  const body = await request.text()
  const res = await fetch(`${BACKEND_URL}/scan/url`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Forwarded-For': request.headers.get('x-forwarded-for') || request.headers.get('x-real-ip') || '127.0.0.1',
    },
    body,
  })
  const data = await res.json()
  return NextResponse.json(data, { status: res.status })
}
