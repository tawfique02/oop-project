import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WasteManagerTest {

    @Test
    void calculateRecyclePoints_ShouldScaleByWasteTypeAndWeight() {
        WasteManager wasteManager = new WasteManager();

        assertEquals(36, wasteManager.calculateRecyclePoints("plastic", 3.0));
        assertEquals(30, wasteManager.calculateRecyclePoints("metal", 2.0));
    }

    @Test
    void categorizeWaste_ShouldReturnMeaningfulCategory() {
        WasteManager wasteManager = new WasteManager();

        assertEquals("Recyclable", wasteManager.categorizeWaste("plastic bottle"));
        assertEquals("Organic", wasteManager.categorizeWaste("food waste"));
        assertEquals("Unknown", wasteManager.categorizeWaste("   "));
    }

    @Test
    void validateRecyclable_ShouldRejectUnknownItems() {
        WasteManager wasteManager = new WasteManager();

        assertTrue(wasteManager.validateRecyclable("cardboard box"));
        assertFalse(wasteManager.validateRecyclable("banana peel"));
    }
}