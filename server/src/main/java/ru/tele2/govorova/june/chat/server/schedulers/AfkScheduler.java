package ru.tele2.govorova.june.chat.server.schedulers;

import ru.tele2.govorova.june.chat.server.ClientHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AfkScheduler {
    private long lastActive;
    private int timeLimit;
    private final ClientHandler clientHandler;
    private final ScheduledExecutorService scheduledExecutorService;


    public AfkScheduler(ClientHandler clientHandler, int secondsLeft) {
        this.lastActive = System.currentTimeMillis();
        this.timeLimit = secondsLeft;
        this.clientHandler = clientHandler;
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    public void setLastActive() {
        this.lastActive = System.currentTimeMillis();
    }

    public void run() {
        scheduledExecutorService.scheduleWithFixedDelay((Runnable) ()-> {
            if (System.currentTimeMillis() - lastActive >= timeLimit * 1000L) {
                clientHandler.sendMessage("/afk");
                stop();
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduledExecutorService.shutdown();
    }
}

