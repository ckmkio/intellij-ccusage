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
            
            // Set up environment to include common Node.js paths
            setupNodeEnvironment(processBuilder)
            
            // Try different commands based on system setup
            val commands = buildCommandList()
            
            var lastException: Exception? = null
            
            for (command in commands) {
                try {
                    logger.info("Attempting command: ${command.joinToString(" ")}")
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
            val commands = buildCommandList("--version")
            
            for (command in commands) {
                try {
                    val processBuilder = ProcessBuilder(command)
                    setupNodeEnvironment(processBuilder)
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
    
    private fun buildCommandList(vararg extraArgs: String): List<List<String>> {
        val baseArgs = if (extraArgs.isEmpty()) {
            listOf("blocks", "--live", "--json")
        } else {
            extraArgs.toList()
        }
        
        val commands = mutableListOf<List<String>>()
        
        // Try with full paths first
        val homeDir = System.getProperty("user.home")
        val nvmDir = java.io.File("$homeDir/.nvm/versions/node")
        if (nvmDir.exists()) {
            nvmDir.listFiles()?.forEach { versionDir ->
                if (versionDir.isDirectory) {
                    val ccusagePath = java.io.File(versionDir, "bin/ccusage")
                    val npxPath = java.io.File(versionDir, "bin/npx")
                    if (ccusagePath.exists()) {
                        commands.add(listOf(ccusagePath.absolutePath) + baseArgs)
                    }
                    if (npxPath.exists()) {
                        commands.add(listOf(npxPath.absolutePath, "ccusage") + baseArgs)
                    }
                }
            }
        }
        
        // Try standard commands
        commands.addAll(listOf(
            listOf("npx", "ccusage") + baseArgs,
            listOf("bunx", "ccusage") + baseArgs,
            listOf("ccusage") + baseArgs
        ))
        
        return commands
    }
    
    private fun setupNodeEnvironment(processBuilder: ProcessBuilder) {
        val currentEnv = processBuilder.environment()
        val currentPath = currentEnv["PATH"] ?: System.getenv("PATH") ?: ""
        
        // Try to detect Node.js installation dynamically
        val possibleNodePaths = mutableListOf<String>()
        
        // Check NVM installations
        val homeDir = System.getProperty("user.home")
        val nvmDir = java.io.File("$homeDir/.nvm/versions/node")
        if (nvmDir.exists()) {
            nvmDir.listFiles()?.forEach { versionDir ->
                if (versionDir.isDirectory) {
                    val binDir = java.io.File(versionDir, "bin")
                    if (binDir.exists()) {
                        possibleNodePaths.add(binDir.absolutePath)
                    }
                }
            }
        }
        
        // Add common installation paths
        possibleNodePaths.addAll(listOf(
            "/usr/local/bin",
            "/opt/homebrew/bin", 
            "$homeDir/.local/bin",
            "/usr/bin"
        ))
        
        // Find paths that exist and contain ccusage
        val validPaths = possibleNodePaths.filter { path ->
            val pathDir = java.io.File(path)
            pathDir.exists() && (
                java.io.File(pathDir, "ccusage").exists() || 
                java.io.File(pathDir, "npx").exists() ||
                java.io.File(pathDir, "node").exists()
            )
        }
        
        // Add valid paths to environment
        if (validPaths.isNotEmpty()) {
            val pathList = currentPath.split(":").toMutableList()
            validPaths.forEach { path ->
                if (!pathList.contains(path)) {
                    pathList.add(0, path) // Add to beginning for priority
                }
            }
            val newPath = pathList.joinToString(":")
            currentEnv["PATH"] = newPath
            logger.info("Updated PATH for ccusage with: ${validPaths.joinToString(", ")}")
        } else {
            logger.warn("No valid Node.js paths found for ccusage")
        }
    }
}