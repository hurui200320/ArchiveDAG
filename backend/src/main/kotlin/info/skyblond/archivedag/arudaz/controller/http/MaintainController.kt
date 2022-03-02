package info.skyblond.archivedag.arudaz.controller.http

import info.skyblond.archivedag.arudaz.service.ApplicationConfigService
import info.skyblond.archivedag.arudaz.utils.requireSortPropertiesInRange
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/maintain")
@PreAuthorize("hasRole('ADMIN')")
class MaintainController(
    private val applicationConfigService: ApplicationConfigService,
) {
    @GetMapping("/listConfig")
    fun listConfig(
        @RequestParam(name = "prefix", defaultValue = "") prefix: String,
        pageable: Pageable
    ): Map<String, String?> {
        requireSortPropertiesInRange(pageable, listOf())
        return applicationConfigService.listConfig(prefix, pageable)
    }

    @PostMapping("/updateConfig")
    fun updateConfig(
        @RequestBody newConfig: Map<String, Any?>
    ): ResponseEntity<*> {
        newConfig.forEach { (k: String, v: Any?) ->
            applicationConfigService.updateConfig(k, v?.toString())
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body<Any>(null)
    }

    // TODO start a process to do the clean up
}
