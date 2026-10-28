package breachvault.ui;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

/**
 * Общий каркас для всех экранов: шрифт, стили, фоновый декор.
 * Экраны (меню, создание, открытие) подставляются внутрь через setContent().
 * Размер всегда 1400×700 — на весь монитор его растягивает FitPane.
 */
public class AppShell extends StackPane {

    public static final double WIDTH = 1400;
    public static final double HEIGHT = 700;

    private final StackPane slot = new StackPane();

    public AppShell(boolean debug) {
        Fonts.load();
        getStylesheets().add(AppShell.class.getResource("/breachvault/neon.css").toExternalForm());
        getStyleClass().add("screen");

        getChildren().addAll(new Decorations(WIDTH, HEIGHT), slot);

        // Звуки всех кнопок — в одном месте. Фильтр видит событие раньше самой кнопки,
        // поэтому экраны про звук ничего не знают. Выключенные кнопки событий не получают — и не звучат.
        Sounds.init();
        addEventFilter(MouseEvent.MOUSE_ENTERED_TARGET, e -> {
            if (e.getTarget() instanceof ButtonBase button && !button.isDisabled()) {
                Sounds.play(Sounds.Cue.HOVER);
            }
        });
        addEventFilter(ActionEvent.ACTION, e -> {
            if (e.getTarget() instanceof ButtonBase button) {
                // Клетка матрицы звучит по-своему, остальные кнопки — обычным щелчком
                Sounds.play(button.getStyleClass().contains("cell") ? Sounds.Cue.PICK : Sounds.Cue.CLICK);
            }
        });

        if (debug) {
            // Маленькая красная метка в углу, чтобы отладочный режим нельзя было не заметить
            Label badge = new Label("DEBUG");
            badge.getStyleClass().add("debug-badge");
            badge.setMouseTransparent(true);
            StackPane.setAlignment(badge, Pos.TOP_LEFT);
            StackPane.setMargin(badge, new Insets(16, 0, 0, 64));
            getChildren().add(badge);
        }
    }

    public void setContent(Node content) {
        slot.getChildren().setAll(content);
    }
}
