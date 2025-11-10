package org.ai.agent.google.configuration;

import com.google.adk.agents.LlmAgent;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.tools.GoogleSearchTool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AgentConfig {

    @Bean
    public LlmAgent rootAgent() {
        // Ensure GOOGLE_API_KEY is set as an environment variable
        if (System.getenv("GOOGLE_API_KEY") == null) {
            throw new RuntimeException("Error: GOOGLE_API_KEY environment variable is not set.");
        }

        return new LlmAgent.Builder()
                .name("helpful_assistant")
                .model("gemini-2.5-flash-lite")
                .description("A simple agent that can answer general questions.")
                .instruction("You are a helpful assistant. Use Google Search for current info or if unsure.")
                .tools(List.of(new GoogleSearchTool()))
                .build();
    }

    @Bean
    public InMemoryRunner inMemoryRunner(LlmAgent agent) {
        // Create the runner using the agent bean
        return new InMemoryRunner(agent);
    }
}