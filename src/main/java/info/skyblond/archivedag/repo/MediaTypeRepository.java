package info.skyblond.archivedag.repo;

import info.skyblond.archivedag.model.entity.MediaTypeEntity;
import info.skyblond.archivedag.model.entity.MetaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaTypeRepository extends JpaRepository<MediaTypeEntity, String> {
}
