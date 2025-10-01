package me.konoplev.autocover.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import dev.langchain4j.service.AiServices
import me.konoplev.autocover.config.factory.LLMFactory
import me.konoplev.autocover.services.CoverageImprovementAssistant
import me.konoplev.autocover.services.ProjectConfigurationAssistant
import me.konoplev.autocover.tools.FileSystemTool
import me.konoplev.autocover.tools.Tool
import me.konoplev.autocover.tools.command.CommandExecutionTool
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AiServicesConfiguration {

    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper().registerModule(kotlinModule())
    }

    @Bean
    fun projectConfigurationAssistant(
        fileSystemTools: List<FileSystemTool>,
        commandExecutionTool: CommandExecutionTool,
        llmFactory: LLMFactory,
    ): ProjectConfigurationAssistant =
        AiServices.builder(ProjectConfigurationAssistant::class.java)
            .chatLanguageModel(llmFactory.createChatModel())
            .tools(fileSystemTools, commandExecutionTool)
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
