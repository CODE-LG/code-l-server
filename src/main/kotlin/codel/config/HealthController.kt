package codel.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.File
import javax.sql.DataSource

@RestController
@RequestMapping("/actuator")
class HealthController {

    @Autowired
    private lateinit var dataSource: DataSource

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, Any>> {
        return try {
            val dbStatus: Map<String, Any> = checkDatabaseHealth()
            val diskStatus: Map<String, Any> = checkDiskSpace()

            val overallStatus =
                if (dbStatus["status"] == "UP" && diskStatus["status"] == "UP") "UP" else "DOWN"

            val healthStatus: Map<String, Any> = mapOf(
                "status" to overallStatus,
                "groups" to listOf("liveness", "readiness"),
                "components" to mapOf(
                    "db" to dbStatus,
                    "diskSpace" to diskStatus,
                    "livenessState" to mapOf("status" to "UP"),
                    "ping" to mapOf("status" to "UP"),
                    "readinessState" to mapOf("status" to "UP")
                )
            )

            ResponseEntity.ok(healthStatus)
        } catch (e: Exception) {
            val errorStatus: Map<String, Any> = mapOf(
                "status" to "DOWN",
                "error" to (e.message ?: "unknown")
            )
            ResponseEntity.status(503).body(errorStatus)
        }
    }

    private fun checkDatabaseHealth(): Map<String, Any> {
        return try {
            dataSource.connection.use { connection ->
                val isValid = connection.isValid(5) // 5초 타임아웃
                if (isValid) {
                    mapOf(
                        "status" to "UP",
                        "details" to mapOf(
                            "database" to "MySQL",
                            "validationQuery" to "isValid()"
                        )
                    )
                } else {
                    mapOf(
                        "status" to "DOWN",
                        "error" to "Database connection invalid"
                    )
                }
            }
        } catch (e: Exception) {
            mapOf(
                "status" to "DOWN",
                "error" to (e.message ?: "db check failed")
            )
        }
    }

    private fun checkDiskSpace(): Map<String, Any> {
        return try {
            val currentDir = File(".")
            val totalSpace: Long = currentDir.totalSpace
            val freeSpace: Long = currentDir.freeSpace
            val threshold: Long = 10 * 1024 * 1024L // 10MB

            val status = if (freeSpace > threshold) "UP" else "DOWN"

            mapOf(
                "status" to status,
                "details" to mapOf(
                    "total" to totalSpace,
                    "free" to freeSpace,
                    "threshold" to threshold,
                    "path" to currentDir.absolutePath,
                    "exists" to currentDir.exists()
                )
            )
        } catch (e: Exception) {
            mapOf(
                "status" to "DOWN",
                "error" to (e.message ?: "disk check failed")
            )
        }
    }
}