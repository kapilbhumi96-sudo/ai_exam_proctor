package com.proctor.capture;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Bounded, thread-safe queue between the capture thread (producer) and the
 * processing thread (consumer).
 *
 * Policy: when the queue is full, the OLDEST item is dropped. For a live
 * proctoring feed, the newest frame is always more useful than a stale one,
 * and dropping keeps memory and latency bounded.
 *
 * Generic so it can be unit-tested without OpenCV.
 */
public class FrameQueue<T> {

    private final ArrayBlockingQueue<T> queue;
    private final Consumer<T> onDrop;
    private final AtomicLong dropped = new AtomicLong();

    /**
     * @param capacity maximum number of items held
     * @param onDrop   called for every item removed without being consumed
     *                 (use it to release native memory)
     */
    public FrameQueue(int capacity, Consumer<T> onDrop) {
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.onDrop = onDrop;
    }

    /** Adds an item; if the queue is full, drops the oldest item(s) to make room. */
    public void offer(T item) {
        while (!queue.offer(item)) {
            T oldest = queue.poll();
            if (oldest != null) {
                dropped.incrementAndGet();
                onDrop.accept(oldest);
            }
        }
    }

    /** Waits up to the timeout for the next item; returns null if none arrived. */
    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }

    public int size() {
        return queue.size();
    }

    public long droppedCount() {
        return dropped.get();
    }

    /** Removes everything still waiting, passing each item to onDrop. */
    public void clear() {
        T item;
        while ((item = queue.poll()) != null) {
            onDrop.accept(item);
        }
    }
}
