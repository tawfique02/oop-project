/**
 * DTO used by controller to populate JTable rows.
 */
public record EntityRowData(
        String id,
        String name,
        String type,
        String energy,
        String extraInfo,
        String billOrPenalty,
        String carbonImpact,
        String ecoScore,
        String status) {

    public Object[] toTableRow() {
        return new Object[] { id, name, type, energy, extraInfo, billOrPenalty, carbonImpact, ecoScore, status };
    }
}
