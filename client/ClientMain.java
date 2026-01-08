package client;

import java.io.IOException;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

public class ClientMain {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 12345;
    
    private final Client client;
    private final Scanner scanner;
    private boolean running;
    
    public ClientMain(String host, int port) {
        this.client = new Client(host, port);
        this.scanner = new Scanner(System.in);
        this.running = false;
    }
    
    public void start() {
        System.out.println("=== Cliente de Eventos ===");
        
        try {
            client.connect();
            System.out.println("Conectado ao servidor");
        } catch (IOException e) {
            System.err.println("Erro ao conectar: " + e.getMessage());
            return;
        }
        
        running = true;
        printHelp();
        
        while (running) {
            try {
                System.out.print("> ");
                String line = scanner.nextLine().trim();
                
                if (line.isEmpty()) {
                    continue;
                }
                
                processCommand(line);
                
            } catch (Exception e) {
                System.err.println("Erro: " + e.getMessage());
            }
        }
        
        client.close();
        scanner.close();
    }


    private void processCommand(String line) throws IOException {
        String command = line.split("\\s+")[0].toLowerCase();
        
        boolean auth = client.isAuthenticated();
        
        // Commands available regardless of auth state
        if (command.equals("status")) {
            handleStatus();
            return;
        } else if (command.equals("help")) {
            printHelp();
            return;
        } else if (command.equals("quit")) {
            running = false;
            System.out.println("A sair...");
            return;
        }

        // Unauthenticated-only commands
        if (!auth) {
            switch (command) {
                case "register":
                    handleRegister();
                    break;
                case "login":
                    handleLogin();
                    break;
                default:
                    System.out.println("Comando não disponível (ou requer autenticação). Digite 'help'.");
            }
            return;
        }
        
        // Authenticated-only commands
        switch (command) {
            case "logout":
                handleLogout();
                break;
            case "register":
            case "login":
                System.out.println("Já está autenticado. Faça logout primeiro.");
                break;
            case "add":
                handleAddEvent();
                break;
            case "quantity":
                handleAggregateQuantity();
                break;
            case "volume":
                handleAggregateVolume();
                break;
            case "average":
                handleAggregateAverage();
                break;
            case "max":
                handleAggregateMax();
                break;
            case "filter":
                handleFilterEvents();
                break;
            case "simultaneous":
                handleSimultaneousSales();
                break;
            case "consecutive":
                handleConsecutiveSales();
                break;
            default:
                System.out.println("Comando desconhecido. Digite 'help' para ajuda.");
        }
    }
    
    private String readString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }
    
    private int readInt(String prompt) {
        while (true) {
            try {
                String input = readString(prompt);
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Por favor insira um número inteiro válido.");
            }
        }
    }
    
    private double readDouble(String prompt) {
        while (true) {
            try {
                String input = readString(prompt);
                // Handle comma vs dot if necessary, though Double.parseDouble works with dot
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println("Por favor insira um número válido (use ponto para decimais).");
            }
        }
    }

    private void handleSimultaneousSales() throws IOException {
        String p1 = readString("Produto 1: ");
        String p2 = readString("Produto 2: ");

        // Executar em thread separada para não bloquear a UI
        new Thread(() -> {
            try {
                System.out.println("Aguardando vendas simultâneas de " + p1 + " e " + p2 + "...");
                Boolean result = client.simultaneousSales(p1, p2);
                if (result == null) {
                    System.out.println("\n[NOTIFICAÇÃO] Erro ao aguardar vendas simultâneas");
                } else if (result) {
                    System.out.println("\n[NOTIFICAÇÃO] Ambos os produtos " + p1 + " e " + p2 + " foram vendidos no dia corrente!");
                } else {
                    System.out.println("\n[NOTIFICAÇÃO] O dia terminou sem vendas simultâneas de " + p1 + " e " + p2);
                }
                System.out.print("> "); // Re-imprimir prompt
            } catch (IOException e) {
                System.err.println("\n[ERRO] " + e.getMessage());
                System.out.print("> ");
            }
        }, "SimulThread").start();

        System.out.println("Pedido submetido.");
    }

    private void handleConsecutiveSales() throws IOException {
        int n = readInt("Número de vendas consecutivas (n): ");
        if (n < 1) {
            System.out.println("n deve ser >= 1");
            return;
        }

        // Executar em thread separada para não bloquear a UI
        new Thread(() -> {
            try {
                System.out.println("Aguardando " + n + " vendas consecutivas...");
                String product = client.consecutiveSales(n);
                if (product != null) {
                    System.out.println("\n[NOTIFICAÇÃO] Produto com " + n + " vendas consecutivas: " + product);
                } else {
                    System.out.println("\n[NOTIFICAÇÃO] O dia terminou sem " + n + " vendas consecutivas do mesmo produto.");
                }
                System.out.print("> "); // Re-imprimir prompt
            } catch (IOException e) {
                System.err.println("\n[ERRO] " + e.getMessage());
                System.out.print("> ");
            }
        }, "ConsecThread").start();

        System.out.println("Pedido submetido.");
    }

    private void handleRegister() throws IOException {
        String username = readString("Username: ");
        String password = readString("Password: ");
        
        boolean success = client.register(username, password);
        if (success) {
            System.out.println("Utilizador registado com sucesso!");
        } else {
            System.out.println("Erro ao registar. Username já existe");
        }
    }
    
    private void handleLogin() throws IOException {
        String username = readString("Username: ");
        String password = readString("Password: ");
        
        boolean success = client.login(username, password);
        if (success) {
            System.out.println("Login bem-sucedido! Bem-vindo " + username);
            printHelp();
        } else {
            System.out.println("Login falhou. Credenciais inválidas?");
        }
    }
    
    private void handleLogout() throws IOException {
        boolean success = client.logout();
        if (success) {
            System.out.println("Logout bem-sucedido!");
        } else {
            System.out.println("Erro ao fazer logout");
        }
    }
    
    private void handleAddEvent() throws IOException {
        String product = readString("Produto: ");
        int quantity = readInt("Quantidade: ");
        double price = readDouble("Preço: ");
        
        boolean success = client.addEvent(product, quantity, price);
        System.out.println(success ? "Evento adicionado" : "Erro ao adicionar evento");
    }
    
    private void handleAggregateQuantity() throws IOException {
        String product = readString("Produto: ");
        int days = readInt("Dias: ");
        
        int result = client.aggregateQuantity(product, days);
        if (result >= 0) {
            System.out.println("Quantidade total: " + result);
        } else {
            System.out.println("Dados insuficientes ou erro");
        }
    }
    
    private void handleAggregateVolume() throws IOException {
        String product = readString("Produto: ");
        int days = readInt("Dias: ");

        double result = client.aggregateVolume(product, days);
        if (result >= 0) {
            System.out.printf("Volume total: %.2f\n", result);
        } else {
            String lastError = client.getLastErrorMessage();
            if (lastError == null || lastError.isEmpty()) {
                lastError = "Dados insuficientes ou erro";
            }
            System.out.println("Erro: " + lastError);
        }
    }
    
    private void handleAggregateAverage() throws IOException {
        String product = readString("Produto: ");
        int days = readInt("Dias: ");
        
        double result = client.aggregateAverage(product, days);
        if (result >= 0) {
            System.out.printf("Preço médio: %.2f\n", result);
        } else {
            System.out.println("Dados insuficientes ou erro");
        }
    }
    
    private void handleAggregateMax() throws IOException {
        String product = readString("Produto: ");
        int days = readInt("Dias: ");
        
        double result = client.aggregateMaxPrice(product, days);
        if (result >= 0) {
            System.out.printf("Preço máximo: %.2f\n", result);
        } else {
            System.out.println("Dados insuficientes ou erro");
        }
    }
    
    private void handleFilterEvents() throws IOException {
        System.out.println("1=ontem, 2=anteontem, etc.");
        int days = readInt("Número do dia: ");
        if (days < 0) {
            System.out.println("dia inválido");
            return;
        }

        List<String> products = new ArrayList<>();
        System.out.println("Insira os produtos a filtrar (uma linha vazia para terminar):");
        while (true) {
            String prod = readString("Produto > ");
            if (prod.isEmpty()) {
                break;
            }
            products.add(prod);
        }
        
        if (products.isEmpty()) {
            System.out.println("Nenhum produto selecionado.");
            return;
        }

        List<geral.Protocol.Event> events = client.filterEvents(products, days);
        if (events.isEmpty()) {
            System.out.println("Nenhum evento encontrado");
        } else {
            System.out.println("\n=== Eventos Encontrados ===");
            for (geral.Protocol.Event event : events) {
                System.out.printf("%s: %d unidades a %.2f EUR = %.2f EUR\n",
                    event.getProduct(), 
                    event.getQuantity(), 
                    event.getPrice(),
                    event.getTotalValue());
            }
            System.out.println("Total de eventos: " + events.size());
            System.out.println("==========================\n");
        }
    }
    
    private void handleStatus() {
        System.out.println("\n=== Estado do Cliente ===");
        System.out.println("Conectado: " + client.isConnected());
        System.out.println("Autenticado: " + client.isAuthenticated());
        if (client.isAuthenticated()) {
            System.out.println("Utilizador: " + client.getCurrentUser());
        }
        System.out.println("========================\n");
    }
    
    private void printHelp() {
        System.out.println("\n=== Comandos Disponíveis ===");
        
        boolean auth = client.isAuthenticated();
        
        if (!auth) {
            // Comandos para utilizadores NÃO autenticados
            System.out.println("register       - Registar novo utilizador");
            System.out.println("login          - Autenticar");
        } else {
            // Comandos para utilizadores autenticados
            System.out.println("logout         - Terminar sessão");
            System.out.println("add            - Adicionar evento");
            System.out.println("quantity       - Agregação da quantidade nos n últimos dias");
            System.out.println("volume         - Agregação do volume de vendas nos n últimos dias");
            System.out.println("average        - Agregação do preço médio nos n últimos dias");
            System.out.println("max            - Agregação do preço máximo nos n últimos dias");
            System.out.println("filter         - Filtrar eventos por produto(s) num dia");
            System.out.println("simultaneous   - Espera vendas simultâneas de dois produtos");
            System.out.println("consecutive    - Espera n vendas consecutivas do mesmo produto");
        }
        
        // Comandos comuns
        System.out.println("status         - Ver estado");
        System.out.println("help           - Mostrar ajuda");
        System.out.println("quit           - Sair");
        System.out.println("=============================\n");
    }
    
    public static void main(String[] args) {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;
        
        if (args.length >= 1) {
            host = args[0];
        }
        
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Porta inválida, usando " + DEFAULT_PORT);
            }
        }
        
        ClientMain ui = new ClientMain(host, port);
        ui.start();
    }
}
