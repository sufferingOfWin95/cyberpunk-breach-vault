package breachvault.ui;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.scene.Group;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * Растягивает холст фиксированного размера на любое окно с сохранением пропорций.
 * Масштаб = min(ширина окна / ширина холста, высота окна / высота холста),
 * поэтому холст всегда влезает целиком, а по краям остаются поля цвета фона.
 */
public class FitPane extends StackPane {

    public FitPane(Region content, double width, double height) {
        content.setMinSize(width, height);
        content.setPrefSize(width, height);
        content.setMaxSize(width, height);

        NumberBinding scale = Bindings.min(widthProperty().divide(width), heightProperty().divide(height));
        content.scaleXProperty().bind(scale);
        content.scaleYProperty().bind(scale);

        // Group учитывает масштаб в своих размерах — StackPane центрирует уже увеличенный холст
        getChildren().add(new Group(content));
        // Поля по краям — того же цвета, что фон холста (его задаёт CSS)
        backgroundProperty().bind(content.backgroundProperty());
    }
}
