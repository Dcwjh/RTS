/**
 * @Description TODO
 * @Author Jianhai Wang
 * @ClassName MyWorkerRush
 * @Date 2019/11/14 15:57
 * @Version 3.0  新版本
 */

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Harvest;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.BFSPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;
import rts.*;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;
import rts.UnitActionAssignment;

public class BronzeAI_25_jiaodeyiban extends AbstractionLayerAI {
    private static boolean flag = false; //发起进攻的信号
    private static int DEFENSEDIS = 1;

    private static int TIME = 0;
    private static int anInt = 10;  //决定发起进攻的时间长度，越大积累的兵就越多。越小发起总进攻的时间就越快
    //进攻的队列

    private static boolean baseSecond = true;


    UnitTypeTable m_utt = null;
    Random r = new Random();
    UnitType workerType;
    UnitType baseType;
    UnitType barracksType;
    UnitType lightType;
    UnitType rangedType;
    UnitType heavyType;
    UnitType resourceType;


    // This is the default constructor that microRTS will call:
    public BronzeAI_25_jiaodeyiban(UnitTypeTable utt) {
        this(utt, new AStarPathFinding());
        System.out.println("Team25");
    }

    public BronzeAI_25_jiaodeyiban(UnitTypeTable utt, PathFinding pf) {
        super(pf);    // # set "time budget" and "iteration budget"
        reset(utt);
        System.out.println("Team25");
    }

    // This will be called by microRTS when it wants to create new instances of this bot (e.g., to play multiple games).
    public AI clone() {
        return new BronzeAI_25(m_utt);
    }

    // This will be called once at the beginning of each new game:    
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

    public PlayerAction getAction(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);

        List<Unit> myBases = new ArrayList<>();
        List<Unit> enemyBases = new ArrayList<>();
        int minDisBases = Integer.MAX_VALUE;
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == baseType) {
                if (u.getPlayer() == player) {
                    myBases.add(u);
                } else {
                    enemyBases.add(u);
                }
            }
        }

        for (Unit mybase : myBases) {
            for (Unit enemyBase : enemyBases) {
                minDisBases = Math.min(minDisBases, Math.abs(mybase.getX() - enemyBase.getX()) + Math.abs(mybase.getY() - enemyBase.getY()));
            }
        }
        int num = 0;
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == barracksType) {
                if (u.getPlayer() == player) {
                    num++;
                }
            }

        }
//        8*8
        if (pgs.getHeight() < 16) {
            workerRush(p, gs);
        }
//        16*16
        else if (pgs.getWidth() == 16)
            if(num == 0) {
                unitsRush(p, gs, 10, 0, 0, 0, 2, true);
            }else
            {
                unitsRush(p, gs, 3, 5, 1, 3, 2, true);
            }
//            32*32
        else
            if(num == 0)
                newRush(p, gs, 4, 0, 0, 0, 2, true);
            else
                unitsRush(p, gs, 3, 5, 1, 3, 4, false);

        return translateActions(player, gs); //返回操作
    }


    //基地行为，只能生产工兵
    public void baseBehavior(Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == baseType && u.getPlayer() == p.getID() && gs.getActionAssignment(u) == null) {
                if (p.getResources() >= workerType.cost && u.getPlayer() == p.getID()) {
                    train(u, workerType); //u是基地
                }
            }
        }
    }

    //产生除了工兵意以外的其他兵, 每个兵营都产生
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

    //跟新：集体采矿行为（把工兵采集到的资源都先放在基地然后在采取其他行为，充分利用了资源）
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

    ///跟新：个体采矿行为（把工兵采集到的资源都先放在基地然后在采取其他行为，充分利用了资源）
    public void harvestWorkerBehavior(Unit u, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestBase = null;
        Unit closestResource = null;
        int closestDistance = 0;

        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType().isResource) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestResource == null || d < closestDistance) {
                    closestResource = u2;
                    closestDistance = d;
                }
            }
        }
        closestDistance = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType().isStockpile && u2.getPlayer() == p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestBase == null || d < closestDistance) {
                    closestBase = u2;
                    closestDistance = d;
                }
            }
        }
        if (closestResource != null && closestBase != null) {
            AbstractAction aa = getAbstractAction(u);
            if (aa instanceof Harvest) {
                Harvest h_aa = (Harvest) aa;
                if (h_aa.getTarget() != closestResource || h_aa.getBase() != closestBase) {
                    harvest(u, closestResource, closestBase);
                }
            } else {
                harvest(u, closestResource, closestBase);
            }
        } else if (p.getResources() != 0 && closestBase != null) {
            harvest(u, u, closestBase);
        } else {
            meleeBehavior(u, p, gs);
        }
    }

    //更新代码：攻击过程中，优先攻击工兵，在攻击基地（小地图很有效）,
    public void meleeBehavior(Unit u, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int closestDistance = 0;
        List<Unit> enemyBase = new LinkedList<>();
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                //攻击非基地目标
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

    //---------------------------策略1：基于改进workerRush策略， 调整情况如函数所示----------------------------
    public void workerRush(Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        List<Unit> freeWorker = new LinkedList<>();

        List<Unit> harvestWorker = new LinkedList<>();
        baseBehavior(p, gs);
        for (Unit u : pgs.getUnits()) {
            if (u.getType().canHarvest && u.getPlayer() == p.getID()) {
                freeWorker.add(u);
            }
        }
        while (harvestWorker.size() < 1) {
            if (!freeWorker.isEmpty())
                harvestWorker.add(freeWorker.remove(0));
            else
                break;
        }

        int number = 0;
        Unit base = null;
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == baseType && u.getPlayer() == p.getID()) {
                base = u;
            }
        }

        if (base == null) {
//            System.out.println("我方输了");
            return;
        }

        for (Unit u : pgs.getUnits()) {
            assert base != null;
            if (u.getType() == resourceType &&
                    (Math.abs(u.getX() - base.getX()) + Math.abs(u.getY() - base.getY())) <= 5) {
                number++;
            }
        }
        if (number == 0)
            flag = true; //基地附近没有资源标志

        if (!flag) { //有资源
            for (Unit u : freeWorker) {
                meleeBehavior(u, p, gs);
            }
            harvestWorkerGroup(harvestWorker, p, gs);
        } else {  //无资源
            for (Unit u : pgs.getUnits()) {
                if (u.getType().canAttack && p.getID() == u.getPlayer()) {
                    if (u.getResources() > 0)
                        harvest(u, u, base);
                    else {
                        meleeBehavior(u, p, gs);
                    }
                }

            }
        }
    }
    //-----------------------------------策略二----------------------------------

    /**
     * @param p
     * @param gs
     * @param numWR      进攻工兵数量
     * @param numLR      进攻lights数量
     * @param numHR      进攻heacys数量
     * @param numRR      进攻远程兵数量
     * @param harvestNum 采矿数量
     * @param f          是否积累兵力 false就积累兵 true就进攻
     * @Description
     */
    public void unitsRush(Player p, GameState gs, int numWR, int numLR, int numHR, int numRR, int harvestNum, boolean f) {
        PhysicalGameState pgs = gs.getPhysicalGameState();

        //基地行为
        int workers = countUnits(p, gs, workerType);
        int nbases = countUnits(p, gs, baseType);
        int barracks = countUnits(p, gs, barracksType);

        if(workers < numWR + harvestNum)
            baseBehavior(p,gs);

        //获取敌方基地距离
        int baseDistance = Integer.MAX_VALUE;
        List<Unit> myBases = new ArrayList<>();
        Unit base = null;
        List<Unit> enemyBases = new ArrayList<>();
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == baseType) {
                if (u.getPlayer() == p.getID()) {
                    base = u;
                    myBases.add(u);
                } else {
                    enemyBases.add(u);
                }
            }
        }


        //所有工兵数量
        List<Unit> freeWorkers = countUnitsList(p, gs, workerType);
        List<Unit> harvestWorkers = new LinkedList<>();



        if (freeWorkers.isEmpty()) {
            return;
        }
        while(!freeWorkers.isEmpty() && harvestWorkers.size() < harvestNum){
            harvestWorkers.add(freeWorkers.remove(0));
        }

        List<Integer> reservedPositions = new LinkedList<Integer>();
        int locationX = 0;
        int locationY = 0;


        //建基地   注意:行为不能覆盖，不能两次获取列表（可以改变行为），可以获取个数
        if (nbases == 0) {
            if (p.getResources() >= baseType.cost && !freeWorkers.isEmpty()) {
                Unit u = null;
                for(Unit unit : freeWorkers) {
                    if(unit!=null && gs.getActionAssignment(unit) == null) {
                        freeWorkers.remove(unit);
                        u = unit;
                        break;
                    }
                }
                if(u != null)
                    buildIfNotAlreadyBuilding(u, baseType, u.getX(), u.getY(), reservedPositions, p, pgs);
            }
        }

        //建兵营  注意:行为不能覆盖，不能两次获取列表（可以改变行为），可以获取个数
        if (barracks == 0) {
            if (p.getResources() >= barracksType.cost && !freeWorkers.isEmpty()) {
                Unit u = freeWorkers.remove(0);
                if (base != null && u!=null) {
//                    System.out.println("基地位置" + base.getX() + ", " + base.getY());
                    locationX = base.getX();
                    locationY = base.getY();
                    if(p.getID() == 0){
                        locationX += 2;
                        locationY += 2;
                    }
                    buildIfNotAlreadyBuilding(u, barracksType, locationX, locationY, reservedPositions, p, pgs);
                }
            }
        }



        harvestWorkerGroup(harvestWorkers, p, gs);

        for (Unit u : freeWorkers) {
                meleeBehavior(u, p, gs);
        }
        //-------------------------以上是工兵行为---------------------------------------

        //-------------------------兵营行为-------------------------------------
        //获取当前各种的总数量
        int numL = countUnits(p, gs, lightType);
        int numR = countUnits(p, gs, rangedType);
        int numH = countUnits(p, gs, heavyType);
        int total = numL + numR + numH;


        //产兵顺序  这样造兵是为了保持造兵平衡
        if (barracks != 0) {
            if (numL < (numLR + 1) / 2 && p.getResources() >= lightType.cost)
                barracksBehavior(p, gs, lightType);
            else if (numR < (numRR + 1) / 2 && p.getResources() >= rangedType.cost)
                barracksBehavior(p, gs, rangedType);
            else if (numH < (numHR + 1) / 2 && p.getResources() >= heavyType.cost)
                barracksBehavior(p, gs, heavyType);
            else if (numL < total * numLR / (numRR + numLR + numHR) && p.getResources() >= lightType.cost) { //ranged太多时
                barracksBehavior(p, gs, lightType);
            } else if (numR < total * numRR / (numRR + numLR + numHR) && p.getResources() >= rangedType.cost) {
                barracksBehavior(p, gs, rangedType);
            } else if (numH < total * numHR / (numRR + numLR + numHR) && p.getResources() >= heavyType.cost)
                barracksBehavior(p, gs, heavyType);
            else
                barracksBehavior(p, gs, lightType);
        }
        //-------------------------兵营行为结束-----------------------------------


        //------------------------除工兵以外其他兵行为------------------------------

        //重新统计兵力情况
        numL = countUnits(p, gs, lightType);
        numR = countUnits(p, gs, rangedType);
        numH = countUnits(p, gs, heavyType);
        int resource = 0;
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == resourceType) {
                resource++;
            }
        }

        if (resource == 0) {
            for (Unit u : pgs.getUnits()) {
                if (u.getType().canAttack && u.getPlayer() == p.getID()) {
                    if (!u.getType().canHarvest) {
                        meleeCloseBehavior(u, p, gs);
                    } else {
                        harvestWorkerBehavior(u, p, gs);
                    }
                }
            }
        } else {
            for (Unit u : pgs.getUnits()) {
                if (u.getType().canAttack && !u.getType().canHarvest && u.getPlayer() == p.getID())
                    meleeCloseBehavior(u, p, gs);
            }
        }
        //-----------------------------除工兵以外的兵种行为结束------------------------------------
    }





    public void newRush(Player p, GameState gs, int numWR, int numLR, int numHR, int numRR, int harvestNum, boolean f) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        boolean lock = true;

        //基地行为
        int workers = countUnits(p, gs, workerType);
        if (workers < numWR + harvestNum)
            baseBehavior(p, gs);

        //基地行为
        //获取敌方基地距离
        int baseDistance = Integer.MAX_VALUE;
        List<Unit> myBases = new ArrayList<>();
        Unit base = null;
        List<Unit> enemyBases = new ArrayList<>();
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == baseType) {
                if (u.getPlayer() == p.getID()) {
                    base = u;
                    myBases.add(u);
                } else {
                    enemyBases.add(u);
                }
            }
        }
        //一般都是一个基地
        for (Unit mybase : myBases) {
            for (Unit enemyBase : enemyBases) {
                baseDistance = Math.min(baseDistance, Math.abs(mybase.getX() - enemyBase.getX()) + Math.abs(mybase.getY() - enemyBase.getY()));
            }
        }

        //所有工兵数量
        List<Unit> freeWorkers = countUnitsList(p, gs, workerType);
        List<Unit> harvestWorkers = new LinkedList<>();
        int nbases = countUnits(p, gs, baseType);
        int barracks = countUnits(p, gs, barracksType);

        if (freeWorkers.isEmpty()) {
            return;
        }




        List<Integer> reservedPositions = new LinkedList<Integer>();


        int barrlocationX = 0;
        int barrlocationY = 0;


        //建基地   注意:行为不能覆盖，不能两次获取列表（可以改变行为），可以获取个数
        if (nbases == 0) {
            if (p.getResources() >= baseType.cost && !freeWorkers.isEmpty()) {
                Unit u = freeWorkers.remove(0);
                buildIfNotAlreadyBuilding(u, baseType, u.getX(), u.getY(), reservedPositions, p, pgs);
            }
        }

        int locationX = 0;
        int locationY = 0;
        //建兵营  注意:行为不能覆盖，不能两次获取列表（可以改变行为），可以获取个数
        if (barracks == 0) {
            if (p.getResources() >= barracksType.cost && !freeWorkers.isEmpty()) {
                Unit u = freeWorkers.remove(0);
                if(base != null) {
                    locationX = base.getX();
                    locationY = base.getY();
                    if(p.getID() == 0){
                        locationX += 2;
                        locationY += 2;
                    }

                    buildIfNotAlreadyBuilding(u, barracksType,  locationX, locationY, reservedPositions, p, pgs);
//                    }
                }
            }
        }

        //worker任务分类
        for (Unit u : freeWorkers) {
            if (harvestWorkers.size() < harvestNum) {
                harvestWorkers.add(u);
            } else
                meleeBehavior(u, p, gs);
        }
        harvestWorkerGroup(harvestWorkers, p, gs);
        //-------------------------以上是工兵行为---------------------------------------


        //工兵都处于等待状态则发生死锁,只要有一个工兵在运动，则死锁解除
        for (Unit worker : pgs.getUnits()) {
            if (worker.getPlayer() == p.getID() && worker.getType() == workerType) {
                UnitActionAssignment uaa = gs.getActionAssignment(worker);
                if (uaa != null && uaa.action.getType() != UnitAction.TYPE_NONE) {
                    lock = false;
//                    System.out.println("Unlock");
                    break;
                }
            }
        }

        if (lock) {
            TIME++;
            lock = TIME % anInt == anInt - 1;
        }

        //-------------------------兵营行为-------------------------------------
        //获取当前各种的总数量
        int numL = countUnits(p, gs, lightType);
        int numR = countUnits(p, gs, rangedType);
        int numH = countUnits(p, gs, heavyType);
        int total = numL + numR + numH;


        //产兵顺序  这样造兵是为了保持造兵平衡
        if (barracks != 0) {
            if (numL < (numLR + 1) / 2 && p.getResources() >= lightType.cost)
                barracksBehavior(p, gs, lightType);
            else if (numR < (numRR + 1) / 2 && p.getResources() >= rangedType.cost)
                barracksBehavior(p, gs, rangedType);
            else if (numH < (numHR + 1) / 2 && p.getResources() >= heavyType.cost)
                barracksBehavior(p, gs, heavyType);
            else if (numL < total * numLR / (numRR + numLR + numHR) && p.getResources() >= lightType.cost) { //ranged太多时
                barracksBehavior(p, gs, lightType);
            } else if (numR < total * numRR / (numRR + numLR + numHR) && p.getResources() >= rangedType.cost) {
                barracksBehavior(p, gs, rangedType);
            } else if (numH < total * numHR / (numRR + numLR + numHR) && p.getResources() >= heavyType.cost)
                barracksBehavior(p, gs, heavyType);
            else
                barracksBehavior(p, gs, lightType);

        }
        //-------------------------兵营行为结束-----------------------------------


        //------------------------除工兵以外其他兵行为------------------------------

        //重新统计兵力情况
        numL = countUnits(p, gs, lightType);
        numR = countUnits(p, gs, rangedType);
        numH = countUnits(p, gs, heavyType);
        total = numL + numR + numH;
        //判断有无矿
        int resource = 0;
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == resourceType) {
                resource++;
            }
        }
//        System.out.println(gs.getTime());
        //策略先积累兵力
        //地图资源无矿则全部进攻
        if (resource == 0) {
            for (Unit u : pgs.getUnits()) {
                if (u.getType().canAttack && u.getPlayer() == p.getID()) {
                    //非工兵
                    if (!u.getType().canHarvest) {
                        meleeCloseBehavior(u, p, gs);
                    }
                    //工兵把资源放进基地在进攻
                    else {
                        harvestWorkerBehavior(u, p, gs);
                    }
                }
            }
        }

        //死锁或者积累兵够了,或者对面基地很近就进攻，除工兵以外都进攻(flag为进攻信号)，只要进攻了就一直进攻
        else if (total >= (numRR + numLR + numHR) || f || flag || (DEFENSEDIS > baseDistance / 2) || (p.getResources() < lightType.cost && pgs.getHeight() > 128)) {
            flag = true;
            for (Unit u : pgs.getUnits()) {
                if (u.getType().canAttack && !u.getType().canHarvest && u.getPlayer() == p.getID())
                    meleeCloseBehavior(u, p, gs);
            }
        } else {
            if (lock)
                DEFENSEDIS++;
            for (Unit u : pgs.getUnits()) {
                if (u.getType().canAttack && !u.getType().canHarvest && u.getPlayer() == p.getID()) {
                    defenseBehavior(u, p, gs);
                }
            }
        }
        //-----------------------------除工兵以外的兵种行为结束------------------------------------
    }


    //按照Unit类型归类
    public List<Unit> countUnitsList(Player p, GameState gs, UnitType type) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        List<Unit> list = new LinkedList<>();
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == type && u.getPlayer() == p.getID()) {
                list.add(u);
            }
        }
        return list;
    }

    //统计特定类型的兵种数量
    public int countUnits(Player p, GameState gs, UnitType type) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        int number = 0;
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == type && u.getPlayer() == p.getID()) {
                number++;
            }
        }
        return number;
    }

    //根据可攻击（除工兵以外）设定防御范围
    public void defenseBehavior(Unit u, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int canAttack = 0;
        for (Unit unit : pgs.getUnits()) {
            if (unit.getType().canAttack && !unit.getType().canHarvest && p.getID() == unit.getPlayer()) {
                canAttack++;
            }
        }
        //与基地的距离,决定占位
        int heavyDefenseDistance = rangedType.attackRange + DEFENSEDIS;
        int lightseDefenseDistance = lightType.attackRange + DEFENSEDIS;
        int rangedDefenseDistance = heavyType.attackRange + DEFENSEDIS;

        //身边的距离
        int closestDistance = 0;
        int lightAttackarea = lightType.attackRange + 2;
        int heavyAttackarea = heavyType.attackRange + 2;
        int rangedAttackarea = rangedType.attackRange + 2;


        int mybase = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestEnemy == null || d < closestDistance) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            } else if (u2.getPlayer() == p.getID() && u2.getType() == baseType) {
                mybase = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
            }
        }

        if (u.getType() == heavyType) {
            if (closestEnemy != null && (closestDistance < heavyAttackarea || mybase < heavyDefenseDistance)) {
                attack(u, closestEnemy);
            } else {
                attack(u, null);
            }
        } else if (u.getType() == lightType) {
            if (closestEnemy != null && (closestDistance < lightAttackarea || mybase < lightseDefenseDistance)) {
                attack(u, closestEnemy);
            } else {
                attack(u, null);
            }
        } else if (u.getType() == rangedType) {
            if (closestEnemy != null && (closestDistance < rangedAttackarea || mybase < rangedDefenseDistance)) {
                attack(u, closestEnemy);
            } else {
                attack(u, null);
            }
        } else {
            if (closestEnemy != null && (closestDistance < 2 || mybase < 5)) {
                attack(u, closestEnemy);
            } else {
                attack(u, null);
            }
        }


    }


    public void meleeCloseBehavior(Unit u, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int closestDistance = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                //攻击非基地目标
                if ((closestEnemy == null || d < closestDistance)) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
        }
        if (closestEnemy != null) {
            attack(u, closestEnemy);
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

