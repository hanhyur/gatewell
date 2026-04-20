"use client";

import { useState } from "react";
import Nav from "@/components/Nav";
import { scanUrl, scanGitHub } from "@/lib/scan-api";
import type { ScanResponse } from "@/types/scan";
import { SCAN_SEVERITY_COLORS, SCAN_DECISION_CONFIG } from "@/types/scan";

type TabType = "url" | "github";

export default function ScanPage() {
  const [tab, setTab] = useState<TabType>("url");
  const [urlInput, setUrlInput] = useState("");
  const [repoInput, setRepoInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState<ScanResponse | null>(null);

  async function handleScan(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setResult(null);
    setLoading(true);

    try {
      const report =
        tab === "url"
          ? await scanUrl(urlInput)
          : await scanGitHub(repoInput);
      setResult(report);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Scan failed");
    } finally {
      setLoading(false);
    }
  }

  const inputStyle: React.CSSProperties = {
    flex: 1,
    padding: "10px 14px",
    borderRadius: 8,
    border: "1px solid var(--border)",
    background: "var(--surface)",
    color: "var(--foreground)",
    fontSize: 14,
    outline: "none",
  };

  return (
    <>
      <Nav />
      <main style={{ flex: 1, maxWidth: 900, margin: "0 auto", padding: "48px 24px", width: "100%" }}>
        <h1 style={{ fontSize: 28, fontWeight: 700, marginBottom: 8 }}>Security Scan</h1>
        <p style={{ fontSize: 15, color: "var(--muted)", marginBottom: 32 }}>
          Scan a live URL for security vulnerabilities or a GitHub repo for hardcoded secrets and code issues.
        </p>

        {/* Tabs */}
        <div style={{ display: "flex", gap: 4, marginBottom: 24 }}>
          {(["url", "github"] as const).map((t) => (
            <button
              key={t}
              onClick={() => { setTab(t); setResult(null); setError(""); }}
              style={{
                padding: "8px 20px",
                borderRadius: 6,
                fontSize: 14,
                fontWeight: 600,
                border: `1px solid ${tab === t ? "var(--accent)" : "var(--border)"}`,
                background: tab === t ? "rgba(99,102,241,0.1)" : "transparent",
                color: tab === t ? "var(--accent)" : "var(--muted)",
                cursor: "pointer",
              }}
            >
              {t === "url" ? "URL Scan" : "GitHub Scan"}
            </button>
          ))}
        </div>

        {/* Input */}
        <form onSubmit={handleScan} style={{ display: "flex", gap: 12, marginBottom: 32 }}>
          {tab === "url" ? (
            <input
              type="text"
              required
              placeholder="https://your-app.com"
              value={urlInput}
              onChange={(e) => setUrlInput(e.target.value)}
              style={inputStyle}
            />
          ) : (
            <input
              type="text"
              required
              placeholder="https://github.com/owner/repo"
              value={repoInput}
              onChange={(e) => setRepoInput(e.target.value)}
              style={inputStyle}
            />
          )}
          <button
            type="submit"
            disabled={loading}
            style={{
              padding: "10px 24px",
              borderRadius: 8,
              background: loading ? "var(--surface-2)" : "var(--accent)",
              color: "white",
              fontWeight: 600,
              fontSize: 14,
              border: "none",
              cursor: loading ? "not-allowed" : "pointer",
              whiteSpace: "nowrap",
            }}
          >
            {loading ? "Scanning..." : "Run Scan"}
          </button>
        </form>

        {error && (
          <div style={{ padding: "10px 14px", borderRadius: 8, background: "rgba(239,68,68,0.1)", border: "1px solid rgba(239,68,68,0.3)", color: "#ef4444", fontSize: 14, marginBottom: 24 }}>
            {error}
          </div>
        )}

        {/* Results */}
        {result && <ScanResults result={result} />}
      </main>
    </>
  );
}

function ScanResults({ result }: { result: ScanResponse }) {
  const decision = SCAN_DECISION_CONFIG[result.decision];

  return (
    <div>
      {/* Decision */}
      <div style={{ textAlign: "center", marginBottom: 32 }}>
        <div style={{ display: "inline-block", padding: "14px 36px", borderRadius: 14, background: decision.bg, border: `2px solid ${decision.color}` }}>
          <div style={{ fontSize: 12, fontWeight: 600, color: decision.color, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 4 }}>
            {decision.label}
          </div>
          <div style={{ fontSize: 28, fontWeight: 800, color: decision.color }}>{result.decision}</div>
        </div>
        <div style={{ fontSize: 14, color: "var(--muted)", marginTop: 12 }}>{result.target}</div>
      </div>

      {/* Summary */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(5, 1fr)", gap: 10, marginBottom: 28 }}>
        {[
          { label: "Critical", value: result.summary.critical, color: SCAN_SEVERITY_COLORS.CRITICAL },
          { label: "High", value: result.summary.high, color: SCAN_SEVERITY_COLORS.HIGH },
          { label: "Medium", value: result.summary.medium, color: SCAN_SEVERITY_COLORS.MEDIUM },
          { label: "Low", value: result.summary.low, color: SCAN_SEVERITY_COLORS.LOW },
          { label: "Info", value: result.summary.info, color: SCAN_SEVERITY_COLORS.INFO },
        ].map((s) => (
          <div key={s.label} style={{ padding: 14, borderRadius: 10, border: "1px solid var(--border)", background: "var(--surface)", textAlign: "center" }}>
            <div style={{ fontSize: 24, fontWeight: 700, color: s.value > 0 ? s.color : "var(--muted)" }}>{s.value}</div>
            <div style={{ fontSize: 11, color: "var(--muted)", marginTop: 2 }}>{s.label}</div>
          </div>
        ))}
      </div>

      {/* Findings */}
      <h2 style={{ fontSize: 16, fontWeight: 600, marginBottom: 14 }}>Findings ({result.findings.length})</h2>
      <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
        {result.findings.map((f, i) => (
          <div key={i} style={{ padding: 14, borderRadius: 10, border: "1px solid var(--border)", background: "var(--surface)" }}>
            <div style={{ display: "flex", gap: 10, alignItems: "center", marginBottom: 6 }}>
              <span style={{ padding: "2px 8px", borderRadius: 4, fontSize: 11, fontWeight: 700, color: "white", background: SCAN_SEVERITY_COLORS[f.severity] || "#6b7280" }}>
                {f.severity}
              </span>
              <span style={{ fontSize: 13, fontWeight: 600 }}>{f.title}</span>
            </div>
            <p style={{ fontSize: 13, color: "var(--muted)", lineHeight: 1.5, marginBottom: 6 }}>{f.detail}</p>
            <div style={{ fontSize: 12, fontFamily: "var(--font-mono)", color: "var(--muted)", background: "var(--surface-2)", padding: "6px 10px", borderRadius: 6 }}>
              {f.evidence}
            </div>
          </div>
        ))}
        {result.findings.length === 0 && (
          <div style={{ padding: 24, textAlign: "center", color: "var(--muted)" }}>No issues found!</div>
        )}
      </div>
    </div>
  );
}
