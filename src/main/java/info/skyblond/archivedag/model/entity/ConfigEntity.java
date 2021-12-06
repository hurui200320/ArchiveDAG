package info.skyblond.archivedag.model.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "config_table")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class ConfigEntity implements Serializable {
    @Id
    @Column(name = "key", updatable = false)
    String key;

    @Column(name = "value")
    String value;

    public ConfigEntity(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
