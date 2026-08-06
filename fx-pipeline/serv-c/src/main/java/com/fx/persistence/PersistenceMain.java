package com.fx.persistence;

import com.fx.common.queue.QueuePaths;
import java.sql.SQLException;
import org.h2.tools.Server;

/**
 * {@code PersistenceMain} — Entry point for serv-c (Persistence Service).
 * @author FX Pipeline Team
 */
public final class PersistenceMain {
    private PersistenceMain() { throw new UnsupportedOperationException("Main class"); }

    public static void main(final String[] args) throws InterruptedException, SQLException {
        System.out.println("[serv-c] Persistence Service starting...");
        System.out.println("[serv-c] Tailing queue-c: " + QueuePaths.QUEUE_C);
        System.out.println("[serv-c] JDBC URL: " + PersistenceEventLoop.DEFAULT_JDBC_URL);

        // Start the H2 TCP server to allow external clients to connect to the in-memory database
        final Server h2Server = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9092").start();
        System.out.println("[serv-c] H2 TCP Server started on port 9092.");
        System.out.println("[serv-c] External JDBC URL: jdbc:h2:tcp://localhost:9092/mem:fxdb");

        final PersistenceEventLoop loop = new PersistenceEventLoop(PersistenceEventLoop.DEFAULT_JDBC_URL);

        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().unstarted(() -> {
            System.out.println("[serv-c] Shutdown signal received.");
            loop.stop();
            try { loop.awaitTermination(); loop.close(); }
            catch (final InterruptedException e) { Thread.currentThread().interrupt(); }
            h2Server.stop();
            System.out.println("[serv-c] Stopped. All batches flushed. H2 Server stopped.");
        }));

        loop.start();
        System.out.println("[serv-c] Event loop started on CPU core " + PersistenceEventLoop.CPU_CORE);
        Thread.currentThread().join();
    }
}
