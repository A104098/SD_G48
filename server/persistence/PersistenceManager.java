package server.persistence;

import server.auth.AuthManager;
import server.auth.User;
import server.data.TimeSeriesManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor centralizado de persistência.
 * Coordena guardar e carregar todos os dados do servidor.
 */
public class PersistenceManager {
    private static final String DEFAULT_DATA_DIR = "data";
    private static final String USERS_FILE = "users.dat";
    private static final String TIMESERIES_FILE = "timeseries.dat";
    
    private final String dataDirectory;
    private final UserPersistence userPersistence;
    private final TimeSeriesPersistence timeSeriesPersistence;
    
    public PersistenceManager() {
        this(DEFAULT_DATA_DIR);
    }
    
    public PersistenceManager(String dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.userPersistence = new UserPersistence(dataDirectory + "/" + USERS_FILE);
        this.timeSeriesPersistence = new TimeSeriesPersistence(dataDirectory + "/" + TIMESERIES_FILE);
    }
    
    /**
     * Guarda todos os dados do servidor.
     * @param authManager Gestor de autenticação
     * @param tsManager Gestor de séries temporais
     * @throws IOException se falhar a escrita
     */
    public void saveAll(AuthManager authManager, TimeSeriesManager tsManager) throws IOException {
        System.out.println("A guardar dados...");
        
        // Guardar utilizadores
        List<User> users = getAllUsers(authManager);
        userPersistence.save(users);
        System.out.println("  - Utilizadores: " + users.size());
        
        // Guardar séries temporais
        timeSeriesPersistence.save(tsManager);
        System.out.println("  - Dia corrente: " + tsManager.getCurrentDayId());
        System.out.println("  - Dias históricos: " + tsManager.getHistoricalDayCount());
        System.out.println("  - Eventos hoje: " + tsManager.getCurrentDay().getEventCount());
        
        System.out.println("Dados guardados com sucesso!");
    }
    
    /**
     * Carrega todos os dados do servidor.
     * @param authManager Gestor de autenticação (será preenchido)
     * @param maxDays Valor de D para criar TimeSeriesManager se necessário
     * @return TimeSeriesManager carregado ou novo se não existir
     * @throws IOException se falhar a leitura
     */
    public TimeSeriesManager loadAll(AuthManager authManager, int maxDays) throws IOException {
        System.out.println("A carregar dados...");
        
        // Carregar utilizadores
        List<User> users = userPersistence.load();
        for (User user : users) {
            authManager.register(user);
        }
        System.out.println("  - Utilizadores: " + users.size());
        
        // Carregar séries temporais
        TimeSeriesManager tsManager = timeSeriesPersistence.load();
        
        if (tsManager == null) {
            System.out.println("  - Nenhuma série temporal encontrada, criando nova");
            tsManager = new TimeSeriesManager(maxDays);
        } else {
            System.out.println("  - Dia corrente: " + tsManager.getCurrentDayId());
            System.out.println("  - Dias históricos: " + tsManager.getHistoricalDayCount());
            System.out.println("  - Eventos hoje: " + tsManager.getCurrentDay().getEventCount());
        }
        
        System.out.println("Dados carregados com sucesso!");
        return tsManager;
    }
    
    /**
     * Guarda apenas utilizadores.
     * @param authManager Gestor de autenticação
     * @throws IOException se falhar a escrita
     */
    public void saveUsers(AuthManager authManager) throws IOException {
        List<User> users = getAllUsers(authManager);
        userPersistence.save(users);
    }
    
    /**
     * Guarda apenas séries temporais.
     * @param tsManager Gestor de séries temporais
     * @throws IOException se falhar a escrita
     */
    public void saveTimeSeries(TimeSeriesManager tsManager) throws IOException {
        timeSeriesPersistence.save(tsManager);
    }
    
    /**
     * Obtém todos os utilizadores do AuthManager.
     * Como não temos um método público para isso, usamos reflexão ou
     * adicionamos um método no AuthManager.
     */
    private List<User> getAllUsers(AuthManager authManager) {
        // NOTA: Isto requer adicionar um método getAllUsers() no AuthManager
        // Por agora, retornamos lista vazia se não tivermos acesso
        // Vou assumir que vamos adicionar esse método
        return authManager.getAllUsers();
    }
    
    /**
     * Obtém o diretório de dados.
     * @return Caminho do diretório
     */
    public String getDataDirectory() {
        return dataDirectory;
    }
}
