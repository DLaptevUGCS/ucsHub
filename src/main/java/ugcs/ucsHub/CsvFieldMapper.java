package ugcs.ucsHub;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.ugcs.ucs.proto.DomainProto.TelemetryType.*;

public final class CsvFieldMapper {
    private final Map<String, String> typeNameToCsvFieldName;

    private static volatile CsvFieldMapper instance;

    public static CsvFieldMapper mapper() {
        if (instance == null) {
            synchronized (CsvFieldMapper.class) {
                if (instance == null) {
                    instance = new CsvFieldMapper();
                }
            }
        }
        return instance;
    }

    private CsvFieldMapper() {
        Map<String, String> typeNameToCsvFieldName = new HashMap<>();
        typeNameToCsvFieldName.put(TT_LATITUDE.name(), "fc:latitude");
        typeNameToCsvFieldName.put(TT_LONGITUDE.name(), "fc:longitude");
        typeNameToCsvFieldName.put(TT_AGL_ALTITUDE.name(), "cs:altitude_agl");
        typeNameToCsvFieldName.put(TT_GROUND_SPEED.name(), "fc:ground_speed");
        typeNameToCsvFieldName.put(TT_BATTERY_VOLTAGE.name(), "fc:main_voltage");
        typeNameToCsvFieldName.put(TT_CURRENT.name(), "fc:main_current");

        this.typeNameToCsvFieldName = Collections.unmodifiableMap(typeNameToCsvFieldName);
    }

    public String convertTypeName(String telemetryTypeName) {
        return typeNameToCsvFieldName.getOrDefault(telemetryTypeName, telemetryTypeName);
    }
}
