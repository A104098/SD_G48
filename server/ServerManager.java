package server;

import java.util.List;
import geral.Protocol;

// Facade principal do servidor.
// Centraliza o acesso a todos os serviços (Autenticação, TimeSeries, Agregação).
public class ServerManager {
    private final Authentication auth;
    private final TimeSeriesManager tsManager;
    private final AggregationService aggregationService;
    
    public ServerManager(Authentication auth, TimeSeriesManager tsManager, AggregationService aggregationService) {
        this.auth = auth;
        this.tsManager = tsManager;
        this.aggregationService = aggregationService;
    }
    
    // ==================== AUTHENTICATION ====================
    
    public boolean register(String username, String password) {
        return auth.register(username, password);
    }
    
    public User authenticate(String username, String password) {
        return auth.authenticate(username, password);
    }
    
    public int getUserCount() {
        return auth.getUserCount();
    }
    
    // ==================== TIME SERIES ====================
    
    public void addEvent(String product, int quantity, double price) {
        tsManager.addEvent(product, quantity, price);
    }
    
    public List<Protocol.Event> getFilteredEvents(List<String> products, Integer dayOffset) {
        return tsManager.getFilteredEvents(products, dayOffset);
    }
    
    public boolean waitForSimultaneousSales(String product1, String product2) {
        return tsManager.waitForSimultaneousSales(product1, product2);
    }
    
    public String waitForConsecutiveSales(Integer n) {
        return tsManager.waitForConsecutiveSales(n);
    }
    
    // ==================== AGGREGATION ====================
    
    public AggregationService.AggregationResult<Integer> aggregateQuantity(String product, int days) {
        return aggregationService.aggregateQuantity(product, days);
    }
    
    public AggregationService.AggregationResult<Double> aggregateVolume(String product, int days) {
        return aggregationService.aggregateVolume(product, days);
    }
    
    public AggregationService.AggregationResult<Double> aggregateAveragePrice(String product, int days) {
        return aggregationService.aggregateAvg(product, days);
    }
    
    public AggregationService.AggregationResult<Double> aggregateMaxPrice(String product, int days) {
        return aggregationService.aggregateMax(product, days);
    }
}
