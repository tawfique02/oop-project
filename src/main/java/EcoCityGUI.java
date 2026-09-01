import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * Main GUI for EcoSmart City Manager.
 */
public class EcoCityGUI extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(EcoCityGUI.class.getName());

    private final EcoSmartCity ecoSmartCity = new EcoSmartCity();
    private final EcoCityController controller = new EcoCityController(ecoSmartCity);
    private final DecimalFormat decimalFormat = new DecimalFormat("0.00");

    private JTextField idField;
    private JTextField nameField;
    private JTextField energyField;
    private JTextField extraValueField;
    private JComboBox<String> typeBox;
    private JLabel extraValueLabel;

    private JTable entityTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JTextField searchField;
    private JComboBox<String> filterTypeBox;
    private JLabel filterStatsLabel;

    private JLabel totalEntitiesLabel;
    private JLabel totalCarbonLabel;
    private JLabel alertsLabel;
    private JLabel ecoScoreLabel;
    private JLabel statusLabel;
    private JLabel statusDetailLabel;
    private JLabel chartResidentialLabel;
    private JLabel chartIndustrialLabel;
    private JLabel chartAlertLabel;

    private CardLayout tableCardLayout;
    private JPanel tableCardPanel;
    private CityMixChartPanel cityMixChartPanel;
    private JButton primaryActionButton;
    private String editingOriginalId;
    private javax.swing.Timer liveUiTimer;
    private long lastActionMillis;
    private final DateTimeFormatter clockFormatter = DateTimeFormatter.ofPattern("hh:mm:ss a");

    public EcoCityGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to default look and feel.
        }

        setTitle("EcoSmart City Manager Bangladesh - Real-time Dashboard");
        setSize(1100, 680);
        setMinimumSize(new java.awt.Dimension(980, 620));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(AppTheme.SPACE_MD, AppTheme.SPACE_MD));
        getContentPane().setBackground(AppTheme.BG_DARK);
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(
                AppTheme.SPACE_SM, AppTheme.SPACE_SM, AppTheme.SPACE_SM, AppTheme.SPACE_SM));

        JPanel headerPanel = buildHeaderPanel();
        JPanel formPanel = buildFormPanel();
        JPanel buttonPanel = buildActionButtons();
        JPanel tableAreaPanel = buildTableArea();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formPanel, tableAreaPanel);
        splitPane.setResizeWeight(0.32);
        splitPane.setDividerLocation(340);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setBackground(AppTheme.BG_DARK);

        add(headerPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        bindKeyboardShortcuts((JButton) buttonPanel.getComponent(0));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopLiveUiTimer();
                controller.saveToDisk();
            }
        });

        loadPersistedData();
        updateDashboardStats();
        updateEmptyState();
        startLiveUiTimer();
    }

    private JPanel buildHeaderPanel() {
        JPanel headerPanel = new JPanel(new GridLayout(1, 5, AppTheme.SPACE_MD, 0));
        styleHeaderPanel(headerPanel);

        totalEntitiesLabel = new JLabel("0", JLabel.CENTER);
        totalCarbonLabel = new JLabel("0.00 kg CO2", JLabel.CENTER);
        alertsLabel = new JLabel("0", JLabel.CENTER);
        ecoScoreLabel = new JLabel("0.00 / 100", JLabel.CENTER);
        statusLabel = new JLabel("READY", JLabel.CENTER);
        statusDetailLabel = new JLabel("Top Green (BD): No entities", JLabel.CENTER);

        headerPanel.add(createMetricCard("ENT", "Total Entities", totalEntitiesLabel,
                AppTheme.NEON_GREEN, "Bangladesh city inventory"));
        headerPanel.add(createMetricCard("CO2", "Total Carbon", totalCarbonLabel,
                AppTheme.NEON_CYAN, "Overall emissions (kg CO2)"));
        headerPanel.add(createMetricCard("ALR", "Alerts", alertsLabel,
                AppTheme.DANGER_BUTTON_HOVER_BG, "High risk entities"));
        headerPanel.add(createMetricCard("ECO", "Eco Score", ecoScoreLabel,
                AppTheme.SECONDARY_BUTTON_HOVER_BG, "Average sustainability (BD)"));
        headerPanel.add(createStatusCard());
        return headerPanel;
    }

    private JPanel buildFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        styleSectionPanel(formPanel, "Add New Entity", AppTheme.NEON_PINK);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(AppTheme.SPACE_SM, AppTheme.SPACE_MD, AppTheme.SPACE_SM, AppTheme.SPACE_MD);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel sectionHint = new JLabel("Track Dhaka/Chattogram style residential and industrial data live");
        sectionHint.setForeground(AppTheme.MUTED_TEXT);
        sectionHint.setFont(AppTheme.BODY_FONT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        formPanel.add(sectionHint, gbc);

        typeBox = new JComboBox<>(new String[] { "Residential", "Industrial" });
        styleTypeCombo(typeBox);
        addFormRow(formPanel, gbc, 1, "Type:", typeBox);

        idField = new JTextField();
        styleTextField(idField);
        addFormRow(formPanel, gbc, 2, "Entity ID:", idField);

        nameField = new JTextField();
        styleTextField(nameField);
        addFormRow(formPanel, gbc, 3, "Name:", nameField);

        energyField = new JTextField();
        styleTextField(energyField);
        addFormRow(formPanel, gbc, 4, "Energy Usage (kW):", energyField);

        extraValueField = new JTextField();
        styleTextField(extraValueField);
        extraValueLabel = addFormRow(formPanel, gbc, 5, "Resident Count:", extraValueField);

        JLabel helpLabel = new JLabel("Press Enter to add / Esc to clear");
        helpLabel.setForeground(AppTheme.MUTED_TEXT);
        helpLabel.setFont(AppTheme.BODY_FONT);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        formPanel.add(helpLabel, gbc);

        JLabel validationHint = new JLabel("No duplicate ID, no negative values, use BDT-aware billing");
        validationHint.setForeground(AppTheme.MUTED_TEXT);
        validationHint.setFont(AppTheme.BODY_FONT);

        gbc.gridy = 7;
        formPanel.add(validationHint, gbc);

        gbc.gridy = 8;
        gbc.weighty = 1.0;
        formPanel.add(new JLabel(""), gbc);

        typeBox.addActionListener(e -> updateExtraFieldLabel());
        updateExtraFieldLabel();

        return formPanel;
    }

    private JPanel buildActionButtons() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, AppTheme.SPACE_MD, AppTheme.SPACE_SM));
        buttonPanel.setBackground(AppTheme.BG_DARK);
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.HEADER_BORDER),
                BorderFactory.createEmptyBorder(6, 8, 8, 8)));
        buttonPanel.setPreferredSize(new Dimension(100, 76));

        primaryActionButton = createPrimaryButton("Add & Calculate", AppTheme.BUTTON_BG, AppTheme.BUTTON_HOVER_BG,
                AppTheme.BUTTON_PRESSED_BG, this::handlePrimaryAction);
        JButton editButton = createPrimaryButton("Edit Selected", AppTheme.SECONDARY_BUTTON_BG,
                AppTheme.SECONDARY_BUTTON_HOVER_BG,
                AppTheme.SECONDARY_BUTTON_PRESSED_BG, this::handleEditSelected);
        JButton saveButton = createPrimaryButton("Save", AppTheme.SECONDARY_BUTTON_BG,
                AppTheme.SECONDARY_BUTTON_HOVER_BG,
                AppTheme.SECONDARY_BUTTON_PRESSED_BG, () -> {
                    controller.saveToDisk();
                    setStatus("Saved", AppTheme.NEON_GREEN);
                });

        JButton exportButton = createPrimaryButton("Export CSV", AppTheme.SECONDARY_BUTTON_BG,
                AppTheme.SECONDARY_BUTTON_HOVER_BG,
                AppTheme.SECONDARY_BUTTON_PRESSED_BG, this::handleExportCsv);

        JButton moreButton = createPrimaryButton("More", AppTheme.NEUTRAL_BUTTON_BG, AppTheme.NEUTRAL_BUTTON_HOVER_BG,
                AppTheme.NEUTRAL_BUTTON_PRESSED_BG, this::showMoreOptionsDialog);

        JButton deleteButton = createPrimaryButton("Delete Selected", AppTheme.DANGER_BUTTON_BG,
                AppTheme.DANGER_BUTTON_HOVER_BG,
                AppTheme.DANGER_BUTTON_PRESSED_BG, this::handleDeleteSelected);

        buttonPanel.add(primaryActionButton);
        buttonPanel.add(editButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(moreButton);
        buttonPanel.add(deleteButton);
        return buttonPanel;
    }

    private JPanel buildTableArea() {
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.SPACE_MD, AppTheme.SPACE_SM));
        toolbarPanel.setBackground(AppTheme.BG_DARK);

        JLabel searchLabel = new JLabel("Search:");
        styleFormLabel(searchLabel);
        toolbarPanel.add(searchLabel);

        searchField = new JTextField(16);
        styleTextField(searchField);
        toolbarPanel.add(searchField);

        JLabel filterLabel = new JLabel("Filter:");
        styleFormLabel(filterLabel);
        toolbarPanel.add(filterLabel);

        filterTypeBox = new JComboBox<>(new String[] { "All", "Residential", "Industrial", "ALERT" });
        styleTypeCombo(filterTypeBox);
        toolbarPanel.add(filterTypeBox);

        filterStatsLabel = new JLabel("Showing 0/0");
        filterStatsLabel.setForeground(AppTheme.NEON_GREEN);
        filterStatsLabel.setFont(AppTheme.BODY_BOLD_FONT);
        toolbarPanel.add(filterStatsLabel);

        String[] columns = {
                "ID", "Name", "Type", "Energy", "Extra Info", "Bill/Penalty (BDT)", "Carbon Impact (kg CO2)",
                "Eco Score",
                "Status"
        };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        entityTable = new JTable(tableModel);
        entityTable.setBackground(AppTheme.TABLE_BG);
        entityTable.setForeground(AppTheme.TEXT_LIGHT);
        entityTable.setSelectionBackground(AppTheme.TABLE_SELECTION_BG);
        entityTable.setSelectionForeground(AppTheme.NEON_CYAN);
        entityTable.setGridColor(AppTheme.TABLE_GRID);
        entityTable.setRowHeight(26);
        entityTable.setFont(AppTheme.BODY_FONT);
        entityTable.getTableHeader().setReorderingAllowed(false);

        entityTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column) {
                Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                JLabel labelCell = (JLabel) cell;
                int modelRow = table.convertRowIndexToModel(row);
                String status = String.valueOf(table.getModel().getValueAt(modelRow, 8));

                if (isSelected) {
                    cell.setBackground(AppTheme.TABLE_SELECTION_BG);
                    cell.setForeground(Color.WHITE);
                    cell.setFont(cell.getFont().deriveFont(Font.BOLD));
                } else if ("ALERT".equals(status)) {
                    cell.setBackground(AppTheme.ALERT_ROW_BG);
                    cell.setForeground(AppTheme.ALERT_ROW_FG);
                    cell.setFont(cell.getFont().deriveFont(Font.BOLD));
                } else {
                    cell.setBackground((row % 2 == 0) ? AppTheme.TABLE_ROW_BG : AppTheme.TABLE_ALT_ROW_BG);
                    cell.setForeground(AppTheme.TEXT_LIGHT);
                    cell.setFont(cell.getFont().deriveFont(Font.PLAIN));
                }

                if (column == 8) {
                    labelCell.setHorizontalAlignment(SwingConstants.CENTER);
                }
                return cell;
            }
        });

        entityTable.getTableHeader().setBackground(AppTheme.TABLE_HEADER_BG);
        entityTable.getTableHeader().setForeground(AppTheme.TEXT_LIGHT);
        entityTable.getTableHeader().setFont(AppTheme.METRIC_TITLE_FONT);
        entityTable.getTableHeader().setOpaque(true);
        entityTable.getTableHeader().setPreferredSize(new java.awt.Dimension(10, 28));
        entityTable.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column) {
                JLabel headerCell = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                headerCell.setOpaque(true);
                headerCell.setBackground(AppTheme.TABLE_HEADER_BG);
                headerCell.setForeground(AppTheme.TEXT_LIGHT);
                headerCell.setFont(AppTheme.METRIC_TITLE_FONT);
                headerCell.setHorizontalAlignment(SwingConstants.CENTER);
                headerCell.setBorder(BorderFactory.createLineBorder(AppTheme.TABLE_GRID, 1));
                return headerCell;
            }
        });

        rowSorter = new TableRowSorter<>(tableModel);
        entityTable.setRowSorter(rowSorter);

        JScrollPane tableScroll = new JScrollPane(entityTable);
        tableScroll.getViewport().setBackground(AppTheme.TABLE_BG);
        TitledBorder tableBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(AppTheme.NEON_GREEN, 1),
                "Real-time Bangladesh City Status");
        tableBorder.setTitleColor(AppTheme.NEON_GREEN);
        tableBorder.setTitleFont(AppTheme.METRIC_TITLE_FONT);
        tableScroll.setBorder(tableBorder);

        tableCardLayout = new CardLayout();
        tableCardPanel = new JPanel(tableCardLayout);
        tableCardPanel.setBackground(AppTheme.BG_DARK);

        JPanel emptyStatePanel = new JPanel(new BorderLayout());
        emptyStatePanel.setBackground(AppTheme.TABLE_BG);
        JPanel emptyContent = new JPanel();
        emptyContent.setOpaque(false);
        emptyContent.setLayout(new javax.swing.BoxLayout(emptyContent, javax.swing.BoxLayout.Y_AXIS));

        JLabel emptyIcon = new JLabel("DATA READY", SwingConstants.CENTER);
        emptyIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyIcon.setFont(AppTheme.METRIC_VALUE_FONT.deriveFont(18f));
        emptyIcon.setForeground(AppTheme.NEON_CYAN);

        JLabel emptyLabel = new JLabel("No entities yet. Add your first entity.", SwingConstants.CENTER);
        emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyLabel.setFont(AppTheme.METRIC_VALUE_FONT.deriveFont(18f));
        emptyLabel.setForeground(AppTheme.NEON_GREEN);

        JLabel emptyHint = new JLabel("Use the form on the left, then save/export BD city dashboard.",
                SwingConstants.CENTER);
        emptyHint.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyHint.setFont(AppTheme.BODY_FONT);
        emptyHint.setForeground(AppTheme.MUTED_TEXT);

        emptyContent.add(Box.createVerticalGlue());
        emptyContent.add(emptyIcon);
        emptyContent.add(Box.createVerticalStrut(AppTheme.SPACE_MD));
        emptyContent.add(emptyLabel);
        emptyContent.add(Box.createVerticalStrut(AppTheme.SPACE_SM));
        emptyContent.add(emptyHint);
        emptyContent.add(Box.createVerticalGlue());
        emptyStatePanel.add(emptyContent, BorderLayout.CENTER);

        tableCardPanel.add(emptyStatePanel, "EMPTY");
        tableCardPanel.add(tableScroll, "TABLE");

        cityMixChartPanel = new CityMixChartPanel();
        cityMixChartPanel.setPreferredSize(new Dimension(360, 230));

        chartResidentialLabel = createMiniStatChip("Residential", "0", AppTheme.NEON_GREEN);
        chartIndustrialLabel = createMiniStatChip("Industrial", "0", AppTheme.SECONDARY_BUTTON_HOVER_BG);
        chartAlertLabel = createMiniStatChip("Alerts", "0", AppTheme.DANGER_BUTTON_HOVER_BG);

        JPanel chartSummaryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.SPACE_MD, AppTheme.SPACE_SM));
        chartSummaryPanel.setOpaque(false);
        chartSummaryPanel.add(chartResidentialLabel);
        chartSummaryPanel.add(chartIndustrialLabel);
        chartSummaryPanel.add(chartAlertLabel);

        JPanel chartCard = new JPanel(new BorderLayout());
        chartCard.setBackground(AppTheme.TABLE_BG);
        TitledBorder chartBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(AppTheme.NEON_CYAN, 1),
                "Bangladesh City Mix (Live)");
        chartBorder.setTitleColor(AppTheme.NEON_CYAN);
        chartBorder.setTitleFont(AppTheme.METRIC_TITLE_FONT);
        chartCard.setBorder(chartBorder);
        chartCard.add(chartSummaryPanel, BorderLayout.NORTH);
        chartCard.add(cityMixChartPanel, BorderLayout.CENTER);

        JPanel mainContent = new JPanel(new BorderLayout(AppTheme.SPACE_MD, AppTheme.SPACE_MD));
        mainContent.setBackground(AppTheme.BG_DARK);
        mainContent.add(toolbarPanel, BorderLayout.NORTH);
        mainContent.add(tableCardPanel, BorderLayout.CENTER);
        mainContent.add(chartCard, BorderLayout.SOUTH);

        bindTableFilters();
        updateFilterStats();
        return mainContent;
    }

    private JLabel addFormRow(JPanel formPanel, GridBagConstraints gbc, int row, String labelText, JComponent input) {
        JLabel label = new JLabel(labelText);
        styleFormLabel(label);
        label.setHorizontalAlignment(SwingConstants.RIGHT);

        gbc.gridwidth = 1;
        gbc.weightx = 0.35;
        gbc.weighty = 0.0;
        gbc.gridx = 0;
        gbc.gridy = row;
        formPanel.add(label, gbc);

        gbc.weightx = 0.65;
        gbc.gridx = 1;
        formPanel.add(input, gbc);
        return label;
    }

    private void bindTableFilters() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilters();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilters();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilters();
            }
        });

        filterTypeBox.addActionListener(e -> applyFilters());
    }

    private void applyFilters() {
        String keyword = searchField.getText().trim().toLowerCase();
        String typeFilter = (String) filterTypeBox.getSelectedItem();

        RowFilter<DefaultTableModel, Object> combinedFilter = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Object> entry) {
                String id = String.valueOf(entry.getValue(0)).toLowerCase();
                String name = String.valueOf(entry.getValue(1)).toLowerCase();
                String type = String.valueOf(entry.getValue(2));
                String status = String.valueOf(entry.getValue(8));

                boolean matchesKeyword = keyword.isEmpty() || id.contains(keyword) || name.contains(keyword);
                boolean matchesType = "All".equals(typeFilter)
                        || typeFilter.equals(type)
                        || ("ALERT".equals(typeFilter) && "ALERT".equals(status));

                return matchesKeyword && matchesType;
            }
        };

        rowSorter.setRowFilter(combinedFilter);
        updateFilterStats();
    }

    private void updateFilterStats() {
        if (filterStatsLabel == null || tableModel == null || entityTable == null) {
            return;
        }

        int visible = entityTable.getRowCount();
        int total = tableModel.getRowCount();
        filterStatsLabel.setText("Showing " + visible + "/" + total);
    }

    private void bindKeyboardShortcuts(JButton addButton) {
        getRootPane().setDefaultButton(addButton);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "clearForm");
        getRootPane().getActionMap().put("clearForm", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                clearInputs();
                setStatus("Form cleared", AppTheme.NEON_CYAN);
            }
        });

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("control S"), "saveData");
        getRootPane().getActionMap().put("saveData", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                controller.saveToDisk();
                setStatus("Saved", AppTheme.NEON_GREEN);
            }
        });

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("control E"), "exportCsv");
        getRootPane().getActionMap().put("exportCsv", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                handleExportCsv();
            }
        });

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("control J"), "exportJson");
        getRootPane().getActionMap().put("exportJson", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                handleExportJson();
            }
        });

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("control I"), "importJson");
        getRootPane().getActionMap().put("importJson", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                handleImportJson();
            }
        });

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("control D"), "deleteSelected");
        getRootPane().getActionMap().put("deleteSelected", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                handleDeleteSelected();
            }
        });

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("control R"), "sustainabilityReport");
        getRootPane().getActionMap().put("sustainabilityReport", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                showSustainabilityReport();
            }
        });
    }

    private JButton createPrimaryButton(
            String text,
            Color base,
            Color hover,
            Color pressed,
            Runnable action) {
        final Color outline = AppTheme.HEADER_BORDER;
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(outline);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        button.setFont(AppTheme.BUTTON_FONT);
        button.setBackground(base);
        button.setForeground(Color.WHITE);
        button.setContentAreaFilled(false);
        button.setBorderPainted(true);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        button.addActionListener(e -> action.run());
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(base);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(pressed);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (button.contains(e.getPoint())) {
                    button.setBackground(hover);
                } else {
                    button.setBackground(base);
                }
            }
        });
        return button;
    }

    private void styleSectionPanel(JPanel panel, String title, Color accent) {
        panel.setBackground(AppTheme.PANEL_DARK);
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(accent, 2),
                        BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, AppTheme.SPACE_MD,
                                AppTheme.SPACE_MD, AppTheme.SPACE_MD)),
                title);
        border.setTitleColor(accent);
        border.setTitleFont(AppTheme.SECTION_TITLE_FONT);
        panel.setBorder(border);
    }

    private void styleHeaderPanel(JPanel panel) {
        panel.setBackground(AppTheme.HEADER_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.HEADER_BORDER, 1),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_SM, AppTheme.SPACE_SM,
                        AppTheme.SPACE_SM, AppTheme.SPACE_SM)));
    }

    private void styleFormLabel(JLabel label) {
        label.setForeground(AppTheme.TEXT_LIGHT);
        label.setFont(AppTheme.LABEL_FONT.deriveFont(14f));
    }

    private void styleTextField(JTextField field) {
        field.setBackground(AppTheme.INPUT_BG);
        field.setForeground(Color.WHITE);
        field.setCaretColor(AppTheme.NEON_CYAN);
        field.setFont(AppTheme.LABEL_FONT.deriveFont(14f));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.INPUT_BORDER, 2),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_SM - 1, AppTheme.SPACE_SM,
                        AppTheme.SPACE_SM - 1, AppTheme.SPACE_SM)));
        field.setPreferredSize(new Dimension(250, 34));
    }

    private void styleTypeCombo(JComboBox<String> combo) {
        combo.setBackground(AppTheme.TEXT_LIGHT);
        combo.setForeground(AppTheme.HEADER_BG);
        combo.setFont(AppTheme.LABEL_FONT.deriveFont(14f));
        combo.setBorder(BorderFactory.createLineBorder(AppTheme.NEON_CYAN, 2));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {
                JLabel cell = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                cell.setOpaque(true);
                if (index == -1) {
                    cell.setBackground(AppTheme.TEXT_LIGHT);
                    cell.setForeground(AppTheme.HEADER_BG);
                } else if (isSelected) {
                    cell.setBackground(AppTheme.NEON_CYAN);
                    cell.setForeground(AppTheme.HEADER_BG);
                } else {
                    cell.setBackground(AppTheme.INPUT_BG);
                    cell.setForeground(AppTheme.TEXT_LIGHT);
                }
                cell.setFont(AppTheme.LABEL_FONT.deriveFont(14f));
                return cell;
            }
        });
    }

    private void updateExtraFieldLabel() {
        boolean isResidential = "Residential".equals(typeBox.getSelectedItem());
        extraValueLabel.setText(isResidential ? "Resident Count:" : "Pollution Level:");
        extraValueField.setText("");
        animateStatusBadge(isResidential ? AppTheme.NEON_GREEN : AppTheme.SECONDARY_BUTTON_HOVER_BG);
    }

    private void handleAddEntity() {
        try {
            double energy;
            try {
                energy = Double.parseDouble(energyField.getText().trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Energy usage must be numeric.");
            }

            EntityFormData formData = new EntityFormData(
                    idField.getText().trim(),
                    nameField.getText().trim(),
                    (String) typeBox.getSelectedItem(),
                    energy,
                    extraValueField.getText().trim());

            EntityRowData rowData = controller.addEntity(formData);
            tableModel.addRow(rowData.toTableRow());
            controller.saveToDisk();

            updateDashboardStats();
            updateEmptyState();
            clearInputs();
            lastActionMillis = System.currentTimeMillis();
            setStatus("Saved", AppTheme.NEON_GREEN, "Alerts: " + controller.getAlertCount());
            flashTableState(rowData.id(), true);
        } catch (IllegalArgumentException ex) {
            LOGGER.log(Level.INFO, "Validation failed: {0}", ex.getMessage());
            setStatus("Validation failed", new Color(255, 160, 160), ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unexpected error while adding entity", ex);
            setStatus("Unexpected error", new Color(255, 160, 160), "Check logs for details");
            JOptionPane.showMessageDialog(this, "Something went wrong. Please try again.",
                    "System Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleExportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Table Data");
        chooser.setSelectedFile(new java.io.File("eco-city-report.csv"));

        int choice = chooser.showSaveDialog(this);
        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            Path exportPath = chooser.getSelectedFile().toPath();
            controller.exportToCsv(exportPath);
            setStatus("Exported", AppTheme.NEON_GREEN, exportPath.getFileName().toString());
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "CSV export failed", ex);
            setStatus("Export failed", new Color(255, 160, 160), "CSV could not be written");
            JOptionPane.showMessageDialog(this, "Failed to export CSV.", "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleExportJson() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export JSON Backup");
        chooser.setSelectedFile(new java.io.File("eco-city-backup.json"));

        int choice = chooser.showSaveDialog(this);
        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            controller.exportToJson(chooser.getSelectedFile().toPath());
            setStatus("Exported", AppTheme.NEON_GREEN, "JSON backup saved");
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "JSON export failed", ex);
            setStatus("Export failed", new Color(255, 160, 160), "JSON backup could not be written");
            JOptionPane.showMessageDialog(this, "Failed to export JSON.", "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleImportJson() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import JSON Backup");

        int choice = chooser.showOpenDialog(this);
        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            List<EntityRowData> rows = controller.importFromJson(chooser.getSelectedFile().toPath());
            tableModel.setRowCount(0);
            for (EntityRowData row : rows) {
                tableModel.addRow(row.toTableRow());
            }
            updateDashboardStats();
            updateEmptyState();
            setStatus("Imported", AppTheme.NEON_GREEN, rows.size() + " entities loaded");
            if (!rows.isEmpty()) {
                flashTableState(rows.get(0).id(), false);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "JSON import failed", ex);
            setStatus("Import failed", new Color(255, 160, 160), "Invalid backup or data format");
            JOptionPane.showMessageDialog(this, "Failed to import JSON backup.", "Import Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleViewSelectedDetails() {
        int viewRow = entityTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a row first.", "Details",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int modelRow = entityTable.convertRowIndexToModel(viewRow);
        String details = "ID: " + tableModel.getValueAt(modelRow, 0)
                + "\nName: " + tableModel.getValueAt(modelRow, 1)
                + "\nType: " + tableModel.getValueAt(modelRow, 2)
                + "\nEnergy: " + tableModel.getValueAt(modelRow, 3)
                + "\nExtra Info: " + tableModel.getValueAt(modelRow, 4)
                + "\nBill/Penalty: " + tableModel.getValueAt(modelRow, 5)
                + "\nCarbon Impact: " + tableModel.getValueAt(modelRow, 6)
                + "\nEco Score: " + tableModel.getValueAt(modelRow, 7)
                + "\nStatus: " + tableModel.getValueAt(modelRow, 8);

        JOptionPane.showMessageDialog(this, details, "Entity Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showSustainabilityReport() {
        JOptionPane.showMessageDialog(this, controller.buildSustainabilityReport(),
                "Sustainability Report", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showMoreOptionsDialog() {
        String[] options = { "Row Details", "Export JSON", "Import JSON", "Report", "Quick Tips", "Cancel" };
        int choice = JOptionPane.showOptionDialog(this,
                "Choose an action:",
                "More Options",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]);

        switch (choice) {
            case 0 -> handleViewSelectedDetails();
            case 1 -> handleExportJson();
            case 2 -> handleImportJson();
            case 3 -> showSustainabilityReport();
            case 4 -> showQuickTipsGuide();
            default -> {
            }
        }
    }

    private void showQuickTipsGuide() {
        String tips = "EcoSmart City Bangladesh Quick Tips\n\n"
                + "1) Add Data Cleanly\n"
                + "   Use unique Entity ID and valid numeric values.\n\n"
                + "2) Carbon Impact Measure\n"
                + "   Residential: Carbon Impact = Energy Usage x 0.45\n"
                + "   Industrial: Carbon Impact = Pollution Level x 2.5\n"
                + "   Total Carbon = sum of all entity carbon impacts.\n\n"
                + "3) How Project Calculates\n"
                + "   Residential Bill (BDT) = Energy Usage x 12.5\n"
                + "   Industrial Penalty (BDT) = 5000 if Pollution Level > 100, else 0\n"
                + "   Residential Eco Score = 100 - (Energy x 0.05) - (Resident Count x 1.5)\n"
                + "   Industrial Eco Score = 100 - (Energy x 0.04) - (Pollution x 0.25)\n"
                + "   Eco Score is clamped to 0-100.\n\n"
                + "4) Use Keyboard Shortcuts\n"
                + "   Ctrl+S Save, Ctrl+E CSV, Ctrl+J JSON Export, Ctrl+I JSON Import, Ctrl+D Delete, Ctrl+R Report.\n\n"
                + "5) Watch Real-time Indicators\n"
                + "   Check Alerts, Eco Score, and Top Green status after each update.\n\n"
                + "6) Keep Data Safe\n"
                + "   Export JSON backup before major edits/imports.\n\n"
                + "7) Present Like a Pro\n"
                + "   Use Search + Filter to quickly show Residential, Industrial, or ALERT entities for Bangladesh city demo.";

        JOptionPane.showMessageDialog(this, tips, "Quick Tips", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleDeleteSelected() {
        int viewRow = entityTable.getSelectedRow();
        if (viewRow < 0) {
            setStatus("Select a row first", new Color(255, 190, 120));
            JOptionPane.showMessageDialog(this, "Please select a row to delete.", "Delete",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete selected entity permanently?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        int modelRow = entityTable.convertRowIndexToModel(viewRow);
        String entityId = String.valueOf(tableModel.getValueAt(modelRow, 0));

        boolean deleted = controller.deleteEntityById(entityId);
        if (!deleted) {
            setStatus("Delete failed", new Color(255, 160, 160));
            JOptionPane.showMessageDialog(this, "Could not delete selected entity.", "Delete Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        tableModel.removeRow(modelRow);
        controller.saveToDisk();
        updateDashboardStats();
        updateEmptyState();
        lastActionMillis = System.currentTimeMillis();
        setStatus("Deleted", AppTheme.NEON_GREEN, entityId + " removed");
    }

    private void loadPersistedData() {
        try {
            List<EntityRowData> rows = controller.loadFromDisk();
            for (EntityRowData row : rows) {
                tableModel.addRow(row.toTableRow());
            }
            if (rows.isEmpty()) {
                setStatus("Ready", AppTheme.NEON_CYAN, "No saved data found");
            } else {
                setStatus("Loaded", AppTheme.NEON_GREEN, rows.size() + " entities restored");
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Load failed", ex);
            setStatus("Load failed", new Color(255, 160, 160), "Could not restore saved data");
        }
    }

    private void updateDashboardStats() {
        totalEntitiesLabel.setText(String.valueOf(controller.getTotalEntities()));
        totalCarbonLabel.setText(decimalFormat.format(controller.getTotalCarbon()) + " kg");
        alertsLabel.setText(String.valueOf(controller.getAlertCount()));
        ecoScoreLabel.setText(decimalFormat.format(controller.getAverageEcoScore()) + " / 100");
        statusLabel.setText(controller.getTotalEntities() == 0 ? "READY" : "ACTIVE");
        statusDetailLabel.setText(controller.getTopGreenEntitySummary());
        animateStatusBadge(controller.getAlertCount() > 0 ? new Color(188, 112, 122) : AppTheme.NEON_GREEN);
        refreshCityMixChart();
        updateFilterStats();
    }

    private void refreshCityMixChart() {
        if (cityMixChartPanel == null || tableModel == null) {
            return;
        }

        int residential = 0;
        int industrial = 0;
        int alert = 0;

        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String type = String.valueOf(tableModel.getValueAt(row, 2));
            String status = String.valueOf(tableModel.getValueAt(row, 8));

            if ("Residential".equals(type)) {
                residential++;
            } else if ("Industrial".equals(type)) {
                industrial++;
            }

            if ("ALERT".equals(status)) {
                alert++;
            }
        }

        cityMixChartPanel.setData(residential, industrial, alert);
        if (chartResidentialLabel != null) {
            chartResidentialLabel.setText("Residential  " + residential);
            chartIndustrialLabel.setText("Industrial  " + industrial);
            chartAlertLabel.setText("Alerts  " + alert);
        }
    }

    private static class CityMixChartPanel extends JPanel {
        private int residential;
        private int industrial;
        private int alert;

        CityMixChartPanel() {
            setOpaque(true);
            setBackground(AppTheme.TABLE_BG);
        }

        void setData(int residential, int industrial, int alert) {
            this.residential = residential;
            this.industrial = industrial;
            this.alert = alert;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int total = residential + industrial;
            int size = Math.min(getWidth(), getHeight()) - 30;
            int x = 14;
            int y = 14;

            if (size <= 0) {
                g2.dispose();
                return;
            }

            if (total == 0) {
                g2.setColor(new Color(60, 70, 95));
                g2.fillOval(x, y, size, size);
                g2.setColor(AppTheme.TEXT_LIGHT);
                g2.setFont(AppTheme.BODY_FONT);
                g2.drawString("No data", x + size / 2 - 24, y + size / 2 + 4);
            } else {
                int residentialAngle = (int) Math.round((residential * 360.0) / total);
                int industrialAngle = 360 - residentialAngle;

                g2.setColor(new Color(112, 174, 132));
                g2.fillArc(x, y, size, size, 90, -residentialAngle);
                g2.setColor(new Color(198, 142, 102));
                g2.fillArc(x, y, size, size, 90 - residentialAngle, -industrialAngle);
            }

            int lx = x + size + 16;
            int ly = y + 20;
            g2.setFont(AppTheme.BODY_BOLD_FONT);

            g2.setColor(new Color(112, 174, 132));
            g2.fillRect(lx, ly - 10, 10, 10);
            g2.setColor(AppTheme.TEXT_LIGHT);
            g2.drawString("Residential: " + residential, lx + 16, ly);

            ly += 22;
            g2.setColor(new Color(198, 142, 102));
            g2.fillRect(lx, ly - 10, 10, 10);
            g2.setColor(AppTheme.TEXT_LIGHT);
            g2.drawString("Industrial: " + industrial, lx + 16, ly);

            ly += 22;
            g2.setColor(new Color(188, 112, 122));
            g2.fillRect(lx, ly - 10, 10, 10);
            g2.setColor(AppTheme.TEXT_LIGHT);
            g2.drawString("Alerts: " + alert, lx + 16, ly);

            g2.dispose();
        }
    }

    private void handlePrimaryAction() {
        if (editingOriginalId == null) {
            handleAddEntity();
        } else {
            handleUpdateEntity();
        }
    }

    private void handleEditSelected() {
        int viewRow = entityTable.getSelectedRow();
        if (viewRow < 0) {
            setStatus("Select a row first", new Color(255, 190, 120));
            JOptionPane.showMessageDialog(this, "Please select a row to edit.", "Edit",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int modelRow = entityTable.convertRowIndexToModel(viewRow);
        editingOriginalId = String.valueOf(tableModel.getValueAt(modelRow, 0));

        idField.setText(String.valueOf(tableModel.getValueAt(modelRow, 0)));
        nameField.setText(String.valueOf(tableModel.getValueAt(modelRow, 1)));
        typeBox.setSelectedItem(String.valueOf(tableModel.getValueAt(modelRow, 2)));
        energyField.setText(String.valueOf(tableModel.getValueAt(modelRow, 3)));

        String extraInfo = String.valueOf(tableModel.getValueAt(modelRow, 4));
        if (extraInfo.contains("Residents")) {
            extraValueField.setText(extraInfo.replace(" Residents", "").trim());
        } else if (extraInfo.contains("Pollution:")) {
            extraValueField.setText(extraInfo.replace("Pollution:", "").trim());
        } else {
            extraValueField.setText("");
        }

        primaryActionButton.setText("Update Entity");
        setStatus("Editing " + editingOriginalId, AppTheme.NEON_CYAN);
    }

    private void handleUpdateEntity() {
        try {
            double energy;
            try {
                energy = Double.parseDouble(energyField.getText().trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Energy usage must be numeric.");
            }

            EntityFormData formData = new EntityFormData(
                    idField.getText().trim(),
                    nameField.getText().trim(),
                    (String) typeBox.getSelectedItem(),
                    energy,
                    extraValueField.getText().trim());

            EntityRowData updatedRow = controller.updateEntity(editingOriginalId, formData);

            for (int row = 0; row < tableModel.getRowCount(); row++) {
                if (String.valueOf(tableModel.getValueAt(row, 0)).equalsIgnoreCase(editingOriginalId)) {
                    tableModel.removeRow(row);
                    tableModel.insertRow(row, updatedRow.toTableRow());
                    break;
                }
            }

            controller.saveToDisk();
            updateDashboardStats();
            updateEmptyState();
            clearInputs();
            editingOriginalId = null;
            primaryActionButton.setText("Add & Calculate");
            lastActionMillis = System.currentTimeMillis();
            setStatus("Updated", AppTheme.NEON_GREEN, updatedRow.id() + " refreshed");
            flashTableState(updatedRow.id(), true);
        } catch (IllegalArgumentException ex) {
            LOGGER.log(Level.INFO, "Validation failed: {0}", ex.getMessage());
            setStatus("Validation failed", new Color(255, 160, 160), ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unexpected error while updating entity", ex);
            setStatus("Unexpected error", new Color(255, 160, 160), "Check logs for details");
            JOptionPane.showMessageDialog(this, "Something went wrong. Please try again.",
                    "System Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setStatus(String message, Color color) {
        setStatus(message, color, controller.getTopGreenEntitySummary());
    }

    private void setStatus(String message, Color color, String detail) {
        statusLabel.setText(message.toUpperCase());
        statusLabel.setForeground(Color.WHITE);
        statusDetailLabel.setText(detail);
        animateStatusBadge(color);
        lastActionMillis = System.currentTimeMillis();
    }

    private void updateEmptyState() {
        if (tableModel.getRowCount() == 0) {
            tableCardLayout.show(tableCardPanel, "EMPTY");
        } else {
            tableCardLayout.show(tableCardPanel, "TABLE");
        }
    }

    private void clearInputs() {
        idField.setText("");
        nameField.setText("");
        energyField.setText("");
        extraValueField.setText("");
        typeBox.setSelectedIndex(0);
    }

    private JPanel createMetricCard(String badgeText, String titleText, JLabel valueLabel, Color badgeColor,
            String subtitleText) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(AppTheme.PANEL_DARK);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, AppTheme.SPACE_MD,
                        AppTheme.SPACE_MD, AppTheme.SPACE_MD)));

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        topRow.setOpaque(false);

        JLabel badge = new JLabel(badgeText, JLabel.CENTER);
        badge.setOpaque(true);
        badge.setBackground(badgeColor);
        badge.setForeground(Color.WHITE);
        badge.setFont(AppTheme.CAPTION_FONT.deriveFont(Font.BOLD));
        badge.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_XS - 1, AppTheme.SPACE_SM,
                AppTheme.SPACE_XS - 1, AppTheme.SPACE_SM));

        JLabel title = new JLabel(titleText);
        title.setFont(AppTheme.METRIC_TITLE_FONT);
        title.setForeground(AppTheme.TEXT_LIGHT);

        topRow.add(badge);
        topRow.add(title);

        valueLabel.setFont(AppTheme.METRIC_VALUE_FONT);
        valueLabel.setForeground(AppTheme.NEON_CYAN);
        valueLabel.setHorizontalAlignment(SwingConstants.LEFT);

        JLabel subtitle = new JLabel(subtitleText);
        subtitle.setFont(AppTheme.CAPTION_FONT);
        subtitle.setForeground(AppTheme.MUTED_TEXT);

        card.add(topRow, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(subtitle, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createStatusCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(AppTheme.PANEL_DARK);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_MD, AppTheme.SPACE_MD,
                        AppTheme.SPACE_MD, AppTheme.SPACE_MD)));

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        topRow.setOpaque(false);

        JLabel badge = new JLabel("TOP", JLabel.CENTER);
        badge.setOpaque(true);
        badge.setBackground(AppTheme.NEON_CYAN);
        badge.setForeground(Color.WHITE);
        badge.setFont(AppTheme.CAPTION_FONT.deriveFont(Font.BOLD));
        badge.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACE_XS - 1, AppTheme.SPACE_SM,
                AppTheme.SPACE_XS - 1, AppTheme.SPACE_SM));

        JLabel title = new JLabel("Status");
        title.setFont(AppTheme.METRIC_TITLE_FONT);
        title.setForeground(AppTheme.TEXT_LIGHT);

        topRow.add(badge);
        topRow.add(title);

        statusLabel.setFont(AppTheme.METRIC_VALUE_FONT);
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);

        statusDetailLabel.setFont(AppTheme.CAPTION_FONT);
        statusDetailLabel.setForeground(AppTheme.MUTED_TEXT);

        card.add(topRow, BorderLayout.NORTH);
        card.add(statusLabel, BorderLayout.CENTER);
        card.add(statusDetailLabel, BorderLayout.SOUTH);
        animateStatusBadge(AppTheme.NEON_CYAN);
        return card;
    }

    private JLabel createMiniStatChip(String labelText, String valueText, Color accentColor) {
        JLabel chip = new JLabel(labelText + "  " + valueText, SwingConstants.CENTER);
        chip.setOpaque(true);
        chip.setBackground(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 40));
        chip.setForeground(AppTheme.TEXT_LIGHT);
        chip.setFont(AppTheme.CAPTION_FONT.deriveFont(Font.BOLD));
        chip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accentColor, 1),
                BorderFactory.createEmptyBorder(AppTheme.SPACE_XS, AppTheme.SPACE_MD - 2,
                        AppTheme.SPACE_XS, AppTheme.SPACE_MD - 2)));
        return chip;
    }

    private void animateStatusBadge(Color color) {
        statusLabel.setOpaque(true);
        statusLabel.setBackground(color);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.TEXT_LIGHT, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
    }

    private void startLiveUiTimer() {
        lastActionMillis = System.currentTimeMillis();
        liveUiTimer = new javax.swing.Timer(1000, e -> {
            updateFilterStats();

            // Keep dashboard in sync even during background/import operations.
            if (System.currentTimeMillis() % 3000 < 1000) {
                updateDashboardStats();
                updateEmptyState();
            }

            if (System.currentTimeMillis() - lastActionMillis > 4000) {
                statusDetailLabel.setText(buildLiveHeartbeat());
            }
        });
        liveUiTimer.start();
    }

    private void stopLiveUiTimer() {
        if (liveUiTimer != null) {
            liveUiTimer.stop();
            liveUiTimer = null;
        }
    }

    private String buildLiveHeartbeat() {
        String activeFilter = filterTypeBox == null ? "All" : String.valueOf(filterTypeBox.getSelectedItem());
        int visible = entityTable == null ? 0 : entityTable.getRowCount();
        return "Live at " + LocalTime.now().format(clockFormatter) + " | Filter: " + activeFilter
                + " | Visible: " + visible;
    }

    private void flashTableState(String entityId, boolean selectRow) {
        if (entityId == null) {
            return;
        }

        for (int row = 0; row < tableModel.getRowCount(); row++) {
            if (String.valueOf(tableModel.getValueAt(row, 0)).equalsIgnoreCase(entityId)) {
                int viewRow = entityTable.convertRowIndexToView(row);
                if (selectRow && viewRow >= 0) {
                    entityTable.getSelectionModel().setSelectionInterval(viewRow, viewRow);
                    entityTable.scrollRectToVisible(entityTable.getCellRect(viewRow, 0, true));
                }
                return;
            }
        }
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> new EcoCityGUI().setVisible(true));
    }
}
