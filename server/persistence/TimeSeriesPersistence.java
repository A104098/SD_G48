package server.persistence;

import geral.Protocol.Event;
import geral.Serializer;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import server.TimeSeriesManager;

/**
 * Persistência de séries temporais com um ficheiro por dia.
 */
public class TimeSeriesPersistence {
    private final File baseDir;
    private static final String METADATA_FILE = "metadata.dat";
    private static final String CURRENT_DAY_FILE = "current.dat";
    private static final Pattern DAY_FILE_PATTERN = Pattern.compile("day_(\\d+)\\.dat");
    
    public TimeSeriesPersistence(String dirPath) {
        this.baseDir = new File(dirPath);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }

    /**
     * Guarda um dia específico no disco.
     */
    public void saveDay(int dayId, List<Event> events) throws IOException {
        File dayFile = new File(baseDir, String.format("day_%d.dat", dayId));
        writeEventFile(dayFile, events);
    }

    /**
     * Lê um dia específico do disco.
     * Retorna lista vazia se não existir.
     */
    public List<Event> loadDay(int dayId) throws IOException {
        File dayFile = new File(baseDir, String.format("day_%d.dat", dayId));
        if (!dayFile.exists()) {
            return new ArrayList<>();
        }
        return readEventFile(dayFile);
    }
    
    /**
     * Apaga um dia do disco (para limpar dias > D).
     */
    public void deleteDay(int dayId) {
        File dayFile = new File(baseDir, String.format("day_%d.dat", dayId));
        if (dayFile.exists()) {
            dayFile.delete();
        }
    }
    
    /**
     * Guarda o TimeSeriesManager no disco (Metadata e Current Day).
     * Os dias históricos já devem ser persistidos incrementalmente em newDay().
     */
    public void saveState(TimeSeriesManager manager) throws IOException {
        // 1. Guardar Metadata
        writeMetadata(manager);
        
        // 2. Guardar dia corrente (ainda incompleto)
        List<Event> currentDayEvents = manager.getCurrentDayEvents();
        writeEventFile(new File(baseDir, CURRENT_DAY_FILE), currentDayEvents);
    }

    /**
     * Carrega o estado do dia corrente para o manager.
     */
    public void loadState(TimeSeriesManager manager) throws IOException {
        List<Event> currentEvents = loadCurrentDay();
        for (Event event : currentEvents) {
            manager.addEvent(event);
        }
    }
    
    /**
     * Carrega a metadata inicial.
     * @return int[] {maxDays, currentDayId} ou null se não existir
     */
    public int[] loadMetadata() throws IOException {
        File metaFile = new File(baseDir, METADATA_FILE);
        if (!metaFile.exists()) {
            return null;
        }
        return readMetadata();
    }
    
    /**
     * Carrega o dia corrente incompleto do disco.
     */
    public List<Event> loadCurrentDay() throws IOException {
        File currentFile = new File(baseDir, CURRENT_DAY_FILE);
        if (currentFile.exists()) {
            return readEventFile(currentFile);
        }
        return new ArrayList<>();
    }
    
    private void writeMetadata(TimeSeriesManager manager) throws IOException {
        File file = new File(baseDir, METADATA_FILE);
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {
            out.writeInt(manager.getMaxDays());
            out.writeInt(manager.getCurrentDayId());
        }
    }
    
    private int[] readMetadata() throws IOException {
        File file = new File(baseDir, METADATA_FILE);
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            int maxDays = in.readInt();
            int currentDayId = in.readInt();
            return new int[]{maxDays, currentDayId};
        }
    }

    private void writeEventFile(File file, List<Event> events) throws IOException {
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {
            out.writeInt(events.size());
            for (Event event : events) {
                writeEvent(out, event);
            }
        }
    }
    
    private List<Event> readEventFile(File file) throws IOException {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            return readEventList(in);
        }
    }

    // Métodos auxiliares de leitura/escrita de eventos
    
    private List<Event> readEventList(DataInputStream in) throws IOException {
        int eventCount = in.readInt();
        List<Event> events = new ArrayList<>();
        
        for (int i = 0; i < eventCount; i++) {
            Event event = readEvent(in);
            events.add(event);
        }
        
        return events;
    }
    
    private void writeEvent(DataOutputStream out, Event event) throws IOException {
        Serializer.writeString(out, event.getProduct());
        out.writeInt(event.getQuantity());
        out.writeDouble(event.getPrice());
    }
    
    private Event readEvent(DataInputStream in) throws IOException {
        String product = Serializer.readString(in);
        int quantity = in.readInt();
        double price = in.readDouble();
        return new Event(product, quantity, price);
    }
}
