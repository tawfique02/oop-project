import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight JSON Lines persistence for city entities.
 */
public class CityPersistenceService {
    private static final Path DATA_PATH = Paths.get("data", "entities.jsonl");
    private static final Pattern ENTITY_PATTERN = Pattern.compile(
            "\\{\\\"id\\\":\\\"([^\\\"]*)\\\",\\\"name\\\":\\\"([^\\\"]*)\\\",\\\"type\\\":\\\"(Residential|Industrial)\\\",\\\"energy\\\":([-+0-9.Ee]+),\\\"extra\\\":([-+0-9.Ee]+)\\}");

    public record PersistedEntity(String id, String name, String type, double energy, double extra) {
    }

    public void save(List<CityEntity> entities) throws IOException {
        createParentDirectories(DATA_PATH);
        List<String> lines = new ArrayList<>();
        for (CityEntity entity : entities) {
            String type;
            double extra;
            if (entity instanceof Residential residential) {
                type = "Residential";
                extra = residential.getResidentCount();
            } else if (entity instanceof Industrial industrial) {
                type = "Industrial";
                extra = industrial.getPollutionLevel();
            } else {
                continue;
            }

            lines.add(String.format(Locale.US,
                    "{\"id\":\"%s\",\"name\":\"%s\",\"type\":\"%s\",\"energy\":%.6f,\"extra\":%.6f}",
                    escape(entity.getEntityID()),
                    escape(entity.getName()),
                    type,
                    entity.getEnergyUsage(),
                    extra));
        }
        Files.write(DATA_PATH, lines, StandardCharsets.UTF_8);
    }

    public List<PersistedEntity> load() throws IOException {
        if (!Files.exists(DATA_PATH)) {
            return List.of();
        }

        return load(DATA_PATH);
    }

    public List<PersistedEntity> load(Path sourcePath) throws IOException {
        if (!Files.exists(sourcePath)) {
            return List.of();
        }

        String content = Files.readString(sourcePath, StandardCharsets.UTF_8);
        List<PersistedEntity> entities = parseContent(content);
        if (!entities.isEmpty()) {
            validateStrictness(content, entities.size());
            return entities;
        }

        List<PersistedEntity> fallback = new ArrayList<>();
        for (String line : content.split("\\R")) {
            PersistedEntity entity = parseLine(line);
            if (entity != null) {
                fallback.add(entity);
            }
        }
        validateStrictness(content, fallback.size());
        return fallback;
    }

    public void saveJsonArray(List<CityEntity> entities, Path targetPath) throws IOException {
        createParentDirectories(targetPath);
        List<String> entries = new ArrayList<>();
        for (CityEntity entity : entities) {
            String type;
            double extra;
            if (entity instanceof Residential residential) {
                type = "Residential";
                extra = residential.getResidentCount();
            } else if (entity instanceof Industrial industrial) {
                type = "Industrial";
                extra = industrial.getPollutionLevel();
            } else {
                continue;
            }

            entries.add(String.format(Locale.US,
                    "{\"id\":\"%s\",\"name\":\"%s\",\"type\":\"%s\",\"energy\":%.6f,\"extra\":%.6f}",
                    escape(entity.getEntityID()),
                    escape(entity.getName()),
                    type,
                    entity.getEnergyUsage(),
                    extra));
        }

        String json = "[\n  " + String.join(",\n  ", entries) + "\n]";
        Files.writeString(targetPath, json, StandardCharsets.UTF_8);
    }

    private List<PersistedEntity> parseContent(String content) {
        List<PersistedEntity> entities = new ArrayList<>();
        Matcher matcher = ENTITY_PATTERN.matcher(content);
        while (matcher.find()) {
            entities.add(toEntity(matcher));
        }
        return entities;
    }

    private PersistedEntity parseLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || "[".equals(trimmed) || "]".equals(trimmed)) {
            return null;
        }
        if (trimmed.endsWith(",")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        Matcher matcher = ENTITY_PATTERN.matcher(trimmed);
        return matcher.matches() ? toEntity(matcher) : null;
    }

    private void validateStrictness(String content, int parsedCount) throws IOException {
        String trimmedContent = content.trim();
        if (trimmedContent.isEmpty() || "[]".equals(trimmedContent)) {
            return;
        }

        String leftover = ENTITY_PATTERN.matcher(content).replaceAll("");
        leftover = leftover.replaceAll("[\\s\\[\\],]", "").trim();

        if (parsedCount == 0 || !leftover.isEmpty()) {
            throw new IOException("Invalid persistence file format.");
        }
    }

    private PersistedEntity toEntity(Matcher matcher) {
        return new PersistedEntity(
                unescape(matcher.group(1)),
                unescape(matcher.group(2)),
                matcher.group(3),
                Double.parseDouble(matcher.group(4)),
                Double.parseDouble(matcher.group(5)));
    }

    private void createParentDirectories(Path targetPath) throws IOException {
        Path parent = targetPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
