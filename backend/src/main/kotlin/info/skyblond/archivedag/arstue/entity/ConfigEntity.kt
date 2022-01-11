package info.skyblond.archivedag.arstue.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "config_table")
class ConfigEntity(
    @Id
    @Column(name = "key", updatable = false)
    val key: String,

    @Column(name = "value")
    val value: String?
)
