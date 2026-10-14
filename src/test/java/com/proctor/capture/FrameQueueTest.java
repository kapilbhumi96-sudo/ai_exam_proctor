package com.proctor.capture;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FrameQueueTest {

    @Test
    void keepsNewestItemsAndDropsOldestWhenFull() throws Exception {
        List<Integer> dropped = new ArrayList<>();
        FrameQueue<Integer> queue = new FrameQueue<>(3, dropped::add);

        for (int i = 1; i <= 5; i++) {
            queue.offer(i);
        }

        assertEquals(List.of(1, 2), dropped);
        assertEquals(2, queue.droppedCount());
        assertEquals(3, queue.size());
        assertEquals(3, queue.poll(10, TimeUnit.MILLISECONDS));
        assertEquals(4, queue.poll(10, TimeUnit.MILLISECONDS));
        assertEquals(5, queue.poll(10, TimeUnit.MILLISECONDS));
    }

    @Test
    void pollReturnsNullWhenEmpty() throws Exception {
        FrameQueue<Integer> queue = new FrameQueue<>(2, x -> { });
        assertNull(queue.poll(10, TimeUnit.MILLISECONDS));
    }

    @Test
    void clearPassesRemainingItemsToDropCallback() {
        List<Integer> released = new ArrayList<>();
        FrameQueue<Integer> queue = new FrameQueue<>(4, released::add);
        queue.offer(1);
        queue.offer(2);

        queue.clear();

        assertEquals(List.of(1, 2), released);
        assertEquals(0, queue.size());
    }

    @Test
    void preservesOrderBelowCapacity() throws Exception {
        FrameQueue<String> queue = new FrameQueue<>(5, x -> { });
        queue.offer("a");
        queue.offer("b");
        assertEquals("a", queue.poll(10, TimeUnit.MILLISECONDS));
        assertEquals("b", queue.poll(10, TimeUnit.MILLISECONDS));
        assertEquals(0, queue.droppedCount());
    }
}
