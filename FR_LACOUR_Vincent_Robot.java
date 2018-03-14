package Informatica;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
 
import robocode.AdvancedRobot;
import robocode.Condition;
import robocode.RobotDeathEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

/**
 * FR_LACOUR_Vincent_Robot - a robot by LACOUR Vincent
 * 
 * Inspired by : BlizBat - a robot by Voidious
 * Code is open source, released under the RoboWiki Public Code License:
 * http://robowiki.net/cgi-bin/robowiki?RWPCL
*/

public class FR_LACOUR_Vincent_Robot extends AdvancedRobot {

  //Constantes
	private static final double TWO_PI = Math.PI * 2;
  
    // Enemigos
    private static String targetName;
    private static double targetDistance;
    private static Map<String, EnemyData> list_enemies = new HashMap<String, EnemyData>();
  
    // Campo y localisacion
    private static Rectangle2D.Double battleField;
    private static Point2D.Double destination;
    private static List<Point2D.Double> recentLocations;
  
 
  public void run() {
  
	setAdjustGunForRobotTurn(true);
    setAdjustRadarForGunTurn(true);
    
	// Colores de la bandera francesa
	Color French_blue = new Color(0,85,164); // BodyColor
	Color French_white = new Color(255,255,255); // GunColor
	Color French_red = new Color(239, 65, 53); // RadarColor
	Color Bullet = new Color(15,157,232); //BulletColor
	Color ScanArc = new Color(255,255,255); // ScanArcColor
    setColors(French_blue, French_white ,French_red,Bullet,ScanArc);
	
 	// Initialisacion de los variables
    battleField = new Rectangle2D.Double(50, 50, getBattleFieldWidth() - 100, getBattleFieldHeight() - 100);
    recentLocations = new ArrayList<Point2D.Double>();
    targetDistance = Double.POSITIVE_INFINITY;
    destination = null;
 
    do {
      Point2D.Double myLocation = myLocation();
      recentLocations.add(0, myLocation);
 	  EnemyData targetData = (EnemyData) list_enemies.get(targetName);
	  
      // Canon : formula clasica
      try {
        setTurnGunRightRadians(Utils.normalRelativeAngle(absoluteBearing(myLocation, targetData) - getGunHeadingRadians()));
      } catch (NullPointerException ex) { }
	  double bullet_power = 3 - ((20 - getEnergy())/6); //Formula bullet power
	  setFire(bullet_power);
 
     // Movimiento : calculo del riesgo
      double bestRisk;
      try {
        bestRisk = evalDestinationRisk(destination) * .85;
      } catch (NullPointerException ex) {
        bestRisk = Double.POSITIVE_INFINITY;
      }
      try {
        for (double d = 0; d < TWO_PI; d += 0.1) {
          Point2D.Double newDest = project(myLocation, d,Math.min(targetDistance, 100 + Math.random() * 500)); //Random 
          double thisRisk = evalDestinationRisk(newDest);
          if (battleField.contains(newDest) && thisRisk < bestRisk) {
            bestRisk = thisRisk;
            destination = newDest;
          }
        }
        double angle = Utils.normalRelativeAngle( absoluteBearing(myLocation, destination) - getHeadingRadians());
        setTurnRightRadians(Math.tan(angle));
        setAhead(Math.cos(angle) * Double.POSITIVE_INFINITY);
      } catch (NullPointerException ex) {
        // expected before we have a destination
		}

	  // Ejecuta acciones
      setTurnRadarRightRadians(1);
      execute(); 
    } while (true); 
  }
 
  public void onScannedRobot(ScannedRobotEvent e) {
  
    double distance = e.getDistance();
    String botName = e.getName();
 
    if (!list_enemies.containsKey(botName)) {
      list_enemies.put(botName, new EnemyData()); // Nuevo enemigo en la lista
    }
	
	// Enemigos datos
    EnemyData enemyData = list_enemies.get(botName);
    enemyData.energy = e.getEnergy();
    enemyData.setLocation(project(myLocation(), e.getBearingRadians() + getHeadingRadians(),distance));
	
	//Cambiar de objetivo : enemigo mas cerca
    if (distance < targetDistance || botName.equals(targetName)) {
      targetDistance = distance;
      targetName = botName;
    }
	
  }
 
  public void onRobotDeath(RobotDeathEvent e) {
    list_enemies.remove(e.getName()); // Eliminar un enemigo de la lista
    targetDistance = Double.POSITIVE_INFINITY;
  }
 
  private double evalDestinationRisk(Point2D.Double destination) {
    double risk = 0;
 
    for (EnemyData enemy1 : list_enemies.values()) {
      double distSq = enemy1.distanceSq(destination);
      int closer = 0;
      for (EnemyData enemy2 : list_enemies.values()) {
        if (enemy1.distanceSq(enemy2) < distSq) {
          closer++;
        }
      }
      java.awt.geom.Point2D.Double myLocation = myLocation();
	  // Formula para calcular el riesgo
      risk += Math.max(0.5, Math.min(enemy1.energy / getEnergy(), 2))
          * (1 + Math.abs(Math.cos(absoluteBearing(myLocation, destination)
              - absoluteBearing(myLocation, enemy1))))
          / closer
          / distSq
          / (200000 + destination.distanceSq(getBattleFieldWidth() / 2, getBattleFieldHeight() / 2));
    }
    for (int x = 1; x < 6; x++) {
      try {
        risk *= 1 + (500 / x
            / recentLocations.get(x * 10).distanceSq(destination));
      } catch (Exception e) {}
    }
    return risk;
  }
 
  public static double absoluteBearing(
      Point2D.Double source, Point2D.Double target) {
    return Math.atan2(target.x - source.x, target.y - source.y);
  }
 
  public static Point2D.Double project(Point2D.Double sourceLocation, 
      double angle, double length) {
    return new Point2D.Double(
        sourceLocation.x + Math.sin(angle) * length,
        sourceLocation.y + Math.cos(angle) * length);
  }
 
  private Point2D.Double myLocation() {
    return new Point2D.Double(getX(), getY());
  }
 
  public static class EnemyData extends Point2D.Double {
    public double energy;
  }
 
}
