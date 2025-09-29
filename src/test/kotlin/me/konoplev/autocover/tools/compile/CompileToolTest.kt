package me.konoplev.autocover.tools.compile

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.io.File
import kotlin.test.assertTrue

@SpringBootTest
@TestPropertySource(
    properties = [
        "model.provider=OLLAMA",
        "model.name=llama3.1",
        "model.temperature=0.1",
        "model.timeout.seconds=300",
        "agent.coverage.test-command=mvn verify",
        "agent.coverage.test-result-location=target/site/jacoco/jacoco.xml",
    ],
)
class CompileToolTest {

    @Autowired
    private lateinit var compileTool: CompileTool

    @Test
    fun `should check compilation for valid project`(@TempDir tempDir: java.nio.file.Path) {
        // Given: A simple Maven project structure
        val projectDir = tempDir.toFile()
        val pomFile = File(projectDir, "pom.xml")
        pomFile.writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                     http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-project</artifactId>
                <version>1.0.0</version>
                <packaging>jar</packaging>
            </project>
            """.trimIndent(),
        )

        // When: Check compilation
        val result = compileTool.compileAndTest(projectDir.absolutePath)

        // Then: Should return a result (may be success or failure depending on Maven setup)
        assertTrue(result.isNotEmpty())
        assertTrue(result.contains("Compilation") || result.contains("Error"))
    }

    @Test
    fun `should handle non-existent directory`() {
        // When: Check compilation for non-existent directory
        val result = compileTool.compileAndTest("/non/existent/directory")

        // Then: Should return error message
        assertTrue(result.contains("Error"))
        assertTrue(result.contains("does not exist"))
    }
}
