package ru.tele2.govorova.june.chat.server.schedulers;

import ru.tele2.govorova.june.chat.server.Server;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UnbanScheduler {
    private final Server server;
    private final ScheduledExecutorService scheduledExecutorService;

    public UnbanScheduler(Server server) {
        this.server = server;
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    public void run() {
        scheduledExecutorService.scheduleWithFixedDelay((Runnable)()-> {
            Set<String> usersToUnban = server.getAuthenticationProvider().getUsersToUnban();
            for (String userName : usersToUnban) {
                server.getAuthenticationProvider().banOrUnbanUser("N", 0, userName);
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduledExecutorService.shutdown();
    }
}
