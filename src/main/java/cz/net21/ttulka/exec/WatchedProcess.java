package cz.net21.ttulka.exec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Proxy process sending a heart beat by each reading.
 *
 * @author ttulka
 */
public class WatchedProcess extends Process {

    private final Process process;
    private final int timeout;

    private final InputStream inputStream;
    private final InputStream errorStream;
    private final OutputStream outputStream;

    private long validTo;

    protected WatchedProcess(Process process, int timeout) {
        this.process = process;
        this.timeout = timeout;

        this.inputStream = new InputStreamWithHeartBeat(process.getInputStream());
        this.errorStream = process.getErrorStream();
        this.outputStream = process.getOutputStream();

        this.validTo = calculateValidTo(timeout);
    }

    private long calculateValidTo(int timeout) {
        return System.currentTimeMillis() + timeout;
    }

    /**
     * Sends a heart beat, increase the valid to value for the current time plus the timeout.
     */
    public void heartBeat() {
        validTo = calculateValidTo(timeout);
    }

    /**
     * Returns the valid to value.
     *
     * @return the valid to
     */
    public long validTo() {
        return validTo;
    }

    /**
     * Returns the timeout.
     *
     * @return the timeout
     */
    public int timeout() {
        return timeout;
    }

    @Override
    public OutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public InputStream getErrorStream() {
        return errorStream;
    }

    @Override
    public int waitFor() throws InterruptedException {
        return process.waitFor();
    }

    @Override
    public int exitValue() {
        return process.exitValue();
    }

    @Override
    public void destroy() {
        process.destroy();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WatchedProcess that = (WatchedProcess) o;

        return process.equals(that.process);
    }

    @Override
    public int hashCode() {
        return process.hashCode();
    }

    @Override
    public String toString() {
        return "WatchedProcess{" + "process=" + process + ", timeout=" + timeout + ", validTo=" + validTo + '}';
    }

    /**
     * Input stream sends a heart beat every time a byte is read.
     */
    protected class InputStreamWithHeartBeat extends InputStream {

        private final InputStream baseInputStream;

        InputStreamWithHeartBeat(InputStream baseInputStream) {
            this.baseInputStream = baseInputStream;
        }

        @Override
        public int read() throws IOException {
            int read = baseInputStream.read();
            heartBeat();
            return read;
        }
    }
}
