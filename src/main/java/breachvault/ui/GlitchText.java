package breachvault.ui;

import java.util.Random;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Текст с эффектом «сбоя». Внутри три одинаковые надписи друг над другом:
 *  - main  — основная, слегка дрожит;
 *  - cyan и red — цветные копии, разъезжаются в стороны (расслоение цвета, как на битом мониторе);
 *    от голубой иногда отрезаем полосу и сдвигаем — будто сбилась строка развёртки.
 * В покое копии невидимы, и текст выглядит обычным.
 */
final class GlitchText extends StackPane {

    private static final double FRAME_MS = 45; // как часто «дёргаем» картинку

    private final Label main;
    private final Label cyan;
    private final Label red;
    private final Random random = new Random();

    GlitchText(String text) {
        getStyleClass().add("glitch-text");
        cyan = layer(text, "glitch-cyan");
        red = layer(text, "glitch-red");
        main = layer(text, "glitch-main");
        getChildren().addAll(cyan, red, main); // main последним — он сверху
        settle();
    }

    /** Анимация сбоя заданной длительности. Каждый кадр — новое случайное искажение, в конце — покой. */
    Animation glitch(Duration duration) {
        Timeline timeline = new Timeline();
        int frames = (int) (duration.toMillis() / FRAME_MS);
        for (int i = 0; i < frames; i++) {
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(i * FRAME_MS), e -> jitter()));
        }
        timeline.getKeyFrames().add(new KeyFrame(duration, e -> settle()));
        return timeline;
    }

    private void jitter() {
        main.setTranslateX(random.nextGaussian() * 3);
        cyan.setOpacity(0.85);
        red.setOpacity(0.85);
        cyan.setTranslateX(-3 - random.nextDouble() * 8);
        red.setTranslateX(3 + random.nextDouble() * 8);
        red.setTranslateY(random.nextGaussian() * 2);

        if (random.nextDouble() < 0.45) {
            // Вырезаем горизонтальную полосу из голубой копии и сильно сдвигаем её
            double h = cyan.getHeight();
            double bandY = random.nextDouble() * h * 0.75;
            double bandH = h * (0.12 + random.nextDouble() * 0.25);
            cyan.setClip(new Rectangle(-40, bandY, cyan.getWidth() + 80, bandH));
            cyan.setTranslateX((random.nextBoolean() ? 1 : -1) * (12 + random.nextDouble() * 22));
        } else {
            cyan.setClip(null);
        }
    }

    private void settle() {
        for (Label label : new Label[] {main, cyan, red}) {
            label.setTranslateX(0);
            label.setTranslateY(0);
            label.setClip(null);
        }
        cyan.setOpacity(0);
        red.setOpacity(0);
    }

    private static Label layer(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().addAll("glitch-layer", styleClass);
        return label;
    }
}
