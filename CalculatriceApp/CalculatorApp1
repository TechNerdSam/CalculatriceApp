import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

/**
 * CalculatorApp is the main class for a scientific calculator GUI.
 * It handles the user interface and delegates calculation logic to CalculatorLogic.
 */
public class CalculatorApp extends JFrame {

    // --- UI Components ---
    private JTextField displayField; // Display field for numbers and results
    private JPanel scientificKeysPanel; // Panel for extra scientific keys
    private JPanel programmerKeysPanel; // Panel for programmer specific keys
    private JPanel keypadsArea;         // Panel holding special key panels and standard keys
    private JPanel specialKeysContainer; // Container for scientific/programmer panels using CardLayout

    // --- CardLayout Constants ---
    private static final String SCI_PANEL_CARD = "ScientificPanel";
    private static final String PROG_PANEL_CARD = "ProgrammerPanel";
    private static final String EMPTY_PANEL_CARD = "EmptyPanel";


    // --- Calculator Logic and State ---
    private final CalculatorState state;
    private final CalculatorLogic logic;

    // --- Display Formats ---
    private final DecimalFormat normalFormat;
    private final DecimalFormat scientificFormat;
    // private final DecimalFormat engineerFormat; // No longer needed

    // --- Enums for Modes ---
    enum AngleMode { DEG, RAD, GRAD }
    enum DisplayMode { NORMAL, SCIENTIFIC, PROGRAMMER } // Changed ENGINEER to PROGRAMMER

    /**
     * Inner class to hold the calculator's current state.
     * This class primarily stores data; logic is handled by CalculatorLogic.
     */
    private static class CalculatorState {
        // Input and Display
        String currentOperandText = "0";    // Text currently being typed or last formatted result
        boolean startNewNumber = true;      // If true, next digit starts a new number
        boolean errorState = false;         // True if an error has occurred (e.g., division by zero)
        String errorMessage = "";           // Stores the current error message

        // Core Calculation Values
        double currentValue = 0.0;          // Numeric value of the current operand or result
        double previousValue = 0.0;         // Previous operand for binary operations
        String pendingOperation = null;     // Operation waiting for a second operand (e.g., "+", "-")
        String lastOperatorForEquals = null; // For repeating equals with last operator
        double lastOperandForEquals = 0.0;  // For repeating equals with last operand


        // Modes
        AngleMode angleMode = AngleMode.DEG;        // Default angle mode
        DisplayMode displayMode = DisplayMode.NORMAL; // Default display mode

        // Memory
        double memoryValue = 0.0;           // Value stored in memory (M+, M-, MR, MC)

        public void setError(String message) {
            this.currentOperandText = message;
            this.errorMessage = message;
            this.errorState = true;
            this.startNewNumber = true;
            this.pendingOperation = null;
        }

        public void clearAll() {
            this.currentOperandText = "0";
            this.currentValue = 0.0;
            this.previousValue = 0.0;
            this.pendingOperation = null;
            this.startNewNumber = true;
            this.errorState = false;
            this.errorMessage = "";
            this.lastOperatorForEquals = null;
            this.lastOperandForEquals = 0.0;
        }

        public void clearEntry() {
            if (this.errorState) {
                clearAll();
            } else {
                this.currentOperandText = "0";
                this.currentValue = 0.0;
                this.startNewNumber = true;
            }
        }

        public void memoryClear() {
            this.memoryValue = 0.0;
        }

        public void memoryRecall() {
            this.currentValue = this.memoryValue;
            this.startNewNumber = true;
            this.errorState = false;
            this.errorMessage = "";
        }

        public void memoryAdd(double value) {
            this.memoryValue += value;
        }

        public void memorySubtract(double value) {
            this.memoryValue -= value;
        }
    }

    /**
     * Handles the core calculation logic for the calculator.
     */
    private static class CalculatorLogic {
        private final CalculatorState state; // Operates on the shared state
        private static final double EPSILON = 1E-12;

        public CalculatorLogic(CalculatorState state) {
            this.state = state;
        }

        public void appendDigit(String digit) {
            if (state.errorState) {
                 state.clearAll();
            }
            if (state.startNewNumber) {
                state.currentOperandText = digit;
                state.startNewNumber = false;
            } else {
                if (state.currentOperandText.equals("0") && !digit.equals("0")) {
                    state.currentOperandText = digit;
                } else if (!state.currentOperandText.equals("0") || !digit.equals("0") || state.currentOperandText.contains(".")) {
                    if (state.currentOperandText.length() < 16) { // Max input length
                        state.currentOperandText += digit;
                    }
                }
            }
        }

        public void appendDecimal() {
            if (state.errorState) {
                state.clearAll();
            }
            if (state.startNewNumber) {
                state.currentOperandText = "0.";
                state.startNewNumber = false;
            } else {
                if (!state.currentOperandText.contains(".")) {
                    if (state.currentOperandText.length() < 16) { // Max input length
                        state.currentOperandText += ".";
                    }
                }
            }
        }

        private boolean parseCurrentOperand() {
            String parsableText = state.currentOperandText.replace(',', '.');
            if (parsableText == null || parsableText.isEmpty() || parsableText.equals(".") || parsableText.equals("-") ||
                parsableText.endsWith("E") || parsableText.endsWith("e") ||
                parsableText.endsWith("E-") || parsableText.endsWith("e-")) {
                if (parsableText.equals(".") || parsableText.equals("-")) {
                    state.currentValue = 0.0;
                    state.currentOperandText = "0";
                    return true;
                }
                state.setError("Invalid Input");
                return false;
            }
            try {
                state.currentValue = Double.parseDouble(parsableText);
                return true;
            } catch (NumberFormatException e) {
                state.setError("Invalid Format");
                return false;
            }
        }

        public void setOperator(String operator) {
            if (state.errorState) return;
            if (!state.startNewNumber) {
                if (!parseCurrentOperand()) return;
            }
            if (state.pendingOperation != null && !state.startNewNumber) {
                 performCalculation();
                 if (state.errorState) return;
            }
            state.previousValue = state.currentValue;
            state.pendingOperation = operator;
            state.startNewNumber = true;
        }

        public void performCalculation() {
            if (state.pendingOperation == null) {
                if (state.lastOperatorForEquals != null) {
                    state.pendingOperation = state.lastOperatorForEquals;
                    state.previousValue = state.currentValue;
                    state.currentValue = state.lastOperandForEquals;
                } else {
                    if (!state.startNewNumber) {
                        if(!parseCurrentOperand()) return;
                    }
                    state.startNewNumber = true;
                    return;
                }
            }

            double rightOperand = state.currentValue;
            double result = 0.0;

            switch (state.pendingOperation) {
                case "+": result = state.previousValue + rightOperand; break;
                case "-": result = state.previousValue - rightOperand; break;
                case "*": result = state.previousValue * rightOperand; break;
                case "/":
                    if (Math.abs(rightOperand) < EPSILON) {
                        state.setError("Division by Zero"); return;
                    }
                    result = state.previousValue / rightOperand; break;
                case "x^y": result = Math.pow(state.previousValue, rightOperand); break;
                default: state.setError("Unknown Operator"); return;
            }

            if (Double.isInfinite(result) || Double.isNaN(result)) {
                state.setError("Overflow / Invalid");
                return;
            }

            state.currentValue = result;
            state.lastOperatorForEquals = state.pendingOperation;
            state.lastOperandForEquals = rightOperand;
            state.pendingOperation = null;
            state.startNewNumber = true;
        }

        public void handleEquals() {
            if (state.errorState) return;
            if (!state.startNewNumber) {
                if (!parseCurrentOperand()) return;
            }
            performCalculation();
        }

        public void changeSign() {
            if (state.errorState) return;
            if (state.startNewNumber) {
                state.currentValue = -state.currentValue;
            } else {
                if (state.currentOperandText.equals("0") || state.currentOperandText.equals("0.0")) return;
                if (state.currentOperandText.startsWith("-")) {
                    state.currentOperandText = state.currentOperandText.substring(1);
                } else {
                    if (state.currentOperandText.length() < 16) {
                        state.currentOperandText = "-" + state.currentOperandText;
                    }
                }
            }
        }

        public void handleTrigonometricFunction(String funcName) {
            if (state.errorState) return;
            if (!state.startNewNumber) {
                if (!parseCurrentOperand()) return;
            }
            double angle = state.currentValue;
            double angleInRadians = angle;
            switch (state.angleMode) {
                case DEG: angleInRadians = Math.toRadians(angle); break;
                case GRAD: angleInRadians = angle * (Math.PI / 200.0); break;
                case RAD: break;
            }
            double result = 0.0;
            boolean calculationError = false;
            switch (funcName) {
                case "sin": result = Math.sin(angleInRadians); break;
                case "cos":
                    if ((state.angleMode == AngleMode.DEG && (Math.abs(angle % 180) == 90)) ||
                        (state.angleMode == AngleMode.GRAD && (Math.abs(angle % 200) == 100)) ||
                        (state.angleMode == AngleMode.RAD && (Math.abs(Math.abs(angleInRadians) % Math.PI - Math.PI/2) < EPSILON ))) {
                        result = 0.0;
                    } else { result = Math.cos(angleInRadians); }
                    break;
                case "tan":
                    if ((state.angleMode == AngleMode.DEG && (Math.abs(angle % 180) == 90)) ||
                        (state.angleMode == AngleMode.GRAD && (Math.abs(angle % 200) == 100)) ||
                        (state.angleMode == AngleMode.RAD && (Math.abs(Math.abs(angleInRadians) % Math.PI - Math.PI/2) < EPSILON ))) {
                        state.setError("Tan Undefined"); calculationError = true;
                    } else { result = Math.tan(angleInRadians); }
                    break;
                default: state.setError("Unknown Trig Func"); calculationError = true; break;
            }
            if (!calculationError) {
                if (Math.abs(result) < EPSILON) result = 0.0;
                state.currentValue = result;
                 if (Double.isInfinite(state.currentValue) || Double.isNaN(state.currentValue)) {
                    state.setError("Invalid Trig Result");
                }
            }
            state.startNewNumber = true;
        }

        public void handleUnaryOperation(String operation) {
            if (state.errorState) return;
            if (!state.startNewNumber) {
                if (!parseCurrentOperand()) return;
            }
            double value = state.currentValue;
            double result = 0.0;
            boolean calcError = false;
            switch (operation) {
                case "log":
                    if (value <= 0) { state.setError("Log Error: Arg <= 0"); calcError = true; }
                    else { result = Math.log10(value); } break;
                case "ln":
                    if (value <= 0) { state.setError("Ln Error: Arg <= 0"); calcError = true; }
                    else { result = Math.log(value); } break;
                case "sqrt":
                    if (value < 0) { state.setError("Sqrt Error: Arg < 0"); calcError = true; }
                    else { result = Math.sqrt(value); } break;
                case "x²": result = value * value; break;
                case "1/x":
                    if (Math.abs(value) < EPSILON) { state.setError("Reciprocal of Zero"); calcError = true; }
                    else { result = 1.0 / value; } break;
                default: state.setError("Unknown Unary Op"); calcError = true; break;
            }
            if (!calcError) {
                if (Math.abs(result) < EPSILON && result != 0.0) {
                    if (Math.abs(result) < 1E-15) result = 0.0;
                }
                state.currentValue = result;
                if (Double.isInfinite(state.currentValue) || Double.isNaN(state.currentValue)) {
                    state.setError("Overflow / Invalid Op");
                }
            }
            state.startNewNumber = true;
        }

        public void handlePercentage() {
            if (state.errorState) return;
            if (!state.startNewNumber) {
                if (!parseCurrentOperand()) return;
            }
            if (state.pendingOperation != null && (state.pendingOperation.equals("+") || state.pendingOperation.equals("-"))) {
                state.currentValue = (state.previousValue * state.currentValue) / 100.0;
                performCalculation();
            } else if (state.pendingOperation != null && (state.pendingOperation.equals("*") || state.pendingOperation.equals("/"))) {
                state.currentValue = state.currentValue / 100.0;
                performCalculation();
            }
            else {
                state.currentValue = state.currentValue / 100.0;
                state.lastOperatorForEquals = null;
                state.lastOperandForEquals = 0.0;
            }
            state.startNewNumber = true;
        }

        public void handleMemoryOperation(String memOp) {
            if (state.errorState && !memOp.equals("MC") && !memOp.equals("MR")) return;
            if (!memOp.equals("MR") && !memOp.equals("MC")) {
                 if (!state.startNewNumber) {
                     if (!parseCurrentOperand()) {
                         if(state.errorState) return;
                     }
                 }
            }
            switch (memOp) {
                case "MC": state.memoryClear(); break;
                case "MR": state.memoryRecall(); break;
                case "M+": state.memoryAdd(state.currentValue); state.startNewNumber = true; break;
                case "M-": state.memorySubtract(state.currentValue); state.startNewNumber = true; break;
            }
        }
    }

    public CalculatorApp() {
        super("Calculatrice Scientifique et Programmeur Java"); // Updated Title
        state = new CalculatorState();
        logic = new CalculatorLogic(state);

        normalFormat = new DecimalFormat("#.##########");
        scientificFormat = new DecimalFormat("0.########E0");
        // engineerFormat = new DecimalFormat("##0.########E0"); // No longer needed

        initComponents();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initComponents() {
        displayField = new JTextField(state.currentOperandText, 16);
        displayField.setEditable(false);
        displayField.setHorizontalAlignment(JTextField.RIGHT);
        displayField.setFont(new Font("Arial", Font.BOLD, 32));
        displayField.setBackground(Color.WHITE);
        displayField.setBorder(BorderFactory.createCompoundBorder(
                displayField.getBorder(),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(displayField, BorderLayout.NORTH);

        ButtonHandler buttonHandler = new ButtonHandler();

        JPanel modeSelectionPanel = new JPanel(new GridLayout(2, 1, 5, 5));

        JPanel angleModePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        angleModePanel.setBorder(BorderFactory.createTitledBorder("Angle"));
        JRadioButton degButton = createRadioButton("DEG", AngleMode.DEG.name(), state.angleMode == AngleMode.DEG, buttonHandler);
        JRadioButton radButton = createRadioButton("RAD", AngleMode.RAD.name(), state.angleMode == AngleMode.RAD, buttonHandler);
        JRadioButton gradButton = createRadioButton("GRAD", AngleMode.GRAD.name(), state.angleMode == AngleMode.GRAD, buttonHandler);
        ButtonGroup angleGroup = new ButtonGroup();
        angleGroup.add(degButton); angleGroup.add(radButton); angleGroup.add(gradButton);
        angleModePanel.add(degButton); angleModePanel.add(radButton); angleModePanel.add(gradButton);
        modeSelectionPanel.add(angleModePanel);

        JPanel displayModePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        displayModePanel.setBorder(BorderFactory.createTitledBorder("Affichage"));
        JRadioButton normalDispButton = createRadioButton("NORMAL", "NORMAL_DISP", state.displayMode == DisplayMode.NORMAL, buttonHandler);
        JRadioButton sciDispButton = createRadioButton("SCI", "SCI_DISP", state.displayMode == DisplayMode.SCIENTIFIC, buttonHandler);
        JRadioButton progDispButton = createRadioButton("PROG", "PROG_DISP", state.displayMode == DisplayMode.PROGRAMMER, buttonHandler); // Changed ENG to PROG
        ButtonGroup displayGroup = new ButtonGroup();
        displayGroup.add(normalDispButton); displayGroup.add(sciDispButton); displayGroup.add(progDispButton);
        displayModePanel.add(normalDispButton); displayModePanel.add(sciDispButton); displayModePanel.add(progDispButton);
        modeSelectionPanel.add(displayModePanel);

        // --- Scientific Keys Panel ---
        scientificKeysPanel = new JPanel(new GridLayout(0, 1, 3, 3));
        scientificKeysPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        scientificKeysPanel.add(createSimpleButton("sin", "SCMD_SIN", buttonHandler));
        scientificKeysPanel.add(createSimpleButton("cos", "SCMD_COS", buttonHandler));
        scientificKeysPanel.add(createSimpleButton("log", "SCMD_LOG", buttonHandler));
        scientificKeysPanel.add(createSimpleButton("π", "SCMD_PI", buttonHandler));
        scientificKeysPanel.add(createSimpleButton("e", "SCMD_E", buttonHandler));

        // --- Programmer Keys Panel (NEW) ---
        programmerKeysPanel = new JPanel(new GridLayout(0, 1, 3, 3)); // Flexible rows, 1 column
        programmerKeysPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5)); // Right margin
        programmerKeysPanel.add(createSimpleButton("HEX", "PCMD_HEX", buttonHandler));
        programmerKeysPanel.add(createSimpleButton("DEC", "PCMD_DEC", buttonHandler));
        programmerKeysPanel.add(createSimpleButton("OCT", "PCMD_OCT", buttonHandler));
        programmerKeysPanel.add(createSimpleButton("BIN", "PCMD_BIN", buttonHandler));
        programmerKeysPanel.add(createSimpleButton("AND", "PCMD_AND", buttonHandler));
        programmerKeysPanel.add(createSimpleButton("OR", "PCMD_OR", buttonHandler));
        programmerKeysPanel.add(createSimpleButton("XOR", "PCMD_XOR", buttonHandler));
        programmerKeysPanel.add(createSimpleButton("NOT", "PCMD_NOT", buttonHandler));
        programmerKeysPanel.add(createSimpleButton("Lsh", "PCMD_LSH", buttonHandler));
        programmerKeysPanel.add(createSimpleButton("Rsh", "PCMD_RSH", buttonHandler));
        programmerKeysPanel.add(createSimpleButton("Mod", "PCMD_MOD", buttonHandler));


        // --- Special Keys Container (using CardLayout) ---
        specialKeysContainer = new JPanel(new CardLayout(0,0)); // CardLayout with no gaps
        specialKeysContainer.add(new JPanel(), EMPTY_PANEL_CARD); // Empty panel for NORMAL mode
        specialKeysContainer.add(scientificKeysPanel, SCI_PANEL_CARD);
        specialKeysContainer.add(programmerKeysPanel, PROG_PANEL_CARD);


        // --- Main Buttons Panel (GridBagLayout) ---
        JPanel standardButtonsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(3,3,3,3);

        String[][] buttonGridConfig = {
            {"MC", "MR", "M+", "M-", "C"},
            {"sin", "cos", "tan", "x^y", "CE"},
            {"log", "ln", "sqrt", "x²", "%"},
            {"7", "8", "9", "/", "1/x"},
            {"4", "5", "6", "*", "+/-"},
            {"1", "2", "3", "-", "="},
            {"0", null, ".", null, null}
        };

        for (int row = 0; row < buttonGridConfig.length; row++) {
            for (int col = 0; col < buttonGridConfig[row].length; col++) {
                String label = buttonGridConfig[row][col];
                if (label == null) continue;

                JButton button = new JButton(label);
                button.setFont(new Font("Arial", Font.PLAIN, 15));
                button.setMargin(new Insets(8,8,8,8));
                button.setActionCommand(label);
                button.addActionListener(buttonHandler);

                if (label.matches("[0-9]")) { button.setForeground(Color.BLUE); button.setBackground(new Color(230, 230, 255));}
                else if (label.matches("[+\\-*/]|x\\^y|%|1/x")) { button.setForeground(Color.RED); button.setBackground(new Color(255, 230, 230));}
                else if (label.equals("C") || label.equals("CE")) { button.setBackground(new Color(255, 180, 180)); button.setForeground(Color.DARK_GRAY); }
                else if (label.equals("sin") || label.equals("cos") || label.equals("tan") || label.equals("log") || label.equals("ln") || label.equals("sqrt") || label.equals("x²")) { button.setBackground(new Color(210, 230, 255)); }
                else if (label.startsWith("M")) { button.setBackground(new Color(220, 255, 220)); }
                else if (label.equals("=")) {button.setBackground(Color.ORANGE); button.setForeground(Color.WHITE); button.setFont(new Font("Arial", Font.BOLD, 18));}
                else if (label.equals("+/-")) {button.setBackground(new Color(230,230,230));}

                gbc.gridx = col;
                gbc.gridy = row;
                gbc.gridwidth = 1;
                gbc.gridheight = 1;

                if (label.equals("=")) { gbc.gridheight = 2; }
                if (label.equals("0")) { gbc.gridwidth = 2; }

                standardButtonsPanel.add(button, gbc);

                if (label.equals("0")) col++;
            }
        }

        keypadsArea = new JPanel(new BorderLayout(5, 5));
        keypadsArea.add(specialKeysContainer, BorderLayout.WEST); // Add CardLayout container to WEST
        keypadsArea.add(standardButtonsPanel, BorderLayout.CENTER);

        JPanel controlsAndKeypadsPanel = new JPanel(new BorderLayout(5, 5));
        controlsAndKeypadsPanel.add(modeSelectionPanel, BorderLayout.NORTH);
        controlsAndKeypadsPanel.add(keypadsArea, BorderLayout.CENTER);

        mainPanel.add(controlsAndKeypadsPanel, BorderLayout.CENTER);
        add(mainPanel);

        updateSpecialKeyPanelsVisibility(); // Set initial visibility using CardLayout
        updateDisplay();
    }

    private JRadioButton createRadioButton(String text, String actionCommand, boolean selected, ActionListener listener) {
        JRadioButton button = new JRadioButton(text, selected);
        button.setActionCommand(actionCommand);
        button.addActionListener(listener);
        return button;
    }

    private JButton createSimpleButton(String text, String actionCommand, ActionListener listener) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.PLAIN, 14));
        button.setMargin(new Insets(5,5,5,5));
        button.setActionCommand(actionCommand);
        button.addActionListener(listener);
        // Differentiate programmer buttons slightly if desired
        if (actionCommand.startsWith("PCMD_")) {
            button.setBackground(new Color(220, 220, 200)); // Slightly different color for programmer buttons
        } else {
            button.setBackground(new Color(200, 220, 255));
        }
        return button;
    }

    private void updateSpecialKeyPanelsVisibility() {
        CardLayout cl = (CardLayout)(specialKeysContainer.getLayout());
        String cardToShow = EMPTY_PANEL_CARD; // Default to empty (for NORMAL mode)

        if (state.displayMode == DisplayMode.SCIENTIFIC) {
            cardToShow = SCI_PANEL_CARD;
        } else if (state.displayMode == DisplayMode.PROGRAMMER) {
            cardToShow = PROG_PANEL_CARD;
        }

        cl.show(specialKeysContainer, cardToShow);
        pack(); // Resize window to fit new layout after CardLayout change
    }


    private void updateDisplay() {
        if (state.errorState) {
            displayField.setText(state.errorMessage);
            displayField.setForeground(Color.RED);
        } else {
            displayField.setForeground(Color.BLACK);
            String textToDisplay;

            if (state.startNewNumber ||
                state.currentOperandText.endsWith("E") || state.currentOperandText.endsWith("e") ||
                state.currentOperandText.endsWith("E-") || state.currentOperandText.endsWith("e-")) {

                if (state.currentOperandText.endsWith("E") || state.currentOperandText.endsWith("e") ||
                    state.currentOperandText.endsWith("E-") || state.currentOperandText.endsWith("e-")) {
                     textToDisplay = state.currentOperandText;
                } else {
                    if (state.currentValue == 0.0 && state.startNewNumber) {
                         textToDisplay = "0";
                    } else {
                        // Note: PROGRAMMER mode might need specific base formatting (Hex, Bin, etc.) later.
                        // For now, it will use NORMAL or SCIENTIFIC formatting rules.
                        switch (state.displayMode) {
                            case SCIENTIFIC:
                                textToDisplay = scientificFormat.format(state.currentValue);
                                break;
                            // Removed ENGINEER case
                            case PROGRAMMER: // Falls through to NORMAL for now for float display
                            case NORMAL:
                            default:
                                if (Math.abs(state.currentValue) > 1E10 || (Math.abs(state.currentValue) < 1E-7 && state.currentValue != 0)) {
                                    textToDisplay = scientificFormat.format(state.currentValue);
                                } else {
                                    textToDisplay = normalFormat.format(state.currentValue);
                                }
                                break;
                        }
                    }
                }
            } else {
                textToDisplay = state.currentOperandText;
            }
            displayField.setText(textToDisplay);
            if (state.startNewNumber && !state.errorState) {
                state.currentOperandText = textToDisplay;
            }
        }
    }

    private class ButtonHandler implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();

            if (state.errorState && !command.equals("C") && !command.equals("CE") &&
                !command.equals("MC") && !command.equals("MR") &&
                !command.equals(AngleMode.DEG.name()) && !command.equals(AngleMode.RAD.name()) && !command.equals(AngleMode.GRAD.name()) &&
                !command.equals("NORMAL_DISP") && !command.equals("SCI_DISP") && !command.equals("PROG_DISP") // Updated for PROG_DISP
                ) {
                return;
            }

            if (command.matches("[0-9]")) logic.appendDigit(command);
            else if (command.equals(".")) logic.appendDecimal();
            else if (command.matches("[+\\-*/]|x\\^y")) logic.setOperator(command);
            else if (command.equals("=")) logic.handleEquals();
            else if (command.equals("C")) state.clearAll();
            else if (command.equals("CE")) state.clearEntry();
            else if (command.equals("+/-")) logic.changeSign();
            else if (command.equals("sin") || command.equals("cos") || command.equals("tan")) logic.handleTrigonometricFunction(command);
            else if (command.equals("log") || command.equals("ln") || command.equals("sqrt") || command.equals("x²") || command.equals("1/x")) logic.handleUnaryOperation(command);
            else if (command.equals("%")) logic.handlePercentage();
            else if (command.equals("MC") || command.equals("MR") || command.equals("M+") || command.equals("M-")) logic.handleMemoryOperation(command);
            else if (command.equals(AngleMode.DEG.name())) state.angleMode = AngleMode.DEG;
            else if (command.equals(AngleMode.RAD.name())) state.angleMode = AngleMode.RAD;
            else if (command.equals(AngleMode.GRAD.name())) state.angleMode = AngleMode.GRAD;
            else if (command.equals("NORMAL_DISP")) {
                state.displayMode = DisplayMode.NORMAL;
                forceDisplayUpdateForModeChange();
                updateSpecialKeyPanelsVisibility();
            } else if (command.equals("SCI_DISP")) {
                state.displayMode = DisplayMode.SCIENTIFIC;
                forceDisplayUpdateForModeChange();
                updateSpecialKeyPanelsVisibility();
            } else if (command.equals("PROG_DISP")) { // Changed from ENG_DISP
                state.displayMode = DisplayMode.PROGRAMMER;
                forceDisplayUpdateForModeChange();
                updateSpecialKeyPanelsVisibility();
            }
            // Placeholder actions for scientific panel buttons (from previous version)
            else if (command.equals("SCMD_SIN")) { System.out.println("Scientific Panel 'sin' pressed."); }
            else if (command.equals("SCMD_COS")) { System.out.println("Scientific Panel 'cos' pressed."); }
            else if (command.equals("SCMD_LOG")) { System.out.println("Scientific Panel 'log' pressed."); }
            else if (command.equals("SCMD_PI")) {
                if (state.errorState) state.clearAll();
                state.currentOperandText = String.valueOf(Math.PI);
                logic.parseCurrentOperand();
                state.startNewNumber = true;
                System.out.println("Scientific Panel 'π' pressed. Value set to PI.");
            }
            else if (command.equals("SCMD_E")) {
                 if (state.errorState) state.clearAll();
                state.currentOperandText = String.valueOf(Math.E);
                logic.parseCurrentOperand();
                state.startNewNumber = true;
                System.out.println("Scientific Panel 'e' pressed. Value set to E.");
            }
            // Placeholder actions for new programmer panel buttons
            else if (command.equals("PCMD_HEX")) { System.out.println("Programmer Panel 'HEX' pressed."); }
            else if (command.equals("PCMD_DEC")) { System.out.println("Programmer Panel 'DEC' pressed."); }
            else if (command.equals("PCMD_OCT")) { System.out.println("Programmer Panel 'OCT' pressed."); }
            else if (command.equals("PCMD_BIN")) { System.out.println("Programmer Panel 'BIN' pressed."); }
            else if (command.equals("PCMD_AND")) { System.out.println("Programmer Panel 'AND' pressed."); }
            else if (command.equals("PCMD_OR")) { System.out.println("Programmer Panel 'OR' pressed."); }
            else if (command.equals("PCMD_XOR")) { System.out.println("Programmer Panel 'XOR' pressed."); }
            else if (command.equals("PCMD_NOT")) { System.out.println("Programmer Panel 'NOT' pressed."); }
            else if (command.equals("PCMD_LSH")) { System.out.println("Programmer Panel 'Lsh' pressed."); }
            else if (command.equals("PCMD_RSH")) { System.out.println("Programmer Panel 'Rsh' pressed."); }
            else if (command.equals("PCMD_MOD")) { System.out.println("Programmer Panel 'Mod' pressed."); }


            updateDisplay();
        }

        private void forceDisplayUpdateForModeChange() {
            if (!state.startNewNumber && !state.errorState) {
                logic.parseCurrentOperand();
            }
            boolean oldStartNewNumber = state.startNewNumber;
            state.startNewNumber = true;
            updateDisplay();
            state.startNewNumber = oldStartNewNumber;
            if (!state.startNewNumber && !state.errorState) {
                 state.currentOperandText = displayField.getText();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean nimbusFound = false;
                    for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                        if ("Nimbus".equals(info.getName())) {
                            UIManager.setLookAndFeel(info.getClassName());
                            nimbusFound = true;
                            break;
                        }
                    }
                    if (!nimbusFound) {
                        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                    }
                } catch (UnsupportedLookAndFeelException e) {
                    System.err.println("Nimbus L&F not supported: " + e.getMessage());
                } catch (ClassNotFoundException e) {
                    System.err.println("Nimbus L&F class not found: " + e.getMessage());
                } catch (InstantiationException e) {
                    System.err.println("Cannot instantiate Nimbus L&F: " + e.getMessage());
                } catch (IllegalAccessException e) {
                    System.err.println("Cannot access Nimbus L&F: " + e.getMessage());
                }
                new CalculatorApp();
            }
        });
    }
}
