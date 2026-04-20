"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import Nav from "@/components/Nav";
import { listAssessments, getDashboardSummary } from "@/lib/api";
import type { AssessmentResponse, DashboardSummary } from "@/types/api";
import { DECISION_CONFIG, SEVERITY_COLORS } from "@/types/api";

export default function DashboardPage() {
  const [reports, setReports] = useState<AssessmentResponse[]>([]);
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [filter, setFilter] = useState<{ decision?: string; severity?: string }>({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    Promise.all([
      listAssessments(filter),
      getDashboardSummary(),
    ])
      .then(([r, s]) => {
        setReports(r);
        setSummary(s);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [filter]);

  const filterBtn = (label: string, key: "decision" | "severity", value?: string): React.CSSProperties => ({
    padding: "6px 14px",
    borderRadius: 6,
    fontSize: 13,
    fontWeight: 500,
    border: `1px solid ${filter[key] === value ? "var(--accent)" : "var(--border)"}`,
    background: filter[key] === value ? "rgba(99,102,241,0.1)" : "transparent",
    color: filter[key] === value ? "var(--accent)" : "var(--muted)",
    cursor: "pointer",
  });

  return (
    <>
      <Nav />
      <main style={{ flex: 1, maxWidth: 1100, margin: "0 auto", padding: "48px 24px", width: "100%" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 32 }}>
          <h1 style={{ fontSize: 28, fontWeight: 700 }}>Dashboard</h1>
          <Link href="/assess" style={{ padding: "10px 20px", borderRadius: 8, background: "var(--accent)", color: "white", fontWeight: 600, fontSize: 14, textDecoration: "none" }}>
            New Assessment
          </Link>
        </div>

        {/* Summary Cards */}
        {summary && (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 12, marginBottom: 32 }}>
            <div style={{ padding: 20, borderRadius: 10, border: "1px solid var(--border)", background: "var(--surface)" }}>
              <div style={{ fontSize: 32, fontWeight: 700 }}>{summary.totalAssessments}</div>
              <div style={{ fontSize: 13, color: "var(--muted)", marginTop: 4 }}>Total Assessments</div>
            </div>
            {(["BLOCK", "CAUTION", "ALLOW"] as const).map((d) => (
              <div key={d} style={{ padding: 20, borderRadius: 10, border: "1px solid var(--border)", background: "var(--surface)" }}>
                <div style={{ fontSize: 32, fontWeight: 700, color: DECISION_CONFIG[d].color }}>
                  {summary.byDecision[d] || 0}
                </div>
                <div style={{ fontSize: 13, color: "var(--muted)", marginTop: 4 }}>{d}</div>
              </div>
            ))}
          </div>
        )}

        {/* Filters */}
        <div style={{ display: "flex", gap: 8, marginBottom: 20, flexWrap: "wrap" }}>
          <button onClick={() => setFilter({})} style={filterBtn("All", "decision", undefined)}>All</button>
          {["BLOCK", "CAUTION", "ALLOW"].map((d) => (
            <button key={d} onClick={() => setFilter({ decision: d })} style={filterBtn(d, "decision", d)}>{d}</button>
          ))}
          <span style={{ width: 1, background: "var(--border)", margin: "0 4px" }} />
          {["HIGH", "MEDIUM", "LOW", "NONE"].map((s) => (
            <button key={s} onClick={() => setFilter({ severity: s })} style={filterBtn(s, "severity", s)}>{s}</button>
          ))}
        </div>

        {/* Reports List */}
        {loading ? (
          <p style={{ color: "var(--muted)", textAlign: "center", padding: 40 }}>Loading...</p>
        ) : reports.length === 0 ? (
          <div style={{ textAlign: "center", padding: 60, color: "var(--muted)" }}>
            <p style={{ fontSize: 16, marginBottom: 12 }}>No assessments yet</p>
            <Link href="/assess" style={{ color: "var(--accent)", textDecoration: "underline" }}>
              Run your first assessment
            </Link>
          </div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
            {reports.map((r) => {
              const dc = DECISION_CONFIG[r.launchDecision];
              return (
                <Link
                  key={r.id}
                  href={`/result/${r.id}`}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: 16,
                    padding: 16,
                    borderRadius: 10,
                    border: "1px solid var(--border)",
                    background: "var(--surface)",
                    textDecoration: "none",
                    color: "var(--foreground)",
                    transition: "border-color 0.15s",
                  }}
                >
                  <span style={{ padding: "4px 10px", borderRadius: 6, fontSize: 12, fontWeight: 700, color: dc.color, background: dc.bg, flexShrink: 0 }}>
                    {r.launchDecision}
                  </span>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontWeight: 600, fontSize: 14 }}>{r.productName}</div>
                    <div style={{ fontSize: 13, color: "var(--muted)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
                      {r.summary}
                    </div>
                  </div>
                  <div style={{ display: "flex", gap: 8, alignItems: "center", flexShrink: 0 }}>
                    <span style={{ fontSize: 12, fontWeight: 600, color: SEVERITY_COLORS[r.severity] }}>{r.severity}</span>
                    <span style={{ fontSize: 12, color: "var(--muted)" }}>
                      {r.findingsSummary.total} findings
                    </span>
                  </div>
                  <span style={{ fontSize: 12, color: "var(--muted)", flexShrink: 0 }}>
                    {new Date(r.createdAt).toLocaleDateString()}
                  </span>
                </Link>
              );
            })}
          </div>
        )}
      </main>
    </>
  );
}
