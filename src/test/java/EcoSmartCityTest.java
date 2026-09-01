import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EcoSmartCityTest {

    @Test
    void addAndRemoveEntity_ShouldUpdateEntityCount() {
        EcoSmartCity city = new EcoSmartCity();
        city.addEntity(new Residential("R100", "Tower", 300.0, 20));
        assertEquals(1, city.getEntities().size());

        city.removeEntity("R100");
        assertEquals(0, city.getEntities().size());
    }

    @Test
    void containsEntityId_ShouldDetectDuplicate() {
        EcoSmartCity city = new EcoSmartCity();
        city.addEntity(new Industrial("I10", "Plant", 700.0, 80.0));
        assertTrue(city.containsEntityId("i10"));
    }

    @Test
    void addEntity_ShouldRejectDuplicateId() {
        EcoSmartCity city = new EcoSmartCity();
        city.addEntity(new Industrial("I10", "Plant", 700.0, 80.0));

        assertThrows(IllegalArgumentException.class,
                () -> city.addEntity(new Industrial("i10", "Other", 500.0, 20.0)));
    }
}
