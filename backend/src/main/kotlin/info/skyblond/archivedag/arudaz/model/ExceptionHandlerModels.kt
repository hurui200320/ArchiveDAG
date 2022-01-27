package info.skyblond.archivedag.arudaz.model

import com.fasterxml.jackson.annotation.JsonProperty
import info.skyblond.archivedag.commons.getUnixTimestamp
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import javax.servlet.http.HttpServletRequest

data class ExceptionResponse(
    @JsonProperty("timestamp")
    val timestamp: Long,
    @JsonProperty("exception")
    val exception: String,
    @JsonProperty("status")
    val status: Int,
    @JsonProperty("error")
    val error: String,
    @JsonProperty("path")
    val path: String,
    @JsonProperty("message")
    val message: String,
) {
    companion object {
        @JvmStatic
        fun generateResp(
            status: HttpStatus,
            request: HttpServletRequest,
            t: Throwable
        ): ResponseEntity<ExceptionResponse> {
            val resp = ExceptionResponse(
                timestamp = getUnixTimestamp(),
                exception = t::class.java.canonicalName,
                status = status.value(),
                error = status.reasonPhrase,
                path = request.requestURI,
                message = t.message ?: ""
            )
            return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(resp)
        }
    }
}
