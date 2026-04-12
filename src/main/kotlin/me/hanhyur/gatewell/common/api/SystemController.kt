package me.hanhyur.gatewell.common.api

import me.hanhyur.gatewell.assessment.domain.policy.RuleVersionInfo
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SystemController {

    @GetMapping("/rule-version")
    fun getRuleVersion(): ResponseEntity<RuleVersionResponse> {
        return ResponseEntity.ok(
            RuleVersionResponse(
                version = RuleVersionInfo.CURRENT.value,
                totalRules = RuleVersionInfo.RULE_DESCRIPTIONS.size,
                rules = RuleVersionInfo.RULE_DESCRIPTIONS,
            )
        )
    }
}

data class RuleVersionResponse(
    val version: String,
    val totalRules: Int,
    val rules: List<String>,
)
