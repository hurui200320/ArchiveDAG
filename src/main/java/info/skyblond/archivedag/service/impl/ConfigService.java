package info.skyblond.archivedag.service.impl;

import info.skyblond.archivedag.model.entity.ConfigEntity;
import info.skyblond.archivedag.repo.ConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ConfigService {
    private final ConfigRepository configRepository;

    public ConfigService(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    public Map<String, String> listConfig(String keyPrefix, Pageable pageable) {
        Map<String, String> result = new HashMap<>();
        this.configRepository.findAllByKeyStartingWith(keyPrefix.toLowerCase(), pageable)
                .forEach(r -> result.put(r.getKey(), r.getValue()));
        return Collections.unmodifiableMap(result);
    }

    @Transactional
    public void updateConfig(String key, String value) {
        ConfigEntity entity = new ConfigEntity(key.toLowerCase(), value);
        this.configRepository.save(entity);
    }

    public static final String ALLOW_GRPC_WRITE_KEY = "archive-dag.grpc.allow-write";

    public boolean allowGrpcWrite() {
        ConfigEntity entity = this.configRepository.findByKey(ALLOW_GRPC_WRITE_KEY);
        if (entity == null) {
            return false;
        }
        return Boolean.parseBoolean(entity.getValue());
    }
}
