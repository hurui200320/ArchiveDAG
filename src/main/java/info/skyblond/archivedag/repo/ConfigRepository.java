package info.skyblond.archivedag.repo;

import info.skyblond.archivedag.model.entity.ConfigEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigRepository extends JpaRepository<ConfigEntity, String> {

    Page<ConfigEntity> findAllByKeyStartingWith(String keyPrefix, Pageable pageable);

    ConfigEntity findByKey(String key);
}
