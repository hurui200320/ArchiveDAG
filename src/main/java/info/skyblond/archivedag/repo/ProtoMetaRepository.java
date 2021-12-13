package info.skyblond.archivedag.repo;

import info.skyblond.archivedag.model.entity.ProtoMetaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ProtoMetaRepository extends JpaRepository<ProtoMetaEntity, String> {
    ProtoMetaEntity findByPrimaryHash(String primaryHash);

    boolean existsByPrimaryHash(String primaryHash);

    boolean existsByPrimaryHashAndSecondaryHash(String primaryHash, String secondaryHash);

    @Modifying
    @Query("update ProtoMetaEntity m set m.mediaType = ?2 where m.primaryHash = ?1")
    void updateMediaType(String primaryHash, String mediaType);

    @Modifying
    void deleteByPrimaryHash(String primaryHash);
}
