"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Nav from "@/components/Nav";
import ScanResults from "@/components/ScanResults";
import { getScanResult } from "@/lib/scan-api";
import type { ScanResponse } from "@/types/scan";

export default function ReportPage() {
  const { id } = useParams<{ id: string }>();
  const [result, setResult] = useState<ScanResponse | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!id) return;
    getScanResult(id)
      .then(setResult)
      .catch((err: unknown) =>
        setError(err instanceof Error ? err.message : "Failed to load report")
      );
  }, [id]);

  return (
    <>
      <Nav />
      <main style={{ flex: 1, maxWidth: 900, margin: "0 auto", padding: "48px 24px", width: "100%" }}>
        {error && (
          <div style={{ textAlign: "center", padding: 60 }}>
            <p style={{ color: "#ef4444", marginBottom: 16 }}>{error}</p>
            <a href="/scan" style={{ color: "var(--accent)" }}>Back to Scan</a>
          </div>
        )}
        {!result && !error && (
          <div style={{ textAlign: "center", padding: 60, color: "var(--muted)" }}>
            Loading report...
          </div>
        )}
        {result && <ScanResults result={result} showShare />}
      </main>
    </>
  );
}
