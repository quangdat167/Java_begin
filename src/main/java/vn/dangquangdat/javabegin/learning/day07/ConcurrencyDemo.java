package vn.dangquangdat.javabegin.learning.day07;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Ngay 7: race condition, thread pool, Future va virtual thread. */
public class ConcurrencyDemo {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        List<Callable<ModelAnswer>> calls = List.of(
                () -> callModel("model-a", 120),
                () -> callModel("model-b", 80),
                () -> callModel("model-c", 100)
        );

        // Virtual thread rat hop voi tac vu I/O blocking nhu goi HTTP/database.
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<ModelAnswer>> futures = executor.invokeAll(calls);
            for (Future<ModelAnswer> future : futures) {
                System.out.println(future.get());
            }
        }
    }

    private static ModelAnswer callModel(String model, long latencyMs) throws InterruptedException {
        Thread.sleep(Duration.ofMillis(latencyMs)); // gia lap I/O, khong phai CPU work
        return new ModelAnswer(model, "answer from " + model, latencyMs);
    }

    record ModelAnswer(String model, String answer, long latencyMs) {
    }
}

