package uk.ac.bris.cs.scotlandyard.multiplayer;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class DaemonExecutorThreadFactory implements ThreadFactory {

    @Override
    public Thread newThread(Runnable runnable) {
        Thread t = Executors.defaultThreadFactory().newThread(runnable);
        t.setDaemon(true);
        return t;
    }
}
