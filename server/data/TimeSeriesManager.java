package server.data;

import geral.Protocol.Event;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Gestor de séries temporais.
 * Gere o dia corrente e os D dias anteriores.
 * Thread-safe para operações concorrentes.
 */
public class TimeSeriesManager {
    private final int maxDays; // D - número máximo de dias históricos
    private final List<TimeSeries> historicalSeries; // Dias completos
    private TimeSeries currentDay; // Dia corrente
    private int currentDayId; // ID do dia corrente
    private final ReentrantReadWriteLock lock;
    
    public TimeSeriesManager(int maxDays) {
        if (maxDays < 1) {
            throw new IllegalArgumentException("maxDays deve ser >= 1");
        }
        this.maxDays = maxDays;
        this.historicalSeries = new ArrayList<>();
        this.currentDayId = 0;
        this.currentDay = new TimeSeries(currentDayId);
        this.lock = new ReentrantReadWriteLock();
    }
    
    /**
     * Adiciona um evento ao dia corrente.
     * @param product Nome do produto
     * @param quantity Quantidade vendida
     * @param price Preço de venda
     */
    public void addEvent(String product, int quantity, double price) {
        lock.readLock().lock();
        try {
            Event event = new Event(product, quantity, price);
            currentDay.addEvent(event);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Adiciona um evento ao dia corrente.
     * @param event Evento a adicionar
     */
    public void addEvent(Event event) {
        lock.readLock().lock();
        try {
            currentDay.addEvent(event);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Começa um novo dia.
     * Move o dia corrente para o histórico e cria um novo dia.
     */
    public void newDay() {
        lock.writeLock().lock();
        try {
            // Completar o dia atual
            currentDay.complete();
            
            // Adicionar ao histórico
            historicalSeries.add(0, currentDay); // Adiciona no início
            
            // Remover dias antigos se exceder o limite
            while (historicalSeries.size() > maxDays) {
                historicalSeries.remove(historicalSeries.size() - 1);
            }
            
            // Criar novo dia
            currentDayId++;
            currentDay = new TimeSeries(currentDayId);
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Obtém o dia corrente.
     * @return Série temporal do dia corrente
     */
    public TimeSeries getCurrentDay() {
        lock.readLock().lock();
        try {
            return currentDay;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Obtém uma série temporal por offset.
     * @param dayOffset Offset do dia (1 = ontem, 2 = anteontem, etc)
     * @return TimeSeries ou null se não existir
     */
    public TimeSeries getSeries(int dayOffset) {
        if (dayOffset < 1 || dayOffset > maxDays) {
            return null;
        }
        
        lock.readLock().lock();
        try {
            int index = dayOffset - 1;
            if (index < historicalSeries.size()) {
                return historicalSeries.get(index);
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Obtém múltiplas séries temporais.
     * @param days Número de dias anteriores (1 a D)
     * @return Lista de séries temporais (do mais recente para o mais antigo)
     */
    public List<TimeSeries> getSeriesList(int days) {
        if (days < 1) {
            return new ArrayList<>();
        }
        
        lock.readLock().lock();
        try {
            int count = Math.min(days, historicalSeries.size());
            List<TimeSeries> result = new ArrayList<>(count);
            
            for (int i = 0; i < count; i++) {
                result.add(historicalSeries.get(i));
            }
            
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Obtém o ID do dia corrente.
     * @return ID do dia
     */
    public int getCurrentDayId() {
        lock.readLock().lock();
        try {
            return currentDayId;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Obtém o número máximo de dias históricos.
     * @return Valor de D
     */
    public int getMaxDays() {
        return maxDays;
    }
    
    /**
     * Obtém o número de dias históricos atualmente armazenados.
     * @return Número de dias (excluindo o dia corrente)
     */
    public int getHistoricalDayCount() {
        lock.readLock().lock();
        try {
            return historicalSeries.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Obtém todos os dias históricos.
     * @return Lista de séries históricas (cópia)
     */
    public List<TimeSeries> getAllHistoricalSeries() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(historicalSeries);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Limpa todas as séries (útil para testes).
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            historicalSeries.clear();
            currentDayId = 0;
            currentDay = new TimeSeries(currentDayId);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public String toString() {
        lock.readLock().lock();
        try {
            return String.format("TimeSeriesManager[currentDay=%d, historical=%d/%d, currentEvents=%d]",
                currentDayId, historicalSeries.size(), maxDays, currentDay.getEventCount());
        } finally {
            lock.readLock().unlock();
        }
    }
}
