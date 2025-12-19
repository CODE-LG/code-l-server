package codel.member.domain

import org.springframework.web.multipart.MultipartFile

interface ImageUploader {
    fun uploadFile(file: MultipartFile): String
}
