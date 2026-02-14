package gui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SlotViewer {
    private final Set<Integer> occupiedSlots;
    private final Integer highlightedSlot;
    private final SlotFlashType flashType;
    private GridPane grid;

    // Enum to define different flash types
    public enum SlotFlashType {
        PARK_CAR, // Green to Yellow (original)
        RELEASE_CAR // Now Red flash, then Yellow flash, then permanently Green
    }

    public SlotViewer(List<Integer> occupiedSlots) {
        this(occupiedSlots, null, SlotFlashType.PARK_CAR); // Default to PARK_CAR if not specified
    }

    public SlotViewer(List<Integer> occupiedSlots, Integer highlightedSlot) {
        this(occupiedSlots, highlightedSlot, SlotFlashType.PARK_CAR); // Default to PARK_CAR
    }

    public SlotViewer(List<Integer> occupiedSlots, Integer highlightedSlot, SlotFlashType flashType) {
        this.occupiedSlots = new HashSet<>(occupiedSlots);
        this.highlightedSlot = highlightedSlot;
        this.flashType = flashType;
        createGrid();
    }

    private void createGrid() {
        grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);

        for (int i = 1; i <= 100; i++) {
            Label slot = new Label(String.valueOf(i));
            slot.getStyleClass().add("slot-label");

            if (occupiedSlots.contains(i)) {
                slot.getStyleClass().add("occupied");
            } else {
                slot.getStyleClass().add("available");
            }

            if (highlightedSlot != null && highlightedSlot == i) {
                if (flashType == SlotFlashType.PARK_CAR) {
                    flashForParkCar(slot);
                } else if (flashType == SlotFlashType.RELEASE_CAR) {
                    // This now handles the Red-Yellow-Green sequence
                    flashForReleaseCar(slot);
                }
            }

            grid.add(slot, (i - 1) % 10, (i - 1) / 10);
        }
    }

    // Original flash method for Park Car (green to yellow)
    private void flashForParkCar(Label slot) {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.seconds(0.0), e -> slot.setStyle("-fx-background-color: yellow")),
            new KeyFrame(Duration.seconds(0.4), e -> slot.setStyle(""))
        );
        timeline.setCycleCount(6); // flash 3 times
        timeline.play();
    }

    // Modified flash method for Release Car: now Red-Yellow-Green sequence
    private void flashForReleaseCar(Label slot) {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.seconds(0.0), e -> slot.setStyle("-fx-background-color: yellow")), // Then Yellow
            new KeyFrame(Duration.seconds(0.4), e -> slot.setStyle("")) // Briefly off
        );
        timeline.setCycleCount(6); // Play the sequence once
        timeline.play();
    }

    public GridPane getGrid() {
        return grid;
    }
}
