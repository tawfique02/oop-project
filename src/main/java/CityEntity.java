import java.util.Objects;

public abstract class CityEntity implements Trackable {
    protected String entityID;
    protected String name;
    protected double energyUsage;

    public CityEntity(String entityID, String name, double energyUsage) {
        setEntityID(entityID);
        this.name = Objects.requireNonNull(name, "Name is required.").trim();
        if (this.name.isEmpty()) {
            throw new IllegalArgumentException("Name is required.");
        }
        if (energyUsage <= 0) {
            throw new IllegalArgumentException("Energy usage must be a positive number.");
        }
        this.energyUsage = energyUsage;
    }

    public String getEntityID() {
        return entityID;
    }

    public void setEntityID(String id) {
        String normalizedId = Objects.requireNonNull(id, "Entity ID is required.").trim();
        if (normalizedId.isEmpty()) {
            throw new IllegalArgumentException("Entity ID is required.");
        }
        this.entityID = normalizedId;
    }

    public String getName() {
        return name;
    }

    public double getEnergyUsage() {
        return energyUsage;
    }

    public abstract void updateEnergy();

    public abstract void reportWaste();
}