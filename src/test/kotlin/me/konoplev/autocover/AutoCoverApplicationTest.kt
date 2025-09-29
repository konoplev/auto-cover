package me.konoplev.autocover

import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

@SpringJUnitConfig
@TestPropertySource(
    properties = [
        "model.api.key=test-api-key",
    ],
)
class AutoCoverApplicationTest {

    @Test
    fun contextLoads() {
        // This test will pass if the Spring context loads successfully
    }
}
