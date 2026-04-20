"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Nav from "@/components/Nav";
import { createAssessment } from "@/lib/api";
import { CAPABILITIES } from "@/types/api";

export default function AssessPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [form, setForm] = useState({
    productName: "",
    summary: "",
    evidences: "",
    capabilities: [] as string[],
  });

  function toggleCapability(value: string) {
    setForm((prev) => ({
      ...prev,
      capabilities: prev.capabilities.includes(value)
        ? prev.capabilities.filter((c) => c !== value)
        : [...prev.capabilities, value],
    }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const evidences = form.evidences
        .split("\n")
        .map((e) => e.trim())
        .filter(Boolean);

      if (evidences.length === 0) {
        setError("Please provide at least one evidence item.");
        setLoading(false);
        return;
      }

      const result = await createAssessment({
        productName: form.productName,
        summary: form.summary,
        evidences,
        capabilities: form.capabilities,
      });

      router.push(`/result/${result.id}`);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Assessment failed");
    } finally {
      setLoading(false);
    }
  }

  const inputStyle: React.CSSProperties = {
    width: "100%",
    padding: "10px 14px",
    borderRadius: 8,
    border: "1px solid var(--border)",
    background: "var(--surface)",
    color: "var(--foreground)",
    fontSize: 14,
    outline: "none",
  };

  const labelStyle: React.CSSProperties = {
    fontSize: 14,
    fontWeight: 600,
    marginBottom: 6,
    display: "block",
  };

  return (
    <>
      <Nav />
      <main
        style={{
          flex: 1,
          maxWidth: 680,
          margin: "0 auto",
          padding: "48px 24px",
          width: "100%",
        }}
      >
        <h1
          style={{
            fontSize: 28,
            fontWeight: 700,
            marginBottom: 8,
          }}
        >
          Risk Assessment
        </h1>
        <p
          style={{
            fontSize: 15,
            color: "var(--muted)",
            marginBottom: 36,
          }}
        >
          Describe your AI product and we will evaluate its launch readiness.
        </p>

        <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: 24 }}>
          <div>
            <label style={labelStyle}>Product Name</label>
            <input
              type="text"
              required
              placeholder="e.g. ChatBot Pro"
              value={form.productName}
              onChange={(e) => setForm((p) => ({ ...p, productName: e.target.value }))}
              style={inputStyle}
            />
          </div>

          <div>
            <label style={labelStyle}>Product Summary</label>
            <textarea
              required
              rows={3}
              placeholder="Describe what your AI product does, how users interact with it, and any key characteristics..."
              value={form.summary}
              onChange={(e) => setForm((p) => ({ ...p, summary: e.target.value }))}
              style={{ ...inputStyle, resize: "vertical" }}
            />
          </div>

          <div>
            <label style={labelStyle}>Capabilities</label>
            <p style={{ fontSize: 13, color: "var(--muted)", marginBottom: 12 }}>
              Select all capabilities your AI product has.
            </p>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8 }}>
              {CAPABILITIES.map((cap) => {
                const selected = form.capabilities.includes(cap.value);
                return (
                  <button
                    key={cap.value}
                    type="button"
                    onClick={() => toggleCapability(cap.value)}
                    style={{
                      padding: "10px 14px",
                      borderRadius: 8,
                      border: `1px solid ${selected ? "var(--accent)" : "var(--border)"}`,
                      background: selected ? "rgba(99, 102, 241, 0.1)" : "var(--surface)",
                      color: selected ? "var(--accent)" : "var(--foreground)",
                      fontSize: 13,
                      fontWeight: 500,
                      textAlign: "left",
                      cursor: "pointer",
                      transition: "border-color 0.15s, background 0.15s",
                    }}
                  >
                    <div>{cap.label}</div>
                    <div style={{ fontSize: 12, color: "var(--muted)", marginTop: 2 }}>
                      {cap.description}
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          <div>
            <label style={labelStyle}>Evidence / Mitigations</label>
            <p style={{ fontSize: 13, color: "var(--muted)", marginBottom: 8 }}>
              One per line. Describe security measures, safeguards, or mitigations in place.
            </p>
            <textarea
              required
              rows={4}
              placeholder={"e.g.\nCode execution runs in sandboxed Docker container\nRate limiting at 100 req/min\nAll user data encrypted at rest"}
              value={form.evidences}
              onChange={(e) => setForm((p) => ({ ...p, evidences: e.target.value }))}
              style={{ ...inputStyle, resize: "vertical" }}
            />
          </div>

          {error && (
            <div
              style={{
                padding: "10px 14px",
                borderRadius: 8,
                background: "rgba(239, 68, 68, 0.1)",
                border: "1px solid rgba(239, 68, 68, 0.3)",
                color: "#ef4444",
                fontSize: 14,
              }}
            >
              {error}
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            style={{
              padding: "12px 28px",
              borderRadius: 8,
              background: loading ? "var(--surface-2)" : "var(--accent)",
              color: "white",
              fontWeight: 600,
              fontSize: 15,
              border: "none",
              cursor: loading ? "not-allowed" : "pointer",
              transition: "opacity 0.15s",
            }}
          >
            {loading ? "Evaluating..." : "Run Assessment"}
          </button>
        </form>
      </main>
    </>
  );
}
