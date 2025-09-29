package me.konoplev.autocover

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AutoCoverApplication

fun main(args: Array<String>) {
    runApplication<AutoCoverApplication>(*args)
}
