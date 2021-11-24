import java.util.ArrayList;
//import java.util.HashSet;
import java.util.List;

import bwapi.*;
import bwta.BaseLocation;
import bwta.BWTA;
import bwta.Chokepoint;
import bwta.Polygon;
import bwta.Region;

import cameraModule.CameraModule;

//import BuildCommander.BuildCommander;
//import BuildCommander.BuildOrder;

public class ULSCAI extends DefaultBWListener {
    
	private Mirror mirror = new Mirror();
    private Game game;
    public Player self;
    
    private CameraModule observer;
    
    //private HashSet enemyBuildingMemory = new HashSet();
    private boolean allBasesChecked = false;
    private ArrayList<Position> enemyBuildingMemory = new ArrayList<Position>();
    private ArrayList<Base> baseCheck = new ArrayList<Base>();
    private ArrayList<BaseLocation> bases = new ArrayList<BaseLocation>();
    private BaseLocation homeLoc = null;
    private Chokepoint homeEnt = null;
    private BaseLocation enemyBaseLocation = null;
    public List<BuildOrder> aBuildOrder = new ArrayList<BuildOrder>();
    public BuildCommander aBuildCommander;
    //public SquadCommander aSquadCommander;
    //private static final int maxGasWorkersPerBuilding = 3;
    private boolean refineryBuilt = false;
    private enum Priority {Attack, Defend, Wait, Explore};
    private Priority currentPriority = Priority.Wait;
    //private enum SquadType {Infantry, Tank, Flying, Undefined};

    private Unit mainBase = null;
    private List<Unit> myBases = new ArrayList<Unit>();
    private ArrayList<Boolean> baseNeedingWorkers = new ArrayList<Boolean>();
    private Unit scout = null;
    private List<Unit> army = new ArrayList<Unit>();
    private List<Unit> medics = new ArrayList<Unit>();    
    
    public class BuildOrder {
    	String name;
    	//int priority;
    	Unit unit;
    	UnitType building;
    	int wait = 0;
    	int started = 0;
    	int minerals;
    	int gas;
    	int finished = 0;
    	
    	public BuildOrder(String orderName, UnitType type) {
    		name = orderName;
    		building = type;
    		minerals = type.mineralPrice() + 25;
    		gas = type.gasPrice();
    	}   	
    }
    public class BuildCommander {
    	int index = 0;
    	Unit builder = null;
    	BuildOrder order = null;
    	TilePosition buildTile = null;
    	List<BuildOrder> orderList;
    	boolean finished = false;
    	
    	//constructor
    	public BuildCommander(List<BuildOrder> buildList) {
    		orderList = buildList;
    	}
    	
    	public void getOrder() {
    		try {
	    		if(orderList.get(index).finished == 1) {
	    			order = null;
	    			index++;
	    		}
	    		if(orderList.get(index).started == 0 && orderList.get(index).minerals < self.minerals()) {
	    			order = orderList.get(index);
	    		}
    		} catch (NullPointerException e) {
    			System.out.println("End of List, Build Order completed");
    			//finished = true;
    		}
    	}
    	
    	public void getBuilder() {
    		//System.out.println("AWDAWDAWD START OF GETBUILDER");
    		if(builder == null) {
        		//System.out.println("AWDAWDAWD BUILDER IS NULL");
    			for(Unit myUnit : self.getUnits()) {
    				if(myUnit.getType().isWorker() && myUnit.getOrder() != Order.PlaceBuilding && myUnit.getOrder() != Order.ConstructingBuilding && myUnit != scout) {
    		    		//System.out.println("AWDAWDAWD LADIES AND GENTLEMEN WE GOT HIM");
    					order.unit = myUnit;
    					builder = myUnit;
    					break;
    				}
    	    		//System.out.println("END OF GET BUILDER FOR LOOP");
    			}
    		}
    		//System.out.println("END OF GET BUILDER IF STATEMENT");
    	}
    	
    	public void checkOrderProgress() {
    		if(order != null) {
    			if(self.getRace() == bwapi.Race.Protoss) { 
    				if(order.unit.isIdle()) {
    					order.finished = 1;
        				builder = null; 
        				//current builder is left to finish building this
        				//new builder can be selected for the next order
        				//without interrupting or overriding current building order
    				}
    			}
    			else if(order.unit.getOrder() == Order.ConstructingBuilding) {
    				order.finished = 1;
    				//Comment out later v
        			order.started = 1;
        			//comment out later ^
        			builder = null; 
    				//current builder is left to finish building this
    				//new builder can be selected for the next order
    				//without interrupting or overriding current building order
    			}
    			else if(order.unit.isGatheringMinerals()) {
    				order.started = 0;
    			}
    		}
    	}
    	
    	public void giveOrder() {
    		if(order != null && order.started == 0) {
    			order.unit = builder;
    			//order.started = 1;
    			if(builder.getOrder() != Order.ConstructingBuilding && builder.getOrder() != Order.PlaceBuilding) {
        			System.out.print("Giving order");
    				buildTile = getBuildTile(builder, order.building, self.getStartLocation());
	    				if (buildTile != null) {
	    					builder.build(order.building, buildTile);
	    					game.drawCircle(bwapi.CoordinateType.Enum.Map , builder.getX(), builder.getY(), 4, bwapi.Color.Yellow, true);
	    				}
    			}
    			
    		}
    	}
    	
    	public void executeOrder() {
    		//if(finished != true) {
	    		//System.out.println("START OF EXECUTE ORDER STEP 1");
	    		getOrder();
	    		if(order != null && order.name != "end") {
		    		//System.out.println("START OF EXECUTE ORDER STEP 2");
		    		getBuilder();
			    	//System.out.println("START OF EXECUTE ORDER STEP 3");
			    	checkOrderProgress();
			    	//System.out.println("START OF EXECUTE ORDER STEP 4");
			    	giveOrder();
			    	//System.out.println("END OF EXECUTE ORDER STEP 5");
	    		}
	    		
    		//}
    	}
    }
    
    /*
    public class Squad {
    	List<Unit> squadmates;
    	int squadsize = 12;
        private Priority currentPriority = Priority.Wait;
    	private SquadType squadType = SquadType.Undefined;
    	
    	public Squad() {
    		//squadmates = s;
    	}
    	
    	public boolean isDefeated() {
    		if(squadmates.isEmpty()) {
    			return true;
    		}
    		else {
    			return false;
    		}
    	}
    }
    public class SquadCommander {
    	
    	List<Squad> squads;
    	int squadsize = 12;
    	//Squad squad = null;
    	
    	public SquadCommander() {
    		//blank constructor
    	}
    	public void update(List<Unit> army) {
    		if (!squads.isEmpty()) {
    			for(Squad s : squads) {
    				if(!s.isDefeated()) {
    					squads.remove(s);
    				}
    			}
    			giveOrders();
    		}
    		
    	}
    	public void assignNewUnit(Unit newUnit) {
    		boolean assigned = false;
    		for(Squad s : squads) {
    			if(s.squadmates.size() < squadsize) {
  				  s.squadmates.add(newUnit);
  				  System.out.println("UNIT ASSIGNED");
  				  game.sendText(newUnit.getType().toString() + " assigned");
  				  assigned = true;
  				  break;
    			}
    		}
    		if(!assigned) {
        		Squad newSquad = new Squad();
        		newSquad.squadmates.add(newUnit);
        		squads.add(newSquad);
    		}
    		System.out.println("awdawd");
    	}
    	public void giveOrders() {
    		boolean attacking = false;
    		Unit target = null;
    		//Unit closestNonMedic = null;
	    	for(Squad squadron : squads) {
	    		if(squadron.squadmates.size() > 15 && squadron.currentPriority != Priority.Attack) {
	    			squadron.currentPriority = Priority.Attack;
	    		}
	    		
	    		//if(army.size() < 10 && currentPriority == Priority.Attack)
	    			//currentPriority = Priority.Defend;
	    		
	    		for(Unit myUnit : squadron.squadmates) {
	    			//if((closestNonMedic == null && myUnit.getType() != UnitType.Terran_Medic) ||
	    			//	(closestNonMedic != null && 
	    			//	 myUnit.getX() - enemyBaseLocation.getX() < closestNonMedic.getX() - enemyBaseLocation.getX() &&
	    			//	 myUnit.getY() - enemyBaseLocation.getY() < closestNonMedic.getY() - enemyBaseLocation.getY())) {
	    			//	closestNonMedic = myUnit;
	    			//	game.drawDotScreen(closestNonMedic.getPosition(), bwapi.Color.Red);
	    			//}
	    				
	    		}
	    		for(Unit myUnit : squadron.squadmates) {
	    			if(myUnit.getType() == UnitType.Terran_Marine) {
	    				target = null;
	    				if(squadron.currentPriority == Priority.Attack) {
	    					for(Unit enemyUnit : game.enemy().getUnits()) {
	    						if(enemyUnit.exists())
	    							if(target == null || myUnit.getDistance(enemyUnit) < myUnit.getDistance(target))
	    								target = enemyUnit;
	    					}
	    					if(target != null) {
	    						if((!myUnit.isStartingAttack()) && 
	    								//(myUnit.getDistance(target) < myUnit.getType().groundWeapon().maxRange()) &&
	    								(myUnit.getGroundWeaponCooldown() <= 0) && 
	    								(myUnit.getAirWeaponCooldown() <= 0))
	    							myUnit.attack(target, false);
	    							if (attacking == false) {
	    								attacking = true;
	    							}
	    						//else
	    							//myUnit.move(target.getPosition(), false);
	    					}
	    					else {
	    						myUnit.move(enemyBaseLocation.getPosition(), false);
	    					}
	    				}
	    				else if(squadron.currentPriority == Priority.Defend) {
	    					if(myUnit.getDistance(homeEnt.getCenter()) > 10) {
	    						myUnit.move(homeEnt.getCenter(), false);
	    					}
	    					else {
	    						myUnit.holdPosition(false);
	    					}
	    				}
	    			}
	    			else if(myUnit.getType() == UnitType.Terran_Medic) {
	    				target = null;
	    				if(squadron.currentPriority == Priority.Attack) {
	    					for(Unit checkUnit : squadron.squadmates) {
	    						if(checkUnit.getHitPoints() < target.getHitPoints() ||  checkUnit.getHitPoints() < (checkUnit.getType().maxHitPoints() * 0.8)) {
	    							target = checkUnit;
	    							System.out.println("MEDIC SHOULD HAVE A TARGET");
	    						}
	    					}
	    					if(target != null && myUnit.getSpellCooldown() <= 0 && !myUnit.isIdle()) {
	    						myUnit.useTech(TechType.Healing, target);
	    						System.out.println("MEDIC SHOULD TRY HEAL THIS TARGET");
	    					}
	    					else //if (currentPriority == Priority.Attack
	    							//&& myUnit.getX() - enemyBaseLocation.getX() < closestNonMedic.getX() - enemyBaseLocation.getX() && myUnit.getY() - enemyBaseLocation.getY() < closestNonMedic.getY() - enemyBaseLocation.getY()
	    							//){
	    						myUnit.move(enemyBaseLocation.getPosition(), false);		
	    						System.out.println("NO TARGET");
	    					//}
	    				}
	    				else if(squadron.currentPriority == Priority.Defend) {
	    					if(myUnit.getDistance(homeEnt.getCenter()) > 10) {
	    						myUnit.move(homeEnt.getCenter(), false);
	    					}
	    					else {
	    						myUnit.holdPosition(false);
	    					}
	    				}
	    			}
	    		}
	    	}
    	}
    }
    */
    
    public class Base {
    	public BaseLocation location;
    	public boolean checked;
    	public Base (BaseLocation loc) {
    		this.location = loc;
    		this.checked = false;
    	}
    	public BaseLocation getLocation() {
    		return location;
    	}
    	public boolean isChecked() {
    		return checked;
    	}
    }

    /*
    //private static void executeInCommandLine(String command) {
    	try {
    		Process process = Runtime.getRuntime().exec(command);
    	} catch (Exception err) {
    		err.printStackTrace();
    	}
    }
    */
    
	// Returns a suitable TilePosition to build a given building type near
	// specified TilePosition aroundTile, or null if not found. (builder parameter is our worker)
	@SuppressWarnings("unused")
	public TilePosition getBuildTile(Unit builder, UnitType buildingType, TilePosition aroundTile) {
		
		TilePosition ret = null;
		int maxDist = 3;
		int stopDist = 200;
		boolean hasAddon = buildingType.canBuildAddon();
		// Refinery, Assimilator, Extractor
		if (buildingType.isRefinery()) {
	 		for (Unit n : game.neutral().getUnits()) {
	 			if ((n.getType() == UnitType.Resource_Vespene_Geyser) &&
	 					( Math.abs(n.getTilePosition().getX() - aroundTile.getX()) < stopDist ) &&
	 					( Math.abs(n.getTilePosition().getY() - aroundTile.getY()) < stopDist )
	 					) return n.getTilePosition();
	 		}
	 	}
		
		else if (buildingType == UnitType.Terran_Command_Center) {
			BaseLocation newBase = null;
				for (BaseLocation b : BWTA.getBaseLocations()) {
					if(newBase == null || b.getGroundDistance(homeLoc) < newBase.getGroundDistance(homeLoc)) {
						if (game.canBuildHere(b.getTilePosition(), buildingType, builder, false)) {
		 					// units that are blocking the tile
		 					boolean unitsInWay = false;
		 					for (Unit u : game.getAllUnits()) {
		 						if (u.getID() == builder.getID()) continue;
		 						if ((Math.abs(u.getTilePosition().getX()-b.getX()) < 4) && (Math.abs(u.getTilePosition().getY()-b.getY()) < 4)) unitsInWay = true;
		 					}
		 					
		 					if (!unitsInWay) {
		 						newBase = b;
		 					}
						}
					}
				}
			if(newBase != null) {
				game.drawCircleMap(newBase.getPosition(), 40, bwapi.Color.Red);
				if(builder.getDistance(newBase.getPosition()) > 5) {
					builder.move(newBase.getPosition());
				}
				return newBase.getTilePosition();
			}
		}
		
		else {
			while ((maxDist < stopDist) && (ret == null)) {
		 		for (int i=aroundTile.getX()-maxDist; i<=aroundTile.getX()+maxDist; i++) {
		 			for (int j=aroundTile.getY()-maxDist; j<=aroundTile.getY()+maxDist; j++) {
		 				if (game.canBuildHere(new TilePosition(i,j), buildingType, builder, false)) {
		 					// units that are blocking the tile
		 					boolean unitsInWay = false;
		 					for (Unit u : game.getAllUnits()) {
		 						if (u.getID() == builder.getID()) continue;
		 						if ((Math.abs(u.getTilePosition().getX()-i) < 4) && (Math.abs(u.getTilePosition().getY()-j) < 4)) unitsInWay = true;
		 					}
		 					
		 					if (!unitsInWay) {
		 						return new TilePosition(i, j);
		 					}
		 					// creep for Zerg
		 					if (buildingType.requiresCreep()) {
		 						boolean creepMissing = false;
		 						for (int k=i; k<=i+buildingType.tileWidth(); k++) {
		 							for (int l=j; l<=j+buildingType.tileHeight(); l++) {
		 								if (!game.hasCreep(k, l)) creepMissing = true;
		 								break;
		 							}
		 						}
		 						if (creepMissing) continue;
		 					}
		 				}
		 			}
		 		}
		 		maxDist += 2;
		 	}
		}
	
	 	//if (ret == null) game.printf("Unable to find suitable build position for "+buildingType.toString());
	 	return ret;
	}
	
	public void squadCommander() {
        List<Unit> marines = new ArrayList<>();
        List<Unit> unsiegedTanks = new ArrayList<>();
        List<Unit> siegedTanks = new ArrayList<>();
        List<Unit> vultures = new ArrayList<>();
        List<Unit> goliaths = new ArrayList<>();
		for(Unit myUnit : army) {
			if(myUnit.getType() == UnitType.Terran_Medic)
				medics.add(myUnit);
			else if(myUnit.getType() == UnitType.Terran_Marine)
				marines.add(myUnit);
			else if(myUnit.getType() == UnitType.Terran_Siege_Tank_Tank_Mode)
				unsiegedTanks.add(myUnit);
			else if(myUnit.getType() == UnitType.Terran_Siege_Tank_Siege_Mode)
				siegedTanks.add(myUnit);
			else if(myUnit.getType() == UnitType.Terran_Vulture)
				vultures.add(myUnit);
			else if(myUnit.getType() == UnitType.Terran_Goliath)
				goliaths.add(myUnit);
		}
		boolean attacking = false;
		Unit target = null;
		Unit moveTarget = null;
		if((currentPriority == Priority.Explore && !enemyBuildingMemory.isEmpty()) || (army.size() + medics.size() > 25 && (currentPriority == Priority.Wait || currentPriority == Priority.Defend))) {
			currentPriority = Priority.Attack;
			game.setLocalSpeed(1);
			game.setFrameSkip(0);
		}
		else if((army.size() < 5 || army.size() < medics.size()) && currentPriority == Priority.Attack) {
			currentPriority = Priority.Defend;
			game.setLocalSpeed(0);
			game.setFrameSkip(0);
		}
		else if(currentPriority == Priority.Attack && enemyBuildingMemory.isEmpty()) {
			currentPriority = Priority.Explore;
			game.setLocalSpeed(0);
			game.setFrameSkip(0);
		}
		game.drawTextScreen(10, 25, currentPriority.toString());
		//if(army.size() < 10 && currentPriority == Priority.Attack)
			//currentPriority = Priority.Defend;
		
		//for(Unit myUnit : army) {
			//if((closestNonMedic == null && myUnit.getType() != UnitType.Terran_Medic) ||
			//	(closestNonMedic != null && 
			//	 myUnit.getX() - enemyBaseLocation.getX() < closestNonMedic.getX() - enemyBaseLocation.getX() &&
			//	 myUnit.getY() - enemyBaseLocation.getY() < closestNonMedic.getY() - enemyBaseLocation.getY())) {
			//	closestNonMedic = myUnit;
			//	game.drawDotScreen(closestNonMedic.getPosition(), bwapi.Color.Red);
			//}
				
		//}
		//attacking unit handler
		for(Unit myUnit : army) {
			target = null;
			Position targetPosition = null;
			switch(currentPriority) {
			case Attack:
				//target = selectTarget(myUnit);
				if(myUnit.getOrder() != Order.AttackMove && myUnit.getOrder() != Order.AttackUnit) {
					if(!enemyBuildingMemory.isEmpty()) {
						for(Position p : enemyBuildingMemory) {
							if(targetPosition == null || myUnit.getDistance(p) < myUnit.getDistance(targetPosition) ) {
								targetPosition = p;
							}
						}
					}
					myUnit.attack(targetPosition);
				}
				/*
				if(target != null) {
					System.out.print("TARGET NOT NULL");
					if((!myUnit.isAttacking())
							)
							//&& 
							//(myUnit.getDistance(target) < myUnit.getType().groundWeapon().maxRange()) &&
							//(myUnit.getGroundWeaponCooldown() <= 0) && 
							//(myUnit.getAirWeaponCooldown() <= 0)) 
							{
						myUnit.attack(target);
					}
				}
				else {
					myUnit.move(enemyBaseLocation.getPosition());
				}
				*(/)
				/*
				if(target != null) {
					if((!myUnit.isStartingAttack()) && 
							(myUnit.getDistance(target) < myUnit.getType().groundWeapon().maxRange()) &&
							(myUnit.getGroundWeaponCooldown() <= 0) && 
							(myUnit.getAirWeaponCooldown() <= 0)) {
						if(target.getPlayer() == self) {
							myUnit.attack(target.getPosition(), false);
						}
						else {
							myUnit.attack(target, false);
						}
					//else
						//myUnit.move(target.getPosition(), false);
					}
				}
				*/
				break;
			case Wait:
				
				BaseLocation natural = bwta.BWTA.getNearestBaseLocation(homeLoc.getPosition());
				List<Chokepoint> chokes = homeLoc.getRegion().getChokepoints();
				Chokepoint natChoke = null;
				Position mapCenter = new Position(game.mapWidth()*16, game.mapHeight()*16);
				for(Chokepoint c : chokes) {
					if(natChoke == null || c.getDistance(mapCenter) < natChoke.getDistance(mapCenter)) {
						natChoke = c;
					}
				}
				myUnit.move(mapCenter);
				break;
			case Defend:
				for(Unit u : self.getUnits()) {
					if(u.getType().isBuilding() && u.isUnderAttack()) {
						target = u;
						target.attack(target.getPosition());
					}
				}
				break;
			case Explore: 
				int maxRadius = Math.max(game.mapHeight(), game.mapWidth());
				int currentRadius = 6;
				while(currentRadius < maxRadius && targetPosition != null) {
					double doubleCurrentRadius = currentRadius * 2;
					for(int dx = -currentRadius; dx <= currentRadius && targetPosition != null; dx += doubleCurrentRadius) {
						for(int dy = -currentRadius; dy <= currentRadius && targetPosition != null; dy += doubleCurrentRadius) {
							Position potentialPosition = new Position(dx, dy);
							if(!game.isVisible(potentialPosition.toTilePosition()) 
									&& bwta.BWTA.getRegion(potentialPosition).isReachable(bwta.BWTA.getRegion(myUnit.getPosition()))
									&& targetPosition != null) {
								targetPosition = potentialPosition;
								break;
							}
						}
						
					}
				}
				if(targetPosition != null) {
					myUnit.move(targetPosition);
				}
				break;
			default: 
				break;
			}
		} //end of attacking unit handler	
		//medic handler
		for(Unit medic : medics) {
			if(medic.getTarget() != null) {
				target = selectMedicTarget(medic);
			}
			if(target != null && medic.getSpellCooldown() <= 0 && !medic.isIdle()) {
				medic.useTech(TechType.Healing, target);
				//System.out.println("MEDIC SHOULD TRY HEAL THIS TARGET");
			}
			else if(currentPriority == Priority.Attack){
				medic.attack(enemyBaseLocation.getRegion().getCenter());
			}
		} //end of medic handler
	}

	private Unit selectTarget(Unit myUnit) {
		Unit target = null;
		//int targetdps = 0;
		int targetDistance = Integer.MAX_VALUE;

		int i = 1;
		if(game.enemy().getUnits().isEmpty()) {
			System.out.println("WE SHOULD RETURN NULL");
			return null;
		}
		else {
			for(Unit enemyUnit : game.enemy().getUnits()) {
				//UnitType type = enemyUnit.getType();
				//WeaponType airWeapon = type.airWeapon();
				//WeaponType groundWeapon = type.groundWeapon();
				int distance = myUnit.getDistance(enemyUnit);
				i++;
				/*
				int dps = 0;
				if(enemyUnit.isFlying()) {
					dps = (airWeapon.damageAmount() / airWeapon.damageCooldown()) * airWeapon.medianSplashRadius();
	 			}
				else {
					dps = (groundWeapon.damageAmount() / groundWeapon.damageCooldown()) * groundWeapon.medianSplashRadius();
				}
				*/
				System.out.println("CURRENTLY LOOKING AT " + enemyUnit.getType().toString() + " with a distance of " + distance + " and some incrementer is: " + i);
				if(target.getType().toString() == "Unknown") {
					System.out.println("This bitch empty");
				}
				if(target != null || distance < targetDistance) {
					target = enemyUnit;
					//targetdps = dps;
					targetDistance = distance;
					System.out.println("found new target");
				}
			}
			System.out.println("RETURNING: " + target.getType().toString());
			game.drawLineMap(myUnit.getPosition(), target.getPosition(), bwapi.Color.Purple);
			return target;
		}
	}

	private Unit selectMedicTarget(Unit medic) {
		Unit target = null;
		int targetMissingHP = 0;
		for(Unit myUnit : army) {
			if(myUnit.isCompleted()) continue;
			int maxHP = myUnit.getInitialHitPoints();
			int currentHP = myUnit.getHitPoints();
			int missingHP = maxHP - currentHP;
			if(target != null || missingHP > targetMissingHP) {
				target = myUnit;
				targetMissingHP = missingHP;
			}
		}
		return target;
	}
	public void attackClosestUnit(Unit myUnit) {
		Unit closestEnemyUnit = null;
		if(!myUnit.isAttacking()) {
			for(Unit enemyUnit : game.enemy().getUnits()) {
				if(closestEnemyUnit == null || myUnit.getDistance(enemyUnit) < myUnit.getDistance(closestEnemyUnit)) {
					closestEnemyUnit = enemyUnit;
				}
			}
			if(closestEnemyUnit != null)
				myUnit.attack(closestEnemyUnit);
		}

	}
	public void explore(Unit myUnit) {
		for(Base b : baseCheck) {
			if(b.isChecked() == false)
				myUnit.move(b.location.getPosition());
		}
	}
	public void gatherMinerals(Unit myUnit) {
		//find the closest mineral
        Unit closestMineral = null;
        /*
        Unit base = null;
        int baseSize = 0;
        for(Unit myBase : myBases) {
        	List<Unit> inRange = myBase.getUnitsInRadius(1024);
        	for(Unit unit : inRange) {
        		if(!unit.getType().isMineralField())
        			inRange.remove(unit);
        	}
        	if(base == null || inRange.size() > baseSize) {
        		base = myBase;
        		baseSize = inRange.size();
        	}
        }
        
        for(Unit neutralUnit : game.neutral().getUnits().) {
        }
        
        */

        for(int i = 0; i < myBases.size(); i++) {
        	if(baseNeedingWorkers.get(i) == true) {
        		for(Unit u : myBases.get(i).getUnitsInRadius(300)) {
            		if(u.getType().isMineralField()) {
            			if(closestMineral == null || myUnit.getDistance(u) < myUnit.getDistance(closestMineral)) {
            				closestMineral = u;
            			}
            		}
            	}
        	}
        }
        //if a mineral patch was found, send the worker to gather it
        if(closestMineral != null && !myUnit.isGatheringMinerals()) {
            myUnit.gather(closestMineral, false);
        }
	}
	public void gatherGas(Unit myUnit) {
		//find the closest mineral
        Unit closestGas = null;
        for(Unit gasUnit : self.getUnits()) {
            if (gasUnit.getType().isRefinery()) {
                if(closestGas == null || myUnit.getDistance(gasUnit) < myUnit.getDistance(closestGas)) {
                	closestGas = gasUnit;
                }
            }
        }
        //if a mineral patch was found, send the worker to gather it
        if(closestGas != null) {
            myUnit.gather(closestGas, false);
        }
	}
    public boolean canTrain(UnitType type) {
    	int minCost = type.mineralPrice();
    	int gasCost = type.gasPrice();
    	int supplyCost = type.supplyRequired();
    	int supplyAvailable = self.supplyTotal() - self.supplyUsed();
    	if(self.minerals() > minCost && self.gas() > gasCost && supplyAvailable > supplyCost) {
    		return true;
    	}
    	else {
    		return false;
    	}
	}

	public void run() {
    	//executeInCommandLine("taskkill /f /im Starcraft.exe");
    	//executeInCommandLine("taskkill /f /im Chaoslauncher.exe");
    	//try {
    	 	//Thread.sleep(250);
    		//executeInCommandLine("F:\\SSCAI\\BWAPI\\BWAPI\\Chaoslauncher\\Chaoslauncher.exe");
    	//} catch (InterruptedException ex) {
    		
    	//}
    	
        mirror.getModule().setEventListener(this);
        mirror.startGame();
    }
    
	@Override
    public void onUnitCreate(Unit unit) {
    	System.out.println(unit.getType().toString() + " found");
    	
    }
	@Override
	public void onUnitComplete(Unit unit) {
		if(unit.getPlayer() == self) {
	
		}
		observer.moveCameraUnitCreated(unit);
	}
		
	@Override
	public void onUnitDestroy(Unit unit) {
		if(unit.getPlayer() == self) {
		}
	}
    @Override
    public void onStart() {
        game = mirror.getGame();
        self = game.self();

        observer = new CameraModule(self.getStartLocation().toPosition(), game);
        observer.toggle(); //Used to enable the observer, by default is disabled
        
        //Fastest: 42ms/frame
        //Faster: 48ms/frame
        //Fast: 56ms/frame
        //Normal: 67ms/frame
        //Slow: 83ms/frame
        //Slower: 111ms/frame
        //Slowest: 167ms/frame
        
        game.setLocalSpeed(0);
        game.setFrameSkip(0);
        //Use BWTA to analyze map
        //This may take a few minutes if the map is processed first time!
        System.out.println("Analyzing map...");
        BWTA.readMap();
        BWTA.analyze();
        System.out.println("Map data ready");
        
        int i = 0;
        for(BaseLocation baseLocation : BWTA.getBaseLocations()){
        	System.out.println("Base location #" + (++i) + ". Printing location's region polygon:");
        	for(Position position : baseLocation.getRegion().getPolygon().getPoints()){
        		System.out.print(position + ", ");
        	}
        }

        BuildOrder supply1 = new BuildOrder("supply", UnitType.Terran_Supply_Depot);
        BuildOrder supply2 = new BuildOrder("supply", UnitType.Terran_Supply_Depot);
        BuildOrder supply3 = new BuildOrder("supply", UnitType.Terran_Supply_Depot);
        BuildOrder supply4 = new BuildOrder("supply", UnitType.Terran_Supply_Depot);
        BuildOrder supply5 = new BuildOrder("supply", UnitType.Terran_Supply_Depot);
        BuildOrder supply6 = new BuildOrder("supply", UnitType.Terran_Supply_Depot);
        BuildOrder supply7 = new BuildOrder("supply", UnitType.Terran_Supply_Depot);
        BuildOrder supply8 = new BuildOrder("supply", UnitType.Terran_Supply_Depot);
        BuildOrder supply9 = new BuildOrder("supply", UnitType.Terran_Supply_Depot);
        BuildOrder supply10 = new BuildOrder("supply", UnitType.Terran_Supply_Depot);
        BuildOrder supply11 = new BuildOrder("supply", UnitType.Terran_Supply_Depot);
        BuildOrder supply12 = new BuildOrder("supply", UnitType.Terran_Supply_Depot);
        BuildOrder supply13 = new BuildOrder("supply", UnitType.Terran_Supply_Depot);
        BuildOrder supply14 = new BuildOrder("supply", UnitType.Terran_Supply_Depot);
        BuildOrder supply15 = new BuildOrder("supply", UnitType.Terran_Supply_Depot);
        BuildOrder supply16 = new BuildOrder("supply", UnitType.Terran_Supply_Depot);
        BuildOrder supply17 = new BuildOrder("supply", UnitType.Terran_Supply_Depot);
        BuildOrder supply18 = new BuildOrder("supply", UnitType.Terran_Supply_Depot);
        BuildOrder supply19 = new BuildOrder("supply", UnitType.Terran_Supply_Depot);
        BuildOrder supply20 = new BuildOrder("supply", UnitType.Terran_Supply_Depot);
        BuildOrder supply21 = new BuildOrder("supply", UnitType.Terran_Supply_Depot);
        BuildOrder barracks1 = new BuildOrder("barracks", UnitType.Terran_Barracks);
        BuildOrder barracks2 = new BuildOrder("barracks", UnitType.Terran_Barracks);
        BuildOrder barracks3 = new BuildOrder("barracks", UnitType.Terran_Barracks);
        BuildOrder barracks4 = new BuildOrder("barracks", UnitType.Terran_Barracks);
        BuildOrder refinery1 = new BuildOrder("assimilator", UnitType.Terran_Refinery);
        BuildOrder refinery2 = new BuildOrder("assimilator", UnitType.Terran_Refinery);
        BuildOrder refinery3 = new BuildOrder("assimilator", UnitType.Terran_Refinery);
        BuildOrder refinery4 = new BuildOrder("assimilator", UnitType.Terran_Refinery);
        BuildOrder refinery5 = new BuildOrder("assimilator", UnitType.Terran_Refinery);
        BuildOrder engineeringBay1 = new BuildOrder("engineeringBay", UnitType.Terran_Engineering_Bay);
        BuildOrder engineeringBay2 = new BuildOrder("engineeringBay", UnitType.Terran_Engineering_Bay);
		BuildOrder academy1 = new BuildOrder("academy", UnitType.Terran_Academy);
		BuildOrder expansion1 = new BuildOrder("expansion", UnitType.Terran_Command_Center);
		BuildOrder expansion2 = new BuildOrder("expansion", UnitType.Terran_Command_Center);
		BuildOrder expansion3 = new BuildOrder("expansion", UnitType.Terran_Command_Center);
		BuildOrder expansion4 = new BuildOrder("expansion", UnitType.Terran_Command_Center);
		BuildOrder factory1 = new BuildOrder("factory", UnitType.Terran_Factory);
		BuildOrder factory2 = new BuildOrder("factory", UnitType.Terran_Factory);
		BuildOrder factory3 = new BuildOrder("factory", UnitType.Terran_Factory);
		BuildOrder armory = new BuildOrder("armory", UnitType.Terran_Armory);
		BuildOrder starport1 = new BuildOrder("armory", UnitType.Terran_Starport);
		BuildOrder starport2 = new BuildOrder("armory", UnitType.Terran_Starport);
		BuildOrder scienceFacility1 = new BuildOrder("Science Facility", UnitType.Terran_Science_Facility);
		BuildOrder end = new BuildOrder("end", UnitType.None);

        aBuildOrder.add(supply1);
        aBuildOrder.add(barracks1);
        aBuildOrder.add(supply2);
        aBuildOrder.add(barracks2);
        aBuildOrder.add(expansion1);
        aBuildOrder.add(refinery1);
        aBuildOrder.add(academy1);
        aBuildOrder.add(supply3);
        aBuildOrder.add(engineeringBay1);
        aBuildOrder.add(supply4);
        aBuildOrder.add(factory1);
        aBuildOrder.add(engineeringBay2);
        aBuildOrder.add(supply5);
        aBuildOrder.add(starport1);
        aBuildOrder.add(supply6);
        aBuildOrder.add(scienceFacility1);
        aBuildOrder.add(barracks3);
        aBuildOrder.add(supply7);
        aBuildOrder.add(barracks4);
        aBuildOrder.add(supply8);
        aBuildOrder.add(factory2);
        aBuildOrder.add(refinery2);
        aBuildOrder.add(expansion2);
        aBuildOrder.add(supply8);
        aBuildOrder.add(armory);
        aBuildOrder.add(supply9);
        aBuildOrder.add(supply10);
        aBuildOrder.add(starport2);
        aBuildOrder.add(factory3);
        aBuildOrder.add(refinery3);
        aBuildOrder.add(supply11);
        aBuildOrder.add(supply12);
        aBuildOrder.add(supply13);
        aBuildOrder.add(supply14);
        aBuildOrder.add(supply15);
        aBuildOrder.add(supply16);
        aBuildOrder.add(supply17);
        aBuildOrder.add(supply18);
        aBuildOrder.add(supply19);
        aBuildOrder.add(supply20);
        aBuildOrder.add(supply21);
        aBuildOrder.add(expansion3);
        aBuildOrder.add(refinery4);
        aBuildOrder.add(expansion4);
        aBuildOrder.add(refinery5);
        aBuildOrder.add(end);
        
        aBuildCommander = new BuildCommander(aBuildOrder);
        
        game.sendText("Good Luck, Have Fun");
		//game.sendText("Black Sheep Wall");
		//game.sendText("Show me the money");
    	game.sendText("Power Overwhelming");
    	for (BaseLocation b : BWTA.getBaseLocations()) {
    		// If this is a possible start location,
    		if (b.isStartLocation()) {
    			bases.add(b);
    			Base base = new Base(b);
    			baseCheck.add(base);
    		}
    		
    	}
    	for(Unit myUnit : self.getUnits()) {
        	if(myUnit.getType() == UnitType.Terran_Command_Center) {
        		mainBase = myUnit;
        		if(homeLoc == null)
                	homeLoc = BWTA.getNearestBaseLocation(mainBase.getPosition());
                if(homeEnt == null)
                	homeEnt = BWTA.getNearestChokepoint(mainBase.getPosition());
        	}
    	} 
    }
    @Override
    public void onFrame() {
//System.out.println("Start of onFrame");
    	   
    	//game.setTextSize(10);
        StringBuilder units = new StringBuilder("My units:\n");
        
        myBases.clear();
        baseNeedingWorkers.clear();
        army.clear();
        medics.clear();
        
        List<Unit> workers = new ArrayList<>();
        List<Unit> minWorkers = new ArrayList<>();
        List<Unit> gasWorkers = new ArrayList<>();
        List<Unit> barracks = new ArrayList<>();
        List<Unit> factories = new ArrayList<>();
        List<Unit> starports = new ArrayList<>();

        observer.onFrame();
        
//System.out.print("1");
        
        for(Region region : BWTA.getRegions()) {
        	game.drawCircleMap(region.getCenter(), 50, bwapi.Color.Teal);
        	List<Position> points = region.getPolygon().getPoints();
        	
        	for(int i = 0; i < points.size(); i++) {
        		if(i >= points.size()) {
        			Position p1 = points.get(i);
        			Position p2 = points.get((i+1) % points.size());
        			game.drawLineMap(p1, p2, bwapi.Color.Yellow);
        		}
        	}
        	//game.drawLineMap(null, null, null);
        	//for()
        	for(Chokepoint re: region.getChokepoints()) {
        		game.drawLineMap(re.getCenter(), region.getCenter(), bwapi.Color.Teal);
        		game.drawLineMap(re.getSides().first, re.getSides().second, bwapi.Color.Yellow);
        		
        	}
        }
        
        //iterate through my units and subdivide them into groups
        boolean underAttack = false;
        for(Unit myUnit : self.getUnits()) {
        	if(myUnit.isCompleted()) {
	        	units.append(myUnit.getType()).append(" ").append(myUnit.getTilePosition()).append("\n");
	            if(myUnit.getType().isBuilding() && myUnit.isUnderAttack()) {
	            	underAttack = true;
	            }
	            if(underAttack) {
	            	currentPriority = Priority.Defend;
	            }
	            else {
	            	currentPriority = Priority.Wait;
	            }
	            if(myUnit.getOrder() != Order.Nothing) {
	            	game.drawTextMap(myUnit.getPosition().getX(), myUnit.getPosition().getY(), myUnit.getOrder().toString());
	            	if(myUnit.getOrderTargetPosition().getX() != 0 && myUnit.getOrderTargetPosition().getY() != 0) {
	            		if(myUnit.getOrder() == Order.AttackUnit) {
	            			game.drawLineMap(myUnit.getPosition(), myUnit.getOrderTargetPosition(), bwapi.Color.Red);
	            		}
	            		else {
	            			game.drawLineMap(myUnit.getPosition(), myUnit.getOrderTargetPosition(), bwapi.Color.Green);
	            		}
	            	}
	            }
	            
	        	
	        	if(myUnit.getType() == UnitType.Terran_Command_Center) {
	        		myBases.add(myUnit);
	        		int scvcount = 0;
	        		int mincount = 0;
	        		game.drawCircleMap(myUnit.getPosition(), 300, bwapi.Color.Orange);
	        		for(Unit u : myUnit.getUnitsInRadius(200)) {
	        			if(u.getType().isWorker()) {
	        				scvcount++;
	        			}
	        			if(u.getType().isMineralField()) {
	        				mincount++;
	        			}
	        		}
	        		if(scvcount/2 >= mincount) {
	        			baseNeedingWorkers.add(false);
	        		}
	        		else {
	        			baseNeedingWorkers.add(true);
	        		}
	        	}
	        	if(myUnit.getType() == UnitType.Terran_Barracks) {
	        		barracks.add(myUnit);
	        	}        
	        	if(myUnit.getType() == UnitType.Terran_Factory) {
					factories.add(myUnit);
	        	}
	        	if(myUnit.getType() == UnitType.Terran_Starport) {
	        		starports.add(myUnit);
	        	}
	        	if(myUnit.getType().isWorker()) {
	        		if(scout == null && !allBasesChecked)
	        			scout = myUnit;
	        		else if(refineryBuilt && gasWorkers.size() < 2*myBases.size()) {
	        			gasWorkers.add(myUnit);
	        		}
	        		else {
	        			minWorkers.add(myUnit);
	        		}
	        	}
	        	if(myUnit.getType() == UnitType.Terran_Marine 
	        			|| myUnit.getType() == UnitType.Terran_Vulture
	        			|| myUnit.getType() == UnitType.Terran_Siege_Tank_Tank_Mode 
	        			|| myUnit.getType() == UnitType.Terran_Siege_Tank_Siege_Mode
	        			|| myUnit.getType() == UnitType.Terran_Goliath) {
	        		army.add(myUnit);
	        	}
	        	if(myUnit.getType() == UnitType.Terran_Medic) {
	        		medics.add(myUnit);
	        	}
	        	if(myUnit.getType() == UnitType.Terran_Refinery && !myUnit.isBeingConstructed()) {
	        		refineryBuilt = true;
	        	}
        	}
        }      
//System.out.print("2");

        //SCV handling
        for(Unit worker : gasWorkers) {
        	if (!worker.isGatheringGas() && worker.getOrder() != Order.PlaceBuilding && worker.getOrder() != Order.ConstructingBuilding) {
        		gatherGas(worker);
        	}
        	else if (worker.isIdle()) {
        		gatherMinerals(worker);
        	}
        }    
//System.out.print("3");
        for(Unit worker : minWorkers) {
        	if (!worker.isGatheringMinerals() && worker.isIdle() && worker.getOrder() != Order.PlaceBuilding && worker.getOrder() != Order.ConstructingBuilding) {
        		gatherMinerals(worker);
        	}
        }
        
        if (scout != null) {
        	if(self.supplyUsed() < 18 || allBasesChecked)
        		gatherMinerals(scout);
        	else
        		explore(scout);
        }
		
//System.out.print("4");
        //gateway handling
	    for(Unit barrack : barracks) {
	    	if(barrack.getTrainingQueue().isEmpty()) {
	        	if(barrack.canTrain(UnitType.Terran_Medic, false) && canTrain(UnitType.Terran_Medic) && (medics.size() < army.size()/4 || medics.size() < 4)) {
	        		barrack.train(UnitType.Terran_Medic);
	        	}
	        	else if(canTrain(UnitType.Terran_Marine)) {
	        		barrack.train(UnitType.Terran_Marine);
	        	}
	    	}
        }
        for(Unit factory : factories) {
        	if(factory.getTrainingQueue().isEmpty()) {
	        	if(factory.canTrain(UnitType.Terran_Goliath) && canTrain(UnitType.Terran_Goliath)) {
	        		factory.train(UnitType.Terran_Goliath);
	        	}
	        	else if(factory.canBuildAddon(UnitType.Terran_Machine_Shop, false)) {
	    			factory.buildAddon(UnitType.Terran_Machine_Shop);
	        	}
	    		else if(factory.canTrain(UnitType.Terran_Siege_Tank_Tank_Mode, false) && canTrain(UnitType.Terran_Siege_Tank_Tank_Mode)){
	    			factory.train(UnitType.Terran_Siege_Tank_Tank_Mode);
	    		}
	    		else if(canTrain(UnitType.Terran_Vulture)) {
	    			factory.train(UnitType.Terran_Vulture);
	    		}
        	}
        }
        for(Unit starport : starports) {
        	if(starport.getTrainingQueue().isEmpty()) {
        		if(starport.canTrain(UnitType.Terran_Science_Vessel) && canTrain(UnitType.Terran_Science_Vessel)) {
        			starport.train(UnitType.Terran_Science_Vessel);
        		}
        	}
        }
//System.out.print("5");
        
//System.out.print("6");	
        //if there's enough minerals, train an SCV
        for(Unit myBase : myBases) {
        	if(self.minerals() >= 50 && self.supplyTotal() - self.supplyUsed() >= 2 && baseNeedingWorkers.contains(true) && myBase.isIdle()) {
            	myBase.train(UnitType.Terran_SCV);
            }
        }
       
//System.out.print("7");
        //always loop over all currently visible enemy units (even though this set is usually empty)
        for(Unit u : game.enemy().getUnits()) {
        	//if this unit is in fact a building
        	if (u.getType().isBuilding()) {
        		//check if we have it's position in memory and add it if we don't
        		if (!enemyBuildingMemory.contains(u.getPosition())) {
        			enemyBuildingMemory.add(u.getPosition());
        			System.out.println(u.getType() + " added to enemy building list");
        		}
        	}
        }
//System.out.print("8");
        //loop over all the positions that we remember
        for(Position p : enemyBuildingMemory) {
        	// compute the TilePosition corresponding to our remembered Position p
        	TilePosition tileCorrespondingToP = new TilePosition(p.getX()/32 , p.getY()/32);
        	
        	//if that tile is currently visible to us...
        	if(game.isVisible(tileCorrespondingToP)) {

        		//loop over all the visible enemy buildings and find out if at least
        		//one of them is still at that remembered position
        		boolean buildingStillThere = false;
        		for(Unit u : game.enemy().getUnits()) {
        			if((u.getType().isBuilding()) && (u.getPosition().equals(p))) {
        				if(enemyBaseLocation == null && BWTA.getNearestBaseLocation(u.getPosition()).isStartLocation()) {
        	        		enemyBaseLocation = BWTA.getNearestBaseLocation(u.getPosition());
        	        	}
        				buildingStillThere = true;
        				break;
        			}
        		}

        		//if there is no more any building, remove that position from our memory
        		if(buildingStillThere == false) {
        			enemyBuildingMemory.remove(p);
        			break;
        		}
        	}
        }
//System.out.print("9");
        int i = 0;
		for(Base b : baseCheck) {
        	if(game.isVisible(b.location.getTilePosition())) {
        		b.checked = true;
        		i++;
        	}
        }
        if(i == baseCheck.size()) 
        	allBasesChecked = true;
		
        if(homeLoc == null)
        	homeLoc = BWTA.getNearestBaseLocation(mainBase.getPosition());
        if(homeEnt == null)
        	homeEnt = BWTA.getNearestChokepoint(mainBase.getPosition());
        //draw my units on screen
        //game.drawTextScreen(10, 25, bases.toString());

        //game.drawTextScreen(10, 25, units.toString());
        aBuildCommander.executeOrder();
        //army handler
        squadCommander();

        game.drawTextScreen(10, 15, "FPS: " + Integer.toString(game.getFPS()));
        game.drawTextScreen(10, 35, "Our unit count: " + Integer.toString(self.getUnits().size()));
        game.drawTextScreen(10, 45, "Enemy Unit count: " + Integer.toString(game.enemy().getUnits().size()));
        //System.out.println("End of onFrame. FPS: " + game.getFPS() + " Frame Count: " + game.getFrameCount());
    }

	public static void main(String[] args) {
    	//executeInCommandLine("taskkill /f /im StarCraft.exe");
    	//executeInCommandLine("taskkill /f /im Chaoslauncher.exe");
    	//try {
        //    Thread.sleep(250);
        //    executeInCommandLine("C:\\SCAI\\StarCraft\\Chaoslauncher\\Chaoslauncher.exe");
        //} catch (InterruptedException ex) {
        //    // Don't do anything
        //}
        
        new ULSCAI().run();
    }
}