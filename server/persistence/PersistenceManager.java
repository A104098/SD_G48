package server.persistence;

import java.io.IOException;
import java.util.List;
import server.Authentication;
import server.TimeSeriesManager;
import server.User;

/**
 * Gestor centralizado de persistência.
 * Coordena guardar e carregar todos os dados do servidor.
 */
public class PersistenceManager {
    private static final String DEFAULT_DATA_DIR = "data";
    private static final String USERS_FILE = "users.dat";
    private static final String TIMESERIES_DIR = "timeseries";
    
    private final String dataDirectory;
    private final UserPersistence userPersistence;
    private final TimeSeriesPersistence timeSeriesPersistence;
    
    public PersistenceManager() {
        this(DEFAULT_DATA_DIR);
    }
    
    public PersistenceManager(String dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.userPersistence = new UserPersistence(dataDirectory + "/" + USERS_FILE);
        this.timeSeriesPersistence = new TimeSeriesPersistence(dataDirectory + "/" + TIMESERIES_DIR);
    }
    
    /**
     * Guarda todos os dados do servidor.
     * @param auth Gestor de autenticação
     * @param tsManager Gestor de séries temporais
     * @throws IOException se falhar a escrita
     */
    public void saveAll(Authentication auth, TimeSeriesManager tsManager) throws IOException {
        System.out.println("A guardar dados...");
        
        // Guardar utilizadores
        List<User> users = getAllUsers(auth);
        userPersistence.save(users);
        System.out.println("  - Utilizadores: " + users.size());
        
        // Guardar séries temporais
        timeSeriesPersistence.saveState(tsManager);
        System.out.println("  - Dia corrente: " + tsManager.getCurrentDayId());
        System.out.println("  - Dias históricos (total): " + tsManager.getHistoricalDayCount());
        System.out.println("  - Eventos hoje: " + tsManager.getCurrentDayEventCount());
        
        System.out.println("Dados guardados com sucesso!");
    }
    
    /**
     * Carrega todos os dados do servidor.
     * @param auth Gestor de autenticação (será preenchido)
     * @param maxDays Valor de D (D)
     * @param maxMemoryDays Valor de S (S)
     * @return TimeSeriesManager carregado ou novo se não existir
     * @throws IOException se falhar a leitura
     */
    public TimeSeriesManager loadAll(Authentication auth, int maxDays, int maxMemoryDays) throws IOException {
        System.out.println("A carregar dados...");
        
        // Carregar utilizadores
        List<User> users = userPersistence.load();
        if (users != null) {
            for (User user : users) {
                auth.register(user);
            }
            System.out.println("  - Utilizadores: " + users.size());
        } else {
             System.out.println("  - Utilizadores: 0");
        }
        
        // Carregar séries temporais
        TimeSeriesManager tsManager;
        int[] meta = timeSeriesPersistence.loadMetadata();
        
        if (meta == null) {
            System.out.println("  - Nenhuma série temporal encontrada, criando nova");
             // Nota: timeSeriesPersistence é uma instância da classe, precisamos passá-la
            tsManager = new TimeSeriesManager(maxDays, maxMemoryDays, timeSeriesPersistence);
        } else {
            // Recriar usando configuração atual (maxMemoryDays)
            tsManager = new TimeSeriesManager(maxDays, maxMemoryDays, timeSeriesPersistence);
            timeSeriesPersistence.loadState(tsManager);
            tsManager.setCurrentDayId(meta[1]);
            // Nota: historicalDays começa vazio, será populado lazy
            
            System.out.println("  - Dia corrente: " + tsManager.getCurrentDayId());
            System.out.println("  - Dias históricos (total): " + tsManager.getHistoricalDayCount());
        }
        
        System.out.println("Dados carregados com sucesso!");
        return tsManager;
    }
    
    /**
     * Guarda apenas utilizadores.
     * @param auth Gestor de autenticação
     * @throws IOException se falhar a escrita
     */
    public void saveUsers(Authentication auth) throws IOException {
        List<User> users = getAllUsers(auth);
        userPersistence.save(users);
    }
    
    /**
     * Guarda apenas séries temporais.
     * @param tsManager Gestor de séries temporais
     * @throws IOException se falhar a escrita
     */
    public void saveTimeSeries(TimeSeriesManager tsManager) throws IOException {
        timeSeriesPersistence.saveState(tsManager);
    }
    
    /**
     * Obtém todos os utilizadores do Authentication.
     */
    private List<User> getAllUsers(Authentication auth) {
        return auth.getAllUsers();
    }
    
    /**
     * Obtém o diretório de dados.
     * @return Caminho do diretório
     */
    public String getDataDirectory() {
        return dataDirectory;
    }

    public TimeSeriesPersistence getTimeSeriesPersistence() {
        return timeSeriesPersistence;
    }
}
