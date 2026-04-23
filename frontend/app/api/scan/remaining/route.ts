import { BACKEND_URL } from '@/lib/server-config'
import { NextRequest, NextResponse } from 'next/server'

export async function GET(request: NextRequest) {
  const res = await fetch(`${BACKEND_URL}/scan/remaining`, {
    headers: {
    },
  })
  const data = await res.json()
  return NextResponse.json(data, { status: res.status })
}
