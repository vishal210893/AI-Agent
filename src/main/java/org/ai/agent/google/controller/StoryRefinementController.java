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

/**
 * Controller exposing iterative story refinement pipeline.
 * Endpoint: /story/refine?prompt=...  (user supplies initial story idea/prompt)
 */
@RestController
@RequestMapping("/story")
@Slf4j
@RequiredArgsConstructor
public class StoryRefinementController {

    private final InMemoryRunner storyPipelineRunner;

    @GetMapping("/refine")
    public ResponseEntity<String> refine(@RequestParam String prompt,
                                         @RequestParam(defaultValue = "vishal210893") String userId,
                                         @RequestParam(defaultValue = "story-refine-session") String sessionKey) {
        log.info("Story refinement requested. userId={}, sessionKey={}, prompt='{}'", userId, sessionKey, prompt);
        try {
            Session session = getOrCreateSession(userId, sessionKey);
            Content userMessage = Content.fromParts(Part.fromText(prompt));
            String output = runPipeline(session, userMessage);
            return ResponseEntity.ok(output);
        } catch (Exception e) {
            log.error("Story refinement failed. userId={}, sessionKey={}, error={}", userId, sessionKey, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failure: " + e.getMessage());
        }
    }

    private Session getOrCreateSession(String userId, String sessionKey) {
        try {
            Session existing = storyPipelineRunner.sessionService()
                    .getSession(storyPipelineRunner.appName(), userId, sessionKey, Optional.empty())
                    .blockingGet();
            if (existing != null) {
                return existing;
            }
            log.debug("No existing story session found. Creating new.");
        } catch (Exception ex) {
            log.debug("Story session lookup failed (will create). reason={}", ex.getMessage());
        }
        Session session = storyPipelineRunner.sessionService()
                .createSession(storyPipelineRunner.appName(), userId)
                .blockingGet();
        log.info("Created story session. userId={}, sessionId={}", session.userId(), session.id());
        return session;
    }

    private String runPipeline(Session session, Content userMessage) {
        StringBuilder out = new StringBuilder();
        Flowable<Event> events = storyPipelineRunner.runAsync(
                session.userId(),
                session.id(),
                userMessage,
                RunConfig.builder().build()
        );
        events.blockingForEach(event -> {
            if (event.finalResponse()) {
                out.append(event.stringifyContent());
            }
        });
        String result = out.toString();
        log.info("Story pipeline final length={}", result.length());
        return result;
    }
}

