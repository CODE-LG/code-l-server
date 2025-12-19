package codel.member.presentation.request

import org.springframework.web.multipart.MultipartFile

data class CodeImageSavedRequest(
    val imageFiles: List<MultipartFile>,
)
