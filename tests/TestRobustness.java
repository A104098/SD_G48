package tests;

import client.Client;
import geral.Protocol;
import geral.Serializer;
import java.io.*;
import java.net.Socket;

public class TestRobustness {
    private static final String HOST = "localhost";
    private static final int PORT = 12345;

    public static void main(String[] args) {
        System.out.println("=== Teste de Robustez ===");
        
        // 1. Iniciar Bad Client (Lê nada) em background
        new Thread(() -> {
            try {
                System.out.println("[BadClient] A conectar...");
                Socket socket = new Socket(HOST, PORT);
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                
                // --- LOGIN MANUAL ---
                // Registar
                sendManualRequest(out, 1, Protocol.OP_REGISTER, "bad", "user");
                // Login
                sendManualRequest(out, 2, Protocol.OP_LOGIN, "bad", "user");
                
                System.out.println("[BadClient] Autenticado. A spammar pedidos sem ler resposta.");
                
                // Spam de pedidos para encher buffer
                // Se o servidor bloquear ao tentar escrever a resposta, este loop eventualmente bloqueia
                // ou o servidor bloqueia nesse thread.
                for (int i = 0; i < 50000; i++) {
                     // Vamos pedir algo que gere resposta (Stats ou AddEvent)
                     // STATS gera resposta.
                     sendManualRequest(out, i + 10, Protocol.OP_QUANTITY_SOLD, "ProdA", 30);
                     
                     // NÃO LER NADA DO INPUT STREAM
                     // Thread.sleep(0, 100); // Muito rápido
                     if (i % 1000 == 0) System.out.println("[BadClient] Enviou " + i);
                }
                
                System.out.println("[BadClient] Terminado envio. A dormir para manter socket aberta...");
                Thread.sleep(Long.MAX_VALUE);
                
            } catch (Exception e) {
                System.out.println("[BadClient] Erro (esperado se buffer encher): " + e.getMessage());
            }
        }).start();
        
        // 2. Tentar usar Good Client
        try {
            System.out.println("A aguardar que BadClient encha buffers...");
            Thread.sleep(2000); 
            System.out.println("[GoodClient] A iniciar operações...");
            
            Client client = new Client(HOST, PORT);
            client.connect();
            client.register("good", "user");
            if (!client.login("good", "user")) {
                System.err.println("GoodClient falhou login");
                return;
            }
            
            long start = System.currentTimeMillis();
            for (int i = 0; i < 100; i++) {
                client.addEvent("ProdGood", 1, 10.0);
                if (i % 10 == 0) System.out.print(".");
            }
            long end = System.currentTimeMillis();
            
            System.out.println("\n[GoodClient] Sucesso! 100 operações em " + (end - start) + "ms");
            client.logout();
            System.out.println("TESTE PASSOU: O GoodClient não foi afetado pelo BadClient.");
            System.exit(0);
            
        } catch (Exception e) {
            System.err.println("\n[GoodClient] FALHA: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    // Constrói pacote manual: [Tag][Len][Payload{ReqId, Op, Params}]
    private static void sendManualRequest(DataOutputStream socketOut, int tag, byte op, Object... params) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream payloadOut = new DataOutputStream(baos);
        
        // Payload Content (Protocol.Request.writeTo)
        payloadOut.writeInt(0); // RequestId (internal)
        payloadOut.writeByte(op);
        
        // Parametros (Serialização manual baseada no Protocol.java)
        // Simplificado para os casos usados
        if (op == Protocol.OP_REGISTER || op == Protocol.OP_LOGIN) {
            Serializer.writeString(payloadOut, (String) params[0]); // username
            Serializer.writeString(payloadOut, (String) params[1]); // password
        } else if (op == Protocol.OP_QUANTITY_SOLD) {
            Serializer.writeString(payloadOut, (String) params[0]); // product
            payloadOut.writeInt((Integer) params[1]); // days
        }
        
        byte[] payload = baos.toByteArray();
        
        // Enviar Frame (Demultiplexer format)
        socketOut.writeInt(tag);
        socketOut.writeInt(payload.length);
        socketOut.write(payload);
        socketOut.flush();
    }
}
