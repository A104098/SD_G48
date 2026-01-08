package server;

import geral.Protocol.Event;
import java.util.List;

/*
Serviço de agregações lazy COM caching.
Calcula agregações sob demanda e guarda resultados.
Thread-safe para acesso concorrente.
 */
public class AggregationService {
    private final TimeSeriesManager tsManager;
    private final Cache<Object> cache;
    
    public static class AggregationResult<T> {
        public final T value;
        public final String warning;
        
        public AggregationResult(T value, String warning) {
            this.value = value;
            this.warning = warning;
        }
    }

    public AggregationService(TimeSeriesManager tsManager) {
        this.tsManager = tsManager;
        this.cache = new Cache<>();
    }

    // Invalida a cache quando necessário (por ex: novo dia)
    public void invalidateCache() {
        cache.clear();
        System.out.println("Cache de agregação limpa");
    }

    //Agrega quantidade total vendida de um produto nos últimos N dias.
    public AggregationResult<Integer> aggregateQuantity(String product, int days) {
        if (days < 1) {
            return new AggregationResult<>(-1, "Número inválido de dias");
        }
        
        // Check cache
        Integer cached = (Integer) cache.get(product, days, "QTY");
        if (cached != null) {
            System.out.println("Cache hit para QTY " + product + " " + days);
            return new AggregationResult<>(cached, null);
        }
        
        String warning = null;
        
        // Verificar se temos dados suficientes
        int availableDays = tsManager.getHistoricalDayCount();
        if (availableDays < days) {
            warning = "Aviso: Pedido " + days + " dias, mas apenas " + availableDays + " disponíveis. Calculando com os disponíveis.";
        }

        // Calcular agregação iterativa (dia a dia)
        int diasParaCalcular = Math.min(days, availableDays);
        int total = 0;
        
        for (int i = 0; i < diasParaCalcular; i++) {
            List<Event> dayEvents = tsManager.getHistoricalDayEvents(i);
            for (Event event : dayEvents) {
                if (event.getProduct().equals(product)) {
                    total += event.getQuantity();
                }
            }
            // A lista dayEvents sai de escopo aqui e pode ser Garbage Collected
        }
        
        if (availableDays >= days) {
            cache.put(product, days, "QTY", total);
        }

        return new AggregationResult<>(total, warning);
    }
    
    // Agrega volume total de vendas (qty * price)
    public AggregationResult<Double> aggregateVolume(String product, int days) {
        if (days < 1) return new AggregationResult<>(-1.0, "Dias invalidos");

        // Check cache
        Double cached = (Double) cache.get(product, days, "VOL");
        if (cached != null) {
            System.out.println("Cache hit para VOL " + product + " " + days);
            return new AggregationResult<>(cached, null);
        }
        
        String warning = null;
        
        // Verificar disponiveis
        int availableDays = tsManager.getHistoricalDayCount();
        if (availableDays < days) {
            warning = "Aviso: Pedido " + days + " dias, mas apenas " + availableDays + " disponíveis. Calculando com os disponíveis.";
        }

        // Calcular agregação iterativa
        int diasParaCalcular = Math.min(days, availableDays);
        double total = 0;
        
        for (int i = 0; i < diasParaCalcular; i++) {
            List<Event> dayEvents = tsManager.getHistoricalDayEvents(i);
            for (Event event : dayEvents) {
                if (event.getProduct().equals(product)) {
                    total += event.getQuantity() * event.getPrice();
                }
            }
        }
        
        if (availableDays >= days) {
            cache.put(product, days, "VOL", total);
        }
        
        return new AggregationResult<>(total, warning);
    }

    // Agrega preço médio (volume total / quantidade total)
    public AggregationResult<Double> aggregateAvg(String product, int days) {
        if (days < 1) return new AggregationResult<>(-1.0, "Dias invalidos");

        // Check cache
        Double cached = (Double) cache.get(product, days, "AVG");
        if (cached != null) {
            System.out.println("Cache hit para AVG " + product + " " + days);
            return new AggregationResult<>(cached, null);
        }
        
        String warning = null;
        
        // Verificar disponiveis
        int availableDays = tsManager.getHistoricalDayCount();
        if (availableDays < days) {
            warning = "Aviso: Pedido " + days + " dias, mas apenas " + availableDays + " disponíveis. Calculando com os disponíveis.";
        }

        // Calcular agregação iterativa
        int diasParaCalcular = Math.min(days, availableDays);
        double totalVolume = 0;
        int totalQuantity = 0;

        for (int i = 0; i < diasParaCalcular; i++) {
            List<Event> dayEvents = tsManager.getHistoricalDayEvents(i);
            for (Event event : dayEvents) {
                if (event.getProduct().equals(product)) {
                    totalVolume += event.getQuantity() * event.getPrice();
                    totalQuantity += event.getQuantity();
                }
            }
        }

        double avgPrice = (totalQuantity == 0) ? 0.0 : (totalVolume / totalQuantity);
        
        if (availableDays >= days) {
            cache.put(product, days, "AVG", avgPrice);
        }

        return new AggregationResult<>(avgPrice, warning);
    }
    
    // Agrega preço máximo
    public AggregationResult<Double> aggregateMax(String product, int days) {
        if (days < 1) return new AggregationResult<>(-1.0, "Dias invalidos");
        
        // Check cache
        Double cached = (Double) cache.get(product, days, "MAX");
        if (cached != null) {
            System.out.println("Cache hit para MAX " + product + " " + days);
            return new AggregationResult<>(cached, null);
        }

        String warning = null;
        
        // Verificar disponiveis
        int availableDays = tsManager.getHistoricalDayCount();
        if (availableDays < days) {
            warning = "Aviso: Pedido " + days + " dias, mas apenas " + availableDays + " disponíveis. Calculando com os disponíveis.";
        }

        // Calcular agregação iterativa
        int diasParaCalcular = Math.min(days, availableDays);
        double maxPrice = Double.NEGATIVE_INFINITY;
        boolean foundProduct = false;

        for (int i = 0; i < diasParaCalcular; i++) {
            List<Event> dayEvents = tsManager.getHistoricalDayEvents(i);
            for (Event event : dayEvents) {
                if (event.getProduct().equals(product)) {
                    maxPrice = Math.max(maxPrice, event.getPrice());
                    foundProduct = true;
                }
            }
        }
        
        double result = foundProduct ? maxPrice : 0.0;
        
        if (availableDays >= days) {
            cache.put(product, days, "MAX", result);
        }

        return new AggregationResult<>(result, warning);
    }


}
