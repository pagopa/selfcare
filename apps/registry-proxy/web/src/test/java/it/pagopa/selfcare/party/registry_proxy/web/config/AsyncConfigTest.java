package it.pagopa.selfcare.party.registry_proxy.web.config;


import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AsyncConfigTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();

    @Test
    void testStorageTaskExecutorBeanCreation() {
        Executor executor = asyncConfig.storageTaskExecutor();

        assertNotNull(executor);
        assertInstanceOf(ThreadPoolTaskExecutor.class, executor);
    }

    @Test
    void testStorageTaskExecutorConfiguration() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.storageTaskExecutor();

        assertEquals(3, executor.getCorePoolSize());
        assertEquals(10, executor.getMaxPoolSize());
        assertEquals(50, executor.getQueueCapacity());
        assertTrue(executor.getThreadNamePrefix().startsWith("StorageAsync-"));
    }

    @Test
    void testStorageTaskExecutorIsInitialized() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.storageTaskExecutor();

        assertNotNull(executor.getThreadPoolExecutor());
    assertFalse(executor.getThreadPoolExecutor().isShutdown());
    }

    @Test
    void testStorageTaskExecutorCanExecuteTasks() throws InterruptedException {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.storageTaskExecutor();
        CountDownLatch latch = new CountDownLatch(1);

        executor.execute(latch::countDown);

        boolean completed = latch.await(1, TimeUnit.SECONDS);
        assertTrue(completed, "Il task non è stato eseguito entro il tempo previsto");

        executor.shutdown();
    }
}
