package io.github.vlsergey.java_jokes_example;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Controller
public class JokesController {

    // тут можно:
    // - настроить правила работы с протоколами
    // - настроить таймауты запросов (по-умолчанию)
    private final RestTemplate restTemplate = new RestTemplateBuilder().build();

    /* Соответствует идеологии Java до 4-й версии */
    @GetMapping(path = "/withThreads")
    public String handleGetWithThreads(@RequestParam(name = "count", defaultValue = "5") int count, Model model) throws Exception {
        Queue<String> jokes = new ConcurrentLinkedQueue<>();
        List<Thread> threads = new ArrayList<>();

        // Создаём и запускаем новые потоки исполнения
        for (int i = 0; i < count; i++) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    Joke apiResponse = restTemplate.getForEntity("https://geek-jokes.sameerkumar" + ".website/api" +
                            "?format=json", Joke.class).getBody();
                    jokes.add(apiResponse.joke);
                }
            };
            thread.start();
            threads.add(thread);
        }

        // Ждём окончания исполнения потоков
        for (final Thread thread : threads) {
            thread.join();
        }

        // Отдаём результат
        model.addAttribute("jokes", jokes);
        return "jokes";
    }

    // Тут можно:
    // - указать максимальное число потоков
    // - указать приоритет, названия, группу исполнения
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /* Идеология выделенных пулов потоков (Java 5+) */
    @GetMapping(path = "/withExectutor")
    public String handleGetWithFutures(@RequestParam(name = "count", defaultValue = "5") int count, Model model) throws Exception {
        List<Future<String>> futures = new ArrayList<>();

        // Создаём и запускаем новые потоки исполнения
        for (int i = 0; i < count; i++) {
            futures.add(executor.submit(() -> {
                Joke apiResponse = restTemplate.getForEntity("https://geek-jokes.sameerkumar.website/api?format=json"
                        , Joke.class).getBody();
                return apiResponse.joke;
            }));
        }

        Queue<String> jokes = new ConcurrentLinkedQueue<>();

        // Ждём окончания исполнения потоков
        for (final Future<String> future : futures) {
            jokes.add(future.get());
        }

        // Отдаём результат
        model.addAttribute("jokes", jokes);
        return "jokes";
    }

    private final WebClient webClient = WebClient.builder().build();

    /* Полностью асинхронный API со Spring Web Flux WebClient */
    @GetMapping(path = "/withWebClient")
    public String handleGetWithWebClient(@RequestParam(name = "count", defaultValue = "5") int count, Model model) throws Exception {
        List<Future<ResponseEntity<Joke>>> futures = new ArrayList<>();

        // Создаём и запускаем новые потоки исполнения
        for (int i = 0; i < count; i++) {
            futures.add(webClient.get().uri("https://geek-jokes.sameerkumar.website/api?format=json").retrieve().toEntity(Joke.class).toFuture());
        }

        Queue<String> jokes = new ConcurrentLinkedQueue<>();

        // Ждём окончания асинхронной загрузки
        for (final Future<ResponseEntity<Joke>> future : futures) {
            jokes.add(future.get().getBody().joke);
        }

        // Отдаём результат
        model.addAttribute("jokes", jokes);
        return "jokes";
    }

}
