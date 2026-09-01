import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ResidentialTest {

    @Test
    void calculateEnergyBill_ShouldReturnCorrectAmount() {
        Residential residential = new Residential("R1", "House A", 120.0, 3);
        assertEquals(1500.0, residential.calculateEnergyBill(), 0.0001);
    }

    @Test
    void checkOveruse_ShouldBeTrue_WhenEnergyAboveThreshold() {
        Residential residential = new Residential("R2", "House B", 700.0, 4);
        assertTrue(residential.checkOveruse());
    }
}
