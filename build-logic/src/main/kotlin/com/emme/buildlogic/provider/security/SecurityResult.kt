package com.emme.buildlogic.provider.security

data class SecurityScanResult(
    val vulnerabilities: Int,
    val critical: Int,
    val high: Int,
    val reportPath: String,
) {
    fun isClean(): Boolean = critical == 0
}
