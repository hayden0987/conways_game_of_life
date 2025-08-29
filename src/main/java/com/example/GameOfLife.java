package com.example;

import java.util.HashSet;
import java.util.Set;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

public class GameOfLife extends Application {

    private static int cellSize = 20;
    Set<Point> aliveCells = new HashSet<>();
    private double offsetX = 0; // camera x position
    private double offsetY = 0; // camera y position
    private double dragStartX, dragStartY;
    private Text xCoordText;
    private Text yCoordText;
    private Text populationText;
    private Text generationText;
    private Canvas canvas;
    private boolean dragDetected;
    private int generation;
    private boolean running = false;
    private Timeline timeline;
    private int runDelay = 200;

    @Override
    public void start(Stage stage) throws Exception {
        // Get the game menu
        VBox gameMenu = initGameMenu();

        // Get the game canvas
        canvas = drawGameCanvas();
        canvas.widthProperty().bind(stage.widthProperty());
        canvas.heightProperty().bind(stage.heightProperty());
        canvas.widthProperty().addListener(e -> draw(canvas.getGraphicsContext2D()));
        canvas.heightProperty().addListener(e -> draw(canvas.getGraphicsContext2D()));

        BorderPane root = new BorderPane();
        root.setTop(gameMenu);
        root.setCenter(canvas);

        Scene scene = new Scene(root, 800, 600);

        scene.setOnScroll(e -> {
            double mouseX = e.getX();
            double mouseY = e.getY();

            double worldX = (mouseX + offsetX) / cellSize;
            double worldY = (mouseY + offsetY) / cellSize;

            int oldCellSize = cellSize;

            if (e.getDeltaY() > 0 && cellSize < 50) {
                cellSize++;
            } else if (e.getDeltaY() < 0 && cellSize > 2) {
                cellSize--;
            }

            if (oldCellSize != cellSize) {
                offsetX = worldX * cellSize - mouseX;
                offsetY = worldY * cellSize - mouseY;
            }

            draw(canvas.getGraphicsContext2D());
        });

        stage.setScene(scene);
        stage.setTitle("Conways Game of Life");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public VBox initGameMenu() {
        xCoordText = new Text("X: " + offsetX);
        yCoordText = new Text("Y: " + offsetY);
        populationText = new Text("Population: " + aliveCells.size());
        generationText = new Text("Generation: " + generation);
        Button jumpToStartButton = new Button("Jump to (0, 0)");
        Button nextGenerationButton = new Button("Step Forward");
        Button toggleRunButton = new Button("Start");
        Text runSpeedText = new Text("Time between generations: ");
        Slider runSpeedSlider = new Slider();

        jumpToStartButton.setOnMouseClicked(e -> {
            offsetX = 0;
            offsetY = 0;
            updateGameMenu();
            draw(canvas.getGraphicsContext2D());
        });

        nextGenerationButton.setOnMouseClicked(e -> {
            nextGeneration();
        });

        toggleRunButton.setOnMouseClicked(e -> {
            if (!running) {
                startAutoRun();
                toggleRunButton.setText("Stop");
            } else {
                stopAutoRun();
                toggleRunButton.setText("Start");
            }
        });

        runSpeedSlider.setMin(1);
        runSpeedSlider.setMax(1000);
        runSpeedSlider.setMajorTickUnit(200);
        runSpeedSlider.setMinorTickCount(100);
        runSpeedSlider.setValue(200);

        runSpeedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            runDelay = newVal.intValue();
            if (timeline != null) {
                timeline.setRate(200.0 / runDelay);
            }
        });

        HBox informationContainer = new HBox(xCoordText, yCoordText, populationText, generationText);
        HBox buttonContainer = new HBox(jumpToStartButton, nextGenerationButton, toggleRunButton, runSpeedText, runSpeedSlider);
        VBox container = new VBox(informationContainer, buttonContainer);
        informationContainer.setSpacing(15);
        informationContainer.setPadding(new Insets(10));
        buttonContainer.setSpacing(15);
        buttonContainer.setPadding(new Insets(10));
        container.setSpacing(15);
        container.setPadding(new Insets(10));
        return container;
    }

    private void startAutoRun() {
        running = true;
        timeline = new Timeline(new KeyFrame(Duration.millis(runDelay), e -> nextGeneration()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void stopAutoRun() {
        running = false;
        if (timeline != null) {
            timeline.stop();
        }
    }

    private void nextGeneration() {
        Set<Point> newAlive = new HashSet<>();
        Set<Point> candidates = new HashSet<>();

        // All live cell and neighbors are candidates
        for (Point p : aliveCells) {
            candidates.add(p);
            candidates.addAll(getNeighbors(p));
        }

        for (Point p : candidates) {
            int aliveNeighbors = 0;
            for (Point n : getNeighbors(p)) {
                if (aliveCells.contains(n)) {
                    aliveNeighbors++;
                }
            }

            if (aliveCells.contains(p)) {
                // Survival: live cell stays alive if 2 or 3 neighbors
                if (aliveNeighbors == 2 || aliveNeighbors == 3) {
                    newAlive.add(p);
                }
            } else {
                // Birth: dead cell becomes alive if exactly 3 neighbors
                if (aliveNeighbors == 3) {
                    newAlive.add(p);
                }
            }
        }

        aliveCells = newAlive;
        generation++;
        updateGameMenu();
        draw(canvas.getGraphicsContext2D());
    }

    // Get the eight neighbors of a point
    private Set<Point> getNeighbors(Point p) {
        Set<Point> neighbors = new HashSet<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) {
                    continue; // skip the cell itself

                }
                neighbors.add(new Point(p.x + dx, p.y + dy));
            }
        }
        return neighbors;
    }

    public Canvas drawGameCanvas() {
        canvas = new Canvas(800, 600);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Example cells
        aliveCells.add(new Point(0, 0));
        aliveCells.add(new Point(1, 0));
        aliveCells.add(new Point(2, 0));

        draw(gc);

        // Allow dragging
        canvas.setOnMousePressed(e -> {
            dragDetected = false;
            dragStartX = e.getX();
            dragStartY = e.getY();
        });

        canvas.setOnMouseDragged(e -> {
            dragDetected = true;
            offsetX -= e.getX() - dragStartX;
            offsetY -= e.getY() - dragStartY;
            dragStartX = e.getX();
            dragStartY = e.getY();
            draw(gc);
            updateGameMenu();
        });

        canvas.setOnMouseReleased(e -> {
            // Handle clicking a cell to alive/unalive it
            if (!dragDetected && e.getButton() == MouseButton.PRIMARY) {
                int x = (int) Math.floor(((e.getX() + offsetX) / cellSize));
                int y = (int) Math.floor(((e.getY() + offsetY) / cellSize));
                Point target = new Point(x, y);

                if (aliveCells.contains(target)) {
                    aliveCells.remove(target);
                } else {
                    aliveCells.add(target);
                }
                draw(gc);
            }
        });

        return canvas;
    }

    // Updates the game menu
    private void updateGameMenu() {
        xCoordText.setText("X: " + (int) offsetX);
        yCoordText.setText("Y: " + (int) -offsetY);
        populationText.setText("Population: " + aliveCells.size());
        generationText.setText("Generation: " + generation);
    }

    // Draw the game canvas
    private void draw(GraphicsContext gc) {
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setStroke(javafx.scene.paint.Color.LIGHTGRAY);
        gc.setLineWidth(3);

        double width = canvas.getWidth();
        double height = canvas.getHeight();

        // Draw grid lines relative to the offset
        for (int x = (int) (-offsetX % cellSize); x < width; x += cellSize) {
            gc.strokeLine(x, 0, x, height);
        }
        for (int y = (int) (-offsetY % cellSize); y < height; y += cellSize) {
            gc.strokeLine(0, y, width, y);
        }

        // Draw alive cells
        gc.setFill(javafx.scene.paint.Color.BLACK);
        for (Point p : aliveCells) {
            double screenX = p.x * cellSize - offsetX;
            double screenY = p.y * cellSize - offsetY;
            gc.fillRect(screenX, screenY, cellSize, cellSize);
        }
    }
}

class Point {

    final int x, y;

    Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Point)) {
            return false;
        }
        Point p = (Point) o;
        return x == p.x && y == p.y;
    }

    @Override
    public int hashCode() {
        return x * 31 + y;
    }
}
