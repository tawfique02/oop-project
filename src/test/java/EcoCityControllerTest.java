import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EcoCityControllerTest {

    @TempDir
    Path tempDir;

    @Test
    void importFromJson_ShouldBeAtomicWhenDataIsInvalid() throws Exception {
        EcoSmartCity city = new EcoSmartCity();
        EcoCityController controller = new EcoCityController(city);
        controller.addEntity(new EntityFormData("R1", "Home", "Residential", 120.0, "4"));

        Path importFile = tempDir.resolve("invalid.json");
        Files.writeString(importFile,
                "[{\"id\":\"R2\",\"name\":\"A\",\"type\":\"Residential\",\"energy\":100.0,\"extra\":3.0},"
                        + "{\"id\":\"R3\",\"name\":\"B\",\"type\":\"Residential\",\"energy\":100.0,\"extra\":0.0}]");

        assertThrows(IllegalArgumentException.class, () -> controller.importFromJson(importFile));
        assertEquals(1, city.getEntities().size());
        assertEquals("R1", city.getEntities().get(0).getEntityID());
    }

    @Test
    void importFromJson_ShouldReplaceCityWithValidData() throws Exception {
        EcoSmartCity city = new EcoSmartCity();
        EcoCityController controller = new EcoCityController(city);

        Path importFile = tempDir.resolve("valid.json");
        Files.writeString(importFile,
                "[{\"id\":\"R2\",\"name\":\"Tower\",\"type\":\"Residential\",\"energy\":100.0,\"extra\":5.0},"
                        + "{\"id\":\"I1\",\"name\":\"Plant\",\"type\":\"Industrial\",\"energy\":300.0,\"extra\":40.0}]");

        List<EntityRowData> rows = controller.importFromJson(importFile);

        assertEquals(2, rows.size());
        assertEquals(2, city.getEntities().size());
        assertEquals("R2", city.getEntities().get(0).getEntityID());
        assertEquals("I1", city.getEntities().get(1).getEntityID());
    }
}