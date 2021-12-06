package info.skyblond.archivedag.controller;

import info.skyblond.archivedag.service.impl.ConfigService;
import info.skyblond.archivedag.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/maintain")
public class MaintainController {
    private final ConfigService configService;
    private final PlatformTransactionManager transactionManager;

    public MaintainController(ConfigService configService, PlatformTransactionManager transactionManager) {
        this.configService = configService;
        this.transactionManager = transactionManager;
    }

    @GetMapping("/listConfig")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> listConfig(
            @RequestParam(name = "prefix", defaultValue = "") String prefix,
            Pageable pageable
    ) {
        SecurityUtils.requireSortPropertiesInRange(pageable, List.of("key", "value"));
        return this.configService.listConfig(prefix, pageable);
    }

    @PostMapping("/updateConfig")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateConfig(
            @RequestBody Map<String, Object> newConfig
    ) {
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            newConfig.forEach((k, v) -> {
                String str = null;
                if (v != null) {
                    str = v.toString();
                }
                this.configService.updateConfig(k, str);
            });
            transactionManager.commit(status);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
        } catch (Throwable t) {
            transactionManager.rollback(status);
            throw t;
        }
    }
}
