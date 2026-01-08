package server;

//Representa um utilizador do sistema
public class User {
    private final String username;
    private final String password;
    
    //Cria um novo utilizador
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getPassword() {
        return password;
    }
    
    //Verifica se a password fornecida está correta
    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof User)) return false;
        User other = (User) obj;
        return username.equals(other.username);
    }
}
