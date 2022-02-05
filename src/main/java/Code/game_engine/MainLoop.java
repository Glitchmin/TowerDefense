package Code.game_engine;

import Code.PlayerValues;
import Code.Vector2d;
import Code.gui.IEnemyChangeObserver;
import Code.gui.MapVisualizer;
import Code.map_handling.AbstractTurret;
import Code.map_handling.Enemy;
import Code.map_handling.EnemyType;
import Code.map_handling.Map;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.out;

public class MainLoop implements Runnable {
    private final PlayerValues playerValues;
    private final MapVisualizer mapVisualizer;
    private final Map map;
    private final Random random;


    public MainLoop(PlayerValues playerValues, MapVisualizer mapVisualizer, Map map) {
        this.playerValues = playerValues;
        this.mapVisualizer = mapVisualizer;
        this.map = map;
        this.random = new Random();
    }

    private void addEnemy(){
        Enemy enemy = new Enemy(EnemyType.RUNNER, System.currentTimeMillis(), map, map.getTurretsList());
        enemy.addObserver(mapVisualizer);
        map.addEnemy(enemy);
    }

    private void moveEnemies() {
        List<Enemy> enemiesToDelete = new ArrayList<>();
        for (Enemy enemy : map.getEnemies()) {
            if (!(enemy.reachedEnd() || enemy.isDead())) {
                enemy.move(System.currentTimeMillis());
            } else {
                if (enemy.reachedEnd()) {
                    playerValues.dealDmg(enemy.getDmg());
                }
                enemiesToDelete.add(enemy);
            }
            enemy.positionChanged();
        }
        for (Enemy enemy : enemiesToDelete) {
            map.removeEnemy(enemy);
        }
    }

    public void calcTurrets(){
        for (AbstractTurret turret : map.getTurretsList()){
            Vector2d firePos = turret.fire(currentTimeMillis());
            if (firePos != null) {
                mapVisualizer.addLine(turret.getPosition(), firePos);
            }
        }
    }


    @Override
    public void run() {
        addEnemy();
        addEnemy();
        while (!playerValues.isPlayerDead()) {
            mapVisualizer.updateTime(System.currentTimeMillis());
            moveEnemies();
            calcTurrets();
            Platform.runLater(mapVisualizer);
            try {
                Thread.sleep(3);
            } catch (InterruptedException e) {
                out.println("Interrupted Threat Simulation Engine");
                e.printStackTrace();
            }

        }
    }
}

