package MyRTS;
/**
 * @Description 8*8地图无敌
 * @Author Jianhai Wang
 * @ClassName MyWorkerRush
 * @Date 2019/11/14 15:57
 * @Version 1.0
 */

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Harvest;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;
import rts.*;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;
import rts.UnitActionAssignment;


public class MyWorkRushPlus extends AbstractionLayerAI {

    private static boolean flag = false; //判断最终工兵的进攻行为
    private static boolean CarryNoResource = false;

    UnitTypeTable m_utt = null;
    Random r = new Random();
    UnitType workerType;
    UnitType baseType;
    UnitType barracksType;
    UnitType lightType;
    UnitType rangedType;
    UnitType heavyType;
    UnitType resourceType;

    public MyWorkRushPlus(UnitTypeTable utt) {
        this(utt, new AStarPathFinding());
    }

    public MyWorkRushPlus(UnitTypeTable utt, PathFinding pf) {
        super(pf);
        reset(utt);
    }

    public AI clone() {
        return new BoJingAI(m_utt);
    }

    public void reset(UnitTypeTable a_utt) {
        m_utt = a_utt;
        workerType = m_utt.getUnitType("Worker");
        baseType = m_utt.getUnitType("Base");
        barracksType = m_utt.getUnitType("Barracks");
        lightType = m_utt.getUnitType("Light");
        rangedType = m_utt.getUnitType("Ranged");
        heavyType = m_utt.getUnitType("Heavy");
        resourceType = m_utt.getUnitType("Resource");
    }

    //加强版workRush
    public PlayerAction getAction(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);
        workerRush(p, gs);
        return translateActions(player, gs);
    }

    public void workerRush(Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();

        List<Unit> harvestWorker = new LinkedList<>();
        baseBehavior(p, gs);
        int number = 0;
        Unit base = null;
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == baseType && u.getPlayer() == p.getID()) {
                base = u;

            }
        }

        for (Unit u : pgs.getUnits()) {
            //判断资源的时候不能带选手ID
            assert base != null;
            if (u.getType() == resourceType &&
                    (Math.abs(u.getX() - base.getX()) + Math.abs(u.getY() - base.getY())) <= 5) {
                number++;
            }
        }
        if (number == 0)
            flag = true; //基地附近没有资源标志

        if (flag) {  //没有兵携带资源,则carryResource = true
            for (Unit u : pgs.getUnits()) {
                if (u.getType().canHarvest && u.getPlayer() == p.getID()) {
                    UnitActionAssignment uaa = gs.getActionAssignment(u);
                    assert base != null;
                    if (uaa != null && uaa.action.getType() == UnitAction.TYPE_NONE && (Math.abs(u.getX() - base.getX()) + Math.abs(u.getY() - base.getY())) <= 3) {
                        CarryNoResource = true;
                    }
                }
            }
        }

        System.out.println("有无资源标志：" + flag);
        System.out.println("小兵是否携带资源" + CarryNoResource);

        if (!flag) { //有资源
            for (Unit u : pgs.getUnits()) {
                if (u.getType().canHarvest && u.getPlayer() == p.getID()) {
                    if (harvestWorker.size() < 1) {
                        harvestWorker.add(u);
                    } else {
                        meleeBehavior(u, p, gs);
                    }
                }
            }
            harvestWorkerGroup(harvestWorker, p, gs);
        } else if (!CarryNoResource) { //没资源，且有一个小兵携带资源，放在基地
            for (Unit u : pgs.getUnits()) {
                if (u.getType().canHarvest && u.getPlayer() == p.getID()) {
                    UnitActionAssignment uaa = gs.getActionAssignment(u);
                    if (uaa != null && uaa.action.getType() != UnitAction.TYPE_RETURN)
                        meleeBehavior(u, p, gs);
                    else
                        harvest(u, u, base);
                }
            }
        } else { //无资源，且小兵没有携带资源
            for (Unit u : pgs.getUnits()) {
                if (u.getType().canAttack && u.getPlayer() == p.getID()) {
                    meleeBehavior(u, p, gs);
                }
            }
        }
    }

    public void baseBehavior(Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == baseType && u.getPlayer() == p.getID() && gs.getActionAssignment(u) == null) {
                if (p.getResources() >= workerType.cost && u.getPlayer() == p.getID()) {
                    train(u, workerType);
                }
            }
        }
    }

    public void barracksBehavior(Player p, GameState gs, UnitType type) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == barracksType && u.getPlayer() == p.getID() && gs.getActionAssignment(u) == null) {
                if (p.getResources() >= type.cost) {
                    train(u, type);
                }
            }
        }
    }

    public void harvestWorkerGroup(List<Unit> harvestWorkers, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestBase = null;
        Unit closestResource = null;
        int closestDistance = 0;
        for (Unit worker : harvestWorkers) {
            for (Unit u2 : pgs.getUnits()) {
                if (u2.getType().isResource) {
                    int d = Math.abs(u2.getX() - worker.getX()) + Math.abs(u2.getY() - worker.getY());
                    if (closestResource == null || d < closestDistance) {
                        closestResource = u2;
                        closestDistance = d;
                    }
                }
            }
            closestDistance = 0;
            for (Unit u2 : pgs.getUnits()) {
                if (u2.getType().isStockpile && u2.getPlayer() == p.getID()) {
                    int d = Math.abs(u2.getX() - worker.getX()) + Math.abs(u2.getY() - worker.getY());
                    if (closestBase == null || d < closestDistance) {
                        closestBase = u2;
                        closestDistance = d;
                    }
                }
            }
            if (closestResource != null && closestBase != null) {
                AbstractAction aa = getAbstractAction(worker);
                if (aa instanceof Harvest) {
                    Harvest h_aa = (Harvest) aa;
                    if (h_aa.getTarget() != closestResource || h_aa.getBase() != closestBase) {
                        harvest(worker, closestResource, closestBase);
                    }
                } else {
                    harvest(worker, closestResource, closestBase);
                }
            } else if (p.getResources() != 0 && closestBase != null) {
                harvest(worker, worker, closestBase);
            } else {
                meleeBehavior(worker, p, gs);
            }
        }
    }

    public void meleeBehavior(Unit u, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int closestDistance = 0;
        List<Unit> enemyBase = new LinkedList<>();
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if ((closestEnemy == null || d < closestDistance) && u2.getType() != baseType) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
        }
        for (Unit u3 : pgs.getUnits()) {
            if (u3.getPlayer() >= 0 && u3.getPlayer() != p.getID() && u3.getType() == baseType) {
                enemyBase.add(u3);
            }
        }
        if (closestEnemy != null) {
            attack(u, closestEnemy);
        } else if (!enemyBase.isEmpty()) {
            for (Unit base : enemyBase)
                attack(u, base);
        } else {
            attack(u, null);
        }
    }


    public List<ParameterSpecification> getParameters() {
        List<ParameterSpecification> parameters = new ArrayList<>();
        parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));
        return parameters;
    }

}

