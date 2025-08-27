package io.ckmk.intellijccusage.model

import com.google.gson.annotations.SerializedName
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class CcUsageResponse(
    val blocks: List<CcUsageBlock> = emptyList()
) {
    fun getDisplayText(): String {
        val activeBlock = blocks.firstOrNull { !it.isGap && it.isActive == true }
        
        if (activeBlock == null) return "Claude: No active session"
        
        // Calculate usage percentage based on max tokens from historical data
        // Find the maximum tokens used in any single block to determine the limit
        val maxTokensLimit = blocks.filterNot { it.isGap }
            .maxOfOrNull { it.totalTokens } ?: 0L
        
        val usagePercentage = if (activeBlock.totalTokens > 0 && maxTokensLimit > 0) {
            val percentage = (activeBlock.totalTokens.toDouble() / maxTokensLimit.toDouble()) * 100
            "${String.format("%.1f", percentage)}%"
        } else {
            "0%"
        }
        
        // Show token usage from active block
        val usedTokens = if (activeBlock.totalTokens > 0) {
            formatNumber(activeBlock.totalTokens)
        } else {
            "0"
        }
        
        // Show reset time if available
        val resetIn = findNextResetTime() ?: "Unknown"
        
        return "Claude: $usagePercentage | $usedTokens | $resetIn"
    }
    
    private fun findNextResetTime(): String? {
        // Look for the active block first
        val activeBlock = blocks.firstOrNull { !it.isGap && it.isActive == true }
        
        // If we have an active block with endTime, use that as the reset time
        activeBlock?.endTime?.let { endTime ->
            try {
                val resetInstantUtc = Instant.parse(endTime) // endTime is in UTC
                val now = Instant.now()
                
                if (resetInstantUtc.isAfter(now)) {
                    val duration = java.time.Duration.between(now, resetInstantUtc)
                    val hoursUntilReset = duration.toHours()
                    val minutesUntilReset = duration.toMinutesPart()
                    
                    return when {
                        hoursUntilReset > 0 -> "${hoursUntilReset}h ${minutesUntilReset}m"
                        minutesUntilReset > 0 -> "${minutesUntilReset}m"
                        else -> "<1m"
                    }
                }
            } catch (e: Exception) {
                // Fall through to calculate next reset
            }
        }
        
        // Fallback: Look for blocks with usageLimitResetTime or calculate based on pattern
        val blockWithReset = blocks.firstOrNull { !it.isGap && it.usageLimitResetTime != null }
        
        return blockWithReset?.usageLimitResetTime?.let { resetTime ->
            try {
                val resetInstantUtc = Instant.parse(resetTime)
                val now = Instant.now()
                
                if (resetInstantUtc.isAfter(now)) {
                    val duration = java.time.Duration.between(now, resetInstantUtc)
                    val hoursUntilReset = duration.toHours()
                    val minutesUntilReset = duration.toMinutesPart()
                    
                    when {
                        hoursUntilReset > 0 -> "${hoursUntilReset}h ${minutesUntilReset}m"
                        minutesUntilReset > 0 -> "${minutesUntilReset}m"
                        else -> "<1m"
                    }
                } else {
                    calculateNextDailyReset(resetInstantUtc)
                }
            } catch (e: Exception) {
                null
            }
        } ?: calculateNext3PMReset()
    }
    
    private fun calculateNext3PMReset(): String {
        val now = Instant.now()
        val localZone = ZoneId.systemDefault()
        val currentLocal = LocalDateTime.ofInstant(now, localZone)
        
        // Claude resets at 3 PM local time
        val resetHour = 15 // 3 PM in 24-hour format
        
        val nextResetLocal = if (currentLocal.hour < resetHour) {
            // Reset is today at 3 PM
            currentLocal.toLocalDate().atTime(resetHour, 0)
        } else {
            // Reset is tomorrow at 3 PM
            currentLocal.toLocalDate().plusDays(1).atTime(resetHour, 0)
        }
        
        val nextResetInstant = nextResetLocal.atZone(localZone).toInstant()
        val duration = java.time.Duration.between(now, nextResetInstant)
        val hoursUntilReset = duration.toHours()
        val minutesUntilReset = duration.toMinutesPart()
        
        return when {
            hoursUntilReset > 0 -> "${hoursUntilReset}h ${minutesUntilReset}m"
            minutesUntilReset > 0 -> "${minutesUntilReset}m"
            else -> "<1m"
        }
    }

    private fun calculateNextDailyReset(lastResetTimeUtc: Instant? = null): String {
        val now = Instant.now()
        val localZone = ZoneId.systemDefault()
        val currentLocal = LocalDateTime.ofInstant(now, localZone)
        
        // Default to next day at midnight local time if no reset time is available
        var nextResetLocal = currentLocal.toLocalDate().plusDays(1).atStartOfDay()
        
        // If we have a last reset time (in UTC), convert to local timezone to determine the reset pattern
        lastResetTimeUtc?.let { lastResetUtc ->
            // Convert UTC reset time to local timezone
            val lastResetLocal = LocalDateTime.ofInstant(lastResetUtc, localZone)
            val resetHour = lastResetLocal.hour
            
            // Calculate next reset at the same hour in local time
            nextResetLocal = if (currentLocal.hour < resetHour) {
                currentLocal.toLocalDate().atTime(resetHour, 0)
            } else {
                currentLocal.toLocalDate().plusDays(1).atTime(resetHour, 0)
            }
        }
        
        val nextResetInstant = nextResetLocal.atZone(localZone).toInstant()
        val duration = java.time.Duration.between(now, nextResetInstant)
        val hoursUntilReset = duration.toHours()
        val minutesUntilReset = duration.toMinutesPart()
        
        return when {
            hoursUntilReset > 0 -> "${hoursUntilReset}h ${minutesUntilReset}m"
            minutesUntilReset > 0 -> "${minutesUntilReset}m"
            else -> "<1m"
        }
    }

    private fun formatNumber(number: Long): String {
        return when {
            number >= 1_000_000 -> "${String.format("%.1f", number / 1_000_000.0)}M"
            number >= 1_000 -> "${String.format("%.1f", number / 1_000.0)}K"
            else -> number.toString()
        }
    }
}

data class CcUsageBlock(
    val id: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val actualEndTime: String? = null,
    val isActive: Boolean? = null,
    val isGap: Boolean = false,
    val entries: Int = 0,
    val tokenCounts: TokenCounts? = null,
    val totalTokens: Long = 0,
    val costUSD: Double = 0.0,
    val models: List<String> = emptyList(),
    val burnRate: Any? = null,
    val projection: Any? = null,
    val usageLimitResetTime: String? = null
)

data class TokenCounts(
    val inputTokens: Long = 0,
    val outputTokens: Long = 0,
    val cacheCreationInputTokens: Long = 0,
    val cacheReadInputTokens: Long = 0
)