package me.hanhyur.gatewell.auth

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "api_keys")
class ApiKeyEntity(
    @Id
    val key: String,
    val owner: String,
    val plan: String = "free",
    val active: Boolean = true,
    val createdAt: Instant = Instant.now(),
)
