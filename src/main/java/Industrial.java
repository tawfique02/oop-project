public class Industrial extends CityEntity {
    private double pollutionLevel;

    public Industrial(String id, String name, double energy, double pollution) {
        super(id, name, energy);
        if (pollution < 0) {
            throw new IllegalArgumentException("Pollution level cannot be negative.");
        }
        this.pollutionLevel = pollution;
    }

    @Override
    public void updateEnergy() {
        System.out.println("Industrial energy tracking...");
    }

    @Override
    public void reportWaste() {
        System.out.println("Industrial waste tracking...");
    }

    public double calculateCarbonFootprint() {
        return pollutionLevel * 2.5;
    }

    public double applyTaxPenalty() {
        return pollutionLevel > 100 ? 5000.0 : 0.0;
    }

    public double getPollutionLevel() {
        return pollutionLevel;
    }
}