package codel.config

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.test.context.TestContextAnnotationUtils
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional

class DataCleanerExtension :
    BeforeEachCallback,
    Loggable {
    override fun beforeEach(extensionContext: ExtensionContext) {
        val applicationContext = SpringExtension.getApplicationContext(extensionContext)

        validateTransactionalAnnotationExists(extensionContext)
        cleanDatabase(applicationContext)
    }

    private fun validateTransactionalAnnotationExists(extensionContext: ExtensionContext) {
        if (TestContextAnnotationUtils.hasAnnotation(extensionContext.requiredTestClass, Transactional::class.java) ||
            TestContextAnnotationUtils.hasAnnotation(extensionContext.requiredTestClass, jakarta.transaction.Transactional::class.java)
        ) {
            Assertions.fail<Unit>("테스트 클래스에 @Transactional 또는 @jakarta.transaction.Transactional 어노테이션이 존재합니다.")
        }

        if (AnnotatedElementUtils.hasAnnotation(extensionContext.requiredTestMethod, Transactional::class.java) ||
            AnnotatedElementUtils.hasAnnotation(extensionContext.requiredTestMethod, jakarta.transaction.Transactional::class.java)
        ) {
            Assertions.fail<Unit>("테스트 메서드에 @Transactional 또는 @jakarta.transaction.Transactional 어노테이션이 존재합니다.")
        }
    }

    private fun cleanDatabase(applicationContext: ApplicationContext) {
        try {
            DatabaseCleaner.clear(applicationContext)
        } catch (e: NoSuchBeanDefinitionException) {
            log.debug { "Database Cleaning not supported." }
        }
    }
}
