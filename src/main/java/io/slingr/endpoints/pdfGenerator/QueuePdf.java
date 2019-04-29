package io.slingr.endpoints.pdfGenerator;

import io.slingr.endpoints.utils.Json;
import io.slingr.endpoints.ws.exchange.FunctionRequest;

import java.util.LinkedList;
import java.util.Queue;

public class QueuePdf {

    static Queue<FunctionRequest> queue = new LinkedList<>();
    private static QueuePdf queueInstance = null;

    public static QueuePdf getStreamInstance() {

        if (queueInstance == null) {
            queueInstance = new QueuePdf();
        }
        return queueInstance;
    }

    public Queue<FunctionRequest> get() {
        return queue;
    }

    // Inserts the specified element into this queue if it is possible to do so
    // immediately without violating capacity restrictions
    public void add(FunctionRequest value) {
        synchronized (queue) {
            queue.add(value);
        }
    }

    // Removes a single instance of the specified element from this collection
    public void remove(Json value) {
        synchronized (queue) {
            queue.remove(value);
        }
    }

    // Retrieves and removes the head of this queue, or returns null if this
    // queue is empty.
    public FunctionRequest poll() {
        FunctionRequest data = queue.poll();
        return data;
    }

    // Returns true if this collection contains no elements
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    // Returns the number of elements in this collection. If this collection
    // contains more than Integer.MAX_VALUE elements, returns Integer.MAX_VALUE
    public int getTotalSize() {
        return queue.size();
    }

}
