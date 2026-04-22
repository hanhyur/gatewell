"use client";

import { useState } from "react";
import type { ScanResponse, ScanFindingResponse } from "@/types/scan";
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
      <div style={{ textAlign: "center", marginBottom: 32 }}>
        <div style={{ display: "inline-block", padding: "14px 36px", borderRadius: 14, background: decision.bg, border: `2px solid ${decision.color}` }}>
          <div style={{ fontSize: 12, fontWeight: 600, color: decision.color, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 4 }}>{decision.label}</div>
          <div style={{ fontSize: 28, fontWeight: 800, color: decision.color }}>{result.decision}</div>
        </div>
        <div style={{ fontSize: 14, color: "var(--muted)", marginTop: 12 }}>{result.target}</div>
        {result.createdAt && <div style={{ fontSize: 12, color: "var(--muted)", marginTop: 4 }}>Scanned {new Date(result.createdAt).toLocaleString()}</div>}
      </div>

      {showShare && result.id && (
        <div style={{ textAlign: "center", marginBottom: 24 }}>
          <button onClick={handleShare} style={{ padding: "8px 20px", borderRadius: 6, border: "1px solid var(--border)", background: "var(--surface)", color: "var(--foreground)", fontSize: 13, fontWeight: 500, cursor: "pointer" }}>
            Copy Share Link
          </button>
        </div>
      )}

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

      <h2 style={{ fontSize: 16, fontWeight: 600, marginBottom: 14 }}>Findings ({result.findings.length})</h2>
      <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
        {result.findings.map((f, i) => <FindingCard key={i} finding={f} />)}
        {result.findings.length === 0 && <div style={{ padding: 24, textAlign: "center", color: "var(--muted)" }}>No issues found!</div>}
      </div>

      <div style={{ textAlign: "center", marginTop: 32 }}>
        <a href="/scan" style={{ padding: "10px 24px", borderRadius: 8, background: "var(--accent)", color: "white", fontWeight: 600, fontSize: 14, textDecoration: "none" }}>Scan Another Site</a>
      </div>
    </div>
  );
}

function FindingCard({ finding: f }: { finding: ScanFindingResponse }) {
  const [showFixes, setShowFixes] = useState(false);
  const [showTech, setShowTech] = useState(false);
  const hasGuide = f.risk || f.impact || (f.fixes && f.fixes.length > 0);

  return (
    <div style={{ borderRadius: 12, border: "1px solid var(--border)", background: "var(--surface)", overflow: "hidden" }}>
      <div style={{ padding: "14px 16px", display: "flex", gap: 10, alignItems: "center" }}>
        <span style={{ padding: "3px 10px", borderRadius: 4, fontSize: 11, fontWeight: 700, color: "white", background: SCAN_SEVERITY_COLORS[f.severity] || "#6b7280", flexShrink: 0 }}>{f.severity}</span>
        <span style={{ fontSize: 14, fontWeight: 600 }}>{f.risk || f.title}</span>
      </div>

      {f.impact && <div style={{ padding: "0 16px 14px", fontSize: 14, color: "#d4d4d8", lineHeight: 1.6 }}>{f.impact}</div>}

      {f.fixes && f.fixes.length > 0 && (
        <div style={{ borderTop: "1px solid var(--border)" }}>
          <button onClick={() => setShowFixes(!showFixes)} style={{ width: "100%", padding: "10px 16px", background: "none", border: "none", color: "var(--accent)", fontSize: 13, fontWeight: 600, cursor: "pointer", textAlign: "left", display: "flex", alignItems: "center", gap: 6 }}>
            <span style={{ transform: showFixes ? "rotate(90deg)" : "none", transition: "transform 0.15s", display: "inline-block" }}>&#9654;</span>
            How to fix
          </button>
          {showFixes && (
            <div style={{ padding: "0 16px 14px", display: "flex", flexDirection: "column", gap: 10 }}>
              {f.fixes.map((fix, i) => (
                <div key={i}>
                  <div style={{ fontSize: 13, fontWeight: 600, color: "var(--foreground)", marginBottom: 4 }}>{fix.platform}</div>
                  <div style={{ fontSize: 13, color: "var(--muted)", marginBottom: 6 }}>{fix.instruction}</div>
                  {fix.code && (
                    <div style={{ position: "relative" }}>
                      <pre style={{ fontSize: 12, fontFamily: "var(--font-mono)", color: "#a1a1aa", background: "#09090b", padding: "10px 12px", borderRadius: 6, overflow: "auto", whiteSpace: "pre-wrap", wordBreak: "break-all", border: "1px solid var(--border)" }}>{fix.code}</pre>
                      <button onClick={() => navigator.clipboard.writeText(fix.code!)} style={{ position: "absolute", top: 6, right: 6, padding: "3px 8px", borderRadius: 4, background: "var(--surface-2)", border: "1px solid var(--border)", color: "var(--muted)", fontSize: 11, cursor: "pointer" }}>copy</button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {hasGuide ? (
        <div style={{ borderTop: "1px solid var(--border)" }}>
          <button onClick={() => setShowTech(!showTech)} style={{ width: "100%", padding: "10px 16px", background: "none", border: "none", color: "var(--muted)", fontSize: 12, cursor: "pointer", textAlign: "left", display: "flex", alignItems: "center", gap: 6 }}>
            <span style={{ transform: showTech ? "rotate(90deg)" : "none", transition: "transform 0.15s", display: "inline-block" }}>&#9654;</span>
            Technical details
          </button>
          {showTech && (
            <div style={{ padding: "0 16px 14px" }}>
              <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 4 }}>{f.title}</div>
              <div style={{ fontSize: 13, color: "var(--muted)", lineHeight: 1.5, marginBottom: 6 }}>{f.detail}</div>
              <div style={{ fontSize: 12, fontFamily: "var(--font-mono)", color: "var(--muted)", background: "#09090b", padding: "6px 10px", borderRadius: 6, border: "1px solid var(--border)" }}>{f.evidence}</div>
            </div>
          )}
        </div>
      ) : (
        <div style={{ padding: "0 16px 14px" }}>
          <div style={{ fontSize: 13, color: "var(--muted)", lineHeight: 1.5, marginBottom: 6 }}>{f.detail}</div>
          <div style={{ fontSize: 12, fontFamily: "var(--font-mono)", color: "var(--muted)", background: "#09090b", padding: "6px 10px", borderRadius: 6, border: "1px solid var(--border)" }}>{f.evidence}</div>
        </div>
      )}
    </div>
  );
}
