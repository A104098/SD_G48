package server.persistence;

import geral.Protocol.Event;
import geral.Serializer;
import java.io.*;
import java.util.List;
import server.TimeSeriesManager;

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
            out.writeInt(2); // Versão 2 (sem TimeSeries)
            
            // Configuração
            out.writeInt(manager.getMaxDays());
            out.writeInt(manager.getCurrentDayId());
            
            // Dia corrente
            List<Event> currentDayEvents = manager.getCurrentDayEvents();
            writeEventList(out, currentDayEvents);
            
            // Dias históricos
            List<List<Event>> allEvents = manager.getAllEvents(manager.getHistoricalDayCount());
            out.writeInt(allEvents.size());
            
            for (List<Event> dayEvents : allEvents) {
                writeEventList(out, dayEvents);
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
            if (version == 1) {
                // Formato antigo com TimeSeries - não suportado
                throw new IOException("Formato antigo não suportado. Delete o ficheiro e reinicie.");
            } else if (version != 2) {
                throw new IOException("Versão não suportada: " + version);
            }
            
            // Configuração
            int maxDays = in.readInt();
            in.readInt(); // currentDayId é gerido internamente
            
            // Criar manager
            TimeSeriesManager manager = new TimeSeriesManager(maxDays);
            
            // Carregar dia corrente
            List<Event> currentDayEvents = readEventList(in);
            
            // Carregar dias históricos
            int historicalCount = in.readInt();
            
            // Restaurar estado
            // Para cada dia histórico, adicionar eventos e avançar dia
            for (int i = 0; i < historicalCount; i++) {
                List<Event> dayEvents = readEventList(in);
                
                // Adicionar eventos ao dia corrente
                for (Event event : dayEvents) {
                    manager.addEvent(event);
                }
                
                // Avançar para próximo dia
                manager.newDay();
            }
            
            // Adicionar eventos do dia corrente final
            for (Event event : currentDayEvents) {
                manager.addEvent(event);
            }
            
            return manager;
        }
    }
    
    /**
     * Escreve uma lista de eventos.
     */
    private void writeEventList(DataOutputStream out, List<Event> events) throws IOException {
        out.writeInt(events.size());
        
        for (Event event : events) {
            writeEvent(out, event);
        }
    }
    
    /**
     * Lê uma lista de eventos.
     */
    private List<Event> readEventList(DataInputStream in) throws IOException {
        int eventCount = in.readInt();
        List<Event> events = new java.util.ArrayList<>();
        
        for (int i = 0; i < eventCount; i++) {
            Event event = readEvent(in);
            events.add(event);
        }
        
        return events;
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
