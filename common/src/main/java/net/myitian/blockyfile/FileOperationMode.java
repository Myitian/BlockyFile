package net.myitian.blockyfile;

import java.util.List;

public enum FileOperationMode {
    STORE,
    LOAD;

    public static final List<String> VALUES = List.of("store", "load");

    public static FileOperationMode parse(String s) {
        return switch (s) {
            case "store" -> STORE;
            case "load" -> LOAD;
            default -> throw new IllegalArgumentException();
        };
    }
}
