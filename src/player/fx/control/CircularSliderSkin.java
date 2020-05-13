package player.fx.control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.control.SkinBase;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.Circle;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.transform.Scale;
import javafx.stage.PopupWindow.AnchorLocation;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;

public class CircularSliderSkin extends SkinBase<CircularSlider> {
    private Shape foregroundMask; // invisible, but throws shadow
    private final DropShadow shadow;
    private final Circle centralClip;

    private final Group centralGroup;
    private final Scale centralScale;

    // Ticks
    private List<TimeTick> ticks = new ArrayList<>();
    private final Group tickGroup;
    private DoubleProperty currentTickOpacy;

    // Bar
    private double barRadius, barWidth; // effective values, depend on layout size
    private final Region styledBarBox;
    private final Path bar;
    private final Path oldBar;
    private FadeTransition oldBarFade;
    private final StackPane cssThumb;
    private final StackPane thumb;
    private double filledAngle = Double.NaN;
    private boolean onBar; // mouse pressed on bar or dragged from bar
    private final Duration barFadeToZeroDuration = new Duration(600);

    // Value tooltips
    private final Tooltip mouseTooltip, barTooltip;
    private Timeline barTooltipAnimation, mouseTooltipAnimation;
    private double mouseTooltipTargetOpacity, barTooltipTargetOpacity;


    public CircularSliderSkin(CircularSlider control) {
        super(control);

        shadow = new DropShadow();
        shadow.setColor(new Color(0, 0, 0, 0.6));

        centralGroup = new Group();
        centralGroup.setClip(centralClip = new Circle(1));
        centralGroup.getTransforms().add(centralScale = new Scale(1, 1));
        getChildren().add(centralGroup);

        // Bar
        bar = new Path();
        bar.setMouseTransparent(true);
        bar.setStrokeLineCap(StrokeLineCap.BUTT);

        oldBar = new Path();
        oldBar.setMouseTransparent(true);
        oldBar.setStrokeLineCap(StrokeLineCap.BUTT);
        oldBar.setOpacity(0);
        centralGroup.getChildren().addAll(oldBar, bar);

        tickGroup = new Group();
        tickGroup.setMouseTransparent(true);
        centralGroup.getChildren().add(tickGroup);

        styledBarBox = new Region();
        styledBarBox.getStyleClass().add("bar");
        styledBarBox.setVisible(false);
        styledBarBox.setManaged(false);
        getChildren().add(styledBarBox);

        // Thumb
        cssThumb = new StackPane() {
            @Override
            public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
                switch (attribute) {
                    case VALUE: return getSkinnable().getValue();
                    default: return super.queryAccessibleAttribute(attribute, parameters);
        } } };
        cssThumb.setVisible(false);
        cssThumb.setMouseTransparent(true);
        cssThumb.setManaged(false);
        cssThumb.getStyleClass().setAll("thumb");
        getChildren().add(cssThumb);

        thumb = new StackPane() {
            @Override
            public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
                switch (attribute) {
                    case VALUE: return getSkinnable().getValue();
                    default: return super.queryAccessibleAttribute(attribute, parameters);
                }
            }
        };
        thumb.setMouseTransparent(true);
        thumb.setOpacity(0);
        thumb.getStyleClass().setAll("thumb");
        thumb.setAccessibleRole(AccessibleRole.THUMB);
        getChildren().add(thumb);


        // Value tooltips
        mouseTooltip = new Tooltip();
        mouseTooltip.setOpacity(0);

        barTooltip = new Tooltip();
        barTooltip.setOpacity(0);
        barTooltip.textProperty().bindBidirectional(control.valueProperty(), new StringConverter<Number>() {

            @Override
            public String toString(Number object) {
                return getLabelFormatter().toString((double) object);
            }

            @Override
            public Number fromString(String string) {
                return getLabelFormatter().fromString(string);
            }
        });
        getSkinnable().valueProperty().addListener(e -> {
            if (barTooltip.isShowing()) positionTooltipAtAngle(barTooltip, angleFromValue(getSkinnable().getValue()));
        });


        // Register listeners

        control.maxProperty().addListener((p,o,n) -> {updateTicks(); rebuildBar();});
        control.minProperty().addListener((p,o,n) -> {updateTicks(); rebuildBar();});
        control.valueProperty().addListener(e -> rebuildBar());

        control.tickLengthProperty().addListener(e -> updateTicks());
        control.minorTickLengthProperty().addListener(e -> updateTicks());
        control.majorTickUnitProperty().addListener(e -> updateTicks());
        control.minorTickCountProperty().addListener(e -> updateTicks());

        control.minorTickVisibleProperty().addListener(e -> updateTickVisibility());
        control.tickMarkVisibleProperty().addListener(e -> updateTickVisibility());

        styledBarBox.backgroundProperty().addListener(e -> updateBarStyle());

        // Mouse events
        getSkinnable().setOnMouseEntered(e -> {
            fadeBarTooltip(1.0);
        });
        getSkinnable().setOnMousePressed(e -> {
            onBar = isOnBar(e.getX(), e.getY());
            if (!onBar) {
                Parent parent = getNode().getParent();
                parent.fireEvent(e.copyFor(parent, parent));
            }
        });
        getSkinnable().setOnMouseMoved(e -> updateMouseOver(e, false));
        getSkinnable().setOnMouseDragged(e -> updateMouseOver(e, true));
        getSkinnable().setOnMouseReleased(e -> {
            if(onBar) {
                getSkinnable().setValue(getValueAt(e.getX(), e.getY()));
                onBar = false;
            } else {
                Parent parent = getNode().getParent();
                parent.fireEvent(e.copyFor(parent, parent));
            }
        });
        getSkinnable().setOnMouseExited(e -> {
            thumb.setOpacity(0);
            fadeBarTooltip(0);
            fadeMouseTooltip(0, e);
        });
    }


    private void updateMouseOver(MouseEvent e, boolean isDragged) {
        double angle = getAngle(e.getX(), e.getY());
        double pos = getValueAt(e.getX(), e.getY());
//        onBar = (onBar && isDragged) || isOnBar(e.getX(), e.getY());

        if (isOnBar(e.getX(), e.getY()) || (isDragged && onBar)) {
            mouseTooltip.setText(getLabelAt(pos));
            fadeMouseTooltip(1, e);
            e.consume();
        }
        if (!onBar && !isOnBar(e.getX(), e.getY())) {
            fadeMouseTooltip(0, e);
            Parent parent = getNode().getParent();
            parent.fireEvent(e.copyFor(parent, parent));
        }

        fadeBarTooltip(1);

        // update thumb
        Point2D thumbLoc = getLocationFromAngle(angle, barRadius);
        thumb.setLayoutX(thumbLoc.getX() - thumb.getWidth()/2);
        thumb.setLayoutY(thumbLoc.getY() - thumb.getHeight()/2);
        thumb.setOpacity(onBar ? 1 : 0.4);
    }


    private void fadeBarTooltip(double targetOpacity) {
        if (targetOpacity == barTooltipTargetOpacity) return;
        barTooltipTargetOpacity = targetOpacity;
        long barTooltipFadeInLength = 300;
        fadeTooltip(barTooltipFadeInLength, targetOpacity, barTooltip, barTooltipAnimation);
    }

    private void fadeMouseTooltip(double targetOpacity, MouseEvent e) {
        positionTooltipAtAngle(mouseTooltip, getAngle(e.getX(), e.getY()));
        if (targetOpacity == mouseTooltipTargetOpacity) return;
        mouseTooltipTargetOpacity = targetOpacity;
        long mouseTooltipFadeInLength = 50;
        fadeTooltip(mouseTooltipFadeInLength, targetOpacity, mouseTooltip, mouseTooltipAnimation);
    }


    private Timeline fadeTooltip(long durationPerUnit, double targetOpacity, Tooltip tooltip, Timeline timeline) {
        if(timeline != null && timeline.getStatus() == Animation.Status.RUNNING) {
            timeline.stop();
        }
        double currentValue = tooltip.getOpacity();
        double distance = Math.abs(currentValue - targetOpacity);
        timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(tooltip.opacityProperty(), currentValue)),
                new KeyFrame(new Duration(durationPerUnit * distance), new KeyValue(tooltip.opacityProperty(), targetOpacity)));

        if(targetOpacity > 0) {
            if (!tooltip.isShowing()) tooltip.show(getSkinnable().getScene().getWindow());
        } else {
            timeline.setOnFinished(e -> {
                tooltip.hide();
            });
        }
        timeline.play();
        return timeline;
    }

    public double getAngle(double x, double y) {
        double dx = x - getSkinnable().getWidth()/2;
        double dy = y - getSkinnable().getHeight()/2;
        double angle = Math.atan2(dx, -dy);
        if(angle < 0) angle += 2*Math.PI;
        return angle;
    }

    private double getValueAt(double x, double y) {
        return getAngle(x,y) / (2*Math.PI) * (getSkinnable().getMax()-getSkinnable().getMin()) + getSkinnable().getMin();
    }

    private String getLabelAt(double pos) {
        return getLabelFormatter().toString(pos);
    }

    private StringConverter<Double> getLabelFormatter() {
        StringConverter<Double> formatter = getSkinnable().getLabelFormatter();
        if(formatter == null) formatter = new DoubleStringConverter();
        return formatter;
    }

    private Point2D getLocationFromAngle(double angle, double rad) {
        return new Point2D(rad*Math.sin(angle)+getSkinnable().getWidth()/2, -rad*Math.cos(angle)+getSkinnable().getHeight()/2);
    }

    private double angleFromValue(double value) {
        double angle = 2*Math.PI * (value-getSkinnable().getMin()) / (getSkinnable().getMax() - getSkinnable().getMin());
        if(Double.isNaN(angle)) angle = 0;
        return angle;
    }

    private void positionTooltipAtAngle(Tooltip tooltip, double angle) {
        double rad = barRadius + barWidth/2;
        double relPos = angle / 2 / Math.PI;
        positionTooltipAt(tooltip, getLocationFromAngle(angle, rad), relPos < 0.5, relPos > 0.25 && relPos < 0.75);
    }

    private void positionTooltipAt(Tooltip tooltip, Point2D point, boolean left, boolean top) {
        CircularSlider control = getSkinnable();
        point = control.localToScreen(point);
        tooltip.setAnchorLocation(left ?
            (top ? AnchorLocation.WINDOW_TOP_LEFT : AnchorLocation.WINDOW_BOTTOM_LEFT) :
            (top ? AnchorLocation.WINDOW_TOP_RIGHT : AnchorLocation.WINDOW_BOTTOM_RIGHT));
        tooltip.setAnchorX(point.getX());
        tooltip.setAnchorY(point.getY());
    }

    private boolean isOnBar(double x, double y) {
        double dx = x - getSkinnable().getWidth()/2;
        double dy = y - getSkinnable().getHeight()/2;
        double rad = Math.sqrt(dx*dx+dy*dy);
        return rad >= barRadius-barWidth/2 && rad <= barRadius+barWidth/2;
    }


    private void updateBarStyle() {
        Background bg = styledBarBox.getBackground();
        if(bg != null) {
            BackgroundFill fill = bg.getFills().get(bg.getFills().size()-1);
            bar.setStroke(fill.getFill());
            oldBar.setStroke(fill.getFill());
        }
    }

    private void updateTickVisibility() {
        boolean mjv = getSkinnable().isShowTickMarks();
        boolean mnv = getSkinnable().isMinorTickVisible();
        for(TimeTick tick : ticks) {
            tick.setVisible(mjv && (tick.isMajor() ? true : mnv));
        }
    }

    private Object currentTickDependencies = Collections.emptyList();
    private double currentMin, currentMax;

    private void updateTicks() {
        double min = getSkinnable().getMin();
        double max = getSkinnable().getMax();
        double majorUnit = getSkinnable().getMajorTickUnit();
        double minorUnit = getSkinnable().getMajorTickUnit() / getSkinnable().getMinorTickCount();

        Object newTickDependencies = Arrays.asList(barWidth, barRadius, min, max, majorUnit, minorUnit, getSkinnable().getTickLength());
        if (newTickDependencies.equals(currentTickDependencies)) return;
        currentTickDependencies = newTickDependencies;
        boolean animate = Math.abs((currentMin - min) / (currentMax - currentMin + 1e-5)) > 1e-2
                || Math.abs((currentMax - max) / (currentMax - currentMin + 1e-5)) > 1e-2;
        currentMax = max; currentMin = min;


    	// Fade old
    	if(currentTickOpacy != null) {
    	    if (animate) {
                Timeline fadeOut = new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(currentTickOpacy, 1)),
                        new KeyFrame(new Duration(300), new KeyValue(currentTickOpacy, 0)));
                fadeOut.play();
                fadeOut.setOnFinished(e -> {
                    removeOldTicks();
                });
            } else removeOldTicks();
    	}

    	// Create new
        currentTickOpacy = new SimpleDoubleProperty(animate ? 0.0 : 1.0);
        List<TimeTick> newTicks = new ArrayList<>();

        for(double pos = Math.ceil(min / minorUnit) * minorUnit; pos < max; pos += minorUnit) {
            boolean major = isInt(pos/majorUnit, 1e-6);
            double preTransformLength = (major ? getSkinnable().getTickLength() : getSkinnable().getMinorTickLength());
            double tickScale = 0.0005;
            TimeTick tick = new TimeTick(pos,
                    major,
                    preTransformLength * preTransformLength * tickScale,
                    major ? "axis-tick-mark" : "axis-minor-tick-mark");
            tick.fitBounds(barWidth/barRadius);
            newTicks.add(tick);
            tick.updatePosition(min, max);
            tick.opacityProperty().bind(currentTickOpacy);
            tickGroup.getChildren().add(tick);
        }

        ticks = newTicks;

        updateTickVisibility();

        if (animate) {
            Timeline fadeIn = new Timeline(
                    new KeyFrame(new Duration(200), new KeyValue(currentTickOpacy, 0)),
                    new KeyFrame(new Duration(1000), new KeyValue(currentTickOpacy, 1)));
            fadeIn.play();
        }

        foregroundMask.toFront();
    }

    private void removeOldTicks() {
        tickGroup.getChildren().removeAll(tickGroup.getChildren().stream().filter(tick -> !ticks.contains(tick)).collect(Collectors.toList()));
    }

    private void rebuildBar() {
        bar.setStrokeWidth(barWidth / barRadius);
        oldBar.setStrokeWidth(barWidth / barRadius);

        double value = getSkinnable().getValue();
        double oldAngle = filledAngle;
        filledAngle = 2*Math.PI * (value - getSkinnable().getMin()) / (getSkinnable().getMax()-getSkinnable().getMin());

        buildBar(bar, filledAngle);

        if(oldAngle > 0 && filledAngle == 0) {
        	buildBar(oldBar, oldAngle);
            oldBar.setOpacity(1);
            if(oldBarFade != null && oldBarFade.getStatus() == Animation.Status.RUNNING) {
                oldBarFade.stop();
            }
            oldBarFade = new FadeTransition(barFadeToZeroDuration, oldBar);
            oldBarFade.setToValue(0);
            oldBarFade.play();
        }

        // Thumb
        thumb.resize(barWidth, barWidth);
    }

    private static void buildBar(Path bar, double filledAngle) {
    	bar.getElements().clear();
        bar.getElements().add(new MoveTo(0, -1));
        if(filledAngle <= Math.PI) {
            bar.getElements().add(new ArcTo(1, 1, 0, Math.sin(filledAngle), - Math.cos(filledAngle), filledAngle > Math.PI, true));
        } else if(filledAngle <= 2*Math.PI){
            bar.getElements().add(new ArcTo(1, 1, 0, 0, 1, true, true));
            bar.getElements().add(new ArcTo(1, 1, 0, Math.sin(filledAngle), - Math.cos(filledAngle), false, true));
        }
    }

    private static boolean isInt(double d, double tolerance) {
        return Math.abs(d%1) < tolerance;
    }


    /**
     * Called when the size of the control changes.
     * @param contentWidth
     * @param contentHeight
     */
    private void recalculateShapes(double contentWidth, double contentHeight) {
        if(foregroundMask != null) {
            getChildren().remove(foregroundMask);
        }

        Rectangle fill = new Rectangle(contentWidth*2, contentHeight*2);
        fill.setX(-contentWidth);
        fill.setY(-contentHeight);

        foregroundMask = Shape.subtract(fill, arcMask());
        foregroundMask.setMouseTransparent(true);
        foregroundMask.setFill(Color.BLACK); // is clipped away
        foregroundMask.setEffect(shadow);
        foregroundMask.setClip(arcMask()); // do not draw shadow outside
        getChildren().add(foregroundMask);

        // Bar
        rebuildBar();

        // Ticks
        updateTicks();

        // Thumb
        if(cssThumb.getBackground() != null) {
        	BackgroundFill[] fills = cssThumb.getBackground().getFills().stream().map(f -> {
        		return new BackgroundFill(f.getFill(), new CornerRadii(barWidth), f.getInsets());
        	}).toArray(s -> new BackgroundFill[s]);

        	thumb.setBackground(new Background(fills));
        }
    }

    private Shape arcMask() {
        return Shape.subtract(new Circle(barRadius+barWidth/2), new Circle(barRadius-barWidth/2));
    }

    @Override
    protected double computeMinWidth(double height,
            double topInset, double rightInset,
            double bottomInset, double leftInset) {
        return height;
    }

    @Override
    protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset,
            double leftInset) {
        return width;
    }

    @Override
    protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset,
            double leftInset) {
        return Math.max(width, getSkinnable().getPrefHeight());
    }

    @Override
    protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset,
            double leftInset) {
        return Math.max(height, getSkinnable().getPrefWidth());
    }

    private Rectangle2D currentShape = new Rectangle2D(0, 0, 0, 0);

    @Override
    protected void layoutChildren(double contentX, double contentY, double contentWidth, double contentHeight) {
        Rectangle2D newShape = new Rectangle2D(contentX, contentY, contentWidth, contentHeight);
        if (newShape.equals(currentShape)) return;
        currentShape = newShape;

        barRadius = Math.max(0, Math.min(contentWidth, contentHeight) * getSkinnable().getDiameter() / 2);
        barWidth = Math.min(contentWidth, contentHeight) * getSkinnable().getThickness();
        centralClip.setRadius(1+barWidth/2/barRadius);

        recalculateShapes(contentWidth, contentHeight);
        layoutInArea(foregroundMask, contentX, contentY, contentWidth, contentHeight, 0, HPos.CENTER, VPos.CENTER);
        centralGroup.setTranslateX(contentX+contentWidth/2);
        centralGroup.setTranslateY(contentY+contentHeight/2);
        centralScale.setX(barRadius);
        centralScale.setY(barRadius);
    }
}
