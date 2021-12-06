package info.skyblond.archivedag.util;

import java.sql.Timestamp;

public class Constants {
    public static final Timestamp MIN_TIMESTAMP = new Timestamp(0);
    public static final Timestamp MAX_TIMESTAMP = Timestamp.valueOf("9999-12-31 23:59:59.999999999");
}
