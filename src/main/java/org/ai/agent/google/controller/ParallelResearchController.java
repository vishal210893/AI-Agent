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
@RequestMapping("/exec")
@Slf4j
@RequiredArgsConstructor
public class ParallelResearchController {

    private final InMemoryRunner parallelResearchRunner;

    @GetMapping("/daily")
    public ResponseEntity<String> dailyBrief(@RequestParam(defaultValue = "Run the daily executive briefing on Tech, Health, and Finance") String prompt,
                                             @RequestParam(defaultValue = "vishal210893") String userId,
                                             @RequestParam(defaultValue = "parallel-research-session") String sessionKey) {
        try {
            Session session = getOrCreateSession(userId, sessionKey);
            Content userMessage = Content.fromParts(Part.fromText(prompt));
            String result = runSystem(session, userMessage);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Parallel+Sequential research system failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failure: " + e.getMessage());
        }
    }

    private Session getOrCreateSession(String userId, String sessionKey) {
        try {
            Session existing = parallelResearchRunner.sessionService()
                    .getSession(parallelResearchRunner.appName(), userId, sessionKey, Optional.empty())
                    .blockingGet();
            if (existing != null) return existing;
        } catch (Exception ignored) {
        }
        return parallelResearchRunner.sessionService()
                .createSession(parallelResearchRunner.appName(), userId)
                .blockingGet();
    }

    private String runSystem(Session session, Content userMessage) {
        StringBuilder out = new StringBuilder();
        Flowable<Event> events = parallelResearchRunner.runAsync(session.userId(), session.id(), userMessage, RunConfig.builder().build());
        events.blockingForEach(ev -> {
            if (ev.finalResponse()) out.append(ev.stringifyContent());
        });
        return out.toString();
    }
}

