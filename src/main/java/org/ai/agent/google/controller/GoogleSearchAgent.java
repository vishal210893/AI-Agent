package org.ai.agent.google.controller;

import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/google")
@Slf4j
public class GoogleSearchAgent {

    private final InMemoryRunner runner;

    public GoogleSearchAgent(InMemoryRunner runner) {
        this.runner = runner;
    }

    /**
     * Execute an agent query within a conversational session.
     * Provide a stable \`sessionKey\` from client to maintain history.
     */
    @GetMapping("/search")
    public ResponseEntity<String> search(@RequestParam String query,
                                         @RequestParam(defaultValue = "vishal210893") String userId,
                                         @RequestParam(defaultValue = "my-web-session") String sessionKey) {

        log.info("Incoming query. userId={}, sessionKey={}, query='{}'", userId, sessionKey, query);

        try {
            // 1. Obtain existing session or create a new one.
            Session session = getOrCreateSession(userId, sessionKey);
            log.debug("Session active. userId={}, sessionId={}", session.userId(), session.id());

            // 2. Prepare user message.
            Content userMessage = Content.fromParts(Part.fromText(query));

            // 3. Run agent asynchronously (blocking consumption for simplicity).
            String agentOutput = runAgent(session, userMessage);

            // 4. Return final response.
            return ResponseEntity.ok(agentOutput);

        } catch (Exception e) {
            log.error("Search failed. userId={}, sessionKey={}, error={}", userId, sessionKey, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Failure: " + e.getMessage());
        }
    }

    /**
     * Attempt to fetch a session; if not found, create it.
     * Assumes correct signature: createSession(userId, sessionId).
     * Adjust if your ADK version differs.
     */
    private Session getOrCreateSession(String userId, String sessionKey) {
        try {
            // Try existing session first.
            Session existing = runner.sessionService()
                    .getSession(runner.appName(), userId, sessionKey, Optional.empty())
                    .blockingGet();
            if (existing != null) {
                return existing;
            }
            log.info("Session lookup returned null. Creating new session. userId={}, sessionKey={}", userId, sessionKey);
        } catch (Exception ex) {
            log.debug("Session not found or lookup failed (will create). userId={}, sessionKey={}, reason={}",
                    userId, sessionKey, ex.getMessage());
        }
        // Create new session with stable external key.
        Session created = runner.sessionService()
                .createSession(runner.appName(), userId)
                .blockingGet();
        log.info("Created new session. userId={}, sessionId={}", created.userId(), created.id());
        return created;
    }

    /**
     * Run the agent and collect only final response parts.
     */
    private String runAgent(Session session, Content userMessage) {
        StringBuilder out = new StringBuilder();
        Flowable<Event> events = runner.runAsync(
                session.userId(),
                session.id(),  // Use the session's internal id returned by creation/get.
                userMessage,
                RunConfig.builder().build()
        );

        // Consume events synchronously; only append final response segments.
        events.blockingForEach(event -> {
            if (event.finalResponse()) {
                String text = event.stringifyContent();
                out.append(text);
            }
        });

        String result = out.toString();
        log.info("Agent final response {}", result);
        return result;
    }
}