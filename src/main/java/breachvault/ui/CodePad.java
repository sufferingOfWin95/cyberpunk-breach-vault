package breachvault.ui;

import breachvault.game.Grid;
import breachvault.vault.VaultService;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Панель ввода секрета: кнопки всех кодов (два ряда по 6), без правила строка/столбец.
 * Набранные коды НЕ показываются — слот просто закрашивается, как точки в поле пароля.
 */
public class CodePad extends VBox {

    private static final int KEYS_PER_ROW = 6;

    private final List<String> entered = new ArrayList<>();
    private final Label[] slots = new Label[VaultService.MAX_SECRET];
    private final List<Button> keys = new ArrayList<>();
    private final Button erase = Hud.button("ERASE");
    private final Button submit = Hud.button("CONFIRM");
    private Consumer<List<String>> onSubmit = secret -> { };

    public CodePad() {
        getStyleClass().add("code-pad");

        HBox slotRow = new HBox();
        slotRow.getStyleClass().add("pad-row");
        for (int i = 0; i < slots.length; i++) {
            slots[i] = new Label();
            slots[i].getStyleClass().addAll("slot", "pad-slot");
            if (i >= VaultService.MIN_SECRET) {
                slots[i].getStyleClass().add("optional"); // 5-й и 6-й коды необязательны
            }
            slotRow.getChildren().add(slots[i]);
        }
        slotRow.getChildren().add(Hud.tech(VaultService.MIN_SECRET + "–" + VaultService.MAX_SECRET + " CODES"));

        // Кнопки кодов рядами по KEYS_PER_ROW
        VBox keyRows = new VBox();
        keyRows.getStyleClass().add("pad-keys");
        HBox keyRow = null;
        for (String code : Grid.CODES) {
            if (keys.size() % KEYS_PER_ROW == 0) {
                keyRow = new HBox();
                keyRow.getStyleClass().add("pad-row");
                keyRows.getChildren().add(keyRow);
            }
            Button key = new Button(code);
            key.getStyleClass().add("pad-key");
            key.setFocusTraversable(false);
            key.setOnAction(e -> add(code));
            keys.add(key);
            keyRow.getChildren().add(key);
        }

        erase.setOnAction(e -> {
            entered.remove(entered.size() - 1);
            refresh();
        });
        submit.setOnAction(e -> {
            List<String> secret = List.copyOf(entered);
            clear(); // сразу забываем набранное — секрет живёт только у того, кто его получил
            onSubmit.accept(secret);
        });
        HBox controlRow = new HBox(erase, submit);
        controlRow.getStyleClass().add("pad-row");

        getChildren().addAll(slotRow, keyRows, controlRow);
        refresh();
    }

    public void setOnSubmit(Consumer<List<String>> onSubmit) {
        this.onSubmit = onSubmit;
    }

    public void clear() {
        entered.clear();
        refresh();
    }

    private void add(String code) {
        if (entered.size() < VaultService.MAX_SECRET) {
            entered.add(code);
            refresh();
        }
    }

    private void refresh() {
        for (int i = 0; i < slots.length; i++) {
            slots[i].getStyleClass().remove("masked");
            if (i < entered.size()) {
                slots[i].getStyleClass().add("masked");
            }
        }
        boolean full = entered.size() == VaultService.MAX_SECRET;
        keys.forEach(key -> key.setDisable(full));
        erase.setDisable(entered.isEmpty());
        submit.setDisable(entered.size() < VaultService.MIN_SECRET);
    }
}
