package breachvault.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Панель SEQUENCE REQUIRED TO UPLOAD: что откроет взлом и сколько кодов нужно собрать.
 * Сами коды не показываем — это секрет. Только столько пустых ячеек, сколько их в секрете.
 */
public class SequenceView extends VBox {

    public SequenceView(int length, String name, String description) {
        getStyleClass().add("sequence-panel");

        HBox header = Hud.header(Icons.sequence(), "SEQUENCE REQUIRED TO UPLOAD", "header-outline",
                "MODEL LINE   V.0.5", "NODE  //  LOCAL");

        Region marker = new Region(); // тёмная вертикальная полоска слева от кодов
        marker.getStyleClass().add("seq-marker");

        HBox codes = new HBox();
        codes.getStyleClass().add("seq-codes");
        for (int i = 0; i < length; i++) {
            Label hidden = new Label("??");
            hidden.getStyleClass().add("seq-code");
            codes.getChildren().add(hidden);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox icon = new VBox(Icons.upload(), Hud.tech("VLT_01A"));
        icon.getStyleClass().add("seq-icon");

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("seq-name");
        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("seq-desc");
        VBox text = new VBox(nameLabel, descLabel);
        text.getStyleClass().add("seq-text");

        HBox row = new HBox(marker, codes, spacer, icon, text);
        row.getStyleClass().add("seq-row");

        VBox body = new VBox(row);
        body.getStyleClass().add("panel-body");

        HBox footer = new HBox(
                Hud.techBlock("CUSTOM DAEMON ON VAULT ACCESS, BASED ON LOCAL ANALYSIS.",
                        "DOCUMENT // 7F3A-0C12-A9D4-0042-AA17", "TYPE: CRYPTOVAULT"),
                spacerGrow(),
                Hud.techBlock("SEQ 01 / 01"));
        footer.getStyleClass().add("panel-footer");

        getChildren().addAll(header, body, footer);
    }

    private static Region spacerGrow() {
        Region region = new Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        return region;
    }
}
