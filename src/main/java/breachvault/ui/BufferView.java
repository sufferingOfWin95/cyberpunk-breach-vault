package breachvault.ui;

import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * Рамка с пунктирными слотами буфера.
 * Собранные коды — это и есть секрет, поэтому их не показываем: слот просто закрашивается.
 */
public class BufferView extends HBox {

    private final Label[] slots;

    public BufferView(int size) {
        getStyleClass().add("buffer");
        setMaxHeight(USE_PREF_SIZE); // не растягиваться по высоте соседей
        slots = new Label[size];
        for (int i = 0; i < size; i++) {
            slots[i] = new Label();
            slots[i].getStyleClass().add("slot");
            getChildren().add(slots[i]);
        }
    }

    public void update(List<String> buffer) {
        for (int i = 0; i < slots.length; i++) {
            slots[i].getStyleClass().remove("masked");
            if (i < buffer.size()) {
                slots[i].getStyleClass().add("masked");
            }
        }
    }
}
