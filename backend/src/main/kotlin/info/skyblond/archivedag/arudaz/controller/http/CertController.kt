package info.skyblond.archivedag.arudaz.controller.http

import info.skyblond.archivedag.arstue.CertService
import info.skyblond.archivedag.arstue.model.CertDetailModel
import info.skyblond.archivedag.arstue.model.CertSigningResult
import info.skyblond.archivedag.arudaz.model.CertChangeStatusRequest
import info.skyblond.archivedag.arudaz.utils.checkCurrentUserIsAdmin
import info.skyblond.archivedag.arudaz.utils.getCurrentUsername
import info.skyblond.archivedag.arudaz.utils.requireSortPropertiesInRange
import info.skyblond.archivedag.commons.PermissionDeniedException
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.util.io.pem.PemObject
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.io.ByteArrayOutputStream

@RestController
@RequestMapping("/cert")
class CertController(
    private val certService: CertService
) {
    private fun writeSignCertToString(result: CertSigningResult): String {
        val outputStream = ByteArrayOutputStream()
        val pemWriter = JcaPEMWriter(outputStream.writer(Charsets.UTF_8))
        pemWriter.writeObject(result.certificate)
        pemWriter.close()
        return String(outputStream.toByteArray(), Charsets.UTF_8)
    }

    private fun writeSignKeyToString(result: CertSigningResult): String {
        val outputStream = ByteArrayOutputStream()
        val pemWriter = JcaPEMWriter(outputStream.writer(Charsets.UTF_8))
        pemWriter.writeObject(PemObject("PRIVATE KEY", result.privateKey.encoded))
        pemWriter.close()
        return String(outputStream.toByteArray(), Charsets.UTF_8)
    }

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
        result["serial_number"] = signingResult.serialNumber
        result["cert"] = writeSignCertToString(signingResult)
        result["private_key"] = writeSignKeyToString(signingResult)
        return result
    }

    @GetMapping("/listCertSerialNumber")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun listCertSerialNumber(
        @RequestParam(name = "owner", required = false) owner: String?,
        pageable: Pageable?
    ): List<String> {
        requireSortPropertiesInRange(
            pageable!!,
            listOf("username", "serialNumber", "issuedTime", "expiredTime", "status")
        )

        return if (owner.isNullOrBlank()) {
            // owner is empty, return current user's cert
            certService.listCertSerialNumber(
                blurOwner = false,
                owner = getCurrentUsername(),
                pageable = pageable
            )
        } else {
            if (!checkCurrentUserIsAdmin()) {
                // if is not admin, and owner not match
                throw PermissionDeniedException("You can only list your own certifications")
            }
            certService.listCertSerialNumber(
                blurOwner = true,
                owner = owner,
                pageable = pageable
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
