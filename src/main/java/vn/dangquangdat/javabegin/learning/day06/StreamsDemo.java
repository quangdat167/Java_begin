package vn.dangquangdat.javabegin.learning.day06;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Ngay 6: lambda, Stream pipeline, grouping, mapping va reducing. */
public class StreamsDemo {

    public static void main(String[] args) {
        List<AgentExecution> executions = List.of(
                new AgentExecution("summarizer", Status.SUCCESS, 120),
                new AgentExecution("summarizer", Status.FAILED, 80),
                new AgentExecution("code-review", Status.SUCCESS, 350),
                new AgentExecution("summarizer", Status.SUCCESS, 100)
        );

        List<String> slowSuccessfulAgents = executions.stream()
                .filter(item -> item.status() == Status.SUCCESS)
                .filter(item -> item.durationMs() >= 100)
                .sorted(Comparator.comparingLong(AgentExecution::durationMs).reversed())
                .map(AgentExecution::agentName)
                .distinct()
                .toList();

        Map<String, Double> averageDurationByAgent = executions.stream()
                .collect(Collectors.groupingBy(
                        AgentExecution::agentName,
                        Collectors.averagingLong(AgentExecution::durationMs)
                ));

        System.out.println(slowSuccessfulAgents);
        System.out.println(averageDurationByAgent);
    }

    enum Status { SUCCESS, FAILED }

    record AgentExecution(String agentName, Status status, long durationMs) {
    }
}

