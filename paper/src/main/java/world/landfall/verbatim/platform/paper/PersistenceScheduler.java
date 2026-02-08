package world.landfall.verbatim.platform.paper;

import world.landfall.verbatim.Verbatim;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodic auto-save scheduler for player data.
 * Runs on a single daemon thread so it doesn't prevent JVM shutdown.
 */
public class PersistenceScheduler {

    private static final long SAVE_INTERVAL_MINUTES = 5;
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 5;

    private final ScheduledExecutorService executor;
    private final Runnable saveTask;

    public PersistenceScheduler(Runnable saveTask) {
        this.saveTask = saveTask;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Verbatim-AutoSave");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        executor.scheduleAtFixedRate(() -> {
            try {
                Verbatim.LOGGER.debug("[Verbatim] Auto-save triggered");
                saveTask.run();
            } catch (Exception e) {
                Verbatim.LOGGER.error("[Verbatim] Auto-save failed: {}", e.getMessage(), e);
            }
        }, SAVE_INTERVAL_MINUTES, SAVE_INTERVAL_MINUTES, TimeUnit.MINUTES);
        Verbatim.LOGGER.info("[Verbatim] Auto-save scheduler started (every {} minutes)", SAVE_INTERVAL_MINUTES);
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                Verbatim.LOGGER.warn("[Verbatim] Auto-save scheduler did not terminate gracefully");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        Verbatim.LOGGER.info("[Verbatim] Auto-save scheduler stopped");
    }
}
