import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller layer: validation, model updates, persistence and export.
 */
public class EcoCityController {
    private static final Logger LOGGER = Logger.getLogger(EcoCityController.class.getName());

    private final EcoSmartCity city;
    private final CityPersistenceService persistenceService;
    private final DecimalFormat decimalFormat;

    public EcoCityController(EcoSmartCity city) {
        this.city = city;
        this.persistenceService = new CityPersistenceService();
        this.decimalFormat = new DecimalFormat("0.00");
    }

    public EntityRowData addEntity(EntityFormData data) {
        validate(data);

        CityEntity entity;
        String extraInfo;
        double cost;
        double carbon;
        double ecoScore;
        boolean alert;

        if ("Residential".equals(data.type())) {
            int residentCount = Integer.parseInt(data.extraRaw().trim());
            Residential residential = new Residential(data.id().trim(), data.name().trim(), data.energy(),
                    residentCount);
            entity = residential;
            extraInfo = residentCount + " Residents";
            cost = residential.calculateEnergyBill();
            carbon = data.energy() * 0.45;
            ecoScore = calculateEcoScore(residential);
            alert = residential.checkOveruse() || ecoScore < 50.0;
        } else {
            double pollution = Double.parseDouble(data.extraRaw().trim());
            Industrial industrial = new Industrial(data.id().trim(), data.name().trim(), data.energy(), pollution);
            entity = industrial;
            extraInfo = "Pollution: " + decimalFormat.format(pollution);
            cost = industrial.applyTaxPenalty();
            carbon = industrial.calculateCarbonFootprint();
            ecoScore = calculateEcoScore(industrial);
            alert = pollution > 100.0 || data.energy() > 900.0 || ecoScore < 50.0;
        }

        city.addEntity(entity);

        return new EntityRowData(
                entity.getEntityID(),
                entity.getName(),
                data.type(),
                decimalFormat.format(data.energy()),
                extraInfo,
                decimalFormat.format(cost),
                decimalFormat.format(carbon),
                decimalFormat.format(ecoScore),
                alert ? "ALERT" : "OK");
    }

    public List<EntityRowData> importFromJson(Path importPath) throws IOException {
        List<CityPersistenceService.PersistedEntity> records = persistenceService.load(importPath);
        return replaceEntities(records);
    }

    public List<EntityRowData> loadFromDisk() {
        try {
            return replaceEntities(persistenceService.load());
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to load persisted data", ex);
        }
        return List.of();
    }

    public void saveToDisk() {
        try {
            persistenceService.save(city.getEntities());
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Failed to save data", ex);
        }
    }

    public void exportToJson(Path exportPath) throws IOException {
        persistenceService.saveJsonArray(city.getEntities(), exportPath);
    }

    public void exportToCsv(Path exportPath) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("ID,Name,Type,Energy,ExtraInfo,BillOrPenaltyBDT,CarbonImpactKgCO2,EcoScore,Status");

        for (CityEntity entity : city.getEntities()) {
            EntityRowData row = mapEntityToRow(entity);
            lines.add(csv(row.id()) + "," + csv(row.name()) + "," + csv(row.type()) + "," + csv(row.energy()) + ","
                    + csv(row.extraInfo()) + "," + csv(row.billOrPenalty()) + "," + csv(row.carbonImpact()) + ","
                    + csv(row.ecoScore()) + "," + csv(row.status()));
        }

        Files.write(exportPath, lines, StandardCharsets.UTF_8);
    }

    public int getTotalEntities() {
        return city.getEntities().size();
    }

    public double getTotalCarbon() {
        double total = 0.0;
        for (CityEntity entity : city.getEntities()) {
            if (entity instanceof Industrial industrial) {
                total += industrial.calculateCarbonFootprint();
            } else {
                total += entity.getEnergyUsage() * 0.45;
            }
        }
        return total;
    }

    public int getAlertCount() {
        int alerts = 0;
        for (CityEntity entity : city.getEntities()) {
            if (entity instanceof Residential residential && residential.checkOveruse()) {
                alerts++;
            }
            if (entity instanceof Industrial industrial &&
                    (industrial.getPollutionLevel() > 100.0 || entity.getEnergyUsage() > 900.0)) {
                alerts++;
            }
        }
        return alerts;
    }

    public double getAverageEcoScore() {
        if (city.getEntities().isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (CityEntity entity : city.getEntities()) {
            total += calculateEcoScore(entity);
        }
        return total / city.getEntities().size();
    }

    public String getTopGreenEntitySummary() {
        if (city.getEntities().isEmpty()) {
            return "No entities";
        }
        CityEntity best = city.getEntities().get(0);
        double bestScore = calculateEcoScore(best);
        for (CityEntity entity : city.getEntities()) {
            double score = calculateEcoScore(entity);
            if (score > bestScore) {
                best = entity;
                bestScore = score;
            }
        }
        return best.getName() + " (" + best.getEntityID() + ") - " + decimalFormat.format(bestScore);
    }

    public double getAverageEnergyUsage() {
        if (city.getEntities().isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (CityEntity entity : city.getEntities()) {
            total += entity.getEnergyUsage();
        }
        return total / city.getEntities().size();
    }

    public String buildSustainabilityReport() {
        return new StringBuilder()
                .append("EcoSmart City Bangladesh Sustainability Report\n")
                .append("Total Entities: ").append(getTotalEntities()).append('\n')
                .append("Total Carbon: ").append(decimalFormat.format(getTotalCarbon())).append(" kg CO2\n")
                .append("Average Energy: ").append(decimalFormat.format(getAverageEnergyUsage())).append(" kW\n")
                .append("Average Eco Score: ").append(decimalFormat.format(getAverageEcoScore())).append(" / 100\n")
                .append("Alerts: ").append(getAlertCount()).append('\n')
                .append("Top Green Entity (BD): ").append(getTopGreenEntitySummary())
                .toString();
    }

    public boolean deleteEntityById(String entityId) {
        if (entityId == null || entityId.isBlank()) {
            return false;
        }
        boolean exists = city.containsEntityId(entityId.trim());
        if (!exists) {
            return false;
        }
        city.removeEntity(entityId.trim());
        return true;
    }

    public EntityRowData updateEntity(String originalId, EntityFormData data) {
        if (originalId == null || originalId.isBlank()) {
            throw new IllegalArgumentException("Original entity ID is required.");
        }

        CityEntity originalEntity = city.findById(originalId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Original entity not found."));

        validate(data, originalId.trim());

        CityEntity updatedEntity = buildEntity(data);
        city.removeEntity(originalEntity.getEntityID());
        city.addEntity(updatedEntity);
        return mapEntityToRow(updatedEntity);
    }

    private CityEntity buildEntity(EntityFormData data) {
        if ("Residential".equals(data.type())) {
            int residentCount = Integer.parseInt(data.extraRaw().trim());
            return new Residential(data.id().trim(), data.name().trim(), data.energy(), residentCount);
        }

        double pollution = Double.parseDouble(data.extraRaw().trim());
        return new Industrial(data.id().trim(), data.name().trim(), data.energy(), pollution);
    }

    private EntityRowData mapEntityToRow(CityEntity entity) {
        if (entity instanceof Residential residential) {
            double cost = residential.calculateEnergyBill();
            double carbon = entity.getEnergyUsage() * 0.45;
            double ecoScore = calculateEcoScore(residential);
            boolean alert = residential.checkOveruse();
            return new EntityRowData(
                    entity.getEntityID(),
                    entity.getName(),
                    "Residential",
                    decimalFormat.format(entity.getEnergyUsage()),
                    residential.getResidentCount() + " Residents",
                    decimalFormat.format(cost),
                    decimalFormat.format(carbon),
                    decimalFormat.format(ecoScore),
                    alert ? "ALERT" : "OK");
        }

        Industrial industrial = (Industrial) entity;
        double cost = industrial.applyTaxPenalty();
        double carbon = industrial.calculateCarbonFootprint();
        double ecoScore = calculateEcoScore(industrial);
        boolean alert = industrial.getPollutionLevel() > 100.0 || entity.getEnergyUsage() > 900.0;
        return new EntityRowData(
                entity.getEntityID(),
                entity.getName(),
                "Industrial",
                decimalFormat.format(entity.getEnergyUsage()),
                "Pollution: " + decimalFormat.format(industrial.getPollutionLevel()),
                decimalFormat.format(cost),
                decimalFormat.format(carbon),
                decimalFormat.format(ecoScore),
                alert ? "ALERT" : "OK");
    }

    private double calculateEcoScore(CityEntity entity) {
        double score;
        if (entity instanceof Residential residential) {
            score = 100.0 - (entity.getEnergyUsage() * 0.05) - (residential.getResidentCount() * 1.5);
        } else if (entity instanceof Industrial industrial) {
            score = 100.0 - (entity.getEnergyUsage() * 0.04) - (industrial.getPollutionLevel() * 0.25);
        } else {
            score = 0.0;
        }
        return Math.max(0.0, Math.min(100.0, score));
    }

    private void validate(EntityFormData data) {
        validate(data, null);
    }

    private void validate(EntityFormData data, String excludeEntityId) {
        if (data.id() == null || data.id().trim().isEmpty()) {
            throw new IllegalArgumentException("Entity ID is required.");
        }
        if (city.containsEntityId(data.id().trim())
                && (excludeEntityId == null || !data.id().trim().equalsIgnoreCase(excludeEntityId))) {
            throw new IllegalArgumentException("Duplicate Entity ID is not allowed.");
        }
        if (data.name() == null || data.name().trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required.");
        }
        if (data.energy() <= 0) {
            throw new IllegalArgumentException("Energy usage must be a positive number.");
        }
        if (data.extraRaw() == null || data.extraRaw().trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Residential".equals(data.type()) ? "Resident Count is required." : "Pollution Level is required.");
        }

        if ("Residential".equals(data.type())) {
            int residentCount;
            try {
                residentCount = Integer.parseInt(data.extraRaw().trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Resident Count must be an integer.");
            }
            if (residentCount <= 0) {
                throw new IllegalArgumentException("Resident Count must be greater than 0.");
            }
        } else {
            double pollution;
            try {
                pollution = Double.parseDouble(data.extraRaw().trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Pollution Level must be numeric.");
            }
            if (pollution < 0) {
                throw new IllegalArgumentException("Pollution Level cannot be negative.");
            }
        }
    }

    private String csv(String value) {
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private List<EntityRowData> replaceEntities(List<CityPersistenceService.PersistedEntity> records) {
        EcoSmartCity stagingCity = new EcoSmartCity();
        EcoCityController stagingController = new EcoCityController(stagingCity);
        List<EntityRowData> loadedRows = new ArrayList<>();

        for (CityPersistenceService.PersistedEntity record : records) {
            String extraRaw = "Residential".equals(record.type())
                    ? String.valueOf((int) Math.round(record.extra()))
                    : String.format(Locale.US, "%.2f", record.extra());

            EntityFormData formData = new EntityFormData(
                    record.id(),
                    record.name(),
                    record.type(),
                    record.energy(),
                    extraRaw);

            loadedRows.add(stagingController.addEntity(formData));
        }

        city.clearEntities();
        for (CityEntity entity : stagingCity.getEntities()) {
            city.addEntity(entity);
        }
        return loadedRows;
    }
}
