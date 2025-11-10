package org.ai.agent.google;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class AiAgentApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(AiAgentApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
//        RunConfig runConfig = RunConfig.builder().build();
//        InMemoryRunner runner = new InMemoryRunner(HelloTimeAgent.ROOT_AGENT);
//
//        Session session = runner
//                .sessionService()
//                .createSession(runner.appName(), "user1234")
//                .blockingGet();
//
//        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
//            while (true) {
//                System.out.print("\nYou > ");
//                String userInput = scanner.nextLine();
//                if ("quit".equalsIgnoreCase(userInput)) {
//                    break;
//                }
//
//                Content userMsg = Content.fromParts(Part.fromText(userInput));
//                Flowable<Event> events = runner.runAsync(session.userId(), session.id(), userMsg, runConfig);
//
//                System.out.print("\nAgent > ");
//                events.blockingForEach(event -> {
//                    if (event.finalResponse()) {
//                        System.out.println(event.stringifyContent());
//                    }
//                });
//            }
//        }
//    }
    }
}
