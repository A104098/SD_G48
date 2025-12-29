package server.data;

import geral.Protocol.Event;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Série temporal de eventos de um dia.
 * Thread-safe para adições e leituras concorrentes.
 */
public class TimeSeries {
    private final int dayId;
    private final List<Event> events;
    private final ReentrantReadWriteLock lock;
    private boolean completed; // true quando o dia terminou
    private final long startTime;
    
    public TimeSeries(int dayId) {
        this.dayId = dayId;
        this.events = new ArrayList<>();
        this.lock = new ReentrantReadWriteLock();
        this.completed = false;
        this.startTime = System.currentTimeMillis();
    }
    
    public int getDayId() {
        return dayId;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public boolean isCompleted() {
        lock.readLock().lock();
        try {
            return completed;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Marca este dia como completo (não podem ser adicionados mais eventos).
     */
    public void complete() {
        lock.writeLock().lock();
        try {
            completed = true;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Adiciona um evento à série temporal.
     * @param event Evento a adicionar
     * @throws IllegalStateException se o dia já estiver completo
     */
    public void addEvent(Event event) {
        lock.writeLock().lock();
        try {
            if (completed) {
                throw new IllegalStateException("Não é possível adicionar eventos a um dia completo");
            }
            events.add(event);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Obtém todos os eventos da série.
     * @return Lista com cópia dos eventos
     */
    public List<Event> getEvents() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(events);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Obtém eventos de um produto específico.
     * @param product Nome do produto
     * @return Lista de eventos do produto
     */
    public List<Event> getEventsByProduct(String product) {
        lock.readLock().lock();
        try {
            List<Event> result = new ArrayList<>();
            for (Event event : events) {
                if (event.getProduct().equals(product)) {
                    result.add(event);
                }
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Filtra eventos por um conjunto de produtos.
     * @param products Conjunto de produtos a filtrar
     * @return Lista de eventos que pertencem ao conjunto
     */
    public List<Event> filterByProducts(List<String> products) {
        lock.readLock().lock();
        try {
            List<Event> result = new ArrayList<>();
            for (Event event : events) {
                if (products.contains(event.getProduct())) {
                    result.add(event);
                }
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Obtém o número total de eventos.
     * @return Número de eventos
     */
    public int getEventCount() {
        lock.readLock().lock();
        try {
            return events.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Verifica se existe pelo menos um evento de um produto.
     * @param product Nome do produto
     * @return true se existe pelo menos um evento
     */
    public boolean hasProduct(String product) {
        lock.readLock().lock();
        try {
            for (Event event : events) {
                if (event.getProduct().equals(product)) {
                    return true;
                }
            }
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Verifica se ambos os produtos foram vendidos neste dia.
     * @param product1 Primeiro produto
     * @param product2 Segundo produto
     * @return true se ambos foram vendidos
     */
    public boolean hasBothProducts(String product1, String product2) {
        lock.readLock().lock();
        try {
            boolean found1 = false;
            boolean found2 = false;
            
            for (Event event : events) {
                if (event.getProduct().equals(product1)) found1 = true;
                if (event.getProduct().equals(product2)) found2 = true;
                if (found1 && found2) return true;
            }
            
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Encontra a sequência máxima de vendas consecutivas de um produto.
     * @param product Nome do produto
     * @return Tamanho da maior sequência consecutiva
     */
    public int getMaxConsecutive(String product) {
        lock.readLock().lock();
        try {
            int maxConsecutive = 0;
            int currentConsecutive = 0;
            
            for (Event event : events) {
                if (event.getProduct().equals(product)) {
                    currentConsecutive++;
                    maxConsecutive = Math.max(maxConsecutive, currentConsecutive);
                } else {
                    currentConsecutive = 0;
                }
            }
            
            return maxConsecutive;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public String toString() {
        lock.readLock().lock();
        try {
            return String.format("TimeSeries[day=%d, events=%d, completed=%s]",
                dayId, events.size(), completed);
        } finally {
            lock.readLock().unlock();
        }
    }
}
