package org.ai.agent.google.configuration;

import com.google.adk.agents.LlmAgent;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.tools.GoogleSearchTool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AgentConfig {

    // Override in `application.properties` (e.g. agent.model=gemini-2.5-flash)
    @Value("${agent.model:gemini-2.5-flash-lite}")
    private String model;

    @Value("${agent.name:helpful_assistant}")
    private String agentName;

    @Value("${agent.description:A simple agent that can answer general questions.}")
    private String description;

    @Value("${agent.instruction:You are a helpful assistant. Use Google Search for current info or if unsure.}")
    private String instruction;

    @Bean
    public GoogleSearchTool googleSearchTool() {
        return new GoogleSearchTool();
    }

    /**
     * Root LLM agent definition. Validates API key before building.
     */
    @Bean
    public LlmAgent rootAgent(GoogleSearchTool googleSearchTool) {
        ensureApiKey();
        return new LlmAgent.Builder()
                .name(agentName)
                .model(model)
                .description(description)
                .instruction(instruction)
                .tools(List.of(googleSearchTool))
                .build();
    }

    /**
     * Runner that hosts the agent in memory.
     */
    @Bean
    public InMemoryRunner inMemoryRunner(LlmAgent rootAgent) {
        return new InMemoryRunner(rootAgent);
    }

    /**
     * Fail fast if required environment variable is absent.
     */
    private void ensureApiKey() {
        String apiKey = System.getenv("GOOGLE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing environment variable: GOOGLE_API_KEY");
        }
    }
}