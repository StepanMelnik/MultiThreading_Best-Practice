package com.sme.multithreading.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sme.multithreading.model.Message;

/**
 * Integration tests to work with {@link SlowService}.
 */
public class SlowServiceIntegrationTest
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SlowServiceIntegrationTest.class);

    private final SlowService slowService = new SlowService();
    private final StopWatch stopWatch = new StopWatch();

    @BeforeEach
    void setUp()
    {
        stopWatch.reset();
    }

    /**
     * <pre>
     * Fetch messages step by step from slow services.
     * The result takes 53 seconds on my machine.
     * </pre>
     */
    @Test
    void testGetMessagesInSequentialComputation() throws Exception
    {
        final int count = 100;

        List<Message> list = new ArrayList<>();

        stopWatch.start();
        IntStream.range(0, count).forEach(id ->
        {
            list.add(slowService.getMessage(id));
        });
        stopWatch.stop();

        LOGGER.debug("Time in seconds: " + stopWatch.getTime(TimeUnit.SECONDS));
        assertEquals(count, list.size());
    }

    /**
     * <pre>
     * Fetch messages in parallel from slow services.
     * {@link Executors#newCachedThreadPool} pool creates new threads as needed, but reuses previously constructed threads when they are available.
     * 
     * The result takes 996 milliseconds on my machine.
     * </pre>
     */
    @Test
    void testGetMessagesInThreadPool() throws Exception
    {
        final int count = 100;

        runInThreadPool(count, 1_500, Executors.newCachedThreadPool());
        stopWatch.reset();
        runInThreadPool(count, 10_000, Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
        stopWatch.reset();
        runInThreadPool(count, 10_000, Executors.newWorkStealingPool());
    }

    private void runInThreadPool(final int count, final int timeout, ExecutorService executorService) throws InterruptedException
    {
        List<Callable<Message>> tasks = new ArrayList<>();

        stopWatch.start();
        IntStream.range(0, count).forEach(id ->
        {
            tasks.add(() -> slowService.getMessage(id));
        });

        List<Future<Message>> futureResult = executorService.invokeAll(tasks, timeout, TimeUnit.MILLISECONDS);
        executorService.shutdown();

        List<Message> result = futureResult.stream() // we do ot use parallel stream here, because we do not expect a lot of rows
                .map(t ->
                {
                    try
                    {
                        return t.get();
                    }
                    catch (InterruptedException | ExecutionException e)
                    {
                        LOGGER.error("Cannot fetch a value", e);
                        throw new RuntimeException(e);
                    }
                })
                .sorted(Comparator.comparing(Message::getDelay)) // extra operation to check the slowest request
                .collect(Collectors.toList());

        stopWatch.stop();

        LOGGER.debug("Time in seconds: " + stopWatch.getTime(TimeUnit.MILLISECONDS));
        assertEquals(count, result.size());

        Message slowestMessage = result.get(count - 1);
        LOGGER.debug("Get the slowest {} message", slowestMessage);

        assertTrue(stopWatch.getTime(TimeUnit.MILLISECONDS) > slowestMessage.getDelay());
    }

    /**
     * <pre>
     * Parallel stream based on Fork-Join framework.
     * 
     * The result takes 4562 milliseconds on my machine.
     * </pre>
     */
    @Test
    void testParallelStream() throws Exception
    {
        final int count = 100;

        stopWatch.start();

        List<Message> result = IntStream.range(0, count)
                .parallel()
                .mapToObj(id -> slowService.getMessage(id))
                .sorted(Comparator.comparing(Message::getDelay)) // extra operation to check the slowest request
                .collect(Collectors.toList());

        stopWatch.stop();

        LOGGER.debug("Time in seconds: " + stopWatch.getTime(TimeUnit.MILLISECONDS));
        assertEquals(count, result.size());

        Message slowestMessage = result.get(count - 1);
        LOGGER.debug("Get the slowest {} message", slowestMessage);

        assertTrue(stopWatch.getTime(TimeUnit.MILLISECONDS) > slowestMessage.getDelay());
    }

    /**
     * <pre>
     * Test call on Fork-Join framework:
     * - ForkJoinPool.commonPool() takes 6015 ms to perform logic;
     * - new ForkJoinPool(100 / 2) takes 1755 ms  to perform logic;
     * </pre>
     */
    @Test
    void testGetMessageInForkJoin() throws Exception
    {
        final int count = 100;

        runInForkJoinPool(count, ForkJoinPool.commonPool());
        stopWatch.reset();
        runInForkJoinPool(count, new ForkJoinPool(count / 2));
    }

    private void runInForkJoinPool(final int count, ForkJoinPool forkJoinPool)
    {
        stopWatch.start();

        MessageRecursiveTask recursiveTask = new MessageRecursiveTask(IntStream.range(0, count).toArray());

        List<Message> result = forkJoinPool.invoke(recursiveTask); // fork and join
        stopWatch.stop();

        LOGGER.debug("Time in seconds: " + stopWatch.getTime(TimeUnit.MILLISECONDS));
        assertEquals(count, result.size());

        Message slowestMessage = result.get(count - 1);
        LOGGER.debug("Get the slowest {} message", slowestMessage);

        assertTrue(stopWatch.getTime(TimeUnit.MILLISECONDS) > slowestMessage.getDelay());
    }

    /**
     * {@link RecursiveTask} implementation to run in Fork-Join pool recursively.
     */
    private class MessageRecursiveTask extends RecursiveTask<List<Message>>
    {
        private final int[] ids;

        MessageRecursiveTask(int[] ids)
        {
            this.ids = ids;
        }

        @Override
        protected List<Message> compute()
        {
            List<Message> list = new ArrayList<>();

            if (ids.length > 1)
            {
                Collection<MessageRecursiveTask> collection = ForkJoinTask.invokeAll(createSubtasks());   // schedule all for asynchronous execution
                list.addAll(collection.stream()
                        .map(ForkJoinTask::join)
                        .flatMap(List::stream)
                        .collect(Collectors.toList()));
            }
            else
            {
                list.add(process(ids[0]));
            }

            return list;
        }

        private Message process(int id)
        {
            return slowService.getMessage(id);
        }

        private List<MessageRecursiveTask> createSubtasks()
        {
            List<MessageRecursiveTask> subTasks = new ArrayList<>();

            int length = ids.length;
            int[] left = Arrays.copyOfRange(ids, 0, (length + 1) / 2);
            int[] right = Arrays.copyOfRange(ids, (length + 1) / 2, length);

            subTasks.add(new MessageRecursiveTask(left));
            subTasks.add(new MessageRecursiveTask(right));

            return subTasks;
        }
    }
}
