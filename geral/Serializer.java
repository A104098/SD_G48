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


    public static void writeBoolean(DataOutputStream out, boolean value) throws IOException {
        out.writeByte(value ? 1 : 0);
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


    public static boolean readBoolean(DataInputStream in) throws IOException {
        return in.readByte() == 1;
    }
}