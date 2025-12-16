package net.myitian.blockyfile;

import java.util.List;

public enum PaletteOperationMode {
    IMPORT,
    EXPORT;

    public static final List<String> VALUES = List.of("import", "export");

    public static PaletteOperationMode parse(String s) {
        return switch (s) {
            case "import" -> IMPORT;
            case "export" -> EXPORT;
            default -> throw new IllegalArgumentException();
        };
    }
}
