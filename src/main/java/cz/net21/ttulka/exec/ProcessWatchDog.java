package cz.net21.ttulka.exec;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Watching dog for processes.
 * <p>
 * Runs in a different thread to watch a set of processes and kills them after a set timeout.
 * <p>
 * Timeout could differ from a delta {@link #DELTA}.
 * <p>
 * The thread runs only when there are any active watches.
 */
public class ProcessWatchDog {

    static final int DELTA = 100;   // timeout between watches in millis

    private static final Log log = LogFactory.getLog(ProcessWatchDog.class);

    protected AtomicBoolean running = new AtomicBoolean(false);

    private final Map<Process, Long> processes = new ConcurrentHashMap<Process, Long>();   // process, validTo

    /**
     * Add a process to watching.
     *
     * @param process         the process
     * @param timeoutInMillis the timeout in millis
     */
    public void watch(Process process, int timeoutInMillis) {
        log.debug("Process watched: " + process);

        if (!addProcessAndCheckRunning(process, timeoutInMillis)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ProcessWatchDog.this.run();
                }
            }).start();
        }
    }

    /**
     * Removes a process from watching.
     *
     * @param process the process
     */
    public void unwatch(Process process) {
        log.debug("Process unwatched: " + process);
        processes.remove(process);
    }

    /**
     * Atomic adds a new process to the watched processes and sets running to true if it is not currently
     *
     * @param process the new watched process
     * @param timeout the timeout in millis
     * @return true if the watch dog is currently running otherwise false
     */
    private boolean addProcessAndCheckRunning(Process process, int timeout) {
        synchronized (running) {
            processes.put(process, System.currentTimeMillis() + timeout);
            return running.getAndSet(true);
        }
    }

    /**
     * Atomic checks if there are any watched processes and sets running to false if not
     *
     * @return true is watched processes are empty otherwise false
     */
    private boolean stopRunningWhenProcessesEmpty() {
        synchronized (running) {
            if (processes.isEmpty()) {
                running.set(false);
                return true;

            } else {
                return false;
            }
        }
    }

    /**
     * Runs watching.
     */
    protected void run() {
        while (!stopRunningWhenProcessesEmpty()) {

            long currentTime = System.currentTimeMillis();

            for (Map.Entry<Process, Long> entry : processes.entrySet()) {
                Process process = entry.getKey();
                Long validTo = entry.getValue();

                if (!isAlive(process)) {
                    unwatch(process);

                } else if (validTo < currentTime) {
                    log.info("Process killed: " + process);
                    process.destroy();
                    unwatch(process);
                }
            }

            try {
                Thread.sleep(DELTA);
            } catch (InterruptedException ignore) {
            }
        }
    }

    private boolean isAlive(Process process) {
        try {
            process.exitValue();
            return false;

        } catch (IllegalThreadStateException e) {
            return true;
        }
    }
}
