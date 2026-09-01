import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class IndustrialTest {

    @Test
    void calculateCarbonFootprint_ShouldUsePollutionMultiplier() {
        Industrial industrial = new Industrial("I1", "Factory A", 500.0, 40.0);
        assertEquals(100.0, industrial.calculateCarbonFootprint(), 0.0001);
    }

    @Test
    void applyTaxPenalty_ShouldReturnPenalty_WhenPollutionAbove100() {
        Industrial industrial = new Industrial("I2", "Factory B", 600.0, 120.0);
        assertEquals(5000.0, industrial.applyTaxPenalty(), 0.0001);
    }
}
