package breachvault.ui;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

/**
 * Фоновый слой: тонкие рамки по краям, яркие риски, глобус и мелкий текст в углах.
 * В обычном Pane детей ставим по точным координатам — окно фиксированного размера.
 */
class Decorations extends Pane {

    Decorations(double w, double h) {
        getStyleClass().add("decorations");
        setMouseTransparent(true); // клики проходят насквозь к панелям

        // Двойные вертикальные рамки: яркая и бледная
        line(70, 30, 70, h - 20, "deco-line");
        line(80, 44, 80, h - 20, "deco-line-faint");
        line(w - 70, 30, w - 70, h - 20, "deco-line");
        line(w - 80, 44, w - 80, h - 20, "deco-line-faint");

        // Верх: рамка и точечный пунктир над ней
        line(70, 30, w - 70, 30, "deco-line");
        line(80, 44, w - 80, 44, "deco-line-faint");
        line(330, 12, 900, 12, "deco-dots");

        // Короткие яркие риски на рамках
        line(52, 330, 52, 380, "deco-tick");
        line(w - 52, 90, w - 52, 150, "deco-tick");
        line(w - 62, h - 170, w - 62, h - 130, "deco-tick");

        place(Icons.globe(), 22, 10);

        // Правый верхний угол: «серийный номер» и три красных индикатора
        place(Hud.tech("VLT-8Z07-BRCH-0X1F-E327"), w - 290, 8);
        for (int i = 0; i < 3; i++) {
            Rectangle led = new Rectangle(w - 150 + i * 14, 10, 10, 6);
            led.getStyleClass().add("deco-led");
            getChildren().add(led);
        }

        // Нижние углы
        place(Hud.techBlock("SYS.VAULT // LOCAL NODE", "ICE STATUS: ACTIVE"), 100, h - 52);
        place(Hud.techBlock("ESC // EXIT FULLSCREEN", "BUILD 0.5 // JAVAFX"), w - 220, h - 52);
    }

    private void line(double x1, double y1, double x2, double y2, String styleClass) {
        Line line = new Line(x1, y1, x2, y2);
        line.getStyleClass().add(styleClass);
        getChildren().add(line);
    }

    private void place(Node node, double x, double y) {
        node.relocate(x, y);
        getChildren().add(node);
    }
}
