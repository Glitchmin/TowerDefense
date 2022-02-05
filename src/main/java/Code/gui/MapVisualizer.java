package Code.gui;

import Code.Vector2d;
import Code.map_handling.*;
import Code.map_handling.turrets.LaserTurret;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import static java.lang.System.out;


public class MapVisualizer implements ITurretChangeObserver, IEnemyChangeObserver, Runnable {
    private final GridPane mapGridPane;
    private final Pane paneOfEverything;
    private final Map map;
    private final MapTileBox[][] landscapeBoxes;
    private final MapTileBox[][] turretBoxes;
    private final TurretShop turretShop;
    private final Label landscapeNameOnCursorLabel;
    private final TurretTracker turretObserver;
    private final TurretBuilder turretBuilder;
    private VBox tmpTurret;
    private final TreeMap<Integer, ImageView> enemyImages;
    private final Double tileSize;
    private final List<Enemy> enemiesToRender;
    private final List<Line> linesList;
    private final List<Long> linesExpTimeList;
    private long time;

    public MapVisualizer(Map map, Pane paneOfEverything, TurretShop turretShop, TurretTracker turretObserver, TurretBuilder turretBuilder, Integer guiElementBoxWidth, Integer guiElementBoxHeight, Double tileSize) {
        enemyImages = new TreeMap<>();
        this.tileSize = tileSize;
        this.paneOfEverything = paneOfEverything;
        landscapeNameOnCursorLabel = new Label("");
        MapTileBox.setWidth(guiElementBoxWidth);
        MapTileBox.setHeight(guiElementBoxHeight);
        this.map = map;
        this.turretShop = turretShop;
        this.turretObserver = turretObserver;
        this.turretBuilder = turretBuilder;
        this.enemiesToRender = new ArrayList<>();
        this.linesList = new ArrayList<>();
        this.linesExpTimeList = new ArrayList<>();
        mapGridPane = new GridPane();

        landscapeBoxes = new MapTileBox[map.getWidth()][map.getHeight()];
        turretBoxes = new MapTileBox[map.getWidth()][map.getHeight()];

        for (int i = 0; i < map.getWidth(); i++) {
            for (int j = 0; j < map.getHeight(); j++) {
                MapTileBox mapTileBox = new MapTileBox(map.getLandscape(i, j));
                mapGridPane.add(mapTileBox.getVBox(), i, j);
                landscapeBoxes[i][j] = mapTileBox;
                mapTileBox.getVBox().setOnMouseEntered(Action ->
                {
                    handleCursorOnMapTile(mapTileBox);
                });
            }
        }
    }

    public void updateTime(long time){
        this.time = time;
    }

    public void addLine(Vector2d start, Vector2d end) {
        synchronized (this) {
            Line line = new Line();
            line.setStartX(start.x * tileSize);
            line.setStartY(start.y * tileSize);
            line.setEndX(end.x * tileSize);
            line.setEndY(end.y * tileSize);
            linesList.add(line);
            linesExpTimeList.add(time+100);
        }
    }

    private void drawLines() {
        synchronized (this) {
            for (int i=0; i<linesList.size();i++) {
                Line line = linesList.get(i);
                if (!paneOfEverything.getChildren().contains(line)) {
                    paneOfEverything.getChildren().add(line);
                }
                if (time > linesExpTimeList.get(i)){
                    paneOfEverything.getChildren().remove(linesList.get(i));
                    linesList.remove(linesList.get(i));
                    linesExpTimeList.remove(linesExpTimeList.get(i));
                    i--;
                }
            }
        }
    }

    private void handleCursorOnMapTile(MapTileBox mapTileBox) {
        for (int i = 0; i < map.getWidth(); i++) {
            for (int j = 0; j < map.getHeight(); j++) {
                if (mapTileBox == landscapeBoxes[i][j]) {
                    landscapeNameOnCursorLabel.setText
                            (map.getLandscape(i, j).landscapeType.toString() + " (" + i + "," + j + ")");
                    showTmpTurret(i, j);
                }
            }
        }
    }


    private void showTmpTurret(int i, int j) {
        mapGridPane.getChildren().remove(tmpTurret);
        if (turretShop.getSelectedTurret() == TurretType.LASER) {
            tmpTurret = new MapTileBox(new LaserTurret(new Vector2d(i + 0.5, j + 0.5))).getVBox();
            tmpTurret.setOpacity(0.5);
            tmpTurret.setOnMouseClicked(Action -> {
                if (map.getLandscape(i, j).landscapeType == LandscapeType.GRASS
                        || map.getLandscape(i, j).landscapeType == LandscapeType.HILL) {
                    turretBuilder.build(i, j, new LaserTurret(new Vector2d(i + 0.5, j + 0.5)));
                }
            });
            mapGridPane.add(tmpTurret, i, j);
        }
    }

    public Label getLandscapeNameOnCursorLabel() {
        return landscapeNameOnCursorLabel;
    }

    public GridPane getMapGridPane() {
        return mapGridPane;
    }

    @Override
    public void enemyChanged(Enemy enemy) {
        synchronized (map) {
            enemiesToRender.add(enemy);
        }
    }

    public void updateEnemies() {
        synchronized (map) {
            for (Enemy enemy : enemiesToRender) {
                ImageView enemyImageView = enemyImages.get(enemy.getID());
                if (enemyImageView == null) {
                    enemyImageView = new ImageView(ImageLoader.loadImage(enemy.getEnemyType().getResourcePath()));
                    enemyImages.put(enemy.getID(), enemyImageView);
                    paneOfEverything.getChildren().add(enemyImages.get(enemy.getID()));
                    enemyImageView.setFitHeight(20);
                    enemyImageView.setFitWidth(20);
                }
                enemyImageView.setX(enemy.getPosition().x * tileSize - 10);
                enemyImageView.setY(enemy.getPosition().y * tileSize - 10);
                if (enemy.reachedEnd() || enemy.isDead()) {
                    paneOfEverything.getChildren().remove(enemyImageView);
                    enemyImages.remove(enemy.getID());
                }
            }
        }
    }

    @Override
    public void run() {
        updateEnemies();
        drawLines();
    }

    @Override
    public void turretChanged(int x, int y) {
        if (turretBoxes[x][y] != null) {
            mapGridPane.getChildren().remove(turretBoxes[x][y].getVBox());
        }
        turretBoxes[x][y] = new MapTileBox(map.getTurret(x, y));
        mapGridPane.add(turretBoxes[x][y].getVBox(), x, y);
        turretBoxes[x][y].getVBox().setOnMouseClicked(Action -> turretObserver.updateVBox(map.getTurret(x, y)));
    }
}
