package cz.net21.ttulka.exec;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author ttulka
 */
public class ProcessWatchDogTest {

    private int processIndex;

    @Before
    public void setUp() {
        initMocks(ProcessWatchDog.class);
        processIndex = 0;
    }

    @Test
    public void watchTest() throws InterruptedException {
        long currentTime = System.currentTimeMillis();

        long validTo = currentTime + 5000;  // process takes 5 secs
        long validToShort = currentTime + 1000;  // short process takes 1 sec

        Process process1 = mockProcess(validTo);
        Process process2 = mockProcess(validTo);
        Process process3 = mockProcess(validToShort);   // process 3 is short
        Process process4 = mockProcess(validTo);

        ProcessWatchDog watchDog = new ProcessWatchDog();
        watchDog.watch(process1, 1000); // kill after 1 sec
        watchDog.watch(process2, 2000); // kill after 2 secs
        watchDog.watch(process3, 2000); // kill after 2 secs

        Thread.sleep(500);

        verify(process1, never()).destroy();
        verify(process2, never()).destroy();
        verify(process3, never()).destroy();
        verify(process4, never()).destroy();

        watchDog.unwatch(process4);     // unwatch

        Thread.sleep(3000);

        assertThat(watchDog.running.get(), is(false));

        verify(process1).destroy();
        verify(process2).destroy();
        verify(process3, never()).destroy();    // died before the timeout
        verify(process4, never()).destroy();    // unwatched
    }

    @Test
    public void heartBeatTest() throws InterruptedException {
        long currentTime = System.currentTimeMillis();

        long validTo = currentTime + 2000;  // process takes 2 secs

        Process process = mockProcess(validTo);

        ProcessWatchDog watchDog = new ProcessWatchDog();
        WatchedProcess watchedProcess = watchDog.watch(process, 1000); // kill after 1 sec

        Thread.sleep(500);

        watchDog.heartBeat(process);    // first option how to send a heartbeat

        Thread.sleep(500);

        watchedProcess.heartBeat();     // second option how to send a heartbeat

        Thread.sleep(1200);

        assertThat(watchDog.running.get(), is(false));

        verify(process, never()).destroy();    // died before the timeout
    }

    @Test
    public void heartBeatViaInputStreamTest() throws InterruptedException, IOException {
        long currentTime = System.currentTimeMillis();

        long validTo = currentTime + 2000;  // process takes 2 secs

        Process process = mockProcess(validTo);

        ProcessWatchDog watchDog = new ProcessWatchDog();
        process = watchDog.watch(process, 1000); // kill after 1 sec

        process = spy(process);

        InputStream inputStream = process.getInputStream();

        Thread.sleep(500);

        inputStream.read();

        Thread.sleep(500);

        inputStream.read();

        Thread.sleep(1200);

        assertThat(watchDog.running.get(), is(false));

        verify(process, never()).destroy();    // died before the timeout
    }

    private Process mockProcess(final long validTo) {
        Process process = mock(Process.class);
        when(process.toString()).thenReturn("Process #" + (++processIndex));
        when(process.getInputStream()).thenReturn(mock(InputStream.class));
        when(process.exitValue()).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) {
                if (System.currentTimeMillis() <= validTo) {
                    throw new IllegalThreadStateException();
                } else {
                    return 0;
                }
            }
        });
        return process;
    }
}
