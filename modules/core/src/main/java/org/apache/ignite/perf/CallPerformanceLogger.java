package org.apache.ignite.perf;

import java.util.Timer;
import java.util.TimerTask;

public class CallPerformanceLogger {
    private final String label;

    private final Object diagTimerLock = new Object();
    private Timer reportTimer = null;
    private long totalCallCount = 0;
    private long accumulatedTime = 0;
    private long accumulatedElapsedTime = 0;
    private long activeCallCount = 0;
    private long startActiveTimestamp = 0;

    public CallPerformanceLogger(String label) {
        this.label = label;
    }

    public class CallTracker {
        private final long start = System.nanoTime();

        public long getStart() {
            return start;
        }

        public void finished() {
            callStopped(this);
        }
    }

    public CallTracker callStarted() {
        synchronized (diagTimerLock) {
            checkReporterScheduledLocked();
            totalCallCount++;
            activeCallCount++;

            CallTracker callTracker = new CallTracker();

            if (activeCallCount == 1) {
                startActiveTimestamp = callTracker.getStart();
            }

            return callTracker;
        }
    }

//========================================
// Internals
//----------------------------------------

    private void callStopped(CallTracker callTracker) {
        long timestamp = System.nanoTime();

        synchronized (diagTimerLock) {
            long elapsed = timestamp - callTracker.getStart();
            accumulatedTime += elapsed;

            activeCallCount--;

            if (activeCallCount == 0) {
                accumulatedElapsedTime += timestamp - startActiveTimestamp;
            }
        }
    }

    private void checkReporterScheduledLocked() {
        if (reportTimer == null) {
            reportTimer = new Timer();
            reportTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    reportTime();
                }
            }, 1000, 1000);
        }
    }

    private void reportTime() {
        long snapshotTotalCallCount;
        long snapshotActiveCallCount;
        long snapshotTotalTime;
        long snapshotTotalElapsedTime;
        synchronized (diagTimerLock) {
            snapshotTotalCallCount = totalCallCount;
            snapshotActiveCallCount = activeCallCount;
            snapshotTotalTime = accumulatedTime;
            snapshotTotalElapsedTime = accumulatedElapsedTime;
        }

        System.out.println("===== DIAG TIME ===== [" +
                label + "] " +
                " total-calls=" + snapshotTotalCallCount +
                " active-calls=" + snapshotActiveCallCount +
                " total-cpu-time=" + formatTime(snapshotTotalTime) +
                " total-elapsed-time=" + formatTime(snapshotTotalElapsedTime)
        );
    }

    private String formatTime(long nano) {
        double nanoDouble = nano;
        return String.format("%.06f", ( nanoDouble / 1000000000.0 ));
    }
}
