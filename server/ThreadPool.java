package server;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadPool {
    private final int nThreads;
    private final List<Runnable> queue;
    private final Worker[] workers;
    
    // Primitivas de Sincronização
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    
    private volatile boolean isStopped = false;

    public ThreadPool(int nThreads) {
        this.nThreads = nThreads;
        this.queue = new LinkedList<>();
        this.workers = new Worker[nThreads];

        lock.lock();
        try {
            // Inicializa e arranca as threads
            for (int i = 0; i < nThreads; i++) {
                workers[i] = new Worker();
                workers[i].start();
            }
        } finally {
            lock.unlock();
        }
    }

    public void execute(Runnable task) {
        lock.lock();
        try {
            if (isStopped) {
                throw new IllegalStateException("ThreadPool is stopped");
            }
            queue.add(task);

            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public void stop() {
        lock.lock();
        try {
            isStopped = true;
            // Aqui usamos signalAll para garantir que todos os workers acordam para ver que a pool parou e terminarem.
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private class Worker extends Thread {
        @Override
        public void run() {
            while (true) {
                Runnable task;
                
                lock.lock();
                try {
                    // Enquanto não houver tarefas e a pool não tiver parado, espera.
                    while (queue.isEmpty() && !isStopped) {
                        try {
                            notEmpty.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    // Se a pool parou e a fila está vazia, termina a thread
                    if (isStopped && queue.isEmpty()) {
                        return;
                    }
                    // Retira a tarefa da fila (FIFO)
                    task = queue.remove(0);
                    
                } finally {
                    lock.unlock();
                }

                // Executa a tarefa fora da secção crítica para permitir que outras threads acessem a fila (concorrência real).
                try {
                    task.run();
                } catch (RuntimeException e) {
                    System.err.println("Erro na execução da tarefa: " + e.getMessage());
                }
            }
        }
    }
}
