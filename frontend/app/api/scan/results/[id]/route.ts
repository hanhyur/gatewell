import { BACKEND_URL } from '@/lib/server-config'
import { NextRequest, NextResponse } from 'next/server'

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id } = await params
  const res = await fetch(`${BACKEND_URL}/scan/results/${id}`)
  const data = await res.json()
  return NextResponse.json(data, { status: res.status })
}
