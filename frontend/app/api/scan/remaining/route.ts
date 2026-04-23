import { BACKEND_URL } from '@/lib/server-config'
import { NextRequest, NextResponse } from 'next/server'

export async function GET(request: NextRequest) {
  const res = await fetch(`${BACKEND_URL}/scan/remaining`, {
    headers: {
      'X-Forwarded-For': request.headers.get('x-forwarded-for') || request.headers.get('x-real-ip') || '127.0.0.1',
    },
  })
  const data = await res.json()
  return NextResponse.json(data, { status: res.status })
}
