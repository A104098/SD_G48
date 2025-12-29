package server.persistence;

import geral.Protocol.Event;
import geral.Serializer;
import server.data.TimeSeries;
import server.data.TimeSeriesManager;
import java.io.*;
import java.util.List;

/**
 * Persistência de séries temporais.
 * Guarda e carrega séries temporais em formato binário.
 */
public class TimeSeriesPersistence {
    private final String filePath;
    
    public TimeSeriesPersistence(String filePath) {
        this.filePath = filePath;
    }
    
    /**
     * Guarda o TimeSeriesManager no disco.
     * @param manager Gestor de séries temporais
     * @throws IOException se falhar a escrita
     */
    public void save(TimeSeriesManager manager) throws IOException {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {
            
            // Header
            out.writeInt(0x54494D45); // "TIME" magic number
            out.writeInt(1); // Versão
            
            // Configuração
            out.writeInt(manager.getMaxDays());
            out.writeInt(manager.getCurrentDayId());
            
            // Dia corrente
            TimeSeries currentDay = manager.getCurrentDay();
            writeSeries(out, currentDay);
            
            // Dias históricos
            List<TimeSeries> historical = manager.getAllHistoricalSeries();
            out.writeInt(historical.size());
            
            for (TimeSeries series : historical) {
                writeSeries(out, series);
            }
        }
    }
    
    /**
     * Carrega o TimeSeriesManager do disco.
     * @return Manager carregado ou null se não existir
     * @throws IOException se falhar a leitura
     */
    public TimeSeriesManager load() throws IOException {
        File file = new File(filePath);
        
        if (!file.exists()) {
            return null;
        }
        
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            
            // Verificar header
            int magic = in.readInt();
            if (magic != 0x54494D45) {
                throw new IOException("Ficheiro inválido (magic number incorreto)");
            }
            
            int version = in.readInt();
            if (version != 1) {
                throw new IOException("Versão não suportada: " + version);
            }
            
            // Configuração
            int maxDays = in.readInt();
            int currentDayId = in.readInt();
            
            // Criar manager
            TimeSeriesManager manager = new TimeSeriesManager(maxDays);
            
            // Carregar dia corrente
            TimeSeries currentDay = readSeries(in);
            
            // Carregar dias históricos
            int historicalCount = in.readInt();
            
            // Restaurar estado
            // Como não temos setters, precisamos simular os dias
            // Para cada dia histórico, avançamos um dia
            for (int i = 0; i < historicalCount; i++) {
                TimeSeries historical = readSeries(in);
                
                // Adicionar eventos do dia histórico ao dia corrente
                for (Event event : historical.getEvents()) {
                    manager.addEvent(event);
                }
                
                // Avançar para próximo dia
                if (i < historicalCount - 1 || currentDay.getEventCount() > 0) {
                    manager.newDay();
                }
            }
            
            // Adicionar eventos do dia corrente
            for (Event event : currentDay.getEvents()) {
                manager.addEvent(event);
            }
            
            return manager;
        }
    }
    
    /**
     * Escreve uma série temporal.
     */
    private void writeSeries(DataOutputStream out, TimeSeries series) throws IOException {
        // Metadados
        out.writeInt(series.getDayId());
        out.writeLong(series.getStartTime());
        out.writeBoolean(series.isCompleted());
        
        // Eventos
        List<Event> events = series.getEvents();
        out.writeInt(events.size());
        
        for (Event event : events) {
            writeEvent(out, event);
        }
    }
    
    /**
     * Lê uma série temporal.
     */
    private TimeSeries readSeries(DataInputStream in) throws IOException {
        // Metadados
        int dayId = in.readInt();
        long startTime = in.readLong();
        boolean completed = in.readBoolean();
        
        TimeSeries series = new TimeSeries(dayId);
        
        // Eventos
        int eventCount = in.readInt();
        
        for (int i = 0; i < eventCount; i++) {
            Event event = readEvent(in);
            series.addEvent(event);
        }
        
        if (completed) {
            series.complete();
        }
        
        return series;
    }
    
    /**
     * Escreve um evento.
     */
    private void writeEvent(DataOutputStream out, Event event) throws IOException {
        Serializer.writeString(out, event.getProduct());
        out.writeInt(event.getQuantity());
        out.writeDouble(event.getPrice());
    }
    
    /**
     * Lê um evento.
     */
    private Event readEvent(DataInputStream in) throws IOException {
        String product = Serializer.readString(in);
        int quantity = in.readInt();
        double price = in.readDouble();
        return new Event(product, quantity, price);
    }
}
