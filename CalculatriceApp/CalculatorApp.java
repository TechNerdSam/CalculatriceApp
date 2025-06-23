import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.DecimalFormat;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.Locale; // For better localization if needed, though DecimalFormat uses default

/**
 * CalculatorApp is the main class for a scientific and programmer calculator GUI.
 * It handles the user interface and delegates calculation logic to CalculatorLogic.
 *
 * This application prioritizes robustness, maintainability, and user experience.
 * It includes:
 * - Advanced programmer mode with multi-base arithmetic and bitwise operations.
 * - Enhanced error messaging with temporary visual cues.
 * - Comprehensive undo/redo functionality for state management.
 * - Extended keyboard input support for efficient interaction.
 * - Customizable display precision for numerical results.
 * - Code follows recent Java coding conventions for readability and maintainability.
 * - Code is documented in both English and French to ensure clarity for diverse audiences.
 *
 * CalculatriceApp est la classe principale pour une interface utilisateur de calculatrice scientifique et programmeur.
 * Elle gère l'interface utilisateur et délègue la logique de calcul à CalculatorLogic.
 *
 * Cette application privilégie la robustesse, la maintenabilité et l'expérience utilisateur.
 * Elle inclut :
 * - Un mode programmeur avancé avec arithmétique multi-base et opérations bit à bit.
 * - Une messagerie d'erreur améliorée avec des indices visuels temporaires.
 * - Une fonctionnalité complète d'annulation/rétablissement (undo/redo) pour la gestion de l'état.
 * - Un support étendu de la saisie au clavier pour une interaction efficace.
 * - Une précision d'affichage personnalisable pour les résultats numériques.
 * - Le code suit les conventions de codage Java récentes pour la lisibilité et la maintenabilité.
 * - Le code est documenté en anglais et en français pour assurer la clarté pour divers publics.
 */
public class CalculatorApp extends JFrame {

    // --- UI Components / Composants de l'interface utilisateur ---
    private JTextField displayField; // Display field for numbers and results / Champ d'affichage pour les nombres et les résultats
    private JPanel scientificKeysPanel; // Panel for extra scientific keys / Panneau pour les touches scientifiques supplémentaires
    private JPanel programmerKeysPanel; // Panel for programmer specific keys / Panneau pour les touches spécifiques au mode programmeur
    private JPanel keypadsArea;         // Panel holding special key panels and standard keys / Panneau contenant les panneaux de touches spéciales et les touches standard
    private JPanel specialKeysContainer; // Container for scientific/programmer panels using CardLayout / Conteneur pour les panneaux scientifique/programmeur utilisant CardLayout

    // --- CardLayout Constants / Constantes CardLayout ---
    private static final String SCI_PANEL_CARD = "ScientificPanel";
    private static final String PROG_PANEL_CARD = "ProgrammerPanel";
    private static final String EMPTY_PANEL_CARD = "EmptyPanel";

    // --- Calculator Logic and State / Logique et État de la Calculatrice ---
    // The state is not final as it will be replaced during undo/redo operations.
    // L'état n'est pas final car il sera remplacé lors des opérations d'annulation/rétablissement.
    private CalculatorState state;
    private final CalculatorLogic logic;

    // --- Display Formats / Formats d'Affichage ---
    // DecimalFormat for normal number display, with grouping.
    // DecimalFormat pour l'affichage normal des nombres, avec regroupement.
    private final DecimalFormat normalFormat;
    // DecimalFormat for scientific notation display, with grouping.
    // DecimalFormat pour l'affichage en notation scientifique, avec regroupement.
    private final DecimalFormat scientificFormat;

    // --- Enums for Modes / Énumérations pour les Modes ---
    /**
     * Defines the angle modes for trigonometric functions.
     * Définit les modes d'angle pour les fonctions trigonométriques.
     */
    enum AngleMode { DEG, RAD, GRAD }
    /**
     * Defines the main display modes of the calculator.
     * Définit les modes d'affichage principaux de la calculatrice.
     */
    enum DisplayMode { NORMAL, SCIENTIFIC, PROGRAMMER }
    /**
     * Defines the base modes for number representation in Programmer Mode.
     * Définit les modes de base pour la représentation numérique en mode Programmeur.
     */
    enum BaseMode { DEC, HEX, OCT, BIN }

    // --- Undo/Redo Stacks / Piles d'Annulation/Rétablissement ---
    // Stores previous states for undo functionality.
    // Stocke les états précédents pour la fonctionnalité d'annulation.
    private Stack<CalculatorState> undoStack;
    // Stores undone states for redo functionality.
    // Stocke les états annulés pour la fonctionnalité de rétablissement.
    private Stack<CalculatorState> redoStack;

    // --- Error Display Timer / Minuteur d'Affichage d'Erreur ---
    // Timer to clear temporary error messages from display.
    // Minuteur pour effacer les messages d'erreur temporaires de l'affichage.
    private Timer errorClearTimer;


    /**
     * Inner class to hold the calculator's current state.
     * This class primarily stores data; calculation logic is handled by CalculatorLogic.
     * It is static to avoid implicit reference to the outer CalculatorApp instance,
     * which helps in memory management and modularity.
     *
     * Classe interne pour contenir l'état actuel de la calculatrice.
     * Cette classe stocke principalement les données ; la logique de calcul est gérée par CalculatorLogic.
     * Elle est statique pour éviter une référence implicite à l'instance externe de CalculatorApp,
     * ce qui aide à la gestion de la mémoire et à la modularité.
     */
    private static class CalculatorState {
        // Input and Display / Entrée et Affichage
        String currentOperandText = "0";    // Text currently being typed or last formatted result / Texte actuellement saisi ou dernier résultat formaté
        boolean startNewNumber = true;      // If true, next digit starts a new number / Si vrai, le prochain chiffre commence un nouveau nombre
        boolean errorState = false;         // True if an error has occurred (e.g., division by zero) / Vrai si une erreur est survenue (par exemple, division par zéro)
        String errorMessage = "";           // Stores the current error message / Stocke le message d'erreur actuel

        // Core Calculation Values / Valeurs de Calcul Principales
        double currentValue = 0.0;          // Numeric value of the current operand or result / Valeur numérique de l'opérande ou du résultat actuel
        double previousValue = 0.0;         // Previous operand for binary operations / Opérande précédente pour les opérations binaires
        String pendingOperation = null;     // Operation waiting for a second operand (e.g., "+", "-") / Opération en attente d'un second opérande (par exemple, "+", "-")
        String lastOperatorForEquals = null; // For repeating equals with last operator / Pour répéter le signe égal avec le dernier opérateur
        double lastOperandForEquals = 0.0;  // For repeating equals with last operand / Pour répéter le signe égal avec le dernier opérande


        // Modes
        AngleMode angleMode = AngleMode.DEG;        // Default angle mode / Mode d'angle par défaut
        DisplayMode displayMode = DisplayMode.NORMAL; // Default display mode / Mode d'affichage par défaut
        BaseMode currentBase = BaseMode.DEC; // Default base mode for programmer calculations / Mode de base par défaut pour les calculs de programmeur
        int decimalPrecision = 10; // Default decimal precision for display / Précision décimale par défaut pour l'affichage

        // Memory / Mémoire
        double memoryValue = 0.0;           // Value stored in memory (M+, M-, MR, MC) / Valeur stockée en mémoire (M+, M-, MR, MC)

        /**
         * Copy constructor for creating snapshots of the calculator state,
         * essential for Undo/Redo functionality.
         *
         * Constructeur de copie pour créer des instantanés de l'état de la calculatrice,
         * essentiel pour la fonctionnalité d'Annulation/Rétablissement.
         * @param other The CalculatorState instance to copy. / L'instance CalculatorState à copier.
         */
        public CalculatorState(CalculatorState other) {
            this.currentOperandText = other.currentOperandText;
            this.startNewNumber = other.startNewNumber;
            this.errorState = other.errorState;
            this.errorMessage = other.errorMessage;
            this.currentValue = other.currentValue;
            this.previousValue = other.previousValue;
            this.pendingOperation = other.pendingOperation;
            this.lastOperatorForEquals = other.lastOperatorForEquals;
            this.lastOperandForEquals = other.lastOperandForEquals;
            this.angleMode = other.angleMode;
            this.displayMode = other.displayMode;
            this.currentBase = other.currentBase;
            this.decimalPrecision = other.decimalPrecision;
            this.memoryValue = other.memoryValue;
        }

        /**
         * Default constructor for CalculatorState, initializing default values.
         * Constructeur par défaut pour CalculatorState, initialisant les valeurs par défaut.
         */
        public CalculatorState() {
        }

        /**
         * Sets an error state with a specific message. This clears pending operations
         * and prepares the display for an error message.
         * Définit un état d'erreur avec un message spécifique. Cela efface les opérations en attente
         * et prépare l'affichage pour un message d'erreur.
         * @param message The error message to display. / Le message d'erreur à afficher.
         */
        public void setError(String message) {
            this.currentOperandText = message;
            this.errorMessage = message;
            this.errorState = true;
            this.startNewNumber = true;
            this.pendingOperation = null;
            this.lastOperatorForEquals = null; // Clear last equals operation on error / Effacer la dernière opération d'égalité en cas d'erreur
            this.lastOperandForEquals = 0.0;
        }

        /**
         * Clears all calculator state, resetting it to its initial default values.
         * Efface tout l'état de la calculatrice, le réinitialisant à ses valeurs par défaut initiales.
         */
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
            this.currentBase = BaseMode.DEC; // Reset base mode to decimal when clearing all / Réinitialiser le mode de base en décimal lors de tout effacement
            this.decimalPrecision = 10; // Reset precision / Réinitialiser la précision
        }

        /**
         * Clears the current entry or the entire state if an error is present.
         * Efface l'entrée actuelle ou l'état entier si une erreur est présente.
         */
        public void clearEntry() {
            if (this.errorState) {
                clearAll();
            } else {
                this.currentOperandText = "0";
                this.currentValue = 0.0;
                this.startNewNumber = true;
            }
        }

        /**
         * Clears the memory value, setting it to 0.0.
         * Efface la valeur en mémoire, la réinitialisant à 0.0.
         */
        public void memoryClear() {
            this.memoryValue = 0.0;
        }

        /**
         * Recalls the value stored in memory to the current operand.
         * Rappelle la valeur stockée en mémoire à l'opérande actuel.
         */
        public void memoryRecall() {
            this.currentValue = this.memoryValue;
            this.startNewNumber = true;
            this.errorState = false;
            this.errorMessage = "";
        }

        /**
         * Adds the given value to the memory.
         * Ajoute la valeur donnée à la mémoire.
         * @param value The value to add to memory. / La valeur à ajouter à la mémoire.
         */
        public void memoryAdd(double value) {
            this.memoryValue += value;
        }

        /**
         * Subtracts the given value from the memory.
         * Soustrait la valeur donnée de la mémoire.
         * @param value The value to subtract from memory. / La valeur à soustraire de la mémoire.
         */
        public void memorySubtract(double value) {
            this.memoryValue -= value;
        }
    }

    /**
     * Handles the core calculation logic for the calculator.
     * It operates on a shared CalculatorState instance.
     *
     * Gère la logique de calcul principale de la calculatrice.
     * Elle opère sur une instance partagée de CalculatorState.
     */
    private static class CalculatorLogic {
        private final CalculatorState state; // Operates on the shared state / Opère sur l'état partagé
        private static final double EPSILON = 1E-12; // Small value for floating point comparisons / Petite valeur pour les comparaisons en virgule flottante

        /**
         * Constructs a CalculatorLogic instance.
         * Construit une instance de CalculatorLogic.
         * @param state The shared CalculatorState instance. / L'instance partagée de CalculatorState.
         */
        public CalculatorLogic(CalculatorState state) {
            this.state = state;
        }

        /**
         * Appends a digit to the current operand text, handling new number initiation
         * and input length limits. In programmer mode, it filters digits based on the current base.
         * Ajoute un chiffre au texte de l'opérande actuel, gérant l'initiation d'un nouveau nombre
         * et les limites de longueur de la saisie. En mode programmeur, elle filtre les chiffres en fonction de la base actuelle.
         * @param digit The digit string to append. / La chaîne de chiffres à ajouter.
         */
        public void appendDigit(String digit) {
            if (state.errorState) {
                 state.clearAll();
            }

            // In programmer mode, filter digits based on current base.
            // En mode programmeur, filtrer les chiffres en fonction de la base actuelle.
            if (state.displayMode == DisplayMode.PROGRAMMER) {
                String validHexDigits = "0123456789ABCDEF";
                String validDecDigits = "0123456789";
                String validOctDigits = "01234567";
                String validBinDigits = "01";

                boolean isValid = false;
                switch (state.currentBase) {
                    case HEX: isValid = validHexDigits.contains(digit.toUpperCase()); break;
                    case DEC: isValid = validDecDigits.contains(digit); break;
                    case OCT: isValid = validOctDigits.contains(digit); break;
                    case BIN: isValid = validBinDigits.contains(digit); break;
                }
                if (!isValid) return; // Ignore invalid digit for current base / Ignorer le chiffre invalide pour la base actuelle
            }

            if (state.startNewNumber) {
                state.currentOperandText = digit;
                state.startNewNumber = false;
            } else {
                // Prevent leading zero if not a decimal and not already "0".
                // Empêcher le zéro non significatif si ce n'est pas un décimal et pas déjà "0".
                if (state.currentOperandText.equals("0") && !digit.equals("0") && !state.currentOperandText.contains(".")) {
                    state.currentOperandText = digit;
                } else if (!state.currentOperandText.equals("0") || !digit.equals("0") || state.currentOperandText.contains(".")) {
                    if (state.currentOperandText.length() < 16) { // Max input length to prevent overflow / Longueur maximale de la saisie pour éviter le dépassement
                        state.currentOperandText += digit;
                    }
                }
            }
        }

        /**
         * Appends a decimal point to the current operand text.
         * Ignored in programmer mode as it typically deals with integers.
         * Ajoute un point décimal au texte de l'opérande actuel.
         * Ignoré en mode programmeur car il gère généralement des entiers.
         */
        public void appendDecimal() {
            if (state.errorState) {
                state.clearAll();
            }
            // Decimal point is generally not used in integer-based programmer modes directly.
            // Le point décimal n'est généralement pas utilisé directement dans les modes programmeur basés sur des entiers.
            if (state.displayMode == DisplayMode.PROGRAMMER) {
                return; // Ignore decimal point in programmer mode / Ignorer le point décimal en mode programmeur
            }

            if (state.startNewNumber) {
                state.currentOperandText = "0.";
                state.startNewNumber = false;
            } else {
                if (!state.currentOperandText.contains(".")) {
                    if (state.currentOperandText.length() < 16) { // Max input length / Longueur maximale de la saisie
                        state.currentOperandText += ".";
                    }
                }
            }
        }

        /**
         * Parses the current operand text into a numeric value (double or long for programmer mode).
         * This method handles various input formats and sets error states for invalid inputs.
         * Analyse le texte de l'opérande actuel en une valeur numérique (double ou long pour le mode programmeur).
         * Cette méthode gère divers formats de saisie et définit les états d'erreur pour les entrées invalides.
         * @return true if parsing was successful, false otherwise. / vrai si l'analyse a réussi, faux sinon.
         */
        private boolean parseCurrentOperand() {
            // Replace comma with dot for consistent parsing, if locale uses comma as decimal separator.
            // Remplacer la virgule par un point pour une analyse cohérente, si la locale utilise la virgule comme séparateur décimal.
            String parsableText = state.currentOperandText.replace(',', '.');
            if (parsableText == null || parsableText.isEmpty() || parsableText.equals(".") || parsableText.equals("-") ||
                parsableText.endsWith("E") || parsableText.endsWith("e") ||
                parsableText.endsWith("E-") || parsableText.endsWith("e-")) {
                // Handle incomplete or special cases that are not yet full numbers.
                // Gérer les cas incomplets ou spéciaux qui ne sont pas encore des nombres complets.
                if (parsableText.equals(".") || parsableText.equals("-")) {
                    state.currentValue = 0.0;
                    state.currentOperandText = "0";
                    return true;
                }
                state.setError("Invalid Input: Cannot parse."); // More specific error message / Message d'erreur plus spécifique
                return false;
            }
            try {
                if (state.displayMode == DisplayMode.PROGRAMMER) {
                    long val;
                    try {
                        // Parse based on current base mode for programmer calculations.
                        // Analyser en fonction du mode de base actuel pour les calculs de programmeur.
                        switch (state.currentBase) {
                            case DEC: val = Long.parseLong(parsableText); break;
                            case HEX: val = Long.parseLong(parsableText, 16); break;
                            case OCT: val = Long.parseLong(parsableText, 8); break;
                            case BIN: val = Long.parseLong(parsableText, 2); break;
                            default: val = 0; // Should not happen with valid enum / Ne devrait pas arriver avec un enum valide
                        }
                        state.currentValue = (double) val; // Store as double for consistency, but treat as long for logic / Stocker en double pour la cohérence, mais traiter comme long pour la logique
                        return true;
                    } catch (NumberFormatException e) {
                        state.setError("Prog Error: Invalid Num Format."); // Specific error for programmer mode parsing / Erreur spécifique pour l'analyse en mode programmeur
                        return false;
                    }
                } else {
                    state.currentValue = Double.parseDouble(parsableText);
                    return true;
                }
            } catch (NumberFormatException e) {
                state.setError("Invalid Format: Number Expected."); // General number format error / Erreur de format numérique générale
                return false;
            }
        }

        /**
         * Sets the pending operation. If there's an existing pending operation,
         * it first performs the previous calculation.
         * Définit l'opération en attente. S'il y a une opération en attente existante,
         * elle effectue d'abord le calcul précédent.
         * @param operator The operator string (e.g., "+", "-", "AND"). / La chaîne d'opérateur (par exemple, "+", "-", "AND").
         */
        public void setOperator(String operator) {
            if (state.errorState) return;
            if (!state.startNewNumber) {
                if (!parseCurrentOperand()) return; // Parse current input before setting operator / Analyser l'entrée actuelle avant de définir l'opérateur
            }
            if (state.pendingOperation != null && !state.startNewNumber) {
                 performCalculation(); // Perform previous pending calculation / Effectuer le calcul en attente précédent
                 if (state.errorState) return; // Exit if calculation resulted in an error / Quitter si le calcul a entraîné une erreur
            }
            state.previousValue = state.currentValue;
            state.pendingOperation = operator;
            state.startNewNumber = true; // Next digit starts a new number / Le prochain chiffre commence un nouveau nombre
        }

        /**
         * Performs the pending calculation based on `pendingOperation`, `previousValue`, and `currentValue`.
         * Handles both floating-point and integer (programmer mode) calculations.
         * Effectue le calcul en attente basé sur `pendingOperation`, `previousValue` et `currentValue`.
         * Gère les calculs en virgule flottante et en entier (mode programmeur).
         */
        public void performCalculation() {
            // If no pending operation, check for repeating equals.
            // S'il n'y a pas d'opération en attente, vérifier la répétition du signe égal.
            if (state.pendingOperation == null) {
                if (state.lastOperatorForEquals != null) {
                    state.pendingOperation = state.lastOperatorForEquals;
                    state.previousValue = state.currentValue;
                    state.currentValue = state.lastOperandForEquals;
                } else {
                    // If no pending operation and no repeat, just parse current operand if not started new.
                    // S'il n'y a pas d'opération en attente et pas de répétition, analyser simplement l'opérande actuel si un nouveau n'a pas été démarré.
                    if (!state.startNewNumber) {
                        if(!parseCurrentOperand()) return;
                    }
                    state.startNewNumber = true;
                    return;
                }
            }

            double rightOperand = state.currentValue;
            double result = 0.0;

            // Differentiate calculation logic for programmer mode (long integers) vs. normal/scientific (doubles).
            // Différencier la logique de calcul pour le mode programmeur (entiers longs) vs normal/scientifique (doubles).
            if (state.displayMode == DisplayMode.PROGRAMMER) {
                long prevVal = (long) state.previousValue; // Cast to long for integer arithmetic / Convertir en long pour l'arithmétique entière
                long rightOp = (long) rightOperand;
                long longResult = 0;

                switch (state.pendingOperation) {
                    case "+": longResult = prevVal + rightOp; break;
                    case "-": longResult = prevVal - rightOp; break;
                    case "*": longResult = prevVal * rightOp; break;
                    case "/":
                        if (rightOp == 0) { state.setError("Prog Error: Div by Zero"); return; }
                        longResult = prevVal / rightOp; break;
                    case "Mod": // Modulo operator specific to programmer mode / Opérateur modulo spécifique au mode programmeur
                        if (rightOp == 0) { state.setError("Prog Error: Mod by Zero"); return; }
                        longResult = prevVal % rightOp; break;
                    case "AND": longResult = prevVal & rightOp; break; // Bitwise AND / ET bit à bit
                    case "OR": longResult = prevVal | rightOp; break;   // Bitwise OR / OU bit à bit
                    case "XOR": longResult = prevVal ^ rightOp; break; // Bitwise XOR / OU exclusif bit à bit
                    case "Lsh": longResult = prevVal << rightOp; break; // Left shift / Décalage à gauche
                    case "Rsh": longResult = prevVal >> rightOp; break; // Right shift / Décalage à droite
                    default: state.setError("Prog Error: Unknown Op"); return; // Unknown operator in programmer mode / Opérateur inconnu en mode programmeur
                }
                result = (double) longResult; // Convert back to double for storage / Reconvertir en double pour le stockage
            } else { // Normal and Scientific mode calculations / Calculs en mode Normal et Scientifique
                switch (state.pendingOperation) {
                    case "+": result = state.previousValue + rightOperand; break;
                    case "-": result = state.previousValue - rightOperand; break;
                    case "*": result = state.previousValue * rightOperand; break;
                    case "/":
                        if (Math.abs(rightOperand) < EPSILON) { // Check for division by near-zero / Vérifier la division par une valeur proche de zéro
                            state.setError("Division by Zero"); return;
                        }
                        result = state.previousValue / rightOperand; break;
                    case "x^y": result = Math.pow(state.previousValue, rightOperand); break;
                    default: state.setError("Unknown Operator"); return;
                }
            }

            // Check for infinite or NaN results from floating point operations.
            // Vérifier les résultats infinis ou NaN des opérations en virgule flottante.
            if (Double.isInfinite(result) || Double.isNaN(result)) {
                state.setError("Calculation Error: Overflow/Invalid Result.");
                return;
            }

            state.currentValue = result;
            state.lastOperatorForEquals = state.pendingOperation; // Store for repeating equals / Stocker pour répéter le signe égal
            state.lastOperandForEquals = rightOperand;
            state.pendingOperation = null; // Clear pending operation / Effacer l'opération en attente
            state.startNewNumber = true; // Next input will start a new number / La prochaine entrée commencera un nouveau nombre
        }

        /**
         * Handles the equals operation. Parses current operand and performs calculation.
         * Gère l'opération d'égalité. Analyse l'opérande actuel et effectue le calcul.
         */
        public void handleEquals() {
            if (state.errorState) return;
            if (!state.startNewNumber) {
                if (!parseCurrentOperand()) return;
            }
            performCalculation();
        }

        /**
         * Changes the sign of the current operand.
         * Modifie le signe de l'opérande actuel.
         */
        public void changeSign() {
            if (state.errorState) return;
            if (state.displayMode == DisplayMode.PROGRAMMER) {
                // In programmer mode, sign change is typically bitwise two's complement.
                // For simplicity here, we just negate the numerical value.
                // En mode programmeur, le changement de signe est typiquement le complément à deux bit à bit.
                // Pour simplifier ici, nous négativons simplement la valeur numérique.
                if (!state.startNewNumber) {
                    if (!parseCurrentOperand()) return;
                }
                state.currentValue = -state.currentValue;
            } else { // Normal/Scientific mode handling / Gestion du mode Normal/Scientifique
                if (state.startNewNumber) {
                    state.currentValue = -state.currentValue;
                } else {
                    if (state.currentOperandText.equals("0") || state.currentOperandText.equals("0.0")) return;
                    if (state.currentOperandText.startsWith("-")) {
                        state.currentOperandText = state.currentOperandText.substring(1);
                    } else {
                        if (state.currentOperandText.length() < 16) { // Ensure string doesn't get too long / S'assurer que la chaîne ne devient pas trop longue
                            state.currentOperandText = "-" + state.currentOperandText;
                        }
                    }
                }
            }
        }

        /**
         * Handles trigonometric functions (sin, cos, tan) based on the current angle mode.
         * Gère les fonctions trigonométriques (sin, cos, tan) en fonction du mode d'angle actuel.
         * @param funcName The name of the trigonometric function (e.g., "sin"). / Le nom de la fonction trigonométrique (par exemple, "sin").
         */
        public void handleTrigonometricFunction(String funcName) {
            if (state.errorState) return;
            if (state.displayMode == DisplayMode.PROGRAMMER) {
                state.setError("Trig Func not available in Programmer Mode.");
                return;
            }
            if (!state.startNewNumber) {
                if (!parseCurrentOperand()) return;
            }
            double angle = state.currentValue;
            double angleInRadians = angle;
            // Convert angle to radians based on current AngleMode.
            // Convertir l'angle en radians en fonction du AngleMode actuel.
            switch (state.angleMode) {
                case DEG: angleInRadians = Math.toRadians(angle); break;
                case GRAD: angleInRadians = angle * (Math.PI / 200.0); break;
                case RAD: break; // Already in radians / Déjà en radians
            }
            double result = 0.0;
            boolean calculationError = false;
            switch (funcName) {
                case "sin": result = Math.sin(angleInRadians); break;
                case "cos":
                    // Special handling for cos(90 deg), cos(100 grad), cos(pi/2 rad) to ensure 0.0 result.
                    // Traitement spécial pour cos(90 deg), cos(100 grad), cos(pi/2 rad) pour assurer un résultat de 0.0.
                    if ((state.angleMode == AngleMode.DEG && (Math.abs(angle % 180) == 90)) ||
                        (state.angleMode == AngleMode.GRAD && (Math.abs(angle % 200) == 100)) ||
                        (state.angleMode == AngleMode.RAD && (Math.abs(Math.abs(angleInRadians) % Math.PI - Math.PI/2) < EPSILON ))) {
                        result = 0.0;
                    } else { result = Math.cos(angleInRadians); }
                    break;
                case "tan":
                    // Handle undefined tangent cases more explicitly.
                    // Gérer les cas de tangente indéfinie plus explicitement.
                    if ((state.angleMode == AngleMode.DEG && (Math.abs(angle % 180) == 90)) ||
                        (state.angleMode == AngleMode.GRAD && (Math.abs(angle % 200) == 100)) ||
                        (state.angleMode == AngleMode.RAD && (Math.abs(Math.abs(angleInRadians) % Math.PI - Math.PI/2) < EPSILON ))) {
                        state.setError("Tan Undefined: Angle approaches singularity."); calculationError = true;
                    } else { result = Math.tan(angleInRadians); }
                    break;
                default: state.setError("Unknown Trig Func: Internal Error."); calculationError = true; break;
            }
            if (!calculationError) {
                if (Math.abs(result) < EPSILON) result = 0.0; // Clean up very small results to 0 / Nettoyer les très petits résultats à 0
                state.currentValue = result;
                 if (Double.isInfinite(state.currentValue) || Double.isNaN(state.currentValue)) {
                    state.setError("Invalid Trig Result: Calculation yielded non-finite value.");
                }
            }
            state.startNewNumber = true;
        }

        /**
         * Handles various unary operations like log, ln, sqrt, x², 1/x, and NOT (for programmer mode).
         * Gère diverses opérations unaires comme log, ln, sqrt, x², 1/x, et NOT (pour le mode programmeur).
         * @param operation The name of the unary operation. / Le nom de l'opération unaire.
         */
        public void handleUnaryOperation(String operation) {
            if (state.errorState) return;
            if (state.displayMode == DisplayMode.PROGRAMMER) {
                // Only 'NOT' is available in programmer mode among these unary ops.
                // Seul 'NOT' est disponible en mode programmeur parmi ces opérations unaires.
                if (!operation.equals("NOT")) {
                    state.setError("Unary Op not available in Programmer Mode (except NOT).");
                    return;
                }
            }
            if (!state.startNewNumber) {
                if (!parseCurrentOperand()) return;
            }
            double value = state.currentValue;
            double result = 0.0;
            boolean calcError = false;

            if (state.displayMode == DisplayMode.PROGRAMMER && operation.equals("NOT")) {
                long longValue = (long) value;
                long resultLong = ~longValue; // Bitwise NOT for long integer / NOT bit à bit pour entier long
                result = (double) resultLong;
            } else {
                switch (operation) {
                    case "log":
                        if (value <= 0) { state.setError("Log Error: Argument <= 0."); calcError = true; }
                        else { result = Math.log10(value); } break;
                    case "ln":
                        if (value <= 0) { state.setError("Ln Error: Argument <= 0."); calcError = true; }
                        else { result = Math.log(value); } break;
                    case "sqrt":
                        if (value < 0) { state.setError("Sqrt Error: Argument < 0."); calcError = true; }
                        else { result = Math.sqrt(value); } break;
                    case "x²": result = value * value; break;
                    case "1/x":
                        if (Math.abs(value) < EPSILON) { state.setError("Reciprocal of Zero: Division by zero."); calcError = true; }
                        else { result = 1.0 / value; } break;
                    default: state.setError("Unknown Unary Operation: Internal Error."); calcError = true; break;
                }
            }

            if (!calcError) {
                // Clean up very small non-zero results to 0.0.
                // Nettoyer les très petits résultats non nuls à 0.0.
                if (Math.abs(result) < EPSILON && result != 0.0) {
                    if (Math.abs(result) < 1E-15) result = 0.0;
                }
                state.currentValue = result;
                if (Double.isInfinite(state.currentValue) || Double.isNaN(state.currentValue)) {
                    state.setError("Calculation Error: Overflow/Invalid Unary Op Result.");
                }
            }
            state.startNewNumber = true;
        }

        /**
         * Handles the percentage operation.
         * Gère l'opération de pourcentage.
         */
        public void handlePercentage() {
            if (state.errorState) return;
            if (state.displayMode == DisplayMode.PROGRAMMER) {
                state.setError("Percentage not available in Programmer Mode.");
                return;
            }
            if (!state.startNewNumber) {
                if (!parseCurrentOperand()) return;
            }
            // Percentage logic varies based on pending operation.
            // La logique du pourcentage varie en fonction de l'opération en attente.
            if (state.pendingOperation != null && (state.pendingOperation.equals("+") || state.pendingOperation.equals("-"))) {
                state.currentValue = (state.previousValue * state.currentValue) / 100.0;
                performCalculation();
            } else if (state.pendingOperation != null && (state.pendingOperation.equals("*") || state.pendingOperation.equals("/"))) {
                state.currentValue = state.currentValue / 100.0;
                performCalculation();
            }
            else { // Direct percentage if no pending operation / Pourcentage direct si aucune opération en attente
                state.currentValue = state.currentValue / 100.0;
                state.lastOperatorForEquals = null;
                state.lastOperandForEquals = 0.0;
            }
            state.startNewNumber = true;
        }

        /**
         * Handles memory operations (MC, MR, M+, M-).
         * Gère les opérations de mémoire (MC, MR, M+, M-).
         * @param memOp The memory operation command. / La commande d'opération de mémoire.
         */
        public void handleMemoryOperation(String memOp) {
            if (state.errorState && !memOp.equals("MC") && !memOp.equals("MR")) return;
            // In programmer mode, M+ and M- operate on the long integer value.
            // En mode programmeur, M+ et M- opèrent sur la valeur entière longue.
            if (state.displayMode == DisplayMode.PROGRAMMER && !memOp.equals("MC") && !memOp.equals("MR")) {
                 if (!state.startNewNumber) {
                     if (!parseCurrentOperand()) {
                         if(state.errorState) return;
                     }
                 }
                long longValue = (long) state.currentValue;
                switch (memOp) {
                    case "M+": state.memoryValue += longValue; state.startNewNumber = true; break;
                    case "M-": state.memoryValue -= longValue; state.startNewNumber = true; break;
                }
            } else { // Normal/Scientific mode memory operations / Opérations de mémoire en mode Normal/Scientifique
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
    }

    /**
     * Constructs the main CalculatorApp frame.
     * Initializes UI components, logic, and state.
     * Construit la fenêtre principale de CalculatorApp.
     * Initialise les composants de l'interface utilisateur, la logique et l'état.
     */
    public CalculatorApp() {
        super("Calculatrice Scientifique et Programmeur Java - Client Haut de Gamme"); // Updated Title for client / Titre mis à jour pour le client
        state = new CalculatorState();
        logic = new CalculatorLogic(state);

        // Initialize DecimalFormat for normal number display with grouping and customizable precision.
        // Initialiser DecimalFormat pour l'affichage normal des nombres avec regroupement et précision personnalisable.
        normalFormat = new DecimalFormat("#,##0.##########", new java.text.DecimalFormatSymbols(Locale.US)); // Use US locale for consistent decimal/grouping
        normalFormat.setMinimumFractionDigits(0); // Allow 0 fractional digits by default
        normalFormat.setMaximumFractionDigits(state.decimalPrecision); // Set max based on state
        normalFormat.setGroupingUsed(true); // Enable grouping

        // Initialize DecimalFormat for scientific notation display with grouping and customizable precision.
        // Initialiser DecimalFormat pour l'affichage en notation scientifique avec regroupement et précision personnalisable.
        scientificFormat = new DecimalFormat("0.#########E0", new java.text.DecimalFormatSymbols(Locale.US)); // Use US locale for consistent decimal/grouping
        scientificFormat.setMinimumFractionDigits(0); // Allow 0 fractional digits by default
        scientificFormat.setMaximumFractionDigits(state.decimalPrecision); // Set max based on state
        scientificFormat.setGroupingUsed(true); // Enable grouping


        undoStack = new Stack<>();
        redoStack = new Stack<>();

        // Initialize error clear timer as a daemon thread.
        // Initialiser le minuteur d'effacement d'erreur comme un thread démon.
        errorClearTimer = new Timer(true);

        initComponents(); // Initialize UI components / Initialiser les composants de l'interface utilisateur

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Set close operation / Définir l'opération de fermeture
        pack(); // Adjust frame size to fit components / Ajuster la taille de la fenêtre aux composants
        setLocationRelativeTo(null); // Center the frame on screen / Centrer la fenêtre sur l'écran
        setVisible(true); // Make the frame visible / Rendre la fenêtre visible

        // Add key listener to the frame for keyboard input.
        // Ajouter un écouteur de clavier à la fenêtre pour la saisie au clavier.
        this.addKeyListener(new KeyboardHandler());
        this.setFocusable(true); // Ensure the frame can receive keyboard focus / S'assurer que la fenêtre peut recevoir le focus clavier
        this.requestFocusInWindow(); // Request focus for keyboard input / Demander le focus pour la saisie au clavier

        // Save initial state to undo stack to allow undoing from the very beginning.
        // Sauvegarder l'état initial dans la pile d'annulation pour permettre l'annulation dès le début.
        pushStateForUndo();
    }

    /**
     * Initializes and lays out all Swing UI components of the calculator.
     * Initialise et dispose tous les composants d'interface utilisateur Swing de la calculatrice.
     */
    private void initComponents() {
        displayField = new JTextField(state.currentOperandText, 16);
        displayField.setEditable(false); // Display field is not directly editable by user / Le champ d'affichage n'est pas directement modifiable par l'utilisateur
        displayField.setHorizontalAlignment(JTextField.RIGHT); // Align text to the right / Aligner le texte à droite
        displayField.setFont(new Font("Arial", Font.BOLD, 32)); // Set font for display / Définir la police pour l'affichage
        displayField.setBackground(Color.WHITE); // White background for display / Fond blanc pour l'affichage
        // Add padding around the display field / Ajouter un remplissage autour du champ d'affichage
        displayField.setBorder(BorderFactory.createCompoundBorder(
                displayField.getBorder(),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(displayField, BorderLayout.NORTH); // Display field at the top / Champ d'affichage en haut

        ButtonHandler buttonHandler = new ButtonHandler(); // Central action listener for all buttons / Écouteur d'actions central pour tous les boutons

        // Panel for mode selection (angle, display, base, and precision)
        // Panneau pour la sélection de mode (angle, affichage, base et précision)
        JPanel modeSelectionPanel = new JPanel(new GridLayout(4, 1, 5, 5)); // 4 rows for modes + precision / 4 lignes pour les modes + précision

        // Angle Mode Panel / Panneau Mode d'Angle
        JPanel angleModePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        angleModePanel.setBorder(BorderFactory.createTitledBorder("Angle Mode")); // Descriptive title / Titre descriptif
        JRadioButton degButton = createRadioButton("DEG", AngleMode.DEG.name(), state.angleMode == AngleMode.DEG, buttonHandler);
        JRadioButton radButton = createRadioButton("RAD", AngleMode.RAD.name(), state.angleMode == AngleMode.RAD, buttonHandler);
        JRadioButton gradButton = createRadioButton("GRAD", AngleMode.GRAD.name(), state.angleMode == AngleMode.GRAD, buttonHandler);
        ButtonGroup angleGroup = new ButtonGroup();
        angleGroup.add(degButton); angleGroup.add(radButton); angleGroup.add(gradButton);
        angleModePanel.add(degButton); angleModePanel.add(radButton); angleModePanel.add(gradButton);
        modeSelectionPanel.add(angleModePanel);

        // Display Mode Panel / Panneau Mode d'Affichage
        JPanel displayModePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        displayModePanel.setBorder(BorderFactory.createTitledBorder("Display Mode")); // Descriptive title / Titre descriptif
        JRadioButton normalDispButton = createRadioButton("NORMAL", "NORMAL_DISP", state.displayMode == DisplayMode.NORMAL, buttonHandler);
        JRadioButton sciDispButton = createRadioButton("SCI", "SCI_DISP", state.displayMode == DisplayMode.SCIENTIFIC, buttonHandler);
        JRadioButton progDispButton = createRadioButton("PROG", "PROG_DISP", state.displayMode == DisplayMode.PROGRAMMER, buttonHandler);
        ButtonGroup displayGroup = new ButtonGroup();
        displayGroup.add(normalDispButton); displayGroup.add(sciDispButton); displayGroup.add(progDispButton);
        displayModePanel.add(normalDispButton); displayModePanel.add(sciDispButton); displayModePanel.add(progDispButton);
        modeSelectionPanel.add(displayModePanel);

        // Base Mode Panel for Programmer Mode / Panneau Mode de Base pour le Mode Programmeur
        JPanel baseModePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        baseModePanel.setBorder(BorderFactory.createTitledBorder("Base Mode"));
        JRadioButton decBaseButton = createRadioButton("DEC", BaseMode.DEC.name(), state.currentBase == BaseMode.DEC, buttonHandler);
        JRadioButton hexBaseButton = createRadioButton("HEX", BaseMode.HEX.name(), state.currentBase == BaseMode.HEX, buttonHandler);
        JRadioButton octBaseButton = createRadioButton("OCT", BaseMode.OCT.name(), state.currentBase == BaseMode.OCT, buttonHandler);
        JRadioButton binBaseButton = createRadioButton("BIN", BaseMode.BIN.name(), state.currentBase == BaseMode.BIN, buttonHandler);
        ButtonGroup baseGroup = new ButtonGroup();
        baseGroup.add(decBaseButton); baseGroup.add(hexBaseButton); baseGroup.add(octBaseButton); baseGroup.add(binBaseButton);
        baseModePanel.add(decBaseButton); baseModePanel.add(hexBaseButton); baseModePanel.add(octBaseButton); baseModePanel.add(binBaseButton);
        modeSelectionPanel.add(baseModePanel);

        // Precision Control Panel / Panneau de Contrôle de la Précision
        JPanel precisionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        precisionPanel.setBorder(BorderFactory.createTitledBorder("Precision (Decimals)")); // Title for precision control
        SpinnerModel precisionModel = new SpinnerNumberModel(state.decimalPrecision, 0, 15, 1); // Range 0-15 decimals
        JSpinner precisionSpinner = new JSpinner(precisionModel);
        precisionSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                state.decimalPrecision = (int) precisionSpinner.getValue(); // Update state with new precision
                updateDisplay(); // Redraw display with new precision
            }
        });
        precisionPanel.add(precisionSpinner);
        modeSelectionPanel.add(precisionPanel);


        // --- Scientific Keys Panel ---
        scientificKeysPanel = new JPanel(new GridLayout(0, 1, 3, 3)); // Flexible rows, 1 column
        scientificKeysPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5)); // Right padding
        scientificKeysPanel.add(createSimpleButton("sin", "SCMD_SIN", buttonHandler));
        scientificKeysPanel.add(createSimpleButton("cos", "SCMD_COS", buttonHandler));
        scientificKeysPanel.add(createSimpleButton("log", "SCMD_LOG", buttonHandler));
        scientificKeysPanel.add(createSimpleButton("π", "SCMD_PI", buttonHandler));
        scientificKeysPanel.add(createSimpleButton("e", "SCMD_E", buttonHandler));

        // --- Programmer Keys Panel ---
        programmerKeysPanel = new JPanel(new GridLayout(0, 1, 3, 3)); // Flexible rows, 1 column
        programmerKeysPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5)); // Right margin
        // Programmer keys for bitwise and integer operations. Base selection is handled by radio buttons.
        programmerKeysPanel.add(createSimpleButton("AND", "AND", buttonHandler));
        programmerKeysPanel.add(createSimpleButton("OR", "OR", buttonHandler));
        programmerKeysPanel.add(createSimpleButton("XOR", "XOR", buttonHandler));
        programmerKeysPanel.add(createSimpleButton("NOT", "NOT", buttonHandler));
        programmerKeysPanel.add(createSimpleButton("Lsh", "Lsh", buttonHandler));
        programmerKeysPanel.add(createSimpleButton("Rsh", "Rsh", buttonHandler));
        programmerKeysPanel.add(createSimpleButton("Mod", "Mod", buttonHandler));


        // --- Special Keys Container (using CardLayout) ---
        // This panel uses CardLayout to switch between scientific, programmer, and empty keypads.
        specialKeysContainer = new JPanel(new CardLayout(0,0)); // CardLayout with no gaps
        specialKeysContainer.add(new JPanel(), EMPTY_PANEL_CARD); // Empty panel for NORMAL mode
        specialKeysContainer.add(scientificKeysPanel, SCI_PANEL_CARD);
        specialKeysContainer.add(programmerKeysPanel, PROG_PANEL_CARD);


        // --- Main Buttons Panel (GridBagLayout) ---
        JPanel standardButtonsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH; // Buttons fill their display area
        gbc.weightx = 1.0; // Distribute horizontal space evenly
        gbc.weighty = 1.0; // Distribute vertical space evenly
        gbc.insets = new Insets(3,3,3,3); // Padding around each button

        // Configuration of standard calculator buttons in a grid layout.
        String[][] updatedButtonGridConfig = {
                {"MC", "MR", "M+", "M-", "C", "Undo", "Redo"}, // Added Undo/Redo for state management
                {"sin", "cos", "tan", "x^y", "CE"},
                {"log", "ln", "sqrt", "x²", "%"},
                {"7", "8", "9", "/", "1/x"},
                {"4", "5", "6", "*", "+/-"},
                {"1", "2", "3", "-", "="},
                {"0", null, ".", null, null}
        };


        for (int row = 0; row < updatedButtonGridConfig.length; row++) {
            for (int col = 0; col < updatedButtonGridConfig[row].length; col++) {
                String label = updatedButtonGridConfig[row][col];
                if (label == null) continue; // Skip null placeholders in grid

                JButton button = new JButton(label);
                button.setFont(new Font("Arial", Font.PLAIN, 15));
                button.setMargin(new Insets(8,8,8,8)); // Inner padding for button text
                button.setActionCommand(label); // Set action command to button label
                button.addActionListener(buttonHandler); // Register common action listener

                // Apply specific styling based on button type for better visual distinction.
                if (label.matches("[0-9A-Fa-f]")) { // Digits and hex characters
                    button.setForeground(Color.BLUE);
                    button.setBackground(new Color(230, 230, 255));
                }
                else if (label.matches("[+\\-*/]|x\\^y|1/x")) { // Standard arithmetic operators
                    button.setForeground(Color.RED);
                    button.setBackground(new Color(255, 230, 230));
                }
                else if (label.equals("C") || label.equals("CE")) { // Clear buttons
                    button.setBackground(new Color(255, 180, 180));
                    button.setForeground(Color.DARK_GRAY);
                }
                else if (label.equals("sin") || label.equals("cos") || label.equals("tan") || // Scientific functions
                         label.equals("log") || label.equals("ln") || label.equals("sqrt") || label.equals("x²")) {
                    button.setBackground(new Color(210, 230, 255));
                }
                else if (label.startsWith("M")) { // Memory operations
                    button.setBackground(new Color(220, 255, 220));
                }
                else if (label.equals("=") || label.equals("Undo") || label.equals("Redo")) { // Action/history buttons
                    button.setBackground(Color.ORANGE);
                    button.setForeground(Color.WHITE);
                    button.setFont(new Font("Arial", Font.BOLD, 18));
                }
                else if (label.equals("+/-") || label.equals("%")) { // Sign change and percentage
                    button.setBackground(new Color(230,230,230));
                }

                gbc.gridx = col;
                gbc.gridy = row;
                gbc.gridwidth = 1;
                gbc.gridheight = 1;

                // Special sizing for "=" and "0" buttons for better layout.
                if (label.equals("=")) { gbc.gridheight = 2; } // "=" button spans two rows
                if (label.equals("0")) { gbc.gridwidth = 2; } // "0" button spans two columns

                standardButtonsPanel.add(button, gbc);

                if (label.equals("0")) col++; // Adjust column index for "0" button's width
            }
        }

        keypadsArea = new JPanel(new BorderLayout(5, 5));
        keypadsArea.add(specialKeysContainer, BorderLayout.WEST); // Special keypads to the west
        keypadsArea.add(standardButtonsPanel, BorderLayout.CENTER); // Standard buttons in the center

        JPanel controlsAndKeypadsPanel = new JPanel(new BorderLayout(5, 5));
        controlsAndKeypadsPanel.add(modeSelectionPanel, BorderLayout.NORTH); // Mode selection at the top
        controlsAndKeypadsPanel.add(keypadsArea, BorderLayout.CENTER); // Keypads in the center

        mainPanel.add(controlsAndKeypadsPanel, BorderLayout.CENTER);
        add(mainPanel);

        updateSpecialKeyPanelsVisibility(); // Set initial visibility of special key panels
        updateDisplay(); // Initial display update
    }

    /**
     * Helper method to create a JRadioButton with common properties.
     * Méthode d'aide pour créer un JRadioButton avec des propriétés communes.
     * @param text The display text for the radio button. / Le texte d'affichage du bouton radio.
     * @param actionCommand The action command string for the button. / La chaîne de commande d'action pour le bouton.
     * @param selected Initial selection state. / État de sélection initial.
     * @param listener The ActionListener for the button. / L'ActionListener pour le bouton.
     * @return A configured JRadioButton. / Un JRadioButton configuré.
     */
    private JRadioButton createRadioButton(String text, String actionCommand, boolean selected, ActionListener listener) {
        JRadioButton button = new JRadioButton(text, selected);
        button.setActionCommand(actionCommand);
        button.addActionListener(listener);
        return button;
    }

    /**
     * Helper method to create a JButton with common properties.
     * Méthode d'aide pour créer un JButton avec des propriétés communes.
     * @param text The display text for the button. / Le texte d'affichage du bouton.
     * @param actionCommand The action command string for the button. / La chaîne de commande d'action pour le bouton.
     * @param listener The ActionListener for the button. / L'ActionListener pour le bouton.
     * @return A configured JButton. / Un JButton configuré.
     */
    private JButton createSimpleButton(String text, String actionCommand, ActionListener listener) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.PLAIN, 14));
        button.setMargin(new Insets(5,5,5,5)); // Smaller margin for these buttons
        button.setActionCommand(actionCommand);
        button.addActionListener(listener);
        // Apply distinct background for programmer mode buttons for visual clarity.
        if (actionCommand.equals("AND") || actionCommand.equals("OR") || actionCommand.equals("XOR") ||
            actionCommand.equals("NOT") || actionCommand.equals("Lsh") || actionCommand.equals("Rsh") ||
            actionCommand.equals("Mod")) {
            button.setBackground(new Color(220, 220, 200));
        } else { // Default background for scientific buttons
            button.setBackground(new Color(200, 220, 255));
        }
        return button;
    }

    /**
     * Updates the visibility of the scientific and programmer key panels using CardLayout.
     * This method ensures only the relevant keypad is shown based on the current display mode.
     * Met à jour la visibilité des panneaux de touches scientifique et programmeur à l'aide de CardLayout.
     * Cette méthode garantit que seul le clavier pertinent est affiché en fonction du mode d'affichage actuel.
     */
    private void updateSpecialKeyPanelsVisibility() {
        CardLayout cl = (CardLayout)(specialKeysContainer.getLayout());
        String cardToShow = EMPTY_PANEL_CARD; // Default to empty panel for NORMAL mode

        if (state.displayMode == DisplayMode.SCIENTIFIC) {
            cardToShow = SCI_PANEL_CARD;
        } else if (state.displayMode == DisplayMode.PROGRAMMER) {
            cardToShow = PROG_PANEL_CARD;
        }

        cl.show(specialKeysContainer, cardToShow); // Show the selected card
        pack(); // Re-pack the frame to adjust layout after card change
    }


    /**
     * Updates the display field with the current operand text or formatted value.
     * Handles error display and numerical formatting based on current modes and precision.
     * Met à jour le champ d'affichage avec le texte de l'opérande actuel ou la valeur formatée.
     * Gère l'affichage des erreurs et le formatage numérique en fonction des modes et de la précision actuels.
     */
    private void updateDisplay() {
        // Stop any existing error timer to prevent conflicting updates.
        if (errorClearTimer != null) {
            errorClearTimer.cancel();
            errorClearTimer.purge();
            errorClearTimer = new Timer(true); // Create a new timer for future errors
        }

        // Update formatters with current precision setting before formatting.
        normalFormat.setMaximumFractionDigits(state.decimalPrecision);
        normalFormat.setMinimumFractionDigits(0); // Allow 0 fractional digits, so 5.0 displays as "5" if precision allows.
        scientificFormat.setMaximumFractionDigits(state.decimalPrecision);
        scientificFormat.setMinimumFractionDigits(0);

        if (state.errorState) {
            displayField.setText(state.errorMessage);
            displayField.setForeground(Color.RED);
            displayField.setBackground(new Color(255, 200, 200)); // Light red background for errors for clear visual cue.
            // Schedule the display to revert after 2 seconds.
            errorClearTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    SwingUtilities.invokeLater(() -> { // Ensure UI update on EDT
                        state.errorState = false; // Clear error state after display duration
                        displayField.setForeground(Color.BLACK);
                        displayField.setBackground(Color.WHITE);
                        updateDisplay(); // Re-display the actual value (or "0") after error message is gone
                    });
                }
            }, 2000); // 2 seconds delay for error message display
        } else {
            displayField.setForeground(Color.BLACK);
            displayField.setBackground(Color.WHITE); // Ensure background is normal
            String textToDisplay;

            // Handle special cases for operand text that might be incomplete (e.g., during 'E' input).
            if (state.startNewNumber ||
                state.currentOperandText.endsWith("E") || state.currentOperandText.endsWith("e") ||
                state.currentOperandText.endsWith("E-") || state.currentOperandText.endsWith("e-")) {

                if (state.currentOperandText.endsWith("E") || state.currentOperandText.endsWith("e") ||
                    state.currentOperandText.endsWith("E-") || state.currentOperandText.endsWith("e-")) {
                     textToDisplay = state.currentOperandText;
                } else {
                    if (state.currentValue == 0.0 && state.startNewNumber) {
                         textToDisplay = "0"; // Display "0" when starting a new number and value is zero
                    } else {
                        // Format the current value based on the selected display mode and base.
                        switch (state.displayMode) {
                            case SCIENTIFIC:
                                textToDisplay = scientificFormat.format(state.currentValue);
                                break;
                            case PROGRAMMER:
                                // Convert to long for integer-based programmer formatting.
                                long longVal = (long) state.currentValue;
                                switch (state.currentBase) {
                                    case DEC: textToDisplay = String.valueOf(longVal); break;
                                    case HEX: textToDisplay = Long.toHexString(longVal).toUpperCase(); break;
                                    case OCT: textToDisplay = Long.toOctalString(longVal); break;
                                    case BIN: textToDisplay = Long.toBinaryString(longVal); break;
                                    default: textToDisplay = String.valueOf(longVal); break; // Fallback
                                }
                                break;
                            case NORMAL:
                            default:
                                // Apply scientific format for very large or very small numbers even in normal mode.
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
                textToDisplay = state.currentOperandText; // Display current input string as is
            }
            displayField.setText(textToDisplay);
            // After displaying, if starting a new number and no error, update currentOperandText for consistency.
            if (state.startNewNumber && !state.errorState) {
                state.currentOperandText = textToDisplay;
            }
        }
    }

    /**
     * Central ActionListener for all calculator buttons.
     * Dispatches commands to the CalculatorLogic or updates CalculatorState directly.
     * ActionListener central pour tous les boutons de la calculatrice.
     * Dispatch les commandes à CalculatorLogic ou met à jour CalculatorState directement.
     */
    private class ButtonHandler implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();

            // Handle Undo/Redo commands separately as they manage state history.
            if (command.equals("Undo")) {
                performUndo();
                return; // Do not push state again for undo/redo actions.
            } else if (command.equals("Redo")) {
                performRedo();
                return; // Do not push state again for undo/redo actions.
            }

            // Save state before performing any action that modifies the calculator state (for Undo).
            pushStateForUndo();

            // If in an error state, only allow specific commands that clear the error or change modes.
            if (state.errorState && !command.equals("C") && !command.equals("CE") &&
                !command.equals("MC") && !command.equals("MR") &&
                !command.equals(AngleMode.DEG.name()) && !command.equals(AngleMode.RAD.name()) && !command.equals(AngleMode.GRAD.name()) &&
                !command.equals(DisplayMode.NORMAL.name() + "_DISP") && !command.equals(DisplayMode.SCIENTIFIC.name() + "_DISP") && !command.equals(DisplayMode.PROGRAMMER.name() + "_DISP") &&
                !command.equals(BaseMode.DEC.name()) && !command.equals(BaseMode.HEX.name()) && !command.equals(BaseMode.OCT.name()) && !command.equals(BaseMode.BIN.name())
                ) {
                return; // Ignore other commands while in error state.
            }

            // Dispatch commands based on their action string.
            if (command.matches("[0-9A-Fa-f]")) { // Digit or hex character input
                logic.appendDigit(command);
            }
            else if (command.equals(".")) { // Decimal point
                logic.appendDecimal();
            }
            // Standard arithmetic operators and bitwise operators for programmer mode.
            else if (command.matches("[+\\-*/]|x\\^y|AND|OR|XOR|Lsh|Rsh")) {
                logic.setOperator(command);
            }
            else if (command.equals("Mod")) { // Modulo operator, distinct from percentage
                logic.setOperator("Mod");
            }
            else if (command.equals("=")) { // Equals command
                logic.handleEquals();
            }
            else if (command.equals("C")) { // Clear All
                state.clearAll();
            }
            else if (command.equals("CE")) { // Clear Entry
                state.clearEntry();
            }
            else if (command.equals("+/-")) { // Change Sign
                logic.changeSign();
            }
            else if (command.equals("sin") || command.equals("cos") || command.equals("tan")) { // Trigonometric functions
                logic.handleTrigonometricFunction(command);
            }
            else if (command.equals("log") || command.equals("ln") || command.equals("sqrt") || // Unary operations
                     command.equals("x²") || command.equals("1/x") || command.equals("NOT")) {
                logic.handleUnaryOperation(command);
            }
            else if (command.equals("%")) { // Percentage operation
                logic.handlePercentage();
            }
            else if (command.equals("MC") || command.equals("MR") || command.equals("M+") || command.equals("M-")) { // Memory operations
                logic.handleMemoryOperation(command);
            }
            // Angle Mode selection
            else if (command.equals(AngleMode.DEG.name())) {
                state.angleMode = AngleMode.DEG;
            }
            else if (command.equals(AngleMode.RAD.name())) {
                state.angleMode = AngleMode.RAD;
            }
            else if (command.equals(AngleMode.GRAD.name())) {
                state.angleMode = AngleMode.GRAD;
            }
            // Display Mode changes
            else if (command.equals("NORMAL_DISP")) {
                state.displayMode = DisplayMode.NORMAL;
                forceDisplayUpdateForModeChange();
                updateSpecialKeyPanelsVisibility();
            } else if (command.equals("SCI_DISP")) {
                state.displayMode = DisplayMode.SCIENTIFIC;
                forceDisplayUpdateForModeChange();
                updateSpecialKeyPanelsVisibility();
            } else if (command.equals("PROG_DISP")) {
                state.displayMode = DisplayMode.PROGRAMMER;
                forceDisplayUpdateForModeChange();
                updateSpecialKeyPanelsVisibility();
            }
            // Base Mode changes
            else if (command.equals(BaseMode.DEC.name())) {
                state.currentBase = BaseMode.DEC;
                forceDisplayUpdateForModeChange();
            } else if (command.equals(BaseMode.HEX.name())) {
                state.currentBase = BaseMode.HEX;
                forceDisplayUpdateForModeChange();
            } else if (command.equals(BaseMode.OCT.name())) {
                state.currentBase = BaseMode.OCT;
                forceDisplayUpdateForModeChange();
            } else if (command.equals(BaseMode.BIN.name())) {
                state.currentBase = BaseMode.BIN;
                forceDisplayUpdateForModeChange();
            }
            // Scientific constants: Pi and e
            else if (command.equals("SCMD_PI")) {
                if (state.errorState) state.clearAll();
                state.currentOperandText = String.valueOf(Math.PI);
                logic.parseCurrentOperand(); // Parse to update currentValue
                state.startNewNumber = true;
            }
            else if (command.equals("SCMD_E")) {
                 if (state.errorState) state.clearAll();
                state.currentOperandText = String.valueOf(Math.E);
                logic.parseCurrentOperand(); // Parse to update currentValue
                state.startNewNumber = true;
            }

            updateDisplay(); // Refresh the display after any action
        }

        /**
         * Forces a display update, especially useful after mode or base changes,
         * to ensure the displayed value is correctly re-formatted according to the new settings.
         * Force une mise à jour de l'affichage, particulièrement utile après des changements de mode ou de base,
         * pour s'assurer que la valeur affichée est correctement reformatée selon les nouveaux réglages.
         */
        private void forceDisplayUpdateForModeChange() {
            if (!state.startNewNumber && !state.errorState) {
                logic.parseCurrentOperand(); // Ensure current value is correctly interpreted
            }
            state.startNewNumber = true; // Set to true to ensure the display logic re-formats from currentValue.
            updateDisplay();
            // After updateDisplay, currentOperandText might be formatted. Re-parse if it was an active input.
            if (!state.startNewNumber && !state.errorState) {
                 logic.parseCurrentOperand(); // Re-parse to ensure internal value consistency
            }
        }
    }

    /**
     * Pushes a copy of the current calculator state onto the undo stack.
     * Clears the redo stack as a new action invalidates future "redo" operations.
     * Pousse une copie de l'état actuel de la calculatrice sur la pile d'annulation.
     * Efface la pile de rétablissement car une nouvelle action invalide les futures opérations de "rétablissement".
     */
    private void pushStateForUndo() {
        undoStack.push(new CalculatorState(state)); // Push a copy of the current state
        redoStack.clear(); // Clear redo stack on new action
    }

    /**
     * Reverts the calculator to a previous state by popping from the undo stack.
     * The current state is pushed onto the redo stack.
     * Rétablit la calculatrice à un état précédent en dépilant de la pile d'annulation.
     * L'état actuel est poussé sur la pile de rétablissement.
     */
    private void performUndo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(new CalculatorState(state)); // Save current state to redo stack
            state = undoStack.pop(); // Revert to previous state
            updateDisplay(); // Update display to reflect the undone state
        }
    }

    /**
     * Reapplies a previously undone state by popping from the redo stack.
     * The current state is pushed onto the undo stack.
     * Réapplique un état précédemment annulé en dépilant de la pile de rétablissement.
     * L'état actuel est poussé sur la pile d'annulation.
     */
    private void performRedo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(new CalculatorState(state)); // Save current state to undo stack
            state = redoStack.pop(); // Reapply a previously undone state
            updateDisplay(); // Update display to reflect the redone state
        }
    }

    /**
     * Handles keyboard input for calculator operations.
     * It maps keyboard events to corresponding calculator button commands.
     * Gère la saisie au clavier pour les opérations de la calculatrice.
     * Elle mappe les événements clavier aux commandes de bouton de calculatrice correspondantes.
     */
    private class KeyboardHandler implements KeyListener {
        @Override
        public void keyTyped(KeyEvent e) {
            // Not used for most actions, as keyPressed is generally preferred for immediate command dispatch.
        }

        @Override
        public void keyPressed(KeyEvent e) {
            String command = null;
            char keyChar = e.getKeyChar();
            int keyCode = e.getKeyCode();

            // Map digit keys (0-9)
            if (Character.isDigit(keyChar)) {
                command = String.valueOf(keyChar);
            }
            // Map decimal point
            else if (keyChar == '.') {
                command = ".";
            }
            // Map common arithmetic operators
            else if (keyChar == '+') {
                command = "+";
            } else if (keyChar == '-') {
                command = "-";
            } else if (keyChar == '*') {
                command = "*";
            } else if (keyChar == '/') {
                command = "/";
            }
            // Map Enter key to "="
            else if (keyCode == KeyEvent.VK_ENTER) {
                command = "=";
            }
            // Map Backspace to Clear Entry (CE) for common calculator behavior.
            else if (keyCode == KeyEvent.VK_BACK_SPACE) {
                command = "CE";
            }
            // Map Escape key to Clear All (C).
            else if (keyCode == KeyEvent.VK_ESCAPE) {
                command = "C";
            }
            // Map percentage key.
            else if (keyChar == '%') {
                command = "%";
            }
            // Map equals key (can be Shift+= on some layouts).
            else if (keyChar == '=') {
                 command = "=";
            }
            // Map hex digits (A-F, a-f) for programmer mode.
            else if (keyChar >= 'a' && keyChar <= 'f' || keyChar >= 'A' && keyChar <= 'F') {
                if (state.displayMode == DisplayMode.PROGRAMMER && state.currentBase == BaseMode.HEX) {
                    command = String.valueOf(Character.toUpperCase(keyChar));
                }
            }
            // Add mapping for Undo (Ctrl+Z) and Redo (Ctrl+Y or Ctrl+Shift+Z)
            else if (e.isControlDown() && keyCode == KeyEvent.VK_Z) { // Ctrl+Z for Undo
                command = "Undo";
            } else if (e.isControlDown() && keyCode == KeyEvent.VK_Y) { // Ctrl+Y for Redo
                command = "Redo";
            }


            // If a command is identified, dispatch it to the ButtonHandler.
            if (command != null) {
                SwingUtilities.invokeLater(() -> {
                    // Create a new ActionEvent and pass it to the ButtonHandler.
                    new ButtonHandler().actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, command));
                });
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            // Not used for this application.
        }
    }


    /**
     * Main method to start the Calculator application.
     * Sets up the Swing GUI on the Event Dispatch Thread (EDT).
     * Méthode principale pour démarrer l'application Calculatrice.
     * Configure l'interface graphique Swing sur le Event Dispatch Thread (EDT).
     * @param args Command line arguments (not used). / Arguments de la ligne de commande (non utilisés).
     */
    public static void main(String[] args) {
        // Ensure GUI updates are done on the Event Dispatch Thread (EDT).
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    // Attempt to set Nimbus Look and Feel for a modern appearance.
                    boolean nimbusFound = false;
                    for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                        if ("Nimbus".equals(info.getName())) {
                            UIManager.setLookAndFeel(info.getClassName());
                            nimbusFound = true;
                            break;
                        }
                    }
                    // Fallback to cross-platform Look and Feel if Nimbus is not available.
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
                new CalculatorApp(); // Create and show the main application frame.
            }
        });
    }
}