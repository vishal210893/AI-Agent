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
 * Controller exposing the orchestrated research + summarization workflow.
 */
@RestController
@RequestMapping("/research")
@Slf4j
@RequiredArgsConstructor
public class ResearchCoordinatorController {

    private final InMemoryRunner researchRunner;

    @GetMapping("/summary")
    public ResponseEntity<String> summarize(@RequestParam String topic,
                                            @RequestParam(defaultValue = "vishal210893") String userId,
                                            @RequestParam(defaultValue = "research-session") String sessionKey) {
        log.info("Research summary request. userId={}, sessionKey={}, topic='{}'", userId, sessionKey, topic);
        try {
            Session session = getOrCreateSession(userId, sessionKey);
            Content userMessage = Content.fromParts(Part.fromText(topic));
            String output = runCoordinator(session, userMessage);
            return ResponseEntity.ok(output);
        } catch (Exception e) {
            log.error("Coordinator failed. userId={}, sessionKey={}, error={}", userId, sessionKey, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failure: " + e.getMessage());
        }
    }

    private Session getOrCreateSession(String userId, String sessionKey) {
        try {
            Session existing = researchRunner.sessionService()
                    .getSession(researchRunner.appName(), userId, sessionKey, Optional.empty())
                    .blockingGet();
            if (existing != null) return existing;
            log.info("No existing research session; creating new. userId={}, sessionKey={}", userId, sessionKey);
        } catch (Exception ex) {
            log.debug("Research session lookup failed; will create. userId={}, sessionKey={}, reason={}", userId, sessionKey, ex.getMessage());
        }
        Session session = researchRunner.sessionService()
                .createSession(researchRunner.appName(), userId)
                .blockingGet();
        log.info("Created research session. userId={}, sessionId={}", session.userId(), session.id());
        return session;
    }

    private String runCoordinator(Session session, Content userMessage) {
        StringBuilder out = new StringBuilder();
        Flowable<Event> events = researchRunner.runAsync(
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
        log.info("Coordinator final summary length={}", result.length());
        return result;
    }
}

