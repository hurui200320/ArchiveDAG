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
@Table(name = "proto_meta")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class MetaEntity {
    @Id
    @Column(name = "primary_hash", updatable = false)
    String primaryMultihashBase58;
    @Column(name = "secondary_hash", updatable = false, nullable = false)
    String secondaryMultihashBase58;
    @Column(name = "obj_type", updatable = false, nullable = false)
    String objType;

    public MetaEntity(
            String primaryMultihashBase58,
            String secondaryMultihashBase58,
            String objType
    ) {
        this.primaryMultihashBase58 = primaryMultihashBase58;
        this.secondaryMultihashBase58 = secondaryMultihashBase58;
        this.objType = objType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        MetaEntity metaEntity = (MetaEntity) o;
        return this.primaryMultihashBase58 != null && Objects.equals(this.primaryMultihashBase58, metaEntity.primaryMultihashBase58);
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
