package info.skyblond.archivedag.arudaz.controller.http

import info.skyblond.archivedag.arstue.ConfigService
import info.skyblond.archivedag.arudaz.utils.requireSortPropertiesInRange
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/maintain")
class MaintainController(
    private val configService: ConfigService,
    private val transactionManager: PlatformTransactionManager
) {
    @GetMapping("/listConfig")
    @PreAuthorize("hasRole('ADMIN')")
    fun listConfig(
        @RequestParam(name = "prefix", defaultValue = "") prefix: String,
        pageable: Pageable
    ): Map<String, String?> {
        requireSortPropertiesInRange(pageable, listOf("key", "value"))
        return configService.listConfig(prefix, pageable)
    }

    @PostMapping("/updateConfig")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateConfig(
        @RequestBody newConfig: Map<String, Any?>
    ): ResponseEntity<*> {
        val status = transactionManager.getTransaction(DefaultTransactionDefinition())
        return try {
            newConfig.forEach { (k: String, v: Any?) ->
                configService.updateConfig(k, v?.toString())
            }
            transactionManager.commit(status)
            ResponseEntity.status(HttpStatus.NO_CONTENT).body<Any>(null)
        } catch (t: Throwable) {
            transactionManager.rollback(status)
            throw t
        }
    }
}
