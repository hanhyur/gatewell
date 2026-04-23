package me.hanhyur.gatewell.scanner

data class HumanReadableGuide(
    val risk: String,
    val impact: String,
    val fixes: List<PlatformFix>,
)

data class PlatformFix(
    val platform: String,
    val instruction: String,
    val code: String? = null,
)

object FindingGuideMap {

    private val guides = mapOf(
        // --- Security Headers ---
        "MISSING_HSTS" to HumanReadableGuide(
            risk = "Your site can be impersonated",
            impact = "Attackers on the same network (e.g. public WiFi) can intercept passwords and personal data by downgrading HTTPS to HTTP.",
            fixes = listOf(
                PlatformFix("Vercel", "Add this header to vercel.json",
                    """{"headers":[{"source":"/(.*)", "headers":[{"key":"Strict-Transport-Security","value":"max-age=31536000; includeSubDomains; preload"}]}]}"""),
                PlatformFix("Netlify", "Add to your _headers file",
                    """/*\n  Strict-Transport-Security: max-age=31536000; includeSubDomains; preload"""),
                PlatformFix("Cloudflare", "Go to SSL/TLS → Edge Certificates and enable 'Always Use HTTPS' and 'HSTS'", null),
                PlatformFix("Nginx", "Add this header to your server config",
                    """add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;"""),
            ),
        ),
        "MISSING_CSP" to HumanReadableGuide(
            risk = "Malicious scripts can be injected into your site",
            impact = "Without a Content Security Policy, attackers can inject scripts that steal login credentials, credit card numbers, and personal data (XSS attack).",
            fixes = listOf(
                PlatformFix("Vercel", "Add a CSP header to vercel.json",
                    """{"headers":[{"source":"/(.*)", "headers":[{"key":"Content-Security-Policy","value":"default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'"}]}]}"""),
                PlatformFix("Next.js", "Configure headers() in next.config.js",
                    """async headers() { return [{ source: '/(.*)', headers: [{ key: 'Content-Security-Policy', value: "default-src 'self'" }] }] }"""),
                PlatformFix("Nginx", "Add a CSP header to your server config",
                    """add_header Content-Security-Policy "default-src 'self'; script-src 'self'" always;"""),
            ),
        ),
        "WEAK_CSP" to HumanReadableGuide(
            risk = "Security policy has gaps",
            impact = "Your CSP contains 'unsafe-inline' or 'unsafe-eval', which still allows malicious scripts to execute.",
            fixes = listOf(
                PlatformFix("All platforms", "Remove 'unsafe-inline' from CSP and switch to nonce-based scripts. Also remove 'unsafe-eval'.", null),
            ),
        ),
        "MISSING_XCTO" to HumanReadableGuide(
            risk = "Files can be disguised as different types",
            impact = "Attackers can upload malicious scripts disguised as image files, and browsers may execute them.",
            fixes = listOf(
                PlatformFix("Vercel", "Add to vercel.json headers",
                    """{"key":"X-Content-Type-Options","value":"nosniff"}"""),
                PlatformFix("Netlify", "Add to _headers file",
                    """/*\n  X-Content-Type-Options: nosniff"""),
                PlatformFix("Nginx", "Add to server config",
                    """add_header X-Content-Type-Options "nosniff" always;"""),
            ),
        ),
        "MISSING_XFO" to HumanReadableGuide(
            risk = "Your site can be hidden inside another site",
            impact = "Attackers can overlay your site in an invisible frame, tricking users into clicking buttons they can't see (clickjacking).",
            fixes = listOf(
                PlatformFix("Vercel", "Add to vercel.json headers",
                    """{"key":"X-Frame-Options","value":"DENY"}"""),
                PlatformFix("Nginx", "Add to server config",
                    """add_header X-Frame-Options "DENY" always;"""),
            ),
        ),
        "MISSING_REFERRER_POLICY" to HumanReadableGuide(
            risk = "Browsing history can leak to third parties",
            impact = "The full URL users visited (including sensitive query parameters) can be sent to external sites.",
            fixes = listOf(
                PlatformFix("Vercel", "Add to vercel.json headers",
                    """{"key":"Referrer-Policy","value":"strict-origin-when-cross-origin"}"""),
                PlatformFix("Nginx", "Add to server config",
                    """add_header Referrer-Policy "strict-origin-when-cross-origin" always;"""),
            ),
        ),
        "MISSING_PERMISSIONS_POLICY" to HumanReadableGuide(
            risk = "Browser features are not restricted",
            impact = "If a malicious script is injected, it could access the camera, microphone, or location without restriction.",
            fixes = listOf(
                PlatformFix("Vercel", "Add to vercel.json headers",
                    """{"key":"Permissions-Policy","value":"camera=(), microphone=(), geolocation=()"}"""),
                PlatformFix("Nginx", "Add to server config",
                    """add_header Permissions-Policy "camera=(), microphone=(), geolocation=()" always;"""),
            ),
        ),

        // --- CORS ---
        "CORS_WILDCARD_WITH_CREDENTIALS" to HumanReadableGuide(
            risk = "Any website can steal your users' data",
            impact = "Any site on the internet can make authenticated requests and read responses. This is the most dangerous CORS misconfiguration.",
            fixes = listOf(
                PlatformFix("All platforms", "Replace '*' with your specific frontend domain",
                    """Access-Control-Allow-Origin: https://your-frontend.com"""),
            ),
        ),
        "CORS_REFLECTS_ORIGIN" to HumanReadableGuide(
            risk = "Server trusts any website that asks",
            impact = "The server echoes back whatever Origin is sent. An attacker's site can make API calls on behalf of logged-in users.",
            fixes = listOf(
                PlatformFix("All platforms", "Don't reflect the Origin header. Compare against a whitelist of allowed domains instead.", null),
            ),
        ),
        "CORS_WILDCARD" to HumanReadableGuide(
            risk = "Any website can read your API responses",
            impact = "If your API returns user-specific data, any website can read it. Fine for truly public APIs, risky otherwise.",
            fixes = listOf(
                PlatformFix("All platforms", "Restrict Access-Control-Allow-Origin to your frontend domain only.", null),
            ),
        ),

        // --- Cookies ---
        "COOKIE_NO_HTTPONLY" to HumanReadableGuide(
            risk = "Cookies are accessible via JavaScript",
            impact = "If an XSS attack occurs, the attacker's script can steal login cookies and hijack user accounts.",
            fixes = listOf(
                PlatformFix("All platforms", "Add the HttpOnly flag when setting cookies",
                    """Set-Cookie: session=abc123; HttpOnly; Secure; SameSite=Lax"""),
            ),
        ),
        "COOKIE_NO_SECURE" to HumanReadableGuide(
            risk = "Cookies can be sent over unencrypted connections",
            impact = "On HTTP connections, cookies are transmitted in plain text and can be intercepted by attackers.",
            fixes = listOf(
                PlatformFix("All platforms", "Add the Secure flag to your cookies", null),
            ),
        ),
        "COOKIE_NO_SAMESITE" to HumanReadableGuide(
            risk = "Cookies are sent with cross-site requests",
            impact = "Attackers can trick users into making unintended actions like payments or password changes (CSRF attack).",
            fixes = listOf(
                PlatformFix("All platforms", "Add SameSite=Lax or SameSite=Strict to your cookies", null),
            ),
        ),

        // --- SSL/TLS ---
        "NO_HTTPS" to HumanReadableGuide(
            risk = "Your site is not encrypted",
            impact = "All traffic is sent in plain text. Passwords, personal data, and everything else is visible to anyone on the network.",
            fixes = listOf(
                PlatformFix("Vercel/Netlify", "HTTPS is enabled by default. Check your custom domain SSL certificate.", null),
                PlatformFix("Server", "Install a free SSL certificate with Let's Encrypt",
                    """sudo certbot --nginx -d yourdomain.com"""),
            ),
        ),
        "HTTP_ACCESSIBLE" to HumanReadableGuide(
            risk = "Unencrypted HTTP is still accessible",
            impact = "Users who accidentally visit http:// will communicate without encryption.",
            fixes = listOf(
                PlatformFix("Nginx", "Set up HTTP to HTTPS redirect",
                    "server { listen 80; return 301 https://\$host\$request_uri; }"),
                PlatformFix("Cloudflare", "Enable 'Always Use HTTPS' in SSL/TLS settings", null),
            ),
        ),

        // --- Exposed Paths ---
        "EXPOSED_PATH___ENV" to HumanReadableGuide(
            risk = "Passwords and API keys are publicly accessible",
            impact = "Your .env file contains database passwords, API keys, and secrets. Anyone can access them right now. Rotate all exposed keys immediately.",
            fixes = listOf(
                PlatformFix("Nginx", "Block access to .env files",
                    """location ~ /\\.env { deny all; return 404; }"""),
                PlatformFix("All platforms", "Ensure .env is in .gitignore and not served by your web server. Rotate all exposed credentials now.", null),
            ),
        ),
        "EXPOSED_PATH___GIT_CONFIG" to HumanReadableGuide(
            risk = "Your entire source code can be downloaded",
            impact = "The .git folder is exposed. Attackers can download your full source code, commit history, and any hardcoded passwords.",
            fixes = listOf(
                PlatformFix("Nginx", "Block access to .git directory",
                    """location ~ /\\.git { deny all; return 404; }"""),
                PlatformFix("Apache", "Add to .htaccess",
                    """RedirectMatch 404 /\\.git"""),
            ),
        ),

        // --- Error Handling ---
        "STACK_TRACE_EXPOSED" to HumanReadableGuide(
            risk = "Internal server details are leaked in errors",
            impact = "Error pages reveal file paths, framework versions, and database structure. Attackers use this information for targeted attacks.",
            fixes = listOf(
                PlatformFix("Spring Boot", "Disable error details in application.yml",
                    """server:\n  error:\n    include-stacktrace: never\n    include-message: never"""),
                PlatformFix("Express.js", "Set up a production error handler",
                    """app.use((err, req, res, next) => { res.status(500).json({ error: 'Internal Server Error' }) })"""),
                PlatformFix("Django", "Turn off DEBUG in settings.py",
                    """DEBUG = False"""),
            ),
        ),
        "REFLECTED_XSS" to HumanReadableGuide(
            risk = "User input is reflected back without sanitization",
            impact = "Attackers can craft malicious links that execute JavaScript in users' browsers, stealing login sessions and personal data.",
            fixes = listOf(
                PlatformFix("All platforms", "Always escape user input before rendering in HTML. Use textContent instead of innerHTML.", null),
            ),
        ),

        // --- Information Leakage ---
        "SERVER_HEADER_EXPOSED" to HumanReadableGuide(
            risk = "Server software is revealed",
            impact = "Knowing the exact server software and version helps attackers find known vulnerabilities to exploit.",
            fixes = listOf(
                PlatformFix("Nginx", "Hide the Server header",
                    """server_tokens off;"""),
                PlatformFix("Express.js", "Use the helmet package",
                    """app.use(helmet())"""),
            ),
        ),
        "POWERED_BY_EXPOSED" to HumanReadableGuide(
            risk = "Framework technology is exposed",
            impact = "The X-Powered-By header reveals your framework (Express, PHP, etc.), making it easier to target known vulnerabilities.",
            fixes = listOf(
                PlatformFix("Express.js", "Disable the X-Powered-By header",
                    """app.disable('x-powered-by')"""),
                PlatformFix("PHP", "Set in php.ini",
                    """expose_php = Off"""),
            ),
        ),

        // --- Bot Protection ---
        "BOT_PROTECTION_DETECTED" to HumanReadableGuide(
            risk = "Bot protection is active",
            impact = "This site uses Cloudflare or similar bot protection, which blocks automated scanning. This scanner is designed for side projects, startups, and vibe-coded apps without enterprise-grade protection.",
            fixes = emptyList(),
        ),
    )

    fun getGuide(code: String): HumanReadableGuide? = guides[code]
}
