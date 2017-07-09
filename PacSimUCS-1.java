/*
** Author: Guillermo Alicea
** UCF Fall 2016 - CAP4630 - Professor Glinos
** 09/26/16
*/
import java.awt.Point;
import java.util.*;

import pacsim.FoodCell;
import pacsim.PacAction;
import pacsim.PacCell;
import pacsim.PacFace;
import pacsim.PacSim;
import pacsim.PacUtils;
import pacsim.PacmanCell;
import pacsim.WallCell;

public class PacSimUCS implements PacAction
{

	private UCS agent;

	private ArrayList<PacFace> solution = null;

	private static int i;

	public PacSimUCS( String fname )
	{
		PacSim sim = new PacSim( fname );
		sim.init(this);
	}

	public static void main( String[] args )
	{
		String fname ="";
		fname = args[ 0 ];
		new PacSimUCS( fname );
	}

	@Override
	public void init()
	{
		agent = new UCS();
		solution = null;
		i = 1;
		System.out.println();
	}

	//if solution is not null then the solution to the board will be found
	//in doUCS, otherwise the next direction in solution will be returned
	@Override
	public PacFace action( Object state )
	{
		PacCell[][] grid = (PacCell[][]) state;
		PacmanCell pc = PacUtils.findPacman( grid );

		if(pc != null && solution == null)
		{
			try
			{
				solution = agent.doUCS(grid, pc);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}

			System.out.println("\nSolution moves:");

			solution.remove(0);
		}

		if(!solution.isEmpty())
		{
			System.out.println(i++ + ": " + solution.get(0).toString());
			return solution.remove(0);
		}

		return PacFace.N;
	}
}

class UCS
{

	private PriorityQueue<State> fringe = new PriorityQueue<State>();

	private ArrayList<State> graphSearch = new ArrayList<State>();

	private State goalState = null;

	private int nodesExpanded = 0;

	private PacCell[][] startGrid;

	public ArrayList<PacFace> doUCS(PacCell[][] grid, PacmanCell pc) throws InterruptedException
	{
		State startState = new State(grid, pc);

		startGrid = startState.getGrid();

		fringe.add(startState);

		while (goalState == null)
			goalState = expand(fringe);

		System.out.println("\nNodes Expanded: " + nodesExpanded + " fringe size = " + fringe.size() + "\n");

		return createSolution(goalState);
	}

	private State expand(PriorityQueue<State> fringe)
	{
		State candidate = fringe.poll();

		State child;

		if (candidate.isSolution())
			return candidate;

		boolean visited = false;

		//search the states that we have already visited
		for (int i = graphSearch.size() - 1; i >= 0; i--)
			if (graphSearch.get(i).equals(candidate))
			{
				visited = true;
				break;
			}

		//graph search - if a state has not been visited, expand it, otherwise skip it
		if (!visited)
			graphSearch.add(candidate);
		else
			return null;

		if (++nodesExpanded % 1000 == 0)
			System.out.println("Nodes expanded: " + nodesExpanded + " fringe size = " + fringe.size());

		//add a new candidate to the fringe in each direction that pacman can make a legal move
		if(!(PacUtils.neighbor(PacFace.N, candidate.getPc(), startGrid) instanceof WallCell))
        {
			child = candidate.setChild(new State(candidate, PacFace.N, candidate.getPc()));

			fringe.add(child);
		}
        if(!(PacUtils.neighbor(PacFace.E, candidate.getPc(), startGrid) instanceof WallCell))
        {
        	child = candidate.setChild(new State(candidate, PacFace.E, candidate.getPc()));

			fringe.add(child);
        }
        if(!(PacUtils.neighbor(PacFace.S, candidate.getPc(), startGrid) instanceof WallCell))
        {
        	child = candidate.setChild(new State(candidate, PacFace.S, candidate.getPc()));

        	fringe.add(child);
        }
        if(!(PacUtils.neighbor(PacFace.W, candidate.getPc(), startGrid) instanceof WallCell))
        {
        	child = candidate.setChild(new State(candidate, PacFace.W, candidate.getPc()));

			fringe.add(child);
        }

		return null;
	}

	private ArrayList<PacFace> createSolution(State goalState)
	{
		ArrayList<State> temp1 = new ArrayList<State>();
		ArrayList<State> temp2 = new ArrayList<State>();
		ArrayList<PacFace> solution = new ArrayList<PacFace>();

		//get our solution path (reversed)
		while (goalState != null)
		{
			temp1.add(goalState);
			goalState = goalState.getParent();
		}

		//flip our solution path so we have start - finish
		for(int i = temp1.size() - 1; i >= 0; i--)
			temp2.add(temp1.get(i));

		int size2 = temp2.size();

		System.out.println("Solution Path:");

		//print our solution's pc locations
		for(int i = 0; i < size2; i++)
			System.out.println(i + ": ( " + temp2.get(i).getPc().getX() + ","
					+ temp2.get(i).getPc().getY() + " )");

		//get directions of solution path
		for(int i = 0; i < size2; i++)
			solution.add(temp2.get(i).getDirection());

		return solution;
	}
}

class State implements Comparable<State>
{

	//characteristics of each state
	private static PacCell[][] grid;

	private PacCell pc;

	private PacFace direction;

	private ArrayList<State> children = new ArrayList<State>(4);

	private ArrayList<Point> foodPellets = new ArrayList<Point>();

	private State parent = null;

	private int foodCount;

	private int cost;

	//constructor for every state after our start state.
	public State(State previous, PacFace direction, PacCell pc)
	{
		this.pc = newPc(pc, direction);

		this.foodCount = previous.getFoodCount();

		this.cost = previous.getCost() + 1;

		this.direction = direction;

		this.foodPellets = previous.getfoodPellets();

		Point food = PacUtils.nearestFood(this.pc.getLoc(), grid.clone());

		if (PacUtils.food(this.pc.getX(), this.pc.getY(), grid) && !foodPellets.contains(this.pc.getLoc()))
		{
			--this.foodCount;
			foodPellets.add(this.pc.getLoc());
		}

		this.parent = previous;

	}

	//Start state constructor
	public State(PacCell[][] grid, PacCell pc)
	{
		State.grid = grid;

		this.pc = pc.clone();

		this.direction = PacFace.E;

		this.foodCount = PacUtils.numFood(grid);

		this.cost = 0;
	}

	//this is essentially our "movePacman" function without actually changing our grid
	private PacCell newPc(PacCell pc, PacFace direction)
	{
		if (direction == PacFace.N)
		{
			return new PacCell(pc.getX(), pc.getY() - 1);
        }
        else if (direction == PacFace.E)
		{
        	return new PacCell(pc.getX() + 1, pc.getY());
		}
        else if (direction == PacFace.S)
		{
        	return new PacCell(pc.getX(), pc.getY() + 1);
		}
        else if (direction == PacFace.W)
		{
        	return new PacCell(pc.getX() - 1, pc.getY());
		}

		return null;
	}

	public PacFace getDirection()
	{
		return direction;
	}

	public int getCost()
	{
		return cost;
	}

	public State getParent()
	{
		return parent;
	}

	//set the next state
	public State setChild(State child)
	{
		this.children.add(child);
		return child;
	}

	//get the next state in this state's solution path
	public ArrayList<State> getChildren()
	{
		return children;
	}

	public PacCell getPc()
	{
		return pc;
	}

	public int getFoodCount()
	{
		return foodCount;
	}

	public PacCell[][] getGrid()
	{
		return grid;
	}

	//clone of food pellets visited by the state
	public ArrayList<Point> getfoodPellets()
	{
		ArrayList<Point> clone = new ArrayList<Point>();

		for (Point f : this.foodPellets)
			clone.add(f);

		return clone;
	}

	//goal test
	public boolean isSolution()
	{
		return (this.foodCount <= 0);
	}

	//used in graph search to determine equivalence
	public boolean equals(State o)
	{
		if (o.getPc().getLoc().equals(this.pc.getLoc()) && this.foodPellets.containsAll(o.getfoodPellets()))
			return true;

		return false;
	}

	//fringe ordering
	@Override
	public int compareTo(State o)
	{
		if (o.getCost() > this.cost)
			return -1;

		return 1;
	}
}
