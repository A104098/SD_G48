package server;

import geral.Protocol;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

//Handler para uma conexão de cliente.
//Processa pedidos e envia respostas usando o protocolo
//Submete cada request como tarefa independente à ThreadPool para processamento concorrente
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ServerManager serverManager;
    private final ThreadPool threadPool;
    private DataInputStream in;
    private DataOutputStream out;
    private User authenticatedUser;
    
    public ClientHandler(Socket socket, ServerManager serverManager, ThreadPool threadPool) {
        this.socket = socket;
        this.serverManager = serverManager;
        this.threadPool = threadPool;
        this.authenticatedUser = null;
    }
    
    @Override
    public void run() {
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            
            System.out.println("Cliente conectado: " + socket.getInetAddress());
            
            // Loop de leitura - submete cada request como tarefa independente à ThreadPool
            while (!socket.isClosed()) {
                // Ler tag (do Demultiplexer)
                int tag = in.readInt();
                // Ler tamanho do request
                int requestLen = in.readInt();
                // Ler dados do request
                byte[] requestData = new byte[requestLen];
                in.readFully(requestData);

                // Desserializar request
                ByteArrayInputStream bais = new ByteArrayInputStream(requestData);
                DataInputStream dis = new DataInputStream(bais);
                Protocol.Request request = Protocol.Request.readFrom(dis);
                
                // Submeter tarefa à ThreadPool para processar este request
                threadPool.execute(() -> {
                    try {
                        // Processar request
                        Protocol.Response response = processRequest(request);
                        
                        // Serializar response
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        DataOutputStream dos = new DataOutputStream(baos);
                        response.writeTo(dos, request.getOperation());
                        dos.flush();
                        byte[] responseData = baos.toByteArray();
                        
                        // Enviar response com formato Demultiplexer
                        synchronized (out) {
                            out.writeInt(tag);
                            out.writeInt(responseData.length);
                            out.write(responseData);
                            out.flush();
                        }
                    } catch (IOException e) {
                        System.err.println("Erro ao enviar resposta: " + e.getMessage());
                    }
                });
            }
            
        } catch (IOException e) {
            if (!socket.isClosed()) {
                System.err.println("Erro na comunicação com cliente: " + e.getMessage());
            }
        } finally {
            cleanup();
        }
    }
    
    private Protocol.Response processRequest(Protocol.Request request) {
        try {
            switch (request.getOperation()) {
                case Protocol.OP_REGISTER:
                    return handleRegister(request);
                case Protocol.OP_LOGIN:
                    return handleLogin(request);
                case Protocol.OP_LOGOUT:
                    return handleLogout(request);
                case Protocol.OP_ADD_EVENT:
                    return handleAddEvent(request);
                case Protocol.OP_QUANTITY_SOLD:
                    return handleQuantitySold(request);
                case Protocol.OP_SALES_VOLUME:
                    return handleSalesVolume(request);
                case Protocol.OP_AVERAGE_PRICE:
                    return handleAveragePrice(request);
                case Protocol.OP_MAX_PRICE:
                    return handleMaxPrice(request);
                case Protocol.OP_FILTER_EVENTS:
                    return handleFilterEvents(request);
                case Protocol.OP_SIMULTANEOUS_SALES:
                    return handleSimultaneousSales(request);
                case Protocol.OP_CONSECUTIVE_SALES:
                    return handleConsecutiveSales(request);
                default:
                    return Protocol.Response.error(request.getRequestId(), 
                        Protocol.STATUS_INVALID_PARAMS, "Operação desconhecida");
            }
        } catch (Exception e) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_ERROR, "Erro: " + e.getMessage());
        }
    }
    
    private Protocol.Response handleRegister(Protocol.Request request) {
        String username = request.getString("username");
        String password = request.getString("password");
        
        if (username == null || password == null) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_INVALID_PARAMS, "Username/password em falta");
        }
        
        boolean success = serverManager.register(username, password);
        
        if (success) {
            System.out.println("Utilizador registado: " + username);
            return Protocol.Response.success(request.getRequestId());
        } else {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_USER_EXISTS, "Username já existe");
        }
    }
    
    private Protocol.Response handleLogin(Protocol.Request request) {
        String username = request.getString("username");
        String password = request.getString("password");
        
        if (username == null || password == null) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_INVALID_PARAMS, "Username/password em falta");
        }
        
        User user = serverManager.authenticate(username, password);
        
        if (user != null) {
            authenticatedUser = user;
            System.out.println("Utilizador autenticado: " + username);
            return Protocol.Response.success(request.getRequestId());
        } else {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_AUTH_FAILED, "Credenciais inválidas");
        }
    }
    
    private Protocol.Response handleLogout(Protocol.Request request) {
        if (authenticatedUser == null) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_NOT_AUTHENTICATED, "Não autenticado");
        }
        
        System.out.println("Utilizador desconectado: " + authenticatedUser.getUsername());
        authenticatedUser = null;
        return Protocol.Response.success(request.getRequestId());
    }
    
    private Protocol.Response handleAddEvent(Protocol.Request request) {
        if (authenticatedUser == null) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_NOT_AUTHENTICATED, "Não autenticado");
        }
        
        String product = request.getString("product");
        Integer quantity = request.getInt("quantity");
        Double price = request.getDouble("price");
        
        if (product == null || quantity == null || price == null) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_INVALID_PARAMS, "Parâmetros inválidos");
        }
        
        try {
            serverManager.addEvent(product, quantity, price);
            System.out.println("Utilizador: " + authenticatedUser.getUsername() 
                + " adicionou Evento: produto:" + product + ", quantidade:" + quantity + ", preço:" + price);
            return Protocol.Response.success(request.getRequestId());
        } catch (Exception e) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_ERROR, "Erro ao adicionar evento");
        }
    }
    
    private Protocol.Response handleQuantitySold(Protocol.Request request) {
        if (authenticatedUser == null) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_NOT_AUTHENTICATED, "Não autenticado");
        }
        
        String product = request.getString("product");
        Integer days = request.getInt("days");
        
        if (product == null || days == null) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_INVALID_PARAMS, "Parâmetros inválidos");
        }
        
        AggregationService.AggregationResult<Integer> result = serverManager.aggregateQuantity(product, days);
        
        if (result.value == -1) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_ERROR, result.warning != null ? result.warning : "Dados insuficientes");
        }
        
        System.out.println("Utilizador: " + authenticatedUser.getUsername() + " consultou Quantidade: produto:" + product + ", dias:" + days + ")");
        
        return Protocol.Response.success(request.getRequestId())
            .setData("quantity", result.value);
    }
    
    private Protocol.Response handleSalesVolume(Protocol.Request request) {
        if (authenticatedUser == null) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_NOT_AUTHENTICATED, "Não autenticado");
        }
        
        String product = request.getString("product");
        Integer days = request.getInt("days");
        
        if (product == null || days == null) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_INVALID_PARAMS, "Parâmetros inválidos");
        }
        
        AggregationService.AggregationResult<Double> result = serverManager.aggregateVolume(product, days);
        if (result.value == -1) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_ERROR, result.warning != null ? result.warning : "Dados insuficientes");
        }
        
        System.out.println("Utilizador: " + authenticatedUser.getUsername() + " consultou Volume: produto:" + product + ", dias:" + days + ")");
        
        return Protocol.Response.success(request.getRequestId())
            .setData("volume", result.value);
    }
    
    private Protocol.Response handleAveragePrice(Protocol.Request request) {
        if (authenticatedUser == null) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_NOT_AUTHENTICATED, "Não autenticado");
        }
        
        String product = request.getString("product");
        Integer days = request.getInt("days");
        
        if (product == null || days == null) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_INVALID_PARAMS, "Parâmetros inválidos");
        }
        
        AggregationService.AggregationResult<Double> result = serverManager.aggregateAveragePrice(product, days);
        
        if (result.value == -1) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_ERROR, result.warning != null ? result.warning : "Dados insuficientes");
        }
        
        System.out.println("Utilizador: " + authenticatedUser.getUsername() + " consultou Preço Médio: produto:" + product + ", dias:" + days + ")");

        return Protocol.Response.success(request.getRequestId())
            .setData("avgPrice", result.value);
    }

    private Protocol.Response handleMaxPrice(Protocol.Request request) {
        if (authenticatedUser == null) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_NOT_AUTHENTICATED, "Não autenticado");
        }
        
        String product = request.getString("product");
        Integer days = request.getInt("days");
        
        if (product == null || days == null) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_INVALID_PARAMS, "Parâmetros inválidos");
        }
        
        AggregationService.AggregationResult<Double> result = serverManager.aggregateMaxPrice(product, days);
        
        if (result.value == -1) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_ERROR, result.warning != null ? result.warning : "Dados insuficientes");
        }
        
        System.out.println("Utilizador: " + authenticatedUser.getUsername() + " consultou Preço Máximo: produto:" + product + ", dias:" + days + ")");
        
        return Protocol.Response.success(request.getRequestId())
            .setData("maxPrice", result.value);
    }

    private Protocol.Response handleFilterEvents(Protocol.Request request) {
        if (authenticatedUser == null) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_NOT_AUTHENTICATED, "Não autenticado");
        }
        
        List<String> products = request.getStringList("products");
        Integer dayOffset = request.getInt("dayOffset");
        
        if (products == null || products.isEmpty() || dayOffset == null) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_INVALID_PARAMS, "Parâmetros inválidos");
        }
        
        if (dayOffset < 0) {
            return Protocol.Response.error(request.getRequestId(), 
                Protocol.STATUS_INVALID_PARAMS, "Offset inválido");
        }
        
        List<Protocol.Event> events = serverManager.getFilteredEvents(products, dayOffset);
        
        System.out.println("Utilizador: " + authenticatedUser.getUsername() + " filtrou eventos: produto(s):" + products + " (dia: " + dayOffset + ")");
        
        return Protocol.Response.success(request.getRequestId())
            .setData("events", events);
    }
    
    // Handler para vendas simultâneas (bloqueante)
    private Protocol.Response handleSimultaneousSales(Protocol.Request request) {
        if (authenticatedUser == null) {
            return Protocol.Response.error(request.getRequestId(), Protocol.STATUS_NOT_AUTHENTICATED, "Não autenticado");
        }
        String product1 = request.getString("product1");
        String product2 = request.getString("product2");
        if (product1 == null || product2 == null) {
            return Protocol.Response.error(request.getRequestId(), Protocol.STATUS_INVALID_PARAMS, "Parâmetros inválidos");
        }
        boolean result = serverManager.waitForSimultaneousSales(product1, product2);
        System.out.println("Utilizador: " + authenticatedUser.getUsername() + " aguardou Simultanêas: produto1:" + product1 + ", produto2:" + product2 + " (" + result + ")");
        return Protocol.Response.success(request.getRequestId()).setData("result", result);
    }
    
    // Handler para vendas consecutivas (bloqueante)
    private Protocol.Response handleConsecutiveSales(Protocol.Request request) {
        if (authenticatedUser == null) {
            return Protocol.Response.error(request.getRequestId(), Protocol.STATUS_NOT_AUTHENTICATED, "Não autenticado");
        }
        Integer n = request.getInt("n");
        if (n == null || n < 1) {
            return Protocol.Response.error(request.getRequestId(), Protocol.STATUS_INVALID_PARAMS, "Parâmetro n inválido");
        }
        String product = serverManager.waitForConsecutiveSales(n);
        System.out.println("Utilizador: " + authenticatedUser.getUsername() + " aguardou Consecutivas: n:" + n + " (produto: " + product + ")");
        return Protocol.Response.success(request.getRequestId()).setData("product", product);
    }
    
    private void cleanup() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (!socket.isClosed()) socket.close();
            System.out.println("Cliente desconectado: " + socket.getInetAddress());
        } catch (IOException e) {
            System.err.println("Erro ao limpar recursos: " + e.getMessage());
        }
    }
}
