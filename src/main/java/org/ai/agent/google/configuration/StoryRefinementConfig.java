package org.ai.agent.google.configuration;

import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.LoopAgent;
import com.google.adk.agents.SequentialAgent;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.tools.FunctionTool;
import com.google.adk.tools.Annotations.Schema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.List;

/**
 * Configuration for the iterative story refinement workflow:
 * Initial writer -> (Critic + Refiner loop) with early exit when approved.
 */
@Configuration
public class StoryRefinementConfig {

    // Property-driven configuration (no hard-coded defaults)
    @Value("${story.initial.name}")
    private String initialWriterName;
    @Value("${story.initial.instruction}")
    private String initialWriterInstruction;
    @Value("${story.initial.output-key}")
    private String initialWriterOutputKey;

    @Value("${story.critic.name}")
    private String criticName;
    @Value("${story.critic.instruction}")
    private String criticInstruction;
    @Value("${story.critic.output-key}")
    private String criticOutputKey;

    @Value("${story.refiner.name}")
    private String refinerName;
    @Value("${story.refiner.instruction}")
    private String refinerInstruction;
    @Value("${story.refiner.output-key}")
    private String refinerOutputKey;

    @Value("${story.loop.name}")
    private String loopName;
    @Value("${story.loop.max-iterations}")
    private int loopMaxIterations;

    @Value("${story.pipeline.name}")
    private String pipelineName;

    // Reuse model from base agent config for consistency
    @Value("${agent.model}")
    private String model;

    /** First draft writer (runs once). */
    @Bean(name = "initialWriterAgent")
    public LlmAgent initialWriterAgent() {
        return new LlmAgent.Builder()
                .name(initialWriterName)
                .model(model)
                .instruction(initialWriterInstruction)
                .outputKey(initialWriterOutputKey)
                .build();
    }

    /** Critic agent provides APPROVED or improvement suggestions. */
    @Bean(name = "criticAgent")
    public LlmAgent criticAgent() {
        return new LlmAgent.Builder()
                .name(criticName)
                .model(model)
                .instruction(criticInstruction)
                .outputKey(criticOutputKey)
                .build();
    }

    /** Tool function enabling loop exit when critique is APPROVED. */
    public static class ExitLoopFunctions {
        @Schema(description = "Call ONLY when the critique is APPROVED to terminate refinement loop.")
        public static Map<String, String> exit_loop() {
            return Map.of("status", "approved", "message", "Story approved. Exiting refinement loop.");
        }
    }

    @Bean
    public FunctionTool exitLoopTool() {
        return FunctionTool.create(ExitLoopFunctions.class, "exit_loop");
    }

    /** Refiner: rewrites or triggers exit via tool. */
    @Bean(name = "refinerAgent")
    public LlmAgent refinerAgent(FunctionTool exitLoopTool) {
        return new LlmAgent.Builder()
                .name(refinerName)
                .model(model)
                .instruction(refinerInstruction)
                .outputKey(refinerOutputKey)
                .tools(List.of(exitLoopTool))
                .build();
    }

    /** Loop containing Critic -> Refiner sequence per iteration. */
    @Bean(name = "storyRefinementLoop")
    public LoopAgent storyRefinementLoop(LlmAgent criticAgent, LlmAgent refinerAgent) {
        return new LoopAgent.Builder()
                .name(loopName)
                .subAgents(List.of(criticAgent, refinerAgent))
                .maxIterations(loopMaxIterations)
                .build();
    }

    /** Root pipeline: Initial write then loop refinement. */
    @Bean(name = "storyPipelineAgent")
    public SequentialAgent storyPipelineAgent(LlmAgent initialWriterAgent, LoopAgent storyRefinementLoop) {
        return new SequentialAgent.Builder()
                .name(pipelineName)
                .subAgents(List.of(initialWriterAgent, storyRefinementLoop))
                .build();
    }

    /** Runner for external triggering via controller. */
    @Bean(name = "storyPipelineRunner")
    public InMemoryRunner storyPipelineRunner(SequentialAgent storyPipelineAgent) {
        return new InMemoryRunner(storyPipelineAgent);
    }
}

