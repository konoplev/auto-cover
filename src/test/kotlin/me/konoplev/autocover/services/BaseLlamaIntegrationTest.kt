package me.konoplev.autocover.services

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.test.context.TestPropertySource
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
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
        "model.name=llama3.1",
        "model.temperature=0.1",
        "model.timeout.seconds=300",
    ],
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseLlamaIntegrationTest {

    protected val logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        const val MODEL_NAME = "llama3.1"

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
                    .withStartupTimeout(Duration.ofMinutes(5)),
            )
    }

    @BeforeAll
    fun setupModel() {
        try {
            // Start container and set Ollama base URL property
            ollamaContainer.start()
            val ollamaBaseUrl = "http://localhost:${ollamaContainer.getMappedPort(11434)}"
            System.setProperty("model.ollama.base-url", ollamaBaseUrl)

            val result = ollamaContainer.execInContainer("ollama", "pull", MODEL_NAME)
            if (result.exitCode != 0) {
                throw RuntimeException("Failed to pull model: ${result.stderr}")
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to pull Ollama model: ${e.message}", e)
        }
    }
}
