package org.ai.agent.google.controller;

import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/blog")
@Slf4j
@RequiredArgsConstructor
public class BlogPipelineController {

    private final InMemoryRunner blogPipelineRunner;

    @GetMapping("/publish")
    public ResponseEntity<String> publish(@RequestParam String topic,
                                          @RequestParam(defaultValue = "vishal210893") String userId,
                                          @RequestParam(defaultValue = "blog-session") String sessionKey) {
        try {
            Session session = getOrCreateSession(userId, sessionKey);
            Content userMessage = Content.fromParts(Part.fromText(topic));
            String result = runPipeline(session, userMessage);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Blog pipeline failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failure: " + e.getMessage());
        }
    }

    private Session getOrCreateSession(String userId, String sessionKey) {
        try {
            Session existing = blogPipelineRunner.sessionService()
                    .getSession(blogPipelineRunner.appName(), userId, sessionKey, Optional.empty())
                    .blockingGet();
            if (existing != null) return existing;
        } catch (Exception ignored) {
        }
        return blogPipelineRunner.sessionService()
                .createSession(blogPipelineRunner.appName(), userId)
                .blockingGet();
    }

    private String runPipeline(Session session, Content userMessage) {
        StringBuilder out = new StringBuilder();
        Flowable<Event> events = blogPipelineRunner.runAsync(session.userId(), session.id(), userMessage, RunConfig.builder().build());
        events.blockingForEach(ev -> {
            if (ev.finalResponse()) out.append(ev.stringifyContent());
        });
        return out.toString();
    }
}

