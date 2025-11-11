package org.ai.agent.google.configuration;

import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.SequentialAgent;
import com.google.adk.runner.InMemoryRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BlogPipelineConfig {

    @Bean(name = "outlineAgent")
    public LlmAgent outlineAgent() {
        return new LlmAgent.Builder()
                .name("OutlineAgent")
                .model("gemini-2.5-flash-lite")
                .instruction("Create a blog outline for the given topic with:\n1. A catchy headline\n2. An introduction hook\n3. 3-5 main sections with 2-3 bullet points for each\n4. A concluding thought")
                .outputKey("blog_outline")
                .build();
    }

    @Bean(name = "writerAgent")
    public LlmAgent writerAgent() {
        return new LlmAgent.Builder()
                .name("WriterAgent")
                .model("gemini-2.5-flash-lite")
                .instruction("Following this outline strictly: {blog_outline}\nWrite a brief, 200 to 300-word blog post with an engaging and informative tone.")
                .outputKey("blog_draft")
                .build();
    }

    @Bean(name = "editorAgent")
    public LlmAgent editorAgent() {
        return new LlmAgent.Builder()
                .name("EditorAgent")
                .model("gemini-2.5-flash-lite")
                .instruction("Edit this draft: {blog_draft}\nYour task is to polish the text by fixing any grammatical errors, improving the flow and sentence structure, and enhancing overall clarity.")
                .outputKey("final_blog")
                .build();
    }

    @Bean(name = "blogSequentialAgent")
    public SequentialAgent blogSequentialAgent(LlmAgent outlineAgent, LlmAgent writerAgent, LlmAgent editorAgent) {
        return new SequentialAgent.Builder()
                .name("BlogPipeline")
                .subAgents(java.util.List.of(outlineAgent, writerAgent, editorAgent))
                .build();
    }

    @Bean(name = "blogPipelineRunner")
    public InMemoryRunner blogPipelineRunner(SequentialAgent blogSequentialAgent) {
        return new InMemoryRunner(blogSequentialAgent);
    }
}
