package me.hanhyur.gatewell

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GatewellApplication

fun main(args: Array<String>) {
    runApplication<GatewellApplication>(*args)
}
