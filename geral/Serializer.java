package geral;

import java.io.*;
import java.util.*;

public class Serializer {
    
    public static void writeString(DataOutputStream out, String str) throws IOException {
        if (str == null) {
            out.writeInt(-1);
        } else {
            byte[] bytes = str.getBytes("UTF-8");
            out.writeInt(bytes.length);
            out.write(bytes);
        }
    }
    
    public static void writeStringList(DataOutputStream out, List<String> list) throws IOException {
        if (list == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(list.size());
            for (String str : list) {
                writeString(out, str);
            }
        }
    }
    
    public static void writeStringListCompressed(DataOutputStream out, List<String> list) throws IOException {
        if (list == null) {
            out.writeInt(-1);
            return;
        }
        
        // Construir dicionário de strings únicas
        Map<String, Short> dictionary = new HashMap<>();
        List<String> uniqueStrings = new ArrayList<>();
        
        for (String str : list) {
            if (!dictionary.containsKey(str)) {
                dictionary.put(str, (short) uniqueStrings.size());
                uniqueStrings.add(str);
            }
        }
        
        // Escrever dicionário
        out.writeInt(uniqueStrings.size());
        for (String str : uniqueStrings) {
            writeString(out, str);
        }
        
        // Escrever índices
        out.writeInt(list.size());
        for (String str : list) {
            out.writeShort(dictionary.get(str));
        }
    }
    
    public static void writeStringIntMap(DataOutputStream out, Map<String, Integer> map) throws IOException {
        if (map == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(map.size());
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                writeString(out, entry.getKey());
                out.writeInt(entry.getValue());
            }
        }
    }
    
    public static void writeStringDoubleMap(DataOutputStream out, Map<String, Double> map) throws IOException {
        if (map == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(map.size());
            for (Map.Entry<String, Double> entry : map.entrySet()) {
                writeString(out, entry.getKey());
                out.writeDouble(entry.getValue());
            }
        }
    }
    

    public static void writeBoolean(DataOutputStream out, boolean value) throws IOException {
        out.writeByte(value ? 1 : 0);
    }
    

    public static void writeByteArray(DataOutputStream out, byte[] data) throws IOException {
        if (data == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(data.length);
            out.write(data);
        }
    }
    
    public static String readString(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length == -1) {
            return null;
        }
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, "UTF-8");
    }
    
    /**
     * Lê uma lista de strings.
     */
    public static List<String> readStringList(DataInputStream in) throws IOException {
        int count = in.readInt();
        if (count == -1) {
            return null;
        }
        List<String> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(readString(in));
        }
        return list;
    }
    
    public static List<String> readStringListCompressed(DataInputStream in) throws IOException {
        int dictSize = in.readInt();
        if (dictSize == -1) {
            return null;
        }
        
        // Ler dicionário
        String[] dictionary = new String[dictSize];
        for (int i = 0; i < dictSize; i++) {
            dictionary[i] = readString(in);
        }
        
        // Ler índices e reconstruir lista
        int count = in.readInt();
        List<String> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            short index = in.readShort();
            list.add(dictionary[index]);
        }
        
        return list;
    }

    public static Map<String, Integer> readStringIntMap(DataInputStream in) throws IOException {
        int size = in.readInt();
        if (size == -1) {
            return null;
        }
        Map<String, Integer> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String key = readString(in);
            int value = in.readInt();
            map.put(key, value);
        }
        return map;
    }

    public static Map<String, Double> readStringDoubleMap(DataInputStream in) throws IOException {
        int size = in.readInt();
        if (size == -1) {
            return null;
        }
        Map<String, Double> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String key = readString(in);
            double value = in.readDouble();
            map.put(key, value);
        }
        return map;
    }
    

    public static boolean readBoolean(DataInputStream in) throws IOException {
        return in.readByte() == 1;
    }
    

    public static byte[] readByteArray(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length == -1) {
            return null;
        }
        byte[] data = new byte[length];
        in.readFully(data);
        return data;
    }
    
    // ==================== Utilitários ====================
    
    public static byte[] toBytes(SerializableWriter writer) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        writer.write(dos);
        dos.flush();
        return baos.toByteArray();
    }
    

    public static <T> T fromBytes(byte[] bytes, SerializableReader<T> reader) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(bais);
        return reader.read(dis);
    }
    
    // Interface funcional para escrever dados serializados.
    @FunctionalInterface
    public interface SerializableWriter {
        void write(DataOutputStream out) throws IOException;
    }
    
    // Interface funcional para ler dados serializados.
    @FunctionalInterface
    public interface SerializableReader<T> {
        T read(DataInputStream in) throws IOException;
    }

    public static int estimateStringSize(String str) {
        if (str == null) return 4; // apenas o int(-1)
        try {
            return 4 + str.getBytes("UTF-8").length; // int + bytes
        } catch (UnsupportedEncodingException e) {
            return 4 + str.length() * 3; // estimativa conservadora
        }
    }
    

    public static boolean worthCompressing(List<String> list) {
        if (list == null || list.size() < 10) return false;
        
        Set<String> unique = new HashSet<>(list);
        // Vale a pena se há muitas repetições (< 30% únicos)
        return unique.size() < list.size() * 0.3;
    }
}
