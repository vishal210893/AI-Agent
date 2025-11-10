package org.ai.agent.google.configuration;

import com.google.adk.agents.LlmAgent;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.tools.AgentTool;
import com.google.adk.tools.GoogleSearchTool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Configuration
public class AgentConfig {

    // Base assistant properties
    @Value("${agent.model}")
    private String model;

    @Value("${agent.name}")
    private String agentName;

    @Value("${agent.description}")
    private String description;

    @Value("${agent.instruction}")
    private String instruction;

    // Research agent properties
    @Value("${agent.research.name}")
    private String researchAgentName;

    @Value("${agent.research.instruction}")
    private String researchInstruction;

    @Value("${agent.research.output-key}")
    private String researchOutputKey;

    // Summarizer agent properties
    @Value("${agent.summarizer.name}")
    private String summarizerAgentName;

    @Value("${agent.summarizer.instruction}")
    private String summarizerInstruction;

    @Value("${agent.summarizer.output-key}")
    private String summarizerOutputKey;

    // Coordinator properties
    @Value("${agent.coordinator.name}")
    private String coordinatorAgentName;

    @Value("${agent.coordinator.instruction}")
    private String coordinatorInstruction;

    @Bean
    public GoogleSearchTool googleSearchTool() { return new GoogleSearchTool(); }

    @Bean(name = "helpfulAssistantAgent")
    public LlmAgent helpfulAssistantAgent(GoogleSearchTool googleSearchTool) {
        ensureApiKey();
        return new LlmAgent.Builder()
                .name(agentName)
                .model(model)
                .description(description)
                .instruction(instruction)
                .tools(List.of(googleSearchTool))
                .build();
    }

    @Bean(name = "researchAgent")
    public LlmAgent researchAgent(GoogleSearchTool googleSearchTool) {
        return new LlmAgent.Builder()
                .name(researchAgentName)
                .model(model)
                .instruction(researchInstruction)
                .tools(List.of(googleSearchTool))
                .outputKey(researchOutputKey)
                .build();
    }

    @Bean(name = "summarizerAgent")
    public LlmAgent summarizerAgent() {
        return new LlmAgent.Builder()
                .name(summarizerAgentName)
                .model(model)
                .instruction(summarizerInstruction)
                .outputKey(summarizerOutputKey)
                .build();
    }

    @Bean(name = "researchCoordinatorAgent")
    public LlmAgent researchCoordinatorAgent(LlmAgent researchAgent, LlmAgent summarizerAgent) {
        return new LlmAgent.Builder()
                .name(coordinatorAgentName)
                .model(model)
                .instruction(coordinatorInstruction)
                .tools(List.of(
                        AgentTool.create(researchAgent),
                        AgentTool.create(summarizerAgent)
                ))
                .build();
    }

    @Bean
    @Qualifier("assistantRunner")
    @Primary
    public InMemoryRunner assistantRunner(LlmAgent helpfulAssistantAgent) {
        return new InMemoryRunner(helpfulAssistantAgent);
    }

    @Bean
    @Qualifier("researchRunner")
    public InMemoryRunner researchRunner(LlmAgent researchCoordinatorAgent) {
        return new InMemoryRunner(researchCoordinatorAgent);
    }

    private void ensureApiKey() {
        String envKey = System.getenv("GOOGLE_API_KEY");
        if (envKey == null || envKey.isBlank()) {
            throw new IllegalStateException("Missing API key: set property 'google.api.key' or environment variable 'GOOGLE_API_KEY'.");
        }
    }
}