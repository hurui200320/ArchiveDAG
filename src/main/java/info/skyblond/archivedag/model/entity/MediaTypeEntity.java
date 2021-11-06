package info.skyblond.archivedag.model.entity;

import lombok.*;
import org.hibernate.Hibernate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

@Builder
@Entity
@Table(name = "proto_media_type")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class MediaTypeEntity {
    @Id
    @Column(name = "primary_hash", updatable = false)
    String primaryMultihashBase58;
    @Column(name = "media_type", updatable = false, nullable = false)
    String mediaType;

    public MediaTypeEntity(
            String primaryMultihashBase58,
            String mediaType
    ) {
        this.primaryMultihashBase58 = primaryMultihashBase58;
        this.mediaType = mediaType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        MediaTypeEntity metaEntity = (MediaTypeEntity) o;
        return this.primaryMultihashBase58 != null && Objects.equals(this.primaryMultihashBase58, metaEntity.primaryMultihashBase58);
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
