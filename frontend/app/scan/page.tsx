"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Nav from "@/components/Nav";
import { scanUrl, scanGitHub, getRemainingScans, registerEmail } from "@/lib/scan-api";

type TabType = "url" | "github";

export default function ScanPage() {
  const router = useRouter();
  const [tab, setTab] = useState<TabType>("url");
  const [urlInput, setUrlInput] = useState("");
  const [repoInput, setRepoInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [remaining, setRemaining] = useState<number | null>(null);
  const [showEmailModal, setShowEmailModal] = useState(false);
  const [emailInput, setEmailInput] = useState("");
  const [emailSubmitting, setEmailSubmitting] = useState(false);

  useEffect(() => {
    getRemainingScans()
      .then((data) => setRemaining(data.remaining))
      .catch(() => {});
  }, []);

  async function handleScan(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const report =
        tab === "url"
          ? await scanUrl(urlInput)
          : await scanGitHub(repoInput);
      if (report.remainingScans !== undefined) {
        setRemaining(report.remainingScans);
      }
      if (report.id) {
        router.push(`/report/${report.id}`);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Scan failed";
      if (msg.includes("limit")) {
        setShowEmailModal(true);
      } else {
        setError(msg);
      }
      setLoading(false);
    }
  }

  async function handleEmailSubmit(e: React.FormEvent) {
    e.preventDefault();
    setEmailSubmitting(true);
    try {
      const result = await registerEmail(emailInput);
      setRemaining(result.remaining);
      setShowEmailModal(false);
      setEmailInput("");
    } catch {
      setError("Failed to register email");
    } finally {
      setEmailSubmitting(false);
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
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
          <h1 style={{ fontSize: 28, fontWeight: 700 }}>Security Scan</h1>
          {remaining !== null && (
            <span style={{
              padding: "4px 12px",
              borderRadius: 6,
              fontSize: 13,
              fontWeight: 500,
              background: remaining > 0 ? "var(--surface)" : "rgba(239,68,68,0.1)",
              color: remaining > 0 ? "var(--muted)" : "#ef4444",
              border: "1px solid var(--border)",
            }}>
              {remaining} scans remaining today
            </span>
          )}
        </div>
        <p style={{ fontSize: 15, color: "var(--muted)", marginBottom: 32 }}>
          Scan a live URL for security vulnerabilities or a GitHub repo for hardcoded secrets and code issues.
        </p>

        {/* Tabs */}
        <div style={{ display: "flex", gap: 4, marginBottom: 24 }}>
          {(["url", "github"] as const).map((t) => (
            <button
              key={t}
              onClick={() => { setTab(t); setError(""); }}
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
            <input type="text" required placeholder="https://your-app.com" value={urlInput}
              onChange={(e) => setUrlInput(e.target.value)} style={inputStyle} />
          ) : (
            <input type="text" required placeholder="https://github.com/owner/repo" value={repoInput}
              onChange={(e) => setRepoInput(e.target.value)} style={inputStyle} />
          )}
          <button type="submit" disabled={loading} style={{
            padding: "10px 24px", borderRadius: 8,
            background: loading ? "var(--surface-2)" : "var(--accent)",
            color: "white", fontWeight: 600, fontSize: 14, border: "none",
            cursor: loading ? "not-allowed" : "pointer", whiteSpace: "nowrap",
          }}>
            {loading ? "Scanning..." : "Run Scan"}
          </button>
        </form>

        {error && (
          <div style={{ padding: "10px 14px", borderRadius: 8, background: "rgba(239,68,68,0.1)", border: "1px solid rgba(239,68,68,0.3)", color: "#ef4444", fontSize: 14, marginBottom: 24 }}>
            {error}
          </div>
        )}
      </main>

      {/* Email Modal */}
      {showEmailModal && (
        <div style={{
          position: "fixed", inset: 0, background: "rgba(0,0,0,0.7)",
          display: "flex", alignItems: "center", justifyContent: "center", zIndex: 50,
        }}>
          <div style={{
            background: "var(--surface)", border: "1px solid var(--border)",
            borderRadius: 16, padding: 32, maxWidth: 440, width: "90%",
          }}>
            <h2 style={{ fontSize: 20, fontWeight: 700, marginBottom: 8 }}>
              Daily limit reached
            </h2>
            <p style={{ fontSize: 14, color: "var(--muted)", marginBottom: 20, lineHeight: 1.5 }}>
              You have used all 3 free scans today. Enter your email to unlock 3 more scans.
            </p>
            <form onSubmit={handleEmailSubmit} style={{ display: "flex", flexDirection: "column", gap: 12 }}>
              <input type="email" required placeholder="your@email.com" value={emailInput}
                onChange={(e) => setEmailInput(e.target.value)}
                style={{
                  padding: "10px 14px", borderRadius: 8, border: "1px solid var(--border)",
                  background: "var(--background)", color: "var(--foreground)", fontSize: 14, outline: "none",
                }} />
              <div style={{ display: "flex", gap: 8 }}>
                <button type="submit" disabled={emailSubmitting} style={{
                  flex: 1, padding: "10px", borderRadius: 8,
                  background: "var(--accent)", color: "white", fontWeight: 600,
                  fontSize: 14, border: "none", cursor: "pointer",
                }}>
                  {emailSubmitting ? "Submitting..." : "Unlock 3 More Scans"}
                </button>
                <button type="button" onClick={() => setShowEmailModal(false)} style={{
                  padding: "10px 16px", borderRadius: 8, border: "1px solid var(--border)",
                  background: "transparent", color: "var(--muted)", fontSize: 14, cursor: "pointer",
                }}>
                  Cancel
                </button>
              </div>
            </form>
            <p style={{ fontSize: 12, color: "var(--muted)", marginTop: 12 }}>
              We will only use your email for product updates. No spam.
            </p>
          </div>
        </div>
      )}
    </>
  );
}
