package breachvault.ui;

import breachvault.game.Axis;
import breachvault.game.BreachGame;
import breachvault.game.Cell;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

/**
 * Тело панели CODE MATRIX: сетка кнопок и под ней сплошная полоса активной линии.
 * Сама ничего не решает: клик передаёт в BreachGame, потом перерисовывается по состоянию игры.
 *
 * Наследуемся от Region и сами раскладываем детей в layoutChildren(),
 * потому что полосе нужны координаты кнопок, а обычные панели так не умеют.
 */
public class GridView extends Region {

    private final BreachGame game;
    private final Button[][] buttons;
    private final Runnable onMove;
    private final GridPane cells = new GridPane();
    private final Region band = new Region();

    public GridView(BreachGame game, Runnable onMove) {
        this.game = game;
        this.onMove = onMove;
        int size = game.getGrid().size();
        this.buttons = new Button[size][size];

        getStyleClass().add("matrix-body");
        cells.getStyleClass().add("matrix-cells");
        band.getStyleClass().add("active-band");

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                Cell cell = new Cell(r, c);
                Button button = new Button(game.getGrid().get(cell));
                button.getStyleClass().add("cell");
                button.setFocusTraversable(false); // без фокуса нет стандартной синей обводки
                button.setOnAction(e -> handleClick(cell));
                buttons[r][c] = button;
                cells.add(button, c, r); // у GridPane порядок (столбец, строка)
            }
        }
        // Полоса добавлена первой — значит, рисуется под кнопками
        getChildren().addAll(band, cells);
        refresh();
    }

    private void handleClick(Cell cell) {
        if (game.pick(cell)) {
            refresh();
            onMove.run();
        }
    }

    /** Расставляет CSS-классы каждой кнопке по текущему состоянию игры. */
    public void refresh() {
        for (int r = 0; r < buttons.length; r++) {
            for (int c = 0; c < buttons.length; c++) {
                Cell cell = new Cell(r, c);
                Button button = buttons[r][c];
                button.getStyleClass().removeAll("active-line", "used");
                if (game.isUsed(cell)) {
                    button.getStyleClass().add("used");
                    button.setText("[ ]");
                } else if (game.isOnActiveLine(cell) && !game.isBufferFull()) {
                    button.getStyleClass().add("active-line");
                }
                button.setDisable(!game.canPick(cell));
            }
        }
        requestLayout(); // активная линия сменилась — полосу надо передвинуть
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets in = getInsets();
        return in.getLeft() + cells.prefWidth(-1) + in.getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets in = getInsets();
        return in.getTop() + cells.prefHeight(-1) + in.getBottom();
    }

    /** Сетку ставим по центру, полосу — по активной строке или столбцу во всю ширину/высоту. */
    @Override
    protected void layoutChildren() {
        Insets in = getInsets();
        double innerWidth = getWidth() - in.getLeft() - in.getRight();
        double innerHeight = getHeight() - in.getTop() - in.getBottom();

        double cellsWidth = cells.prefWidth(-1);
        double cellsHeight = cells.prefHeight(-1);
        double x = in.getLeft() + (innerWidth - cellsWidth) / 2;
        double y = in.getTop() + (innerHeight - cellsHeight) / 2;
        cells.resizeRelocate(x, y, cellsWidth, cellsHeight);
        cells.layout(); // сразу расставить кнопки, чтобы их координаты были актуальны

        band.setVisible(!game.isBufferFull());
        int index = game.getActiveIndex();
        if (game.getAxis() == Axis.ROW) {
            Button first = buttons[index][0];
            band.resizeRelocate(in.getLeft(), y + first.getLayoutY(), innerWidth, first.getHeight());
        } else {
            Button first = buttons[0][index];
            band.resizeRelocate(x + first.getLayoutX(), in.getTop(), first.getWidth(), innerHeight);
        }
    }
}
