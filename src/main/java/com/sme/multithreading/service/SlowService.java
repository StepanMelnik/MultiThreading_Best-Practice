package com.sme.multithreading.service;

import static com.sme.multithreading.util.ThreadUtil.sleepInMilliSeconds;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sme.multithreading.model.Message;

/**
 * Slow service implementation.
 */
public class SlowService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SlowService.class);
    private static final int ONE_THOUSAND = 1_000;

    private final Random random;

    public SlowService()
    {
        random = new Random();
    }

    /**
     * Fetch a message with delay.
     * 
     * @return Returns {@link Message} by id.
     */
    public Message getMessage(int id)
    {
        int delay = random.nextInt(ONE_THOUSAND);
        sleepInMilliSeconds(delay, s -> LOGGER.error(s, Thread.currentThread().getName()), "{} thread is interrupted");
        return new Message(id, delay, "Slow service message");
    }
}
