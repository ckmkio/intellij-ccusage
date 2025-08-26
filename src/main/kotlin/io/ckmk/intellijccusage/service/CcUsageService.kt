package io.ckmk.intellijccusage.service

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import io.ckmk.intellijccusage.model.CcUsageResponse
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

@Service
class CcUsageService {
    private val logger = Logger.getInstance(CcUsageService::class.java)
    private val gson = Gson()
    
    companion object {
        private const val DEFAULT_REFRESH_INTERVAL = 5L // seconds
    }
    
    suspend fun fetchCcUsageData(): Result<CcUsageResponse> = withContext(Dispatchers.IO) {
        try {
            val processBuilder = ProcessBuilder()
            
            // Try different commands based on system setup
            val commands = listOf(
                listOf("npx", "ccusage", "blocks", "--live", "--json"),
                listOf("bunx", "ccusage", "blocks", "--live", "--json"),
                listOf("ccusage", "blocks", "--live", "--json")
            )
            
            var lastException: Exception? = null
            
            for (command in commands) {
                try {
                    processBuilder.command(command)
                    processBuilder.redirectErrorStream(true)
                    
                    val process = processBuilder.start()
                    val timeoutCompleted = process.waitFor(10, TimeUnit.SECONDS)
                    
                    if (!timeoutCompleted) {
                        process.destroyForcibly()
                        throw RuntimeException("ccusage command timed out")
                    }
                    
                    if (process.exitValue() == 0) {
                        val output = BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                            reader.readText()
                        }
                        
                        if (output.isNotBlank()) {
                            val data = parseJsonOutput(output)
                            return@withContext Result.success(data)
                        }
                    }
                    
                } catch (e: Exception) {
                    lastException = e
                    logger.debug("Failed with command: ${command.joinToString(" ")}", e)
                }
            }
            
            Result.failure(lastException ?: RuntimeException("All ccusage commands failed"))
            
        } catch (e: Exception) {
            logger.warn("Failed to fetch ccusage data", e)
            Result.failure(e)
        }
    }
    
    private fun parseJsonOutput(output: String): CcUsageResponse {
        return try {
            // Try to parse as direct JSON first
            gson.fromJson(output, CcUsageResponse::class.java)
        } catch (e: JsonSyntaxException) {
            logger.debug("JSON parsing failed, attempting fallback", e)
            
            try {
                // If direct parsing fails, try to extract JSON from output
                val jsonStart = output.indexOf('{')
                val jsonEnd = output.lastIndexOf('}')
                
                if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                    val jsonPart = output.substring(jsonStart, jsonEnd + 1)
                    gson.fromJson(jsonPart, CcUsageResponse::class.java)
                } else {
                    // Fallback: return empty response
                    logger.warn("Could not extract JSON from ccusage output")
                    CcUsageResponse()
                }
            } catch (e2: Exception) {
                logger.warn("All JSON parsing attempts failed", e2)
                CcUsageResponse()
            }
        } catch (e: Exception) {
            logger.warn("Unexpected error parsing ccusage output", e)
            CcUsageResponse()
        }
    }
    
    
    fun isCcUsageAvailable(): Boolean {
        return try {
            val commands = listOf(
                listOf("npx", "ccusage", "--version"),
                listOf("bunx", "ccusage", "--version"),
                listOf("ccusage", "--version")
            )
            
            for (command in commands) {
                try {
                    val processBuilder = ProcessBuilder(command)
                    processBuilder.redirectErrorStream(true)
                    val process = processBuilder.start()
                    val completed = process.waitFor(5, TimeUnit.SECONDS)
                    
                    if (completed && process.exitValue() == 0) {
                        return true
                    }
                } catch (e: Exception) {
                    logger.debug("Command failed: ${command.joinToString(" ")}", e)
                }
            }
            false
        } catch (e: Exception) {
            logger.debug("Error checking ccusage availability", e)
            false
        }
    }
}