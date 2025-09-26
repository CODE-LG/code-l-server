package codel

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class CodelApplication

fun main(args: Array<String>) {
    runApplication<CodelApplication>(*args)
}
