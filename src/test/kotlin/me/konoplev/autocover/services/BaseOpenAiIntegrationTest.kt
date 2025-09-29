package me.konoplev.autocover.services

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@ComponentScan(
    basePackages = ["me.konoplev.autocover"],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [me.konoplev.autocover.cli.AutoCoverCLI::class],
        ),
    ],
)
@TestPropertySource(
    properties = [
        "model.provider=OPENAI",
        "model.name=gpt-4o-mini",
        "model.temperature=0.1",
        "model.timeout.seconds=300",
    ],
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseOpenAiIntegrationTest {

    protected val logger = LoggerFactory.getLogger(this::class.java)

    @BeforeAll
    fun setupModel() {
        // OpenAI model setup - no container needed
        logger.info("Using OpenAI gpt-4o-mini model for testing")
    }
}
