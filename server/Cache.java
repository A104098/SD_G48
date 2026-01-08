package server;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Cache<T> {
    
    // Chave composta da cache: Produto + Número de dias + Tipo de agregação
    private static class CacheKey {
        final String product;
        final int days;
        final String type;
        
        CacheKey(String product, int days, String type) {
            this.product = product;
            this.days = days;
            this.type = type;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return days == cacheKey.days && 
                   Objects.equals(product, cacheKey.product) && 
                   Objects.equals(type, cacheKey.type);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(product, days, type);
        }
    }
    
    // Map para guardar os resultados
    private final Map<CacheKey, T> cacheMap;
    private final ReentrantReadWriteLock lock;
    
    public Cache() {
        this.cacheMap = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }
    
    public void put(String product, int days, String type, T value) {
        lock.writeLock().lock();
        try {
            cacheMap.put(new CacheKey(product, days, type), value);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public T get(String product, int days, String type) {
        lock.readLock().lock();
        try {
            return cacheMap.get(new CacheKey(product, days, type));
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public void clear() {
        lock.writeLock().lock();
        try {
            cacheMap.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
