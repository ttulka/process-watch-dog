package cz.net21.ttulka.exec;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
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
 *
 * @author ttulka
 */
public class ProcessWatchDog {

    static final int DELTA = 100;   // timeout between watches in millis

    private static final Log log = LogFactory.getLog(ProcessWatchDog.class);

    private final Set<WatchedProcess> processes = new CopyOnWriteArraySet<WatchedProcess>();

    protected AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Add a process to watching.
     *
     * @param process         the process
     * @param timeoutInMillis the timeout in millis
     * @return the watched process
     */
    public WatchedProcess watch(Process process, int timeoutInMillis) {
        log.debug("Process watched: " + process);

        WatchedProcess watchedProcess = new WatchedProcess(process, timeoutInMillis);

        if (!addProcessAndCheckRunning(watchedProcess)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ProcessWatchDog.this.run();
                }
            }).start();
        }
        return watchedProcess;
    }

    /**
     * Atomic adds a new process to the watched processes and sets running to true if it is not.
     *
     * @param process the new watched process
     * @return true if the watch dog is currently running otherwise false
     */
    private boolean addProcessAndCheckRunning(WatchedProcess process) {
        synchronized (running) {
            processes.add(process);
            return running.getAndSet(true);
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
     * Sends a heart beat to the process, increase the valid to value for the current time plus the timeout.
     *
     * @param process the process
     */
    public void heartBeat(Process process) {
        if (processes.contains(process)) {

            for (WatchedProcess watchedProcess : processes) {
                if (watchedProcess.equals(process)) {

                    watchedProcess.heartBeat();
                    return;
                }
            }
        }
    }

    /**
     * Atomic checks if there are any watched processes and sets running to false if not.
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

            for (WatchedProcess process : processes) {

                if (!isAlive(process)) {
                    unwatch(process);

                } else if (process.validTo() < currentTime) {
                    log.info("Process killed: " + process);

                    unwatch(process);
                    killProcess(process);
                }
            }

            try {
                Thread.sleep(DELTA);
            } catch (InterruptedException ignore) {
            }
        }
    }

    /**
     * Checks if the process is alive.
     *
     * @param process the process
     * @return true if the process is alive, otherwise false
     */
    private boolean isAlive(Process process) {
        try {
            process.exitValue();
            return false;

        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

    /**
     * Kills the process.
     *
     * @param process the process
     */
    private void killProcess(Process process) {
        try {
            process.destroy();

        } catch (Exception e) {
            log.warn("Exception by killing the process.", e);
        }
    }
}
