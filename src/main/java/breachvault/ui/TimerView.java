package breachvault.ui;

import java.util.Locale;
import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Панель BREACH TIME REMAINING: число в рамке и полоса, которая укорачивается.
 * На последних criticalSeconds всё краснеет (CSS-класс "critical"), а число мигает.
 * Когда время вышло — один раз вызывает onTimeout.
 */
public class TimerView extends VBox {

    private final double totalSeconds;
    private final double criticalSeconds;
    private final Runnable onTimeout;
    private final Label value = new Label();
    private final DoubleProperty fraction = new SimpleDoubleProperty(1); // доля оставшегося времени, 1 → 0
    private final AnimationTimer ticker;
    private long startNanos;
    private boolean started;

    public TimerView(double totalSeconds, double criticalSeconds, Runnable onTimeout) {
        this.totalSeconds = totalSeconds;
        this.criticalSeconds = criticalSeconds;
        this.onTimeout = onTimeout;
        getStyleClass().add("timer");

        Label title = new Label("BREACH TIME REMAINING");
        title.getStyleClass().add("timer-title");
        value.getStyleClass().add("timer-value");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(title, spacer, value);
        top.getStyleClass().add("timer-top");

        // Полоса: StackPane растягивает fill на всю ширину, но не больше maxWidth.
        // maxWidth привязан к доле времени — полоса укорачивается сама.
        Region fill = new Region();
        fill.getStyleClass().add("progress-fill");
        StackPane track = new StackPane(fill);
        track.getStyleClass().add("progress-track");
        track.setAlignment(Pos.CENTER_LEFT);
        fill.maxWidthProperty().bind(track.widthProperty().multiply(fraction));

        getChildren().addAll(top, track);
        show(totalSeconds);

        // AnimationTimer вызывает handle() на каждом кадре (~60 раз в секунду), в потоке JavaFX
        ticker = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double elapsed = (now - startNanos) / 1e9;
                double left = Math.max(0, totalSeconds - elapsed);
                show(left);
                if (left > 0 && left <= criticalSeconds) {
                    // Мигание: прозрачность гуляет по синусоиде примерно 2 раза в секунду
                    value.setOpacity(0.55 + 0.45 * Math.abs(Math.cos(elapsed * Math.PI * 2)));
                }
                if (left == 0) {
                    TimerView.this.stop();
                    onTimeout.run();
                }
            }
        };
    }

    public void start() {
        startNanos = System.nanoTime();
        started = true;
        ticker.start();
    }

    public void stop() {
        ticker.stop();
        value.setOpacity(1);
    }

    public boolean isStarted() {
        return started;
    }

    private void show(double seconds) {
        value.setText(String.format(Locale.US, "%.2f", seconds)); // Locale.US — чтобы была точка, а не запятая
        fraction.set(seconds / totalSeconds);
        boolean critical = seconds <= criticalSeconds;
        if (critical && !getStyleClass().contains("critical")) {
            getStyleClass().add("critical");
        }
    }
}
