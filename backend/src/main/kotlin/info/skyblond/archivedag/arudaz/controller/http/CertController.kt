package info.skyblond.archivedag.arudaz.controller.http

import info.skyblond.archivedag.arstue.CertService
import info.skyblond.archivedag.arstue.model.CertDetailModel
import info.skyblond.archivedag.arstue.model.CertSigningResult
import info.skyblond.archivedag.arstue.utils.writeSignCertToString
import info.skyblond.archivedag.arstue.utils.writeSignKeyToString
import info.skyblond.archivedag.arudaz.model.controller.CertChangeStatusRequest
import info.skyblond.archivedag.arudaz.utils.checkCurrentUserIsAdmin
import info.skyblond.archivedag.arudaz.utils.getCurrentUsername
import info.skyblond.archivedag.arudaz.utils.requireSortPropertiesInRange
import info.skyblond.archivedag.commons.PermissionDeniedException
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.sql.Timestamp

@RestController
@RequestMapping("/cert")
class CertController(
    private val certService: CertService
) {

    private val minTimestamp = Timestamp(0)
    private val maxTimestamp = Timestamp.valueOf("9999-12-31 23:59:59.999999999")

    @GetMapping("/signNewCert")
    @PreAuthorize("hasRole('USER')")
    fun signNewCert(): Map<String, String> {
        val username = getCurrentUsername()
        val signingResult: CertSigningResult = try {
            certService.signCert(username)
        } catch (t: Throwable) {
            throw RuntimeException("Failed to sign new certification", t)
        }
        val result: MutableMap<String, String> = HashMap()
        result["cert"] = writeSignCertToString(signingResult)
        result["private_key"] = writeSignKeyToString(signingResult)
        return result
    }

    @GetMapping("/listCertSerialNumber")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun listCertSerialNumber(
        @RequestParam(name = "owner", required = false) owner: String?,
        @RequestParam(name = "issue_start", required = false) issueStart: Timestamp?,
        @RequestParam(name = "issue_end", required = false) issueEnd: Timestamp?,
        @RequestParam(name = "expire_start", required = false) expireStart: Timestamp?,
        @RequestParam(name = "expire_end", required = false) expireEnd: Timestamp?,
        pageable: Pageable?
    ): List<String> {
        requireSortPropertiesInRange(
            pageable!!,
            listOf("username", "serialNumber", "issuedTime", "expiredTime", "status")
        )
        val username = owner ?: getCurrentUsername()
        // check permission
        if (!checkCurrentUserIsAdmin() && getCurrentUsername() != username) {
            // if is not admin, and owner not match
            throw PermissionDeniedException("You can only list your own certifications")
        }
        return if (checkCurrentUserIsAdmin()) {
            certService.listCertSerialNumber(
                true, username,
                issueStart ?: minTimestamp,
                issueEnd ?: maxTimestamp,
                expireStart ?: minTimestamp,
                expireEnd ?: maxTimestamp, pageable
            )
        } else {
            // if is user
            certService.listCertSerialNumber(
                false, username,
                issueStart ?: minTimestamp,
                issueEnd ?: maxTimestamp,
                expireStart ?: minTimestamp,
                expireEnd ?: maxTimestamp, pageable
            )
        }
    }

    @GetMapping("/queryCert")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun queryCert(
        @RequestParam(name = "serial_number") serialNumber: String
    ): CertDetailModel {
        // check permission
        if (!checkCurrentUserIsAdmin()
            && !certService.userOwnCert(getCurrentUsername(), serialNumber)
        ) {
            // if is not admin, and not own this cert
            throw PermissionDeniedException("You can only query your own certifications")
        }
        return certService.queryCert(serialNumber)
    }

    @PostMapping("/changeCertStatus")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun changeCertStatus(
        @RequestBody request: CertChangeStatusRequest
    ): ResponseEntity<*> {
        // check permission
        if (!checkCurrentUserIsAdmin()
            && !certService.userOwnCert(getCurrentUsername(), request.serialNumber)
        ) {
            // if is not admin, and not own this cert
            throw PermissionDeniedException("You can only change your own certifications")
        }
        certService.changeCertStatus(request.serialNumber, request.newStatus)
        // return 204
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body<Any>(null)
    }

    @DeleteMapping("/deleteCert")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun deleteCert(
        @RequestParam("serial_number") serialNumber: String
    ): ResponseEntity<*> {
        // check permission
        if (!checkCurrentUserIsAdmin()
            && !certService.userOwnCert(getCurrentUsername(), serialNumber)
        ) {
            // if is not admin, and not own this cert
            throw PermissionDeniedException("You can only delete your own certifications")
        }
        certService.deleteCert(serialNumber)
        // return 204
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body<Any>(null)
    }
}
