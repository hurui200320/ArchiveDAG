package info.skyblond.archivedag.repo;

import info.skyblond.archivedag.model.entity.MetaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetaRepository extends JpaRepository<MetaEntity, String> {
}
