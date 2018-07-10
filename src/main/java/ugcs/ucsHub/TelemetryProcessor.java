package ugcs.ucsHub;

import com.ugcs.ucs.proto.DomainProto;
import com.ugcs.ucs.proto.DomainProto.TelemetryDto;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

public class TelemetryProcessor {
    private final List<TelemetryDto> telemetryDtoList;

    private final static List<String> ALL_TELEMETRY_TYPE_NAMES;

    static {
        ALL_TELEMETRY_TYPE_NAMES = Arrays.stream(DomainProto.TelemetryType.values())
                .map(Enum::toString).collect(Collectors.toList());
    }

    private SortedMap<Long, Map<String, TelemetryDto>> processedTelemetry = null;

    public TelemetryProcessor(List<TelemetryDto> telemetryDtoList) {
        this.telemetryDtoList = telemetryDtoList;
    }

    private SortedMap<Long, Map<String, TelemetryDto>> getProcessedTelemetry() {
        if (processedTelemetry == null) {
            synchronized (this) {
                if (processedTelemetry == null) {
                    processedTelemetry = telemetryDtoList.stream()
                            .sorted(comparing(TelemetryDto::getTime))
                            .collect(groupingBy(TelemetryDto::getTime, TreeMap::new,
                                    toMap(dto -> dto.getType().toString(), dto -> dto, (dto1, dto2) -> {
                                        System.err.println("*** Merge fail:");
                                        System.err.println(dto1);
                                        System.err.println(dto2);
                                        return dto1; // TODO: fix this data loss
                                    })));
                }
            }
        }
        return processedTelemetry;
    }

    public void printAsCsv(OutputStream out) {
        final PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(out)));
        printCsvHeader(writer);

        final Map<String, Float> currentRecord = new HashMap<>();
        ALL_TELEMETRY_TYPE_NAMES.forEach(typeName -> currentRecord.put(typeName, null));

        getProcessedTelemetry().forEach((epochMilli, dtoMap) -> {
            dtoMap.forEach((typeName, dto) ->
                    currentRecord.compute(typeName, (k, v) -> dto.getValue()));
            writer.println(convertDateTime(epochMilli) + "," +
                    ALL_TELEMETRY_TYPE_NAMES.stream()
                            .map(typeName -> {
                                final Float value = currentRecord.get(typeName);
                                if (value == null) {
                                    return "";
                                }
                                return value.toString();
                            })
                            .collect(joining(",")));
        });
        writer.flush();
    }

    private void printCsvHeader(PrintWriter writer) {
        writer.println("Time," + ALL_TELEMETRY_TYPE_NAMES.stream()
                .map(typeName -> CsvFieldMapper.mapper().convertTypeName(typeName))
                .collect(joining(",")));
    }

    private static String convertDateTime(long epochMilli) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault()).toString();
    }
}
