package me.konoplev.autocover.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(
    ModelProperties::class,
    AgentProperties::class,
)
class AutoCoverConfiguration
