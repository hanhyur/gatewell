import Link from "next/link";
import Nav from "@/components/Nav";

const features = [
  { title: "14 Risk Rules", desc: "Capability analysis, text scanning, combination detection across 9 risk categories." },
  { title: "Evidence-Based Mitigation", desc: "Show your safeguards and watch your risk score drop." },
  { title: "Instant Decision", desc: "BLOCK, CAUTION, or ALLOW — a clear launch recommendation in seconds." },
  { title: "Reassess & Compare", desc: "Add mitigations, reassess, and compare before vs after." },
];

const steps = [
  { num: "1", title: "Describe your product", desc: "Name, summary, capabilities, and evidence." },
  { num: "2", title: "Get your risk report", desc: "Findings, severity, and launch decision." },
  { num: "3", title: "Fix and reassess", desc: "Address findings, submit new evidence, improve your score." },
];

export default function LandingPage() {
  return (
    <>
      <Nav />
      <main style={{ flex: 1 }}>
        <section style={{ maxWidth: 1100, margin: "0 auto", padding: "80px 24px 60px", textAlign: "center" }}>
          <div style={{ display: "inline-block", padding: "4px 14px", borderRadius: 100, border: "1px solid var(--border)", fontSize: 13, color: "var(--muted)", marginBottom: 24 }}>
            Launch with confidence, not hope
          </div>
          <h1 style={{ fontSize: "clamp(2.5rem, 5vw, 4rem)", fontWeight: 800, lineHeight: 1.1, letterSpacing: "-0.03em", marginBottom: 20 }}>
            Is your AI product<br />
            <span style={{ color: "var(--accent)" }}>safe to launch?</span>
          </h1>
          <p style={{ fontSize: 18, color: "var(--muted)", maxWidth: 560, margin: "0 auto 36px", lineHeight: 1.6 }}>
            Gatewell evaluates your AI product for security risks, data leakage, prompt injection, and 6 more risk categories — before your users find them first.
          </p>
          <div style={{ display: "flex", gap: 12, justifyContent: "center" }}>
            <Link href="/assess" style={{ padding: "12px 28px", borderRadius: 8, background: "var(--accent)", color: "white", fontWeight: 600, fontSize: 15, textDecoration: "none" }}>
              Try Free Assessment
            </Link>
            <Link href="/dashboard" style={{ padding: "12px 28px", borderRadius: 8, border: "1px solid var(--border)", color: "var(--foreground)", fontWeight: 500, fontSize: 15, textDecoration: "none" }}>
              View Dashboard
            </Link>
          </div>
        </section>

        <section style={{ maxWidth: 1100, margin: "0 auto", padding: "40px 24px 60px" }}>
          <h2 style={{ fontSize: 13, fontWeight: 600, textTransform: "uppercase" as const, letterSpacing: "0.08em", color: "var(--accent)", marginBottom: 32, textAlign: "center" }}>How it works</h2>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))", gap: 24 }}>
            {steps.map((s) => (
              <div key={s.num} style={{ padding: 28, borderRadius: 12, border: "1px solid var(--border)", background: "var(--surface)" }}>
                <div style={{ width: 36, height: 36, borderRadius: 8, background: "var(--surface-2)", display: "flex", alignItems: "center", justifyContent: "center", fontWeight: 700, fontSize: 14, color: "var(--accent)", marginBottom: 16 }}>{s.num}</div>
                <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 8 }}>{s.title}</h3>
                <p style={{ fontSize: 14, color: "var(--muted)", lineHeight: 1.5 }}>{s.desc}</p>
              </div>
            ))}
          </div>
        </section>

        <section style={{ maxWidth: 1100, margin: "0 auto", padding: "20px 24px 80px" }}>
          <h2 style={{ fontSize: 13, fontWeight: 600, textTransform: "uppercase" as const, letterSpacing: "0.08em", color: "var(--accent)", marginBottom: 32, textAlign: "center" }}>Why Gatewell</h2>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))", gap: 20 }}>
            {features.map((f) => (
              <div key={f.title} style={{ padding: 24, borderRadius: 12, border: "1px solid var(--border)", background: "var(--surface)" }}>
                <h3 style={{ fontSize: 15, fontWeight: 600, marginBottom: 8 }}>{f.title}</h3>
                <p style={{ fontSize: 14, color: "var(--muted)", lineHeight: 1.5 }}>{f.desc}</p>
              </div>
            ))}
          </div>
        </section>
      </main>
      <footer style={{ borderTop: "1px solid var(--border)", padding: "20px 24px", textAlign: "center", fontSize: 13, color: "var(--muted)" }}>
        Gatewell — AI Launch Risk Evaluation
      </footer>
    </>
  );
}
