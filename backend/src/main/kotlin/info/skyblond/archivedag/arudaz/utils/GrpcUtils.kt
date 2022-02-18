package info.skyblond.archivedag.arudaz.utils

import info.skyblond.archivedag.arudaz.protos.common.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

fun parsePagination(p: Page): Pageable = PageRequest.of(p.page, p.size)
