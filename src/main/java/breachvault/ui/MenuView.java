package breachvault.ui;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Стартовый экран: создать сейф, открыть сейф, выйти. */
public class MenuView extends VBox {

    public MenuView(Runnable onCreate, Runnable onOpen, Runnable onExit) {
        getStyleClass().add("menu");

        Label title = new Label("BREACH VAULT");
        title.getStyleClass().add("menu-title");

        Button create = menuButton("CREATE VAULT", onCreate);
        Button open = menuButton("OPEN VAULT", onOpen);
        Button exit = menuButton("EXIT", onExit);

        getChildren().addAll(title, Hud.tech("LOCAL CRYPTOVAULT // AES-256-GCM // PBKDF2"), create, open, exit);
    }

    private static Button menuButton(String text, Runnable action) {
        Button button = Hud.button(text);
        button.getStyleClass().add("menu-button");
        button.setOnAction(e -> action.run());
        return button;
    }
}
