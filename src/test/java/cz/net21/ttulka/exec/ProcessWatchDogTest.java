package cz.net21.ttulka.exec;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessWatchDogTest {

    @Before
    public void setUp() {
        initMocks(ProcessWatchDog.class);
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

    private int processIndex = 0;

    private Process mockProcess(final long validTo) {
        Process process = mock(Process.class);
        when(process.toString()).thenReturn("Process #" + (++processIndex));
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
