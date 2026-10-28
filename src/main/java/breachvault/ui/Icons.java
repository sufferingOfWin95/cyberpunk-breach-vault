package breachvault.ui;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;

/**
 * Иконки из простых фигур JavaFX. Цвет задаёт CSS через классы
 * icon-shape (залитые) и icon-stroke (контурные), поэтому одна и та же иконка
 * может быть лаймовой на тёмном фоне и тёмной на лаймовой плашке.
 */
final class Icons {

    private Icons() {
    }

    /** Глобус в углу экрана: круг, меридиан, параллели. */
    static Node globe() {
        return group("icon-stroke",
                new Circle(16, 16, 15),
                new Ellipse(16, 16, 6, 15),
                new Line(1, 16, 31, 16),
                new Line(4, 9, 28, 9),
                new Line(4, 23, 28, 23),
                new Line(16, 1, 16, 31));
    }

    /** Значок матрицы: буква «N» из полос и две засечки справа. */
    static Node matrix() {
        SVGPath letter = new SVGPath();
        letter.setContent("M0 2 H4 L12 13 V2 H16 V18 H12 L4 7 V18 H0 Z");
        SVGPath marks = new SVGPath();
        marks.setContent("M19 2 H23 V6 H19 Z M19 9 H23 V11 H19 Z M19 14 H23 V18 H19 Z");
        return group("icon-shape", letter, marks);
    }

    /** Значок последовательности: полоска-«ярлык» и две точки. */
    static Node sequence() {
        return group("icon-shape",
                new Rectangle(0, 0, 20, 3),
                new Circle(5, 11, 4.5),
                new Circle(15, 11, 4.5));
    }

    /** Значок загрузки: столбик рисок над ромбом, в ромбе вырезана стрелка вниз. */
    static Node upload() {
        SVGPath ticks = new SVGPath();
        ticks.setContent("M14 0 H18 V3 H14 Z M13 5 H19 V8 H13 Z M12 10 H20 V13 H12 Z");
        SVGPath diamond = new SVGPath();
        // Внешний контур — ромб, внутренний — стрелка. EVEN_ODD делает внутренний контур дыркой.
        diamond.setContent("M16 15 L32 31 L16 47 L0 31 Z M13 22 H19 V30 H24 L16 39 L8 30 H13 Z");
        diamond.setFillRule(FillRule.EVEN_ODD);
        return group("icon-shape", ticks, diamond);
    }

    private static Group group(String styleClass, Shape... shapes) {
        for (Shape shape : shapes) {
            shape.getStyleClass().add(styleClass);
        }
        return new Group(shapes);
    }
}
