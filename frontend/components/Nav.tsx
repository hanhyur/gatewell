"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const links = [
  { href: "/", label: "Home" },
  { href: "/scan", label: "Scan" },
];

export default function Nav() {
  const pathname = usePathname();

  return (
    <nav
      style={{
        borderBottom: "1px solid var(--border)",
        background: "var(--surface)",
        position: "sticky",
        top: 0,
        zIndex: 40,
      }}
    >
      <div
        style={{
          maxWidth: 1100,
          margin: "0 auto",
          padding: "0 24px",
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          height: 56,
        }}
      >
        <Link
          href="/"
          style={{
            fontWeight: 700,
            fontSize: 18,
            color: "var(--foreground)",
            textDecoration: "none",
            display: "flex",
            alignItems: "center",
            gap: 8,
          }}
        >
          <span
            style={{
              width: 28,
              height: 28,
              borderRadius: 6,
              background: "var(--accent)",
              display: "inline-flex",
              alignItems: "center",
              justifyContent: "center",
              fontSize: 14,
              color: "white",
              fontWeight: 800,
            }}
          >
            G
          </span>
          Gatewell
        </Link>

        <div style={{ display: "flex", gap: 4 }}>
          {links.map((link) => {
            const active = pathname === link.href;
            return (
              <Link
                key={link.href}
                href={link.href}
                style={{
                  padding: "6px 14px",
                  borderRadius: 6,
                  fontSize: 14,
                  fontWeight: 500,
                  color: active ? "var(--foreground)" : "var(--muted)",
                  background: active ? "var(--surface-2)" : "transparent",
                  textDecoration: "none",
                  transition: "color 0.15s, background 0.15s",
                }}
              >
                {link.label}
              </Link>
            );
          })}
        </div>
      </div>
    </nav>
  );
}
