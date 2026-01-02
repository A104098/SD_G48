package server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Gestor de autenticação de utilizadores.
 * Thread-safe para acesso concorrente de múltiplos clientes.
 */
public class ServerManager {
    private final Map<String, User> users;
    private final ReentrantReadWriteLock lock;
    
    public ServerManager() {
        this.users = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }
    
    //Regista um novo utilizador no sistema
    public boolean register(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        if (password == null || password.isEmpty()) {
            return false;
        }
        
        lock.writeLock().lock();
        try {
            // Verificar se o utilizador já existe
            if (users.containsKey(username)) {
                return false;
            }
            
            // Criar e guardar novo utilizador
            User user = new User(username, password);
            users.put(username, user);
            return true;
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    //Autentica um utilizador
    public User authenticate(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        
        lock.readLock().lock();
        try {
            User user = users.get(username);
            
            if (user == null) {
                return null; // Utilizador não existe
            }
            
            if (user.checkPassword(password)) {
                return user;
            }
            
            return null; // Password incorreta
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // Verifica se um utilizador existe.
    public boolean userExists(String username) {
        lock.readLock().lock();
        try {
            return users.containsKey(username);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    //Obtém o número total de utilizadores registados.
    public int getUserCount() {
        return users.size();
    }
    

    //Remove um utilizador.
    public boolean removeUser(String username) {
        lock.writeLock().lock();
        try {
            return users.remove(username) != null;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    //Obtém todos os utilizadores (para persistência)
    public java.util.List<User> getAllUsers() {
        lock.readLock().lock();
        try {
            return new java.util.ArrayList<>(users.values());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    //Regista um utilizador já existente (com hash de password : persistência)
    public boolean register(User user) {
        if (user == null) {
            return false;
        }
        
        lock.writeLock().lock();
        try {
            if (users.containsKey(user.getUsername())) {
                return false;
            }
            users.put(user.getUsername(), user);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
