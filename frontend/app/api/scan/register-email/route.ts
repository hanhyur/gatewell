import { BACKEND_URL } from '@/lib/server-config'
import { NextRequest, NextResponse } from 'next/server'

export async function POST(request: NextRequest) {
  const body = await request.text()
  const res = await fetch(`${BACKEND_URL}/scan/register-email`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body,
  })
  const data = await res.json()
  return NextResponse.json(data, { status: res.status })
}
