package quantumquest.triviathrone.jeopardy.session;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

final class GameBuzzer {
    private static final long COLLECTION_WINDOW_MS = 120;

    private final Map<String, Long> buzzTimes = new HashMap<>();
    private final Set<String> attemptedPlayers = new HashSet<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "game-buzzer");
        thread.setDaemon(true);
        return thread;
    });
    private final Consumer<String> onBuzzFinalized;

    private boolean open;
    private boolean collecting;
    private String winner;
    private ScheduledFuture<?> pendingBuzz;

    GameBuzzer(Consumer<String> onBuzzFinalized) {
        this.onBuzzFinalized = Objects.requireNonNull(onBuzzFinalized, "onBuzzFinalized");
    }

    synchronized void open() {
        cancelPendingBuzz();
        winner = null;
        buzzTimes.clear();
        open = true;
        collecting = false;
    }

    synchronized Registration register(String username) {
        Objects.requireNonNull(username, "username");
        if ((!open && !collecting) || attemptedPlayers.contains(username) || buzzTimes.containsKey(username)) {
            return Registration.IGNORED;
        }

        buzzTimes.put(username, System.nanoTime());
        if (open) {
            open = false;
            collecting = true;
            pendingBuzz = scheduler.schedule(this::finalizeBuzz, COLLECTION_WINDOW_MS, TimeUnit.MILLISECONDS);
            return Registration.FIRST;
        }
        return Registration.ADDITIONAL;
    }

    synchronized String closeBuzz() {
        open = false;
        if (collecting) {
            finalizeBuzzLocked();
        } else {
            cancelPendingBuzz();
        }
        return winner;
    }

    synchronized String winner() {
        return winner;
    }

    synchronized void markIncorrect() {
        if (winner == null) {
            return;
        }
        attemptedPlayers.add(winner);
        winner = null;
        buzzTimes.clear();
        open = false;
        collecting = false;
        cancelPendingBuzz();
    }

    synchronized Set<String> getBuzzedPlayers() {
        Set<String> buzzedPlayers = new HashSet<>(attemptedPlayers);
        buzzedPlayers.addAll(buzzTimes.keySet());
        return Set.copyOf(buzzedPlayers);
    }

    synchronized void reset() {
        cancelPendingBuzz();
        winner = null;
        buzzTimes.clear();
        attemptedPlayers.clear();
        open = false;
        collecting = false;
    }

    synchronized void close() {
        reset();
        scheduler.shutdownNow();
    }

    private void finalizeBuzz() {
        String finalizedWinner;
        synchronized (this) {
            if (!collecting) {
                return;
            }
            finalizedWinner = finalizeBuzzLocked();
        }
        onBuzzFinalized.accept(finalizedWinner);
    }

    private String finalizeBuzzLocked() {
        cancelPendingBuzz();
        winner = buzzTimes.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        collecting = false;
        open = false;
        return winner;
    }

    private void cancelPendingBuzz() {
        if (pendingBuzz != null) {
            pendingBuzz.cancel(false);
            pendingBuzz = null;
        }
    }

    enum Registration {
        IGNORED,
        FIRST,
        ADDITIONAL
    }
}
