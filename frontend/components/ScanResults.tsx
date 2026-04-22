"use client";

import type { ScanResponse } from "@/types/scan";
import { SCAN_SEVERITY_COLORS, SCAN_DECISION_CONFIG } from "@/types/scan";

export default function ScanResults({ result, showShare = false }: { result: ScanResponse; showShare?: boolean }) {
  const decision = SCAN_DECISION_CONFIG[result.decision];

  function handleShare() {
    const url = `${window.location.origin}/report/${result.id}`;
    navigator.clipboard.writeText(url);
    alert("Link copied to clipboard!");
  }

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
        {result.createdAt && (
          <div style={{ fontSize: 12, color: "var(--muted)", marginTop: 4 }}>
            Scanned {new Date(result.createdAt).toLocaleString()}
          </div>
        )}
      </div>

      {/* Share button */}
      {showShare && result.id && (
        <div style={{ textAlign: "center", marginBottom: 24 }}>
          <button
            onClick={handleShare}
            style={{
              padding: "8px 20px",
              borderRadius: 6,
              border: "1px solid var(--border)",
              background: "var(--surface)",
              color: "var(--foreground)",
              fontSize: 13,
              fontWeight: 500,
              cursor: "pointer",
            }}
          >
            Copy Share Link
          </button>
        </div>
      )}

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

      {/* Scan again */}
      <div style={{ textAlign: "center", marginTop: 32 }}>
        <a href="/scan" style={{ padding: "10px 24px", borderRadius: 8, background: "var(--accent)", color: "white", fontWeight: 600, fontSize: 14, textDecoration: "none" }}>
          Scan Another Site
        </a>
      </div>
    </div>
  );
}
