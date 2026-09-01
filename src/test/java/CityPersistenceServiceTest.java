import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CityPersistenceServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void saveJsonArrayAndLoad_ShouldRoundTripEntities() throws Exception {
        CityPersistenceService service = new CityPersistenceService();
        Path exportFile = tempDir.resolve("backup.json");

        List<CityEntity> entities = List.of(
                new Residential("R1", "Home", 120.0, 4),
                new Industrial("I1", "Plant", 300.0, 40.0));

        service.saveJsonArray(entities, exportFile);
        List<CityPersistenceService.PersistedEntity> loaded = service.load(exportFile);

        assertEquals(2, loaded.size());
        assertEquals("R1", loaded.get(0).id());
        assertEquals("I1", loaded.get(1).id());
    }

    @Test
    void load_ShouldRejectMalformedMixedContent() throws Exception {
        CityPersistenceService service = new CityPersistenceService();
        Path badFile = tempDir.resolve("bad.json");

        Files.writeString(badFile,
                "[{\"id\":\"R1\",\"name\":\"Home\",\"type\":\"Residential\",\"energy\":120.0,\"extra\":4.0},"
                        + "bad-record]");

        assertThrows(IOException.class, () -> service.load(badFile));
    }
}