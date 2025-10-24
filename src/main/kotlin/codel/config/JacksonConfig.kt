package codel.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Jackson JSON 직렬화/역직렬화 설정
 * 
 * ## 날짜/시간 직렬화 정책
 * - **모든 LocalDateTime 필드는 KST(Asia/Seoul, +09:00) 타임존이 명시된 ISO-8601 형식으로 직렬화됩니다**
 * - 형식: `yyyy-MM-dd'T'HH:mm:ss.SSSXXX`
 * - 예시: `"2025-01-15T14:30:00.000+09:00"`
 * 
 * ## 적용 범위
 * - BaseTimeEntity를 상속하는 모든 엔티티의 `createdAt`, `updatedAt`
 * - Chat 엔티티의 `sentAt`
 * - 기타 모든 Response DTO의 LocalDateTime 필드
 * 
 * ## 클라이언트 측 처리
 * - iOS: `ISO8601DateFormatter` 사용 (iOS 10+)
 * - Android: `ZonedDateTime.parse()` 또는 `OffsetDateTime.parse()` 사용
 * - 대부분의 JSON 파서는 ISO-8601 타임존 형식을 자동 인식합니다
 * 
 * @see LocalDateTimeToKstSerializer
 */
@Configuration
class JacksonConfig {

    @Bean
    fun objectMapper(builder: Jackson2ObjectMapperBuilder): ObjectMapper {
        val mapper = builder.build<ObjectMapper>()
        
        val module = SimpleModule().apply {
            addSerializer(LocalDateTime::class.java, LocalDateTimeToKstSerializer())
        }
        
        mapper.registerModule(module)
        mapper.registerModule(JavaTimeModule())
        
        return mapper
    }

    /**
     * LocalDateTime을 KST(Asia/Seoul) 타임존이 포함된 ISO-8601 형식으로 직렬화
     * 
     * ## 변환 과정
     * 1. LocalDateTime (타임존 정보 없음)
     * 2. → ZonedDateTime (Asia/Seoul)
     * 3. → ISO-8601 문자열 (타임존 포함)
     * 
     * ## 출력 예시
     * ```
     * LocalDateTime.of(2025, 1, 15, 14, 30, 0)
     * → "2025-01-15T14:30:00.000+09:00"
     * ```
     * 
     * ## 참고
     * - DB 저장: LocalDateTime으로 KST 기준 저장 (`jdbc.time_zone=Asia/Seoul`)
     * - JSON 응답: 타임존 정보 포함하여 전송 (이 Serializer가 처리)
     * - 클라이언트: 타임존 정보를 보고 정확한 시간 파싱 가능
     */
    class LocalDateTimeToKstSerializer : JsonSerializer<LocalDateTime>() {
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        private val kstZoneId = ZoneId.of("Asia/Seoul")

        override fun serialize(
            value: LocalDateTime?,
            gen: JsonGenerator,
            serializers: SerializerProvider
        ) {
            if (value != null) {
                val zonedDateTime = value.atZone(kstZoneId)
                val formatted = zonedDateTime.format(formatter)
                gen.writeString(formatted)
            }
        }
    }
}
