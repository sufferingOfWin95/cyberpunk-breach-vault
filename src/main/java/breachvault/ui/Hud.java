package breachvault.ui;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Мелкие общие детали интерфейса: заголовки панелей и декоративный «технический» текст. */
final class Hud {

    /** Ширина левой колонки на всех экранах — чтобы колонки совпадали по вертикали. */
    static final double LEFT_COLUMN = 440;

    private Hud() {
    }

    /** Кнопка в стиле интерфейса: тонкая лаймовая рамка, при наведении — голубая. */
    static Button button(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("hud-button");
        button.setFocusTraversable(false); // без фокуса нет стандартной синей обводки
        return button;
    }

    /** Панель: заголовок сверху, под ним тело в рамке. Тело растягивается по высоте. */
    static VBox panel(HBox header, Node... content) {
        VBox body = new VBox(content);
        body.getStyleClass().add("panel-content");
        VBox.setVgrow(body, Priority.ALWAYS);
        return new VBox(header, body);
    }

    /**
     * Раскладка экранов «создать» и «открыть»:
     * заголовок, полоса с текущим шагом, две панели, внизу сообщение и кнопка BACK.
     */
    static VBox twoPanelScreen(String title, Label stepLabel, Region left, Region right,
                               Label message, Button back) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("panel-title");
        HBox titleRow = new HBox(titleLabel);
        titleRow.getStyleClass().add("row");

        stepLabel.getStyleClass().add("step-label");
        HBox bandRow = new HBox(stepLabel);
        bandRow.getStyleClass().addAll("row", "top-band");

        left.setPrefWidth(LEFT_COLUMN);
        left.setMinWidth(LEFT_COLUMN);
        HBox.setHgrow(right, Priority.ALWAYS);
        HBox panelsRow = new HBox(left, right);
        panelsRow.getStyleClass().addAll("row", "panels-row");

        message.getStyleClass().add("status");
        message.setWrapText(true);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        back.setMinWidth(Region.USE_PREF_SIZE); // длинное сообщение не должно сжимать кнопку до «...»
        HBox bottomRow = new HBox(message, spacer, back);
        bottomRow.getStyleClass().addAll("row", "bottom-row");

        VBox content = new VBox(titleRow, bandRow, panelsRow, bottomRow);
        content.getStyleClass().add("content");
        return content;
    }

    /** Текст сообщения внизу экрана. kind: "info", "ok" (голубой) или "error" (красный). */
    static void status(Label label, String text, String kind) {
        label.setText(text);
        label.getStyleClass().removeAll("info", "ok", "error");
        label.getStyleClass().add(kind);
    }

    /** Мелкая подпись вроде серийного номера. Чисто для атмосферы. */
    static Label tech(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("tech");
        return label;
    }

    /** Столбик из нескольких мелких подписей. */
    static VBox techBlock(String... lines) {
        VBox box = new VBox();
        box.getStyleClass().add("tech-block");
        for (String line : lines) {
            box.getChildren().add(tech(line));
        }
        return box;
    }

    /**
     * Заголовок панели: иконка, название и мелкий текст справа.
     * styleClass выбирает вид: "header-filled" (лаймовая плашка) или "header-outline" (рамка).
     */
    static HBox header(Node icon, String title, String styleClass, String... techLines) {
        Label label = new Label(title);
        label.getStyleClass().add("panel-header-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS); // распорка толкает тех. текст к правому краю

        HBox header = new HBox(icon, label, spacer, techBlock(techLines));
        header.getStyleClass().addAll("panel-header", styleClass);
        return header;
    }

    /** Пустой блок фиксированной ширины — чтобы выровнять колонки. */
    static Region gap(double width) {
        Region region = new Region();
        region.setMinWidth(width);
        region.setPrefWidth(width);
        return region;
    }
}
