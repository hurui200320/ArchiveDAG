package info.skyblond.archivedag.arudaz.model

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import info.skyblond.archivedag.arstue.entity.CertEntity

data class CertChangeStatusRequest(
    @JsonProperty("serial_number")
    @JsonAlias("serialNumber")
    val serialNumber: String,
    @JsonProperty("new_status")
    @JsonAlias("newStatus")
    val newStatus: CertEntity.Status
)
