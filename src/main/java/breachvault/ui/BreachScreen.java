package breachvault.ui;

import breachvault.game.BreachGame;
import java.util.List;
import java.util.function.Consumer;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Экран взлома: таймер, буфер, матрица и последовательность.
 * Сам не решает, верно ли собрано, — отдаёт результат наружу через колбэки:
 *   onComplete — буфер заполнен или ходить некуда (передаём собранное);
 *   onTimeout  — время вышло;
 *   onAbort    — игрок нажал ABORT.
 * Срабатывает ровно один колбэк.
 */
public class BreachScreen extends VBox {

    private static final double BREACH_SECONDS = 40;
    private static final double CRITICAL_SECONDS = 5;

    private final BreachGame game;
    private final Consumer<List<String>> onComplete;
    private final BufferView bufferView;
    private final TimerView timerView;
    private final GridView gridView;
    private boolean finished;

    public BreachScreen(BreachGame game, Consumer<List<String>> onComplete, Runnable onTimeout, Runnable onAbort) {
        this.game = game;
        this.onComplete = onComplete;
        getStyleClass().add("content");

        bufferView = new BufferView(game.getBufferSize());
        timerView = new TimerView(BREACH_SECONDS, CRITICAL_SECONDS, () -> finish(onTimeout));
        timerView.setPrefWidth(Hud.LEFT_COLUMN);
        gridView = new GridView(game, this::onMove);

        // Ряд 1: подпись BUFFER над правой колонкой
        Label bufferTitle = new Label("BUFFER");
        bufferTitle.getStyleClass().add("panel-title");
        HBox titleRow = new HBox(Hud.gap(Hud.LEFT_COLUMN), bufferTitle);
        titleRow.getStyleClass().add("row");

        // Ряд 2: полупрозрачная полоса на всю ширину окна — таймер и буфер
        HBox bandRow = new HBox(timerView, bufferView);
        bandRow.getStyleClass().addAll("row", "top-band");

        // Ряд 3: матрица и последовательность
        SequenceView sequenceView = new SequenceView(game.getBufferSize(),
                "VAULT_ACCESS_V1", "Decrypts the vault and restores the protected folder");
        HBox.setHgrow(sequenceView, Priority.ALWAYS);
        HBox panelsRow = new HBox(buildMatrixPanel(), sequenceView);
        panelsRow.getStyleClass().addAll("row", "panels-row");
        panelsRow.setFillHeight(false); // панели разной высоты, не растягивать

        // Ряд 4: кнопка прервать взлом
        Button abort = Hud.button("ABORT");
        abort.setOnAction(e -> finish(onAbort));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottomRow = new HBox(spacer, abort);
        bottomRow.getStyleClass().addAll("row", "bottom-row");

        getChildren().addAll(titleRow, bandRow, panelsRow, bottomRow);
        timerView.start(); // время идёт сразу, как только матрица на экране — думать тоже надо быстро
    }

    /** Вызывается после каждого удачного хода. */
    private void onMove() {
        bufferView.update(game.getBuffer());
        if (game.isBufferFull() || !game.hasMoves()) {
            List<String> buffer = List.copyOf(game.getBuffer());
            finish(() -> onComplete.accept(buffer));
        }
    }

    /**
     * Завершает взлом один раз. Последний ход и конец времени могут совпасть
     * почти в один кадр — флаг finished не даёт сработать двум колбэкам.
     */
    private void finish(Runnable then) {
        if (finished) {
            return;
        }
        finished = true;
        timerView.stop();
        gridView.setDisable(true);
        then.run();
    }

    private VBox buildMatrixPanel() {
        HBox header = Hud.header(Icons.matrix(), "CODE MATRIX", "header-filled",
                "ACCESS POINT  0x1F", "SUBNET  //  VAULT");

        HBox footer = new HBox(
                Hud.techBlock("PROTOCOL", "4S2S-A44"),
                Hud.techBlock("0.0418   2 . 4 4 8 0", "0.0391   1 . 0 9 2 4", "0.0377   0 . 4 4 1 0"),
                Hud.techBlock("0032  1.07743  0 . 4 4 3", "0035  1.07743  0 . 0 0 0"),
                Hud.techBlock("0034  LAT", "0046"));
        footer.getStyleClass().add("panel-footer");

        VBox panel = new VBox(header, gridView, footer);
        panel.getStyleClass().add("matrix-panel");
        panel.setPrefWidth(Hud.LEFT_COLUMN);
        panel.setMaxWidth(Hud.LEFT_COLUMN);
        return panel;
    }
}
