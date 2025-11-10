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
    @Value("${agent.model:gemini-2.5-flash-lite}")
    private String model;
    @Value("${agent.name:helpful_assistant}")
    private String agentName;
    @Value("${agent.description:A simple agent that can answer general questions.}")
    private String description;
    @Value("${agent.instruction:You are a helpful assistant. Use Google Search for current info or if unsure.}")
    private String instruction;

    // API key (optional property; fallback to env)
    @Value("${google.api.key:}")
    private String googleApiKey;

    // Research agent properties
    @Value("${agent.research.name:ResearchAgent}")
    private String researchAgentName;
    @Value("${agent.research.instruction:You are a specialized research agent. Your only job is to use the google_search tool to find 2-3 pieces of relevant information on the given topic and present the findings with citations.}")
    private String researchInstruction;
    @Value("${agent.research.output-key:research_findings}")
    private String researchOutputKey;

    // Summarizer agent properties
    @Value("${agent.summarizer.name:SummarizerAgent}")
    private String summarizerAgentName;
    @Value("${agent.summarizer.instruction:Read the provided research findings: {research_findings} Create a concise summary as a bulleted list with 3-5 key points.}")
    private String summarizerInstruction;
    @Value("${agent.summarizer.output-key:final_summary}")
    private String summarizerOutputKey;

    // Coordinator properties
    @Value("${agent.coordinator.name:ResearchCoordinator}")
    private String coordinatorAgentName;
    @Value("${agent.coordinator.instruction:You are a research coordinator. Your goal is to answer the user's query by orchestrating a workflow. 1. First, you MUST call the `ResearchAgent` tool to find relevant information on the topic provided by the user. 2. Next, after receiving the research findings, you MUST call the `SummarizerAgent` tool to create a concise summary. 3. Finally, present the final summary clearly to the user as your response.}")
    private String coordinatorInstruction;

    @Bean
    public GoogleSearchTool googleSearchTool() {
        return new GoogleSearchTool();
    }

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
        if ((googleApiKey == null || googleApiKey.isBlank()) && (envKey == null || envKey.isBlank())) {
            throw new IllegalStateException("Missing API key: set property 'google.api.key' or environment variable 'GOOGLE_API_KEY'.");
        }
    }
}