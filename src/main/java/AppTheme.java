import java.awt.Color;
import java.awt.Font;

/**
 * Centralized UI theme constants. Keep all visual tokens here.
 */
public final class AppTheme {
    private AppTheme() {
    }

    public static final Color BG_DARK = new Color(11, 20, 31);
    public static final Color PANEL_DARK = new Color(20, 33, 47);
    public static final Color NEON_CYAN = new Color(96, 198, 203);
    public static final Color NEON_PINK = new Color(111, 146, 196);
    public static final Color NEON_GREEN = new Color(120, 198, 144);
    public static final Color TEXT_LIGHT = new Color(230, 238, 246);
    public static final Color MUTED_TEXT = new Color(156, 178, 198);
    public static final Color HEADER_BG = new Color(14, 27, 40);
    public static final Color HEADER_BORDER = new Color(80, 122, 158);
    public static final Color CARD_BORDER = new Color(69, 102, 132);
    public static final Color TABLE_BG = new Color(22, 37, 53);
    public static final Color TABLE_ROW_BG = new Color(26, 44, 62);
    public static final Color TABLE_ALT_ROW_BG = new Color(21, 38, 56);
    public static final Color TABLE_HEADER_BG = new Color(17, 33, 50);
    public static final Color TABLE_SELECTION_BG = new Color(52, 88, 123);
    public static final Color TABLE_GRID = new Color(58, 88, 118);
    public static final Color ALERT_ROW_BG = new Color(90, 41, 48);
    public static final Color ALERT_ROW_FG = new Color(255, 220, 220);
    public static final Color INPUT_BG = new Color(15, 27, 40);
    public static final Color INPUT_BORDER = new Color(77, 112, 142);

    public static final Color BUTTON_BG = new Color(44, 108, 166);
    public static final Color BUTTON_TEXT = new Color(248, 252, 255);
    public static final Color BUTTON_HOVER_BG = new Color(58, 126, 188);
    public static final Color BUTTON_PRESSED_BG = new Color(36, 92, 143);
    public static final Color SECONDARY_BUTTON_BG = new Color(61, 103, 146);
    public static final Color SECONDARY_BUTTON_HOVER_BG = new Color(75, 119, 164);
    public static final Color SECONDARY_BUTTON_PRESSED_BG = new Color(50, 88, 126);
    public static final Color NEUTRAL_BUTTON_BG = new Color(84, 95, 112);
    public static final Color NEUTRAL_BUTTON_HOVER_BG = new Color(100, 112, 132);
    public static final Color NEUTRAL_BUTTON_PRESSED_BG = new Color(68, 79, 96);
    public static final Color DANGER_BUTTON_BG = new Color(155, 63, 70);
    public static final Color DANGER_BUTTON_HOVER_BG = new Color(176, 77, 84);
    public static final Color DANGER_BUTTON_PRESSED_BG = new Color(132, 52, 60);

    public static final int SPACE_XS = 4;
    public static final int SPACE_SM = 8;
    public static final int SPACE_MD = 12;
    public static final int SPACE_LG = 16;

    public static final Font LABEL_FONT = new Font("Consolas", Font.BOLD, 13);
    public static final Font BODY_FONT = new Font("Consolas", Font.PLAIN, 12);
    public static final Font BODY_BOLD_FONT = new Font("Consolas", Font.BOLD, 12);
    public static final Font SECTION_TITLE_FONT = new Font("Consolas", Font.BOLD, 14);
    public static final Font METRIC_TITLE_FONT = new Font("Consolas", Font.BOLD, 13);
    public static final Font METRIC_VALUE_FONT = new Font("Consolas", Font.BOLD, 22);
    public static final Font CAPTION_FONT = new Font("Consolas", Font.PLAIN, 11);
    public static final Font BUTTON_FONT = new Font("Consolas", Font.BOLD, 14);
}
