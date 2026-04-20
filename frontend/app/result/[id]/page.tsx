"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import Nav from "@/components/Nav";
import { getAssessment } from "@/lib/api";
import type { AssessmentResponse } from "@/types/api";
import { DECISION_CONFIG, SEVERITY_COLORS } from "@/types/api";

export default function ResultPage() {
  const { id } = useParams<{ id: string }>();
  const [report, setReport] = useState<AssessmentResponse | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!id) return;
    getAssessment(id)
      .then(setReport)
      .catch((err: unknown) =>
        setError(err instanceof Error ? err.message : "Failed to load report")
      );
  }, [id]);

  if (error) {
    return (
      <>
        <Nav />
        <main style={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center" }}>
          <p style={{ color: "#ef4444" }}>{error}</p>
        </main>
      </>
    );
  }

  if (!report) {
    return (
      <>
        <Nav />
        <main style={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center" }}>
          <p style={{ color: "var(--muted)" }}>Loading report...</p>
        </main>
      </>
    );
  }

  const decision = DECISION_CONFIG[report.launchDecision];

  return (
    <>
      <Nav />
      <main style={{ flex: 1, maxWidth: 800, margin: "0 auto", padding: "48px 24px", width: "100%" }}>
        {/* Decision Badge */}
        <div style={{ textAlign: "center", marginBottom: 40 }}>
          <div
            style={{
              display: "inline-block",
              padding: "16px 40px",
              borderRadius: 16,
              background: decision.bg,
              border: `2px solid ${decision.color}`,
            }}
          >
            <div style={{ fontSize: 13, fontWeight: 600, color: decision.color, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 4 }}>
              {decision.label}
            </div>
            <div style={{ fontSize: 32, fontWeight: 800, color: decision.color }}>
              {report.launchDecision}
            </div>
          </div>
          <h1 style={{ fontSize: 24, fontWeight: 700, marginTop: 20 }}>{report.productName}</h1>
          <p style={{ fontSize: 14, color: "var(--muted)", marginTop: 8 }}>{report.summary}</p>
        </div>

        {/* Summary Stats */}
        <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 12, marginBottom: 32 }}>
          {[
            { label: "Total Findings", value: report.findingsSummary.total, color: "var(--foreground)" },
            { label: "High", value: report.findingsSummary.high, color: SEVERITY_COLORS.HIGH },
            { label: "Medium", value: report.findingsSummary.medium, color: SEVERITY_COLORS.MEDIUM },
            { label: "Low", value: report.findingsSummary.low, color: SEVERITY_COLORS.LOW },
          ].map((stat) => (
            <div key={stat.label} style={{ padding: 16, borderRadius: 10, border: "1px solid var(--border)", background: "var(--surface)", textAlign: "center" }}>
              <div style={{ fontSize: 28, fontWeight: 700, color: stat.color }}>{stat.value}</div>
              <div style={{ fontSize: 12, color: "var(--muted)", marginTop: 4 }}>{stat.label}</div>
            </div>
          ))}
        </div>

        {/* Recommendation */}
        <div style={{ padding: 20, borderRadius: 10, border: "1px solid var(--border)", background: "var(--surface)", marginBottom: 32 }}>
          <div style={{ fontSize: 13, fontWeight: 600, color: "var(--accent)", marginBottom: 8, textTransform: "uppercase", letterSpacing: "0.06em" }}>
            Recommendation
          </div>
          <p style={{ fontSize: 15, lineHeight: 1.6 }}>{report.recommendation}</p>
        </div>

        {/* Findings */}
        <div style={{ marginBottom: 32 }}>
          <h2 style={{ fontSize: 16, fontWeight: 600, marginBottom: 16 }}>
            Findings ({report.findings.length})
          </h2>
          <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
            {report.findings.map((f, i) => (
              <div
                key={i}
                style={{
                  padding: 16,
                  borderRadius: 10,
                  border: "1px solid var(--border)",
                  background: "var(--surface)",
                  display: "flex",
                  gap: 12,
                  alignItems: "flex-start",
                }}
              >
                <span
                  style={{
                    padding: "2px 8px",
                    borderRadius: 4,
                    fontSize: 11,
                    fontWeight: 700,
                    color: "white",
                    background: SEVERITY_COLORS[f.severity] || "var(--muted)",
                    flexShrink: 0,
                  }}
                >
                  {f.severity}
                </span>
                <div style={{ flex: 1 }}>
                  <div style={{ display: "flex", gap: 8, alignItems: "center", marginBottom: 4 }}>
                    <span style={{ fontSize: 13, fontWeight: 600 }}>{f.category}</span>
                    <span style={{ fontSize: 11, color: "var(--muted)", fontFamily: "var(--font-mono)" }}>{f.code}</span>
                  </div>
                  <p style={{ fontSize: 13, color: "var(--muted)", lineHeight: 1.5 }}>{f.message}</p>
                </div>
              </div>
            ))}
            {report.findings.length === 0 && (
              <div style={{ padding: 24, textAlign: "center", color: "var(--muted)", fontSize: 14 }}>
                No findings — your product looks safe!
              </div>
            )}
          </div>
        </div>

        {/* Risk Categories */}
        {report.findingsSummary.categories.length > 0 && (
          <div style={{ marginBottom: 32 }}>
            <h2 style={{ fontSize: 16, fontWeight: 600, marginBottom: 12 }}>Risk Categories</h2>
            <div style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
              {report.findingsSummary.categories.map((cat) => (
                <span key={cat} style={{ padding: "4px 12px", borderRadius: 6, border: "1px solid var(--border)", background: "var(--surface-2)", fontSize: 12, fontWeight: 500 }}>
                  {cat.replace(/_/g, " ")}
                </span>
              ))}
            </div>
          </div>
        )}

        {/* Meta */}
        <div style={{ display: "flex", gap: 16, fontSize: 13, color: "var(--muted)", marginBottom: 32 }}>
          <span>Rule Version: {report.ruleVersion}</span>
          <span>ID: {report.id.slice(0, 8)}...</span>
          <span>{new Date(report.createdAt).toLocaleString()}</span>
        </div>

        {/* Actions */}
        <div style={{ display: "flex", gap: 12 }}>
          <Link href="/assess" style={{ padding: "10px 20px", borderRadius: 8, background: "var(--accent)", color: "white", fontWeight: 600, fontSize: 14, textDecoration: "none" }}>
            New Assessment
          </Link>
          <Link href="/dashboard" style={{ padding: "10px 20px", borderRadius: 8, border: "1px solid var(--border)", color: "var(--foreground)", fontWeight: 500, fontSize: 14, textDecoration: "none" }}>
            Dashboard
          </Link>
        </div>
      </main>
    </>
  );
}
