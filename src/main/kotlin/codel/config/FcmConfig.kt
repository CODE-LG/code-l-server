package codel.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource

@Configuration
@Profile("!test")
class FcmConfig {
    @PostConstruct
    fun initialize() {
        try {
            val options =
                FirebaseOptions
                    .builder()
                    .setCredentials(
                        GoogleCredentials.fromStream(ClassPathResource("code-l-b109b-firebase-adminsdk-fbsvc-8c4eb2e6f2.json").inputStream),
                    ).build()

            FirebaseApp.initializeApp(options)
        } catch (e: Exception) {
            throw IllegalArgumentException("fcm 연결에 실패하였습니다.")
        }
    }
}
