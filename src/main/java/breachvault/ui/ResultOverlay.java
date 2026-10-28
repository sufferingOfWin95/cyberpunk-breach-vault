package breachvault.ui;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Полноэкранная надпись ACCESS GRANTED / ACCESS DENIED со сбоем.
 * Появляется поверх экрана, держится пару секунд, исчезает и вызывает after.
 * Пока она на экране, клики под неё не проходят.
 */
final class ResultOverlay extends StackPane {

    private static final Duration FADE = Duration.millis(180);
    private static final Duration GLITCH = Duration.millis(750);
    private static final Duration HOLD = Duration.millis(1500);

    private final GlitchText title;

    private ResultOverlay(boolean granted, String detail) {
        getStyleClass().addAll("result-overlay", granted ? "granted" : "denied");
        setPickOnBounds(true); // ловим клики по всей площади, даже по прозрачным местам

        title = new GlitchText(granted ? "ACCESS GRANTED" : "ACCESS DENIED");
        Label detailLabel = new Label(detail);
        detailLabel.getStyleClass().add("result-detail");
        detailLabel.setWrapText(true);
        detailLabel.setMaxWidth(1100); // длинный путь переносится, а не тянется от края до края

        VBox band = new VBox(
                Hud.tech(granted ? "BREACH SUCCESSFUL // DAEMON UPLOADED" : "ICE COUNTERMEASURE // CONNECTION TERMINATED"),
                title,
                detailLabel);
        band.getStyleClass().add("result-band");
        band.setMaxHeight(USE_PREF_SIZE); // полоса по центру, а не на всю высоту
        getChildren().add(band);
    }

    /** Показать поверх host. После исчезновения надпись убирается и вызывается after. */
    static void show(StackPane host, boolean granted, String detail, Runnable after) {
        ResultOverlay overlay = new ResultOverlay(granted, detail);
        host.getChildren().add(overlay);
        Sounds.play(granted ? Sounds.Cue.GRANTED : Sounds.Cue.DENIED);

        FadeTransition fadeIn = new FadeTransition(FADE, overlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        FadeTransition fadeOut = new FadeTransition(FADE, overlay);
        fadeOut.setToValue(0);

        // Появление и сбой одновременно, потом пауза, потом исчезновение
        SequentialTransition sequence = new SequentialTransition(
                new ParallelTransition(fadeIn, overlay.title.glitch(GLITCH)),
                new PauseTransition(HOLD),
                fadeOut);
        sequence.setOnFinished(e -> {
            host.getChildren().remove(overlay);
            after.run();
        });
        sequence.play();
    }
}
