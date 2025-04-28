package codel.config

import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate

object DatabaseCleaner {
    fun clear(applicationContext: ApplicationContext) {
        val entityManager = applicationContext.getBean(EntityManager::class.java)
        val jdbcTemplate = applicationContext.getBean(JdbcTemplate::class.java)
        val transactionTemplate = applicationContext.getBean(TransactionTemplate::class.java)

        transactionTemplate.execute {
            entityManager.clear()
            deleteAll(jdbcTemplate, entityManager)
            null
        }
    }

    private fun deleteAll(
        jdbcTemplate: JdbcTemplate,
        entityManager: EntityManager,
    ) {
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate()

        findDatabaseTableNames(jdbcTemplate).forEach { tableName ->
            entityManager.createNativeQuery("DELETE FROM $tableName").executeUpdate()
            entityManager.createNativeQuery("ALTER TABLE $tableName ALTER COLUMN id RESTART WITH 1").executeUpdate()
        }

        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate()
    }

    private fun findDatabaseTableNames(jdbcTemplate: JdbcTemplate): List<String> =
        jdbcTemplate.query("SHOW TABLES") { rs, _ -> rs.getString(1) }
}
