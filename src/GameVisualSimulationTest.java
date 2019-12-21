 /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */



 import ai.abstraction.*;
 import ai.abstraction.pathfinding.AStarPathFinding;

 import ai.core.AI;
 import gui.PhysicalGameStatePanel;
 import rts.GameState;
 import rts.PhysicalGameState;
 import rts.PlayerAction;
 import rts.units.UnitTypeTable;

 import javax.swing.*;

 /**
  *
  * @author santi
  */
 public class GameVisualSimulationTest {
     public static void main(String args[]) throws Exception {
         UnitTypeTable utt = new UnitTypeTable();
//         PhysicalGameState pgs = PhysicalGameState.load("C:\\Users\\50339\\Documents\\microrts-master_new\\microrts-master\\maps\\8x8\\bases8x8.xml", utt);

//         PhysicalGameState pgs = PhysicalGameState.load("C:\\Users\\50339\\Documents\\microrts-master_new\\microrts-master\\maps\\16x16\\basesWorkers16x16A.xml", utt);
         PhysicalGameState pgs = PhysicalGameState.load("C:\\Users\\50339\\Documents\\microrts-master_new\\microrts-master\\maps\\BWDistantResources32x32.xml", utt);

         //        PhysicalGameState pgs = MapGenerator.basesWorkers8x8Obstacle();

         GameState gs = new GameState(pgs, utt);
         int MAXCYCLES = 9000;
         int PERIOD = 10;
         boolean gameover = false;

//         AI ai1 = new MyWorkRushPlus(utt,new GreedyPathFinding());
         AI ai1 = new HeavyDefense(utt);

         //经过测试BFSPathFinding()基地距离远更厉害一些
         AI ai2 = new BronzeAI_25(utt,new AStarPathFinding());

         JFrame w = PhysicalGameStatePanel.newVisualizer(gs,640,640,false,PhysicalGameStatePanel.COLORSCHEME_BLACK);
 //        JFrame w = PhysicalGameStatePanel.newVisualizer(gs,640,640,false,PhysicalGameStatePanel.COLORSCHEME_WHITE);

         long nextTimeToUpdate = System.currentTimeMillis() + PERIOD;
         do{
             if (System.currentTimeMillis()>=nextTimeToUpdate) {
                 PlayerAction pa1 = ai1.getAction(0, gs);
                 PlayerAction pa2 = ai2.getAction(1, gs);
                 gs.issueSafe(pa1);
                 gs.issueSafe(pa2);

                 // simulate:
                 gameover = gs.cycle();
                 w.repaint();
                 nextTimeToUpdate+=PERIOD;
             } else {
                 try {
                     Thread.sleep(1);
                 } catch (Exception e) {
                     e.printStackTrace();
                 }
             }
         }while(!gameover && gs.getTime()<MAXCYCLES);
         ai1.gameOver(gs.winner());
         ai2.gameOver(gs.winner());

         System.out.println("Game Over");
     }
 }
