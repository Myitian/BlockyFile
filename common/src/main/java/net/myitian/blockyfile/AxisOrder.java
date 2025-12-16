package net.myitian.blockyfile;

import java.util.List;

public enum AxisOrder {
    XYZ,
    XZY,
    YXZ,
    YZX,
    ZXY,
    ZYX;

    public static final List<String> VALUES = List.of("xyz", "xzy", "yxz", "yzx", "zxy", "zyx");

    public static AxisOrder parse(String s) {
        return switch (s) {
            case "xyz" -> XYZ;
            case "xzy" -> XZY;
            case "yxz" -> YXZ;
            case "yzx" -> YZX;
            case "zxy" -> ZXY;
            case "zyx" -> ZYX;
            default -> throw new IllegalArgumentException();
        };
    }
}
