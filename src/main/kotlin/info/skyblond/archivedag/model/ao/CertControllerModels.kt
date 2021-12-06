package info.skyblond.archivedag.model.ao

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import info.skyblond.archivedag.model.entity.CertEntity

data class CertChangeStatusRequest(
    @JsonProperty("serial_number")
    @JsonAlias("serialNumber")
    val serialNumber: String,
    @JsonProperty("new_status")
    @JsonAlias("newStatus")
    val newStatus: CertEntity.Status
)
