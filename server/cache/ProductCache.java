package server.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Cache LRU para agregações de produtos.
 * Mantém no máximo S séries em memória.
 * Thread-safe para acesso concorrente.
 */
public class ProductCache {
    private final int maxSeries; // S - número máximo de séries em cache
    private final Map<String, CachedAggregation> cache;
    private final ReentrantReadWriteLock lock;
    
    public ProductCache(int maxSeries) {
        if (maxSeries < 1) {
            throw new IllegalArgumentException("maxSeries deve ser >= 1");
        }
        this.maxSeries = maxSeries;
        this.lock = new ReentrantReadWriteLock();
        
        // LinkedHashMap com ordem de acesso (LRU)
        this.cache = new LinkedHashMap<String, CachedAggregation>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CachedAggregation> eldest) {
                return size() > maxSeries;
            }
        };
    }
    
    /**
     * Obtém uma agregação do cache.
     * @param key Chave da agregação
     * @return Agregação ou null se não existir
     */
    public CachedAggregation get(String key) {
        lock.readLock().lock();
        try {
            return cache.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Adiciona ou atualiza uma agregação no cache.
     * @param key Chave da agregação
     * @param aggregation Agregação a guardar
     */
    public void put(String key, CachedAggregation aggregation) {
        lock.writeLock().lock();
        try {
            cache.put(key, aggregation);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Verifica se uma chave existe no cache.
     * @param key Chave a verificar
     * @return true se existe
     */
    public boolean contains(String key) {
        lock.readLock().lock();
        try {
            return cache.containsKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Invalida uma entrada do cache.
     * @param key Chave a remover
     */
    public void invalidate(String key) {
        lock.writeLock().lock();
        try {
            cache.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Invalida todas as entradas que contêm um produto específico.
     * @param product Nome do produto
     */
    public void invalidateProduct(String product) {
        lock.writeLock().lock();
        try {
            cache.entrySet().removeIf(entry -> entry.getKey().contains(product));
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Invalida todas as agregações que dependem de um dia específico.
     * @param dayId ID do dia
     */
    public void invalidateDay(int dayId) {
        lock.writeLock().lock();
        try {
            cache.entrySet().removeIf(entry -> 
                entry.getValue().getLastDayId() >= dayId
            );
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Limpa todo o cache.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Obtém o número de entradas no cache.
     * @return Tamanho do cache
     */
    public int size() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Obtém o número máximo de séries.
     * @return Valor de S
     */
    public int getMaxSeries() {
        return maxSeries;
    }
    
    @Override
    public String toString() {
        lock.readLock().lock();
        try {
            return String.format("ProductCache[size=%d/%d]", cache.size(), maxSeries);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Classe que representa uma agregação em cache.
     */
    public static class CachedAggregation {
        private final Object value;
        private final int lastDayId; // Último dia incluído na agregação
        private final long timestamp;
        
        public CachedAggregation(Object value, int lastDayId) {
            this.value = value;
            this.lastDayId = lastDayId;
            this.timestamp = System.currentTimeMillis();
        }
        
        public Object getValue() {
            return value;
        }
        
        public int getLastDayId() {
            return lastDayId;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        /**
         * Verifica se a agregação é válida para um determinado dia.
         * @param currentDayId ID do dia corrente
         * @return true se ainda é válida
         */
        public boolean isValid(int currentDayId) {
            return lastDayId == currentDayId;
        }
        
        @Override
        public String toString() {
            return String.format("CachedAggregation[value=%s, dayId=%d]", value, lastDayId);
        }
    }
}
