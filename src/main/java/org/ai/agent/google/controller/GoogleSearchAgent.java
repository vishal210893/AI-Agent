package org.ai.agent.google.controller;

import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public GoogleSearchAgent(InMemoryRunner runner) {
        this.runner = runner;
    }

    @GetMapping("/search")
    public ResponseEntity<String> getGoogleSearchAgentResponse(@RequestParam String query) {

        final StringBuilder agentResponse = new StringBuilder();
        String userId = "vishal210893";
        String sessionId = "my-web-session"; // This is our human-readable session key

        try {
            log.info("Attempting to get or create session with UserID: {}, SessionID: {}", userId, sessionId);

            // 1. Manually implement "get or create"
            Session session;
            try {
                // Try to get the session first.
                // We assume the signature is (userId, sessionId)
                session = runner.sessionService()
                        .getSession(runner.appName(), userId, sessionId, Optional.empty()) // <-- Get existing
                        .blockingGet(); // This will be null if not found, or throw

                if (session == null) {
                    // This block may be hit if getSession returns null instead of throwing
                    log.info("getSession returned null, creating new session...");
                    session = runner.sessionService()
                            .createSession(runner.appName(), userId) // <-- Create new
                            .blockingGet();
                } else {
                    log.info("Successfully found existing session.");
                }

            } catch (Exception e) {
                // If getSession throws an error (like "not found"), we create a new one.
                log.warn("Could not find session, creating new one. Error: {}", e.getMessage());
                session = runner.sessionService()
                        .createSession(userId, sessionId) // <-- Create new
                        .blockingGet();
            }

            log.info("Session context ready. User: {}, Client-Side ID: {}", session.userId(), sessionId);

            // 2. Create the User's Message
            Content userMessage = Content.fromParts(
                    Part.fromText(query)
            );

            // 3. Run the Query
            // --- THIS IS THE CRITICAL FIX ---
            // We pass the *original string variable* 'sessionId' to 'runAsync',
            // NOT the 'session.id()' from the object.
            Flowable<Event> events = runner.runAsync(
                    session.userId(), // From the session object
                    session.id(),        // The original string variable
                    userMessage,
                    RunConfig.builder().build()
            );

            System.out.print("\nAgent > ");

            // 4. Process Events
            events.blockingForEach(event -> {
                if (event.finalResponse()) {
                    String responseText = event.stringifyContent();
                    System.out.print(responseText);
                    agentResponse.append(responseText);
                }
            });

            System.out.println(); // Add a newline

        } catch (Exception e) {
            log.error("Error during agent run: " + e.getMessage(), e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }

        return ResponseEntity.ok(agentResponse.toString());
    }
}