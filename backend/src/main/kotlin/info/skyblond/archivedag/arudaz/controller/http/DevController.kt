package info.skyblond.archivedag.arudaz.controller.http

import info.skyblond.archivedag.arstue.FileRecordService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/public/dev")
class DevController(
    private val fileRecordService: FileRecordService
) {
    @GetMapping
    fun dev(
        @RequestParam(name = "name") name: String,
        @RequestParam(name = "multihash") multihash: String,
        @RequestParam(name = "owner") owner: String,
    ): String {
        return fileRecordService.createRecord(name, owner).toString()
    }
}
