import java.util.*;

public class EcoSmartCity {
    private final List<CityEntity> entities = new ArrayList<>();

    public void addEntity(CityEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null.");
        }
        if (containsEntityId(entity.getEntityID())) {
            throw new IllegalArgumentException("Duplicate Entity ID is not allowed.");
        }
        entities.add(entity);
    }

    public boolean containsEntityId(String entityID) {
        if (entityID == null || entityID.isBlank()) {
            return false;
        }
        return entities.stream().anyMatch(e -> e.getEntityID().equalsIgnoreCase(entityID.trim()));
    }

    public Optional<CityEntity> findById(String entityID) {
        if (entityID == null || entityID.isBlank()) {
            return Optional.empty();
        }
        return entities.stream().filter(e -> e.getEntityID().equalsIgnoreCase(entityID.trim())).findFirst();
    }

    public String generateSustainabilityReport() {
        return "Sustainability Report: Total Entities = " + entities.size();
    }

    public CityEntity getTopGreenEntity() {
        if (entities.isEmpty()) {
            return null;
        }

        CityEntity best = entities.get(0);
        double bestScore = estimateEcoScore(best);

        for (CityEntity entity : entities) {
            double score = estimateEcoScore(entity);
            if (score > bestScore) {
                best = entity;
                bestScore = score;
            }
        }
        return best;
    }

    public void removeEntity(String entityID) {
        if (entityID == null || entityID.isBlank()) {
            return;
        }
        entities.removeIf(e -> e.getEntityID().equalsIgnoreCase(entityID.trim()));
    }

    public void clearEntities() {
        entities.clear();
    }

    public List<CityEntity> getEntities() {
        return Collections.unmodifiableList(entities);
    }

    private double estimateEcoScore(CityEntity entity) {
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
}
