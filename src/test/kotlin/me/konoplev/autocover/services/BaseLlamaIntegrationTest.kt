package me.konoplev.autocover.services

import me.konoplev.autocover.services.BaseLlamaIntegrationTest.Companion.MODEL_NAME
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Duration


@SpringBootTest
@Testcontainers
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
        "model.provider=OLLAMA",
        "model.name=$MODEL_NAME",
        "model.temperature=0.1",
        "model.timeout.seconds=300",
    ],
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseLlamaIntegrationTest {

    companion object {
        const val MODEL_NAME = "llama3.2:latest"

        @Container
        @JvmStatic
        private val ollamaContainer = GenericContainer("ollama/ollama:latest")
            .withExposedPorts(11434)
            .withFileSystemBind("/tmp/ollama", "/root/.ollama", BindMode.READ_WRITE)
            .withCommand("serve")
            .waitingFor(
                HttpWaitStrategy()
                    .forPort(11434)
                    .forPath("/")
                    .withStartupTimeout(Duration.ofMinutes(5))
            )

        @DynamicPropertySource
        @JvmStatic
        fun runContainerAndSetProperties(registry: DynamicPropertyRegistry) {
            ollamaContainer.start()
            val ollamaBaseUrl = "http://localhost:${ollamaContainer.getMappedPort(11434)}"
            registry.add("model.ollama.base-url") { ollamaBaseUrl }

            // pull the model
            val result = ollamaContainer.execInContainer("ollama", "pull", MODEL_NAME)
            if (result.exitCode != 0) {
                throw RuntimeException("Failed to pull model: ${result.stderr}")
            }

            // Wait for the model to be available using Testcontainers wait strategy
            Wait.forHttp("/api/tags")
                .forPort(11434)
                .withStartupTimeout(Duration.ofMinutes(2))
                .waitUntilReady(ollamaContainer)

        }
    }
}
