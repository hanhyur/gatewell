package me.hanhyur.gatewell.scanner

data class ScanFinding(
    val severity: ScanSeverity,
    val category: ScanCategory,
    val code: String,
    val title: String,
    val detail: String,
    val evidence: String,
)

enum class ScanSeverity { CRITICAL, HIGH, MEDIUM, LOW, INFO }

enum class ScanCategory {
    MISSING_SECURITY_HEADER,
    CORS_MISCONFIGURATION,
    INFORMATION_LEAKAGE,
    EXPOSED_SENSITIVE_PATH,
    COOKIE_SECURITY,
    SSL_TLS,
    ERROR_HANDLING,
    AUTHENTICATION,
    HARDCODED_SECRET,
    CODE_VULNERABILITY,
    SENSITIVE_FILE_EXPOSED,
}
