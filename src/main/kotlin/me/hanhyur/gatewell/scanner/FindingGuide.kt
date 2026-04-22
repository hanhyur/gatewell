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
            risk = "사이트가 가짜로 바꿔치기될 수 있어요",
            impact = "해커가 같은 네트워크(카페 와이파이 등)에서 사용자의 비밀번호, 개인정보를 도청할 수 있습니다.",
            fixes = listOf(
                PlatformFix("Vercel", "vercel.json에 헤더를 추가하세요",
                    """{"headers":[{"source":"/(.*)", "headers":[{"key":"Strict-Transport-Security","value":"max-age=31536000; includeSubDomains; preload"}]}]}"""),
                PlatformFix("Netlify", "_headers 파일에 추가하세요",
                    """/*\n  Strict-Transport-Security: max-age=31536000; includeSubDomains; preload"""),
                PlatformFix("Cloudflare", "SSL/TLS → Edge Certificates에서 'Always Use HTTPS'와 'HSTS'를 활성화하세요", null),
                PlatformFix("일반 서버", "웹서버(Nginx/Apache) 설정에 헤더를 추가하세요",
                    """add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;"""),
            ),
        ),
        "MISSING_CSP" to HumanReadableGuide(
            risk = "악성 코드가 사이트에 삽입될 수 있어요",
            impact = "해커가 사이트에 악성 스크립트를 주입해서 사용자의 로그인 정보, 카드번호 등을 훔칠 수 있습니다 (XSS 공격).",
            fixes = listOf(
                PlatformFix("Vercel", "vercel.json에 CSP 헤더를 추가하세요",
                    """{"headers":[{"source":"/(.*)", "headers":[{"key":"Content-Security-Policy","value":"default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'"}]}]}"""),
                PlatformFix("Next.js", "next.config.js에서 headers()를 설정하세요",
                    """async headers() { return [{ source: '/(.*)', headers: [{ key: 'Content-Security-Policy', value: "default-src 'self'" }] }] }"""),
                PlatformFix("일반 서버", "웹서버 설정에 CSP 헤더를 추가하세요",
                    """add_header Content-Security-Policy "default-src 'self'; script-src 'self'" always;"""),
            ),
        ),
        "WEAK_CSP" to HumanReadableGuide(
            risk = "보안 설정이 있지만 허점이 있어요",
            impact = "CSP가 설정되어 있지만 'unsafe-inline'이나 'unsafe-eval' 때문에 악성 스크립트가 여전히 실행될 수 있습니다.",
            fixes = listOf(
                PlatformFix("모든 플랫폼", "CSP에서 'unsafe-inline'을 제거하고 nonce 방식으로 전환하세요. 'unsafe-eval'도 제거하세요.", null),
            ),
        ),
        "MISSING_XCTO" to HumanReadableGuide(
            risk = "파일 형식이 위조될 수 있어요",
            impact = "해커가 이미지 파일로 위장한 악성 스크립트를 업로드하면, 브라우저가 이를 실행할 수 있습니다.",
            fixes = listOf(
                PlatformFix("Vercel", "vercel.json 헤더에 추가",
                    """{"key":"X-Content-Type-Options","value":"nosniff"}"""),
                PlatformFix("Netlify", "_headers 파일에 추가",
                    """/*\n  X-Content-Type-Options: nosniff"""),
                PlatformFix("일반 서버", "웹서버 설정에 추가",
                    """add_header X-Content-Type-Options "nosniff" always;"""),
            ),
        ),
        "MISSING_XFO" to HumanReadableGuide(
            risk = "사이트가 다른 사이트 안에 숨겨질 수 있어요",
            impact = "해커가 보이지 않는 프레임으로 사이트를 덮어씌워서, 사용자가 모르는 사이에 클릭하게 만들 수 있습니다 (클릭재킹).",
            fixes = listOf(
                PlatformFix("Vercel", "vercel.json 헤더에 추가",
                    """{"key":"X-Frame-Options","value":"DENY"}"""),
                PlatformFix("일반 서버", "웹서버 설정에 추가",
                    """add_header X-Frame-Options "DENY" always;"""),
            ),
        ),
        "MISSING_REFERRER_POLICY" to HumanReadableGuide(
            risk = "방문 경로가 외부에 노출될 수 있어요",
            impact = "사용자가 어디서 왔는지(URL) 정보가 외부 사이트로 전달되어 개인정보가 유출될 수 있습니다.",
            fixes = listOf(
                PlatformFix("Vercel", "vercel.json 헤더에 추가",
                    """{"key":"Referrer-Policy","value":"strict-origin-when-cross-origin"}"""),
                PlatformFix("일반 서버", "웹서버 설정에 추가",
                    """add_header Referrer-Policy "strict-origin-when-cross-origin" always;"""),
            ),
        ),
        "MISSING_PERMISSIONS_POLICY" to HumanReadableGuide(
            risk = "브라우저 기능이 제한되지 않았어요",
            impact = "악성 스크립트가 삽입되면 카메라, 마이크, 위치 정보에 접근할 수 있습니다.",
            fixes = listOf(
                PlatformFix("Vercel", "vercel.json 헤더에 추가",
                    """{"key":"Permissions-Policy","value":"camera=(), microphone=(), geolocation=()"}"""),
                PlatformFix("일반 서버", "웹서버 설정에 추가",
                    """add_header Permissions-Policy "camera=(), microphone=(), geolocation=()" always;"""),
            ),
        ),

        // --- CORS ---
        "CORS_WILDCARD_WITH_CREDENTIALS" to HumanReadableGuide(
            risk = "어떤 사이트에서든 사용자 데이터를 빼갈 수 있어요",
            impact = "해커가 만든 사이트에서 로그인된 사용자의 데이터를 몰래 가져갈 수 있습니다. 가장 위험한 CORS 설정입니다.",
            fixes = listOf(
                PlatformFix("모든 플랫폼", "CORS Access-Control-Allow-Origin을 '*'이 아닌 정확한 도메인으로 바꾸세요",
                    """Access-Control-Allow-Origin: https://your-frontend.com"""),
            ),
        ),
        "CORS_REFLECTS_ORIGIN" to HumanReadableGuide(
            risk = "모든 외부 사이트의 요청을 허용하고 있어요",
            impact = "서버가 요청한 사이트를 무조건 신뢰합니다. 해커 사이트에서도 사용자 대신 API를 호출할 수 있습니다.",
            fixes = listOf(
                PlatformFix("모든 플랫폼", "Origin 헤더를 반사하지 말고, 허용된 도메인 목록과 비교해서 응답하세요", null),
            ),
        ),
        "CORS_WILDCARD" to HumanReadableGuide(
            risk = "모든 사이트에서 API 응답을 읽을 수 있어요",
            impact = "공개 API라면 괜찮지만, 사용자별 데이터를 반환하는 API라면 데이터 유출 위험이 있습니다.",
            fixes = listOf(
                PlatformFix("모든 플랫폼", "Access-Control-Allow-Origin을 프론트엔드 도메인으로 제한하세요", null),
            ),
        ),

        // --- Cookies ---
        "COOKIE_NO_HTTPONLY" to HumanReadableGuide(
            risk = "쿠키가 JavaScript로 접근 가능해요",
            impact = "XSS 공격이 발생하면 해커의 스크립트가 로그인 쿠키를 훔쳐서 계정을 탈취할 수 있습니다.",
            fixes = listOf(
                PlatformFix("모든 플랫폼", "쿠키 설정 시 HttpOnly 플래그를 추가하세요",
                    """Set-Cookie: session=abc123; HttpOnly; Secure; SameSite=Lax"""),
            ),
        ),
        "COOKIE_NO_SECURE" to HumanReadableGuide(
            risk = "쿠키가 암호화 안 된 연결로 전송될 수 있어요",
            impact = "HTTP 연결에서 쿠키가 평문으로 전송되어 도청될 수 있습니다.",
            fixes = listOf(
                PlatformFix("모든 플랫폼", "쿠키에 Secure 플래그를 추가하세요", null),
            ),
        ),
        "COOKIE_NO_SAMESITE" to HumanReadableGuide(
            risk = "다른 사이트에서 쿠키가 함께 전송될 수 있어요",
            impact = "CSRF 공격으로 사용자 모르게 결제, 비밀번호 변경 등이 실행될 수 있습니다.",
            fixes = listOf(
                PlatformFix("모든 플랫폼", "쿠키에 SameSite=Lax 또는 Strict를 추가하세요", null),
            ),
        ),

        // --- SSL/TLS ---
        "NO_HTTPS" to HumanReadableGuide(
            risk = "사이트가 암호화되지 않았어요",
            impact = "모든 통신이 평문으로 전송됩니다. 비밀번호, 개인정보가 네트워크에서 그대로 보입니다.",
            fixes = listOf(
                PlatformFix("Vercel/Netlify", "기본으로 HTTPS가 적용됩니다. 커스텀 도메인 SSL 인증서를 확인하세요", null),
                PlatformFix("일반 서버", "Let's Encrypt로 무료 SSL 인증서를 설치하세요",
                    """sudo certbot --nginx -d yourdomain.com"""),
            ),
        ),
        "HTTP_ACCESSIBLE" to HumanReadableGuide(
            risk = "HTTP(암호화 안 됨)로도 접속이 가능해요",
            impact = "사용자가 실수로 http://로 접속하면 암호화 없이 통신하게 됩니다.",
            fixes = listOf(
                PlatformFix("Nginx", "HTTP → HTTPS 리다이렉트를 설정하세요",
                    "server { listen 80; return 301 https://\$host\$request_uri; }"),
                PlatformFix("Cloudflare", "'Always Use HTTPS' 옵션을 켜세요", null),
            ),
        ),

        // --- Exposed Paths ---
        "EXPOSED_PATH___ENV" to HumanReadableGuide(
            risk = "비밀번호, API 키가 공개되어 있어요",
            impact = ".env 파일에는 데이터베이스 비밀번호, API 키 등이 저장됩니다. 누구나 접근 가능한 상태입니다. 즉시 키를 교체하세요.",
            fixes = listOf(
                PlatformFix("모든 플랫폼", ".env 파일이 웹에서 접근 불가하도록 서버 설정을 확인하세요. 노출된 키는 즉시 교체하세요.",
                    """# Nginx: .env 접근 차단\nlocation ~ /\\.env { deny all; return 404; }"""),
            ),
        ),
        "EXPOSED_PATH___GIT_CONFIG" to HumanReadableGuide(
            risk = "소스 코드 전체가 다운로드될 수 있어요",
            impact = ".git 폴더가 노출되면 해커가 전체 소스코드, 커밋 히스토리, 하드코딩된 비밀번호를 모두 가져갈 수 있습니다.",
            fixes = listOf(
                PlatformFix("Nginx", ".git 접근을 차단하세요",
                    """location ~ /\\.git { deny all; return 404; }"""),
                PlatformFix("Apache", ".htaccess에 추가",
                    """RedirectMatch 404 /\\.git"""),
            ),
        ),

        // --- Error Handling ---
        "STACK_TRACE_EXPOSED" to HumanReadableGuide(
            risk = "에러 발생 시 서버 내부 정보가 노출돼요",
            impact = "파일 경로, 프레임워크 버전, 데이터베이스 구조 등이 에러 페이지에 표시됩니다. 해커가 이 정보로 정밀 공격을 할 수 있습니다.",
            fixes = listOf(
                PlatformFix("Spring Boot", "application.yml에서 에러 상세를 비활성화하세요",
                    """server:\n  error:\n    include-stacktrace: never\n    include-message: never"""),
                PlatformFix("Express.js", "프로덕션에서 에러 핸들러를 설정하세요",
                    """app.use((err, req, res, next) => { res.status(500).json({ error: 'Internal Server Error' }) })"""),
                PlatformFix("Django", "settings.py에서 DEBUG를 끄세요",
                    """DEBUG = False"""),
            ),
        ),
        "REFLECTED_XSS" to HumanReadableGuide(
            risk = "사용자 입력이 그대로 페이지에 표시돼요",
            impact = "해커가 특수한 링크를 만들어 사용자가 클릭하면, 사용자의 브라우저에서 악성 코드가 실행됩니다. 로그인 정보 탈취가 가능합니다.",
            fixes = listOf(
                PlatformFix("모든 플랫폼", "사용자 입력을 HTML에 표시할 때 반드시 이스케이프(escape) 처리하세요. innerHTML 대신 textContent를 사용하세요.", null),
            ),
        ),

        // --- Information Leakage ---
        "SERVER_HEADER_EXPOSED" to HumanReadableGuide(
            risk = "서버 종류가 외부에 노출돼요",
            impact = "해커가 서버 소프트웨어(Nginx, Apache 등)와 버전을 알면, 해당 버전의 알려진 취약점을 이용해 공격할 수 있습니다.",
            fixes = listOf(
                PlatformFix("Nginx", "nginx.conf에서 Server 헤더를 숨기세요",
                    """server_tokens off;"""),
                PlatformFix("Express.js", "helmet 패키지를 사용하세요",
                    """app.use(helmet())"""),
            ),
        ),
        "POWERED_BY_EXPOSED" to HumanReadableGuide(
            risk = "사용 중인 프레임워크가 노출돼요",
            impact = "X-Powered-By 헤더로 Express, PHP 등의 프레임워크가 드러나면 버전별 알려진 취약점으로 공격 대상이 됩니다.",
            fixes = listOf(
                PlatformFix("Express.js", "X-Powered-By 헤더를 제거하세요",
                    """app.disable('x-powered-by')"""),
                PlatformFix("PHP", "php.ini에서 설정하세요",
                    """expose_php = Off"""),
            ),
        ),

        // --- Bot Protection ---
        "BOT_PROTECTION_DETECTED" to HumanReadableGuide(
            risk = "봇 차단이 활성화되어 있어요",
            impact = "이 사이트는 Cloudflare 등의 봇 차단을 사용하고 있어 자동 스캔이 제한됩니다. 이 스캐너는 사이드 프로젝트, 스타트업 앱을 위해 설계되었습니다.",
            fixes = emptyList(),
        ),
    )

    fun getGuide(code: String): HumanReadableGuide? = guides[code]
}
