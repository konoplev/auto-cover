package me.konoplev.autocover.config

import dev.langchain4j.service.AiServices
import me.konoplev.autocover.config.factory.LLMFactory
import me.konoplev.autocover.services.CoverageImprovementAssistant
import me.konoplev.autocover.services.ProjectConfigurationAssistant
import me.konoplev.autocover.tools.FileSystemTool
import me.konoplev.autocover.tools.Tool
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AiServicesConfiguration {

    @Bean
    fun projectConfigurationAssistant(
        fileSystemTools: List<FileSystemTool>,
        llmFactory: LLMFactory,
    ): ProjectConfigurationAssistant =
        AiServices.builder(ProjectConfigurationAssistant::class.java)
            .chatLanguageModel(llmFactory.createChatModel())
            .tools(fileSystemTools)
            .build()

    @Bean
    fun coverageImprovementAssistant(
        tools: List<Tool>,
        llmFactory: LLMFactory,
    ): CoverageImprovementAssistant =
        AiServices.builder(CoverageImprovementAssistant::class.java)
            .chatLanguageModel(llmFactory.createChatModel())
            .tools(tools)
            .build()
}
