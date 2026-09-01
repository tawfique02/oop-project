import java.util.Locale;

public class WasteManager {
    private static final String[] RECYCLABLE_KEYWORDS = {
            "plastic",
            "paper",
            "cardboard",
            "glass",
            "metal",
            "aluminum",
            "tin"
    };

    private double totalWasteCollected;

    public int calculateRecyclePoints(String wasteType, double weightKg) {
        if (weightKg <= 0) {
            return 0;
        }

        String normalizedWasteType = wasteType == null ? "" : wasteType.trim().toLowerCase(Locale.ROOT);
        int multiplier = switch (normalizedWasteType) {
            case "plastic" -> 12;
            case "paper", "cardboard" -> 8;
            case "glass" -> 10;
            case "metal", "aluminum", "tin" -> 15;
            default -> 5;
        };
        return (int) Math.round(weightKg * multiplier);
    }

    public String categorizeWaste(String item) {
        if (item == null || item.isBlank()) {
            return "Unknown";
        }

        String normalized = item.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("plastic") || normalized.contains("bottle") || normalized.contains("pack")) {
            return "Recyclable";
        }
        if (normalized.contains("paper") || normalized.contains("cardboard")) {
            return "Paper";
        }
        if (normalized.contains("glass")) {
            return "Glass";
        }
        if (normalized.contains("metal") || normalized.contains("tin") || normalized.contains("aluminum")) {
            return "Metal";
        }
        if (normalized.contains("food") || normalized.contains("organic") || normalized.contains("leaf")) {
            return "Organic";
        }
        return "General";
    }

    public String issueBadge(int points) {
        if (points >= 250) {
            return "Green Hero";
        }
        if (points >= 100) {
            return "Recycler";
        }
        if (points > 0) {
            return "Beginner";
        }
        return "No Badge";
    }

    public boolean validateRecyclable(String item) {
        if (item == null || item.isBlank()) {
            return false;
        }

        String normalized = item.trim().toLowerCase(Locale.ROOT);
        for (String keyword : RECYCLABLE_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public double getAverageWastePerDay() {
        return totalWasteCollected == 0.0 ? 0.0 : totalWasteCollected / 30.0;
    }

    public void addWasteCollected(double weightKg) {
        if (weightKg > 0) {
            totalWasteCollected += weightKg;
        }
    }

    public double getTotalWasteCollected() {
        return totalWasteCollected;
    }
}