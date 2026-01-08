package tests;

import client.Client;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestScalability {
    private static final String HOST = "localhost";
    private static final int PORT = 12345;
    
    public static void main(String[] args) {
        int numClients = 10;
        int durationSeconds = 30;
        
        if (args.length > 0) numClients = Integer.parseInt(args[0]);
        if (args.length > 1) durationSeconds = Integer.parseInt(args[1]);
        
        System.out.println("=== Teste de Escalabilidade ===");
        System.out.println("Clientes: " + numClients);
        System.out.println("Duração: " + durationSeconds + "s");
        
        AtomicLong totalOps = new AtomicLong(0);
        AtomicBoolean running = new AtomicBoolean(true);
        Thread[] threads = new Thread[numClients];
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numClients; i++) {
            final int id = i;
            threads[i] = new Thread(() -> {
                try {
                    Client client = new Client(HOST, PORT);
                    client.connect();
                    
                    String user = "user" + id + "_" + System.nanoTime();
                    String pass = "pass";
                    
                    client.register(user, pass);
                    if (!client.login(user, pass)) {
                        System.err.println("Cliente " + id + " falhou login");
                        return;
                    }
                    
                    Random rand = new Random();
                    String[] products = {"ProdA", "ProdB", "ProdC", "ProdD", "ProdE"};
                    
                    while (running.get()) {
                        int op = rand.nextInt(10);
                        String prod = products[rand.nextInt(products.length)];
                        
                        try {
                            if (op < 6) { // 60% Writes
                                client.addEvent(prod, rand.nextInt(10) + 1, rand.nextDouble() * 100);
                            } else { // 40% Reads
                                int days = rand.nextInt(5) + 1;
                                switch (rand.nextInt(4)) {
                                    case 0: client.aggregateQuantity(prod, days); break;
                                    case 1: client.aggregateVolume(prod, days); break;
                                    case 2: client.aggregateAverage(prod, days); break;
                                    case 3: client.aggregateMaxPrice(prod, days); break;
                                }
                            }
                            totalOps.incrementAndGet();
                        } catch (Exception e) {
                            System.err.println("Erro na op: " + e.getMessage());
                        }
                    }
                    
                    client.logout();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            threads[i].start();
        }
        
        try {
            Thread.sleep(durationSeconds * 1000L);
        } catch (InterruptedException e) {}
        
        running.set(false);
        
        for (Thread t : threads) {
            try { t.join(); } catch (InterruptedException e) {}
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        long ops = totalOps.get();
        
        System.out.println("=== Resultados ===");
        System.out.println("Total Operações: " + ops);
        System.out.println("Tempo Total: " + duration + "ms");
        System.out.println("Throughput: " + (ops * 1000.0 / duration) + " ops/s");
    }
}
