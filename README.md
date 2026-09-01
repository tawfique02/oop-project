# EcoSmart City Manager

এটি একটি Java Swing ভিত্তিক OOP প্রজেক্ট, যেখানে city entity track করা, sustainability indicator monitor করা, এবং eco-related calculation real-time এ পরিচালনা করা হয়।

## সারসংক্ষেপ
এই অ্যাপ্লিকেশনটি Residential এবং Industrial city entity ম্যানেজ করতে সাহায্য করে। লাইভ ড্যাশবোর্ডে নিচের মেট্রিকগুলো দেখা যায়:
- মোট entity সংখ্যা
- মোট carbon impact
- alert সংখ্যা
- গড় eco score
- top green entity

প্রজেক্টটি Core Java (Java 21), Swing UI, OOP principle, এবং Maven ব্যবহার করে build ও test করা হয়েছে।

## প্রধান ফিচারসমূহ
- Residential বা Industrial entity add, update, delete করা
- Table row search এবং filter করা (All, Residential, Industrial, ALERT)
- Duplicate ID ও invalid input এর জন্য auto validation
- Real-time dashboard update
- Sustainability report তৈরি
- CSV export
- JSON backup export/import
- Local data file এ automatic persistence
- দ্রুত কাজের জন্য keyboard shortcut support

## OOP ডিজাইন হাইলাইটস
- Abstract/base model: CityEntity
- Concrete entity type: Residential, Industrial
- Controller layer: EcoCityController
- In-memory model manager: EcoSmartCity
- Persistence layer: CityPersistenceService
- UI layer: EcoCityGUI

এই separation এর কারণে business logic, persistence, এবং UI cleanly organized থাকে।

## মূল Business Rules
- Residential bill (BDT): Energy Usage x 12.5
- Industrial penalty (BDT): Pollution Level > 100 হলে 5000, না হলে 0
- Residential carbon impact: Energy Usage x 0.45
- Industrial carbon impact: Pollution Level x 2.5
- Residential eco score: 100 - (Energy x 0.05) - (Resident Count x 1.5)
- Industrial eco score: 100 - (Energy x 0.04) - (Pollution x 0.25)
- Eco score সবসময় 0 থেকে 100 এর মধ্যে clamp করা হয়
- সাধারণ alert condition:
  - Residential: energy overuse
  - Industrial: high pollution বা খুব বেশি energy usage

## Project Structure
- Main.java: অ্যাপ্লিকেশনের entry point
- EcoCityGUI.java: Swing GUI এবং interaction workflow
- EcoCityController.java: Validation, calculation, import/export handling
- CityPersistenceService.java: JSONL এবং JSON persistence handling
- EcoSmartCity.java: Entity collection এবং city-level operation
- src/test/java: Unit test files
- data/entities.jsonl: Default local persisted data file

## Implementation Details (Code Walkthrough)

### 1) App startup flow
1. Main.java থেকে main() method call হয়।
2. main() এর ভিতরে EcoCityGUI.launch() call করা হয়।
3. launch() SwingUtilities.invokeLater(...) ব্যবহার করে GUI thread-safe ভাবে start করে।
4. EcoCityGUI constructor UI build করে, persisted data load করে, তারপর live dashboard start করে।

### 2) Java Swing কোথায় কোথায় ব্যবহার করা হয়েছে
Swing-এর প্রধান implementation EcoCityGUI.java ফাইলে।

- Window level:
   - EcoCityGUI extends JFrame
   - setTitle, setSize, setLayout, setDefaultCloseOperation দিয়ে main window configure করা হয়েছে

- Header metrics panel:
   - buildHeaderPanel() method
   - JPanel + GridLayout
   - JLabel card দিয়ে Total Entities, Carbon, Alerts, Eco Score, Status দেখানো হয়

- Input form panel:
   - buildFormPanel() method
   - JPanel + GridBagLayout
   - JTextField: idField, nameField, energyField, extraValueField
   - JComboBox: typeBox
   - JLabel: field label + hints

- Action buttons area:
   - buildActionButtons() method
   - JButton: Add/Update, Edit, Save, Export CSV, More, Delete
   - প্রতিটি button createPrimaryButton(...) দিয়ে custom style + action listener পায়

- Data table + filter area:
   - buildTableArea() method
   - JTable + DefaultTableModel + TableRowSorter
   - Search: JTextField searchField
   - Filter: JComboBox filterTypeBox
   - Row color renderer দিয়ে ALERT row আলাদা highlight করা হয়েছে

- Extra UI components:
   - JSplitPane: form panel এবং table area split করা
   - JScrollPane: table scrolling
   - CardLayout: empty state vs table state switch
   - JFileChooser: CSV/JSON export-import
   - JOptionPane: message, error, confirmation, report dialog

- Keyboard integration:
   - bindKeyboardShortcuts() method
   - RootPane InputMap/ActionMap দিয়ে Ctrl+S, Ctrl+E, Ctrl+J, Ctrl+I, Ctrl+D, Ctrl+R, Esc bind করা হয়েছে

### 3) Swing এবং Java logic কীভাবে connected
EcoCityGUI.java এর ভিতরে দুটি core object আছে:
- EcoSmartCity ecoSmartCity = new EcoSmartCity();
- EcoCityController controller = new EcoCityController(ecoSmartCity);

মানে GUI সরাসরি model manipulate করে না, controller-এর মাধ্যমে সব business logic execute হয়।

### 4) User action থেকে data পর্যন্ত flow

#### Add entity flow
1. User form fill করে Add button press করে।
2. handleAddEntity() form input থেকে EntityFormData বানায়।
3. controller.addEntity(formData) validation + calculation + model insert করে EntityRowData return দেয়।
4. GUI tableModel.addRow(...) দিয়ে JTable update করে।
5. controller.saveToDisk() call হয়ে data/entities.jsonl এ save হয়।
6. updateDashboardStats() call হয়ে header metrics refresh হয়।

#### Update entity flow
1. Row select করে Edit Selected চাপলে form pre-fill হয়।
2. Update action handleUpdateEntity() trigger করে।
3. controller.updateEntity(...) old entity replace করে updated row ফেরত দেয়।
4. JTable model row replace হয়, তারপর save + stats refresh হয়।

#### Delete flow
1. handleDeleteSelected() selected ID নেয়।
2. controller.deleteEntityById(id) model থেকে remove করে।
3. GUI table row remove করে, save + stats update করে।

#### Import/Export flow
- Export CSV: controller.exportToCsv(path)
- Export JSON: controller.exportToJson(path)
- Import JSON: controller.importFromJson(path) -> returned rows দিয়ে JTable rebuild

### 5) Controller layer এ কী implement করা হয়েছে
EcoCityController.java ফাইলে:
- Form data validation (ID, name, numeric checks, duplicate checks)
- Residential/Industrial object creation
- Bill/penalty/carbon/eco score calculation
- Alert condition evaluation
- DTO mapping: model -> EntityRowData
- Persistence service call: load/save/export/import
- Dashboard helper methods: total carbon, alert count, average eco score, top green entity

### 6) Domain layer এ কী implement করা হয়েছে
- CityEntity.java: abstract base class (entityID, name, energyUsage)
- Residential.java: residentCount, calculateEnergyBill(), checkOveruse()
- Industrial.java: pollutionLevel, calculateCarbonFootprint(), applyTaxPenalty()
- Trackable.java: updateEnergy(), reportWaste() contract
- EcoSmartCity.java: in-memory entity list, add/remove/find, duplicate check

### 7) Persistence layer এ কী implement করা হয়েছে
CityPersistenceService.java ফাইলে:
- Default file path: data/entities.jsonl
- save(): entity list -> JSONL-like lines
- load(): file parse করে PersistedEntity list return
- saveJsonArray(): backup JSON array export
- Parse এবং format strictness check আছে যাতে invalid format detect হয়

### 8) Swing + OOP + Layer Architecture (summary)
UI (EcoCityGUI) -> Controller (EcoCityController) -> Domain Model (EcoSmartCity + CityEntity subclasses) -> Persistence (CityPersistenceService)

এই layered flow maintain করার কারণে:
- UI code reusable থাকে
- Business rules এক জায়গায় maintain করা যায়
- Testing করা সহজ হয়

### 9) Test mapping (কোন layer কীভাবে verify করা হয়েছে)
- EcoCityControllerTest.java: validation, calculation, update/delete logic
- CityPersistenceServiceTest.java: save/load/import/export correctness
- EcoSmartCityTest.java: model behavior (add/remove/find)
- ResidentialTest.java এবং IndustrialTest.java: entity specific rules
- WasteManagerTest.java: waste categorization এবং points logic

## Requirements
- Java 21 (LTS)
- Maven 3.9+
- Windows, Linux, বা macOS

## Build এবং Run
1. Project root folder এ terminal open করুন।
2. Project compile করুন:
   mvn clean compile
3. App run করুন:
   java -cp target/classes Main

বিকল্পভাবে:
- আপনার IDE থেকে সরাসরি Main.java run করতে পারেন।

## Test চালানো
Maven test command ব্যবহার করুন:
- mvn test

## Keyboard Shortcuts
- Ctrl+S: Data save
- Ctrl+E: CSV export
- Ctrl+J: JSON backup export
- Ctrl+I: JSON backup import
- Ctrl+D: Selected row delete
- Ctrl+R: Sustainability report দেখান
- Esc: Form clear
- Enter: Default action submit

## Data Persistence
- Save/close করার সময় data local file এ persist হয়।
- Default storage path: data/entities.jsonl
- JSON import এ app-generated backup style data support করে।

## Sample Use Flow
1. Residential এবং Industrial entity add করুন।
2. Dashboard metrics এবং ALERT status পর্যবেক্ষণ করুন।
3. Eco score improve করতে entity edit করুন।
4. Presentation/report এর জন্য CSV export করুন।
5. Recovery/backup এর জন্য JSON export করুন।

## Testing Coverage
Core layer গুলোর জন্য unit test আছে, যেমন:
- Persistence service
- Controller
- Domain entities
- City model manager

## Notes
- Entity ID অবশ্যই unique রাখুন।
- Numeric field গুলো valid দিন এবং যেখানে দরকার non-negative রাখুন।
- বড় data operation এর আগে JSON backup export করে রাখুন।


### Made with ❤️ by Tawfique Elahey [Github Profile](https://github.com/tawfique02)
