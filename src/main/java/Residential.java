public class Residential extends CityEntity {
    private int residentCount;

    public Residential(String id, String name, double energy, int count) {
        super(id, name, energy);
        if (count <= 0) {
            throw new IllegalArgumentException("Resident count must be greater than 0.");
        }
        this.residentCount = count;
    }

    @Override
    public void updateEnergy() {
        System.out.println(name + " energy updated for " + residentCount + " residents.");
    }

    @Override
    public void reportWaste() {
        System.out.println(name + " waste reported for " + residentCount + " residents.");
    }

    public double calculateEnergyBill() {
        return energyUsage * 12.5;
    }

    public boolean checkOveruse() {
        return energyUsage > 500;
    }

    public int getResidentCount() {
        return residentCount;
    }
}