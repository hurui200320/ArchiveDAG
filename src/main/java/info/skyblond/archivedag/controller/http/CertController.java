package info.skyblond.archivedag.controller.http;

import info.skyblond.archivedag.model.PermissionDeniedException;
import info.skyblond.archivedag.model.ao.CertChangeStatusRequest;
import info.skyblond.archivedag.model.bo.CertDetailModel;
import info.skyblond.archivedag.model.bo.CertSigningResult;
import info.skyblond.archivedag.service.impl.CertService;
import info.skyblond.archivedag.util.Constants;
import info.skyblond.archivedag.util.GeneralKt;
import info.skyblond.archivedag.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/cert")
public class CertController {
    private final CertService certService;

    public CertController(CertService certService) {
        this.certService = certService;
    }

    @GetMapping("/signNewCert")
    @PreAuthorize("hasRole('USER')")
    public Map<String, String> signNewCert() {
        String username = SecurityUtils.getCurrentUsername();
        CertSigningResult signingResult;
        try {
            signingResult = this.certService.signCert(username);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to sign new certification", t);
        }

        Map<String, String> result = new HashMap<>();
        result.put("cert", GeneralKt.writeSignCertToString(signingResult));
        result.put("private_key", GeneralKt.writeSignKeyToString(signingResult));
        return result;
    }

    @GetMapping("/listCertSerialNumber")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<String> listCertSerialNumber(
            @RequestParam(name = "owner", required = false) String owner,
            @RequestParam(name = "issue_start", required = false) Timestamp issueStart,
            @RequestParam(name = "issue_end", required = false) Timestamp issueEnd,
            @RequestParam(name = "expire_start", required = false) Timestamp expireStart,
            @RequestParam(name = "expire_end", required = false) Timestamp expireEnd,
            Pageable pageable
    ) {
        SecurityUtils.requireSortPropertiesInRange(pageable, List.of("username", "serialNumber", "issuedTime", "expiredTime", "status"));
        if (owner == null) {
            owner = SecurityUtils.getCurrentUsername();
        }
        if (issueStart == null) {
            issueStart = Constants.MIN_TIMESTAMP;
        }
        if (expireStart == null) {
            expireStart = Constants.MIN_TIMESTAMP;
        }
        if (issueEnd == null) {
            issueEnd = Constants.MAX_TIMESTAMP;
        }
        if (expireEnd == null) {
            expireEnd = Constants.MAX_TIMESTAMP;
        }
        // check permission
        if (!SecurityUtils.checkCurrentUserIsAdmin()
                && !SecurityUtils.getCurrentUsername().equals(owner)) {
            // if is not admin, and owner not match
            throw new PermissionDeniedException("You can only list your own certifications");
        }

        if (SecurityUtils.checkCurrentUserIsAdmin()) {
            return this.certService.listCertSerialNumber(true, owner,
                    issueStart, issueEnd, expireStart, expireEnd, pageable);
        } else {
            // if is user
            return this.certService.listCertSerialNumber(false, owner,
                    issueStart, issueEnd, expireStart, expireEnd, pageable);
        }
    }

    @GetMapping("/queryCert")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public CertDetailModel queryCert(
            @RequestParam(name = "serial_number") String serialNumber
    ) {
        // check permission
        if (!SecurityUtils.checkCurrentUserIsAdmin()
                && !this.certService.userOwnCert(SecurityUtils.getCurrentUsername(), serialNumber)) {
            // if is not admin, and not own this cert
            throw new PermissionDeniedException("You can only query your own certifications");
        }
        return this.certService.queryCert(serialNumber);
    }

    @PostMapping("/changeCertStatus")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> changeCertStatus(
            @RequestBody CertChangeStatusRequest request
    ) {
        // check permission
        if (!SecurityUtils.checkCurrentUserIsAdmin()
                && !this.certService.userOwnCert(SecurityUtils.getCurrentUsername(),
                request.getSerialNumber())) {
            // if is not admin, and not own this cert
            throw new PermissionDeniedException("You can only change your own certifications");
        }
        this.certService.changeCertStatus(request.getSerialNumber(), request.getNewStatus());
        // return 204
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

    @DeleteMapping("/deleteCert")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> deleteCert(
            @RequestParam("serial_number") String serialNumber
    ) {
        // check permission
        if (!SecurityUtils.checkCurrentUserIsAdmin()
                && !this.certService.userOwnCert(SecurityUtils.getCurrentUsername(),
                serialNumber)) {
            // if is not admin, and not own this cert
            throw new PermissionDeniedException("You can only delete your own certifications");
        }
        this.certService.deleteCert(serialNumber);
        // return 204
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }
}
