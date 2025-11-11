package org.ai.agent.google.configuration;

import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.ParallelAgent;
import com.google.adk.agents.SequentialAgent;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.tools.GoogleSearchTool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ParallelResearchConfig {

    @Bean(name = "techResearcher")
    public LlmAgent techResearcher(GoogleSearchTool searchTool) {
        return new LlmAgent.Builder()
                .name("TechResearcher")
                .model("gemini-2.5-flash-lite")
                .instruction("Research the latest AI/ML trends. Include 3 key developments, the main companies involved, and the potential impact. Keep the report very concise (100 words).")
                .tools(List.of(searchTool))
                .outputKey("tech_research")
                .build();
    }

    @Bean(name = "healthResearcher")
    public LlmAgent healthResearcher(GoogleSearchTool searchTool) {
        return new LlmAgent.Builder()
                .name("HealthResearcher")
                .model("gemini-2.5-flash-lite")
                .instruction("Research recent medical breakthroughs. Include 3 significant advances, their practical applications, and estimated timelines. Keep the report concise (100 words).")
                .tools(List.of(searchTool))
                .outputKey("health_research")
                .build();
    }

    @Bean(name = "financeResearcher")
    public LlmAgent financeResearcher(GoogleSearchTool searchTool) {
        return new LlmAgent.Builder()
                .name("FinanceResearcher")
                .model("gemini-2.5-flash-lite")
                .instruction("Research current fintech trends. Include 3 key trends, their market implications, and the future outlook. Keep the report concise (100 words).")
                .tools(List.of(searchTool))
                .outputKey("finance_research")
                .build();
    }

    @Bean(name = "aggregatorAgent")
    public LlmAgent aggregatorAgent() {
        return new LlmAgent.Builder()
                .name("AggregatorAgent")
                .model("gemini-2.5-flash-lite")
                .instruction("Combine these three research findings into a single executive summary:\n**Technology Trends:** {tech_research}\n**Health Breakthroughs:** {health_research}\n**Finance Innovations:** {finance_research}\nYour summary should highlight common themes, surprising connections, and the most important key takeaways from all three reports. The final summary should be around 200 words.")
                .outputKey("executive_summary")
                .build();
    }

    @Bean(name = "parallelResearchTeam")
    public ParallelAgent parallelResearchTeam(LlmAgent techResearcher, LlmAgent healthResearcher, LlmAgent financeResearcher) {
        return new ParallelAgent.Builder()
                .name("ParallelResearchTeam")
                .subAgents(List.of(techResearcher, healthResearcher, financeResearcher))
                .build();
    }

    @Bean(name = "researchSystemAgent")
    public SequentialAgent researchSystemAgent(ParallelAgent parallelResearchTeam, LlmAgent aggregatorAgent) {
        return new SequentialAgent.Builder()
                .name("ResearchSystem")
                .subAgents(List.of(parallelResearchTeam, aggregatorAgent))
                .build();
    }

    @Bean(name = "parallelResearchRunner")
    public InMemoryRunner parallelResearchRunner(SequentialAgent researchSystemAgent) {
        return new InMemoryRunner(researchSystemAgent);
    }
}
