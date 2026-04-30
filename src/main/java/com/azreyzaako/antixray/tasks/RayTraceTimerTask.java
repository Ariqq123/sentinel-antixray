package com.azreyzaako.antixray.tasks;

import java.util.TimerTask;
import java.util.concurrent.RejectedExecutionException;

import com.azreyzaako.antixray.SentinelAntiXray;

public final class RayTraceTimerTask extends TimerTask {
    private final SentinelAntiXray plugin;

    public RayTraceTimerTask(SentinelAntiXray plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        boolean timingsEnabled = plugin.isTimingsEnabled();
        long start = timingsEnabled ? System.currentTimeMillis() : 0L;

        try {
            plugin.getExecutorService().invokeAll(plugin.getPlayerData().values());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RejectedExecutionException e) {

        }

        if (timingsEnabled && !plugin.getPlayerData().isEmpty()) {
            long stop = System.currentTimeMillis();
            plugin.getLogger().info((stop - start) + "ms per ray trace tick.");
        }
    }
}
