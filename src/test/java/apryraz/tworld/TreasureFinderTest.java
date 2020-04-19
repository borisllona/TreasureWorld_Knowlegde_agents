package apryraz.tworld;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import static java.lang.System.exit;

import org.sat4j.core.VecInt;
import org.sat4j.specs.*;
import org.sat4j.minisat.*;
import org.sat4j.reader.*;


import apryraz.tworld.*;

import static org.junit.Assert.assertEquals;
import org.junit.*;

/**
*  Class for testing the TreasureFinder agent
**/
public class TreasureFinderTest {


 /**
 *  This function should execute the next step of the agent, and the assert
 *  whether the resulting state is equal to the targetState
 *
 *  @param tAgent       TreasureFinder agent
 *  @param targetState  the state that should be equal to the resulting state of
 *                      the agent after performing the next step
 **/
  public void testMakeSimpleStep(  TreasureFinder tAgent,
                                   TFState targetState )  throws
                                           IOException,  ContradictionException, TimeoutException {
    // Check (assert) whether the resulting state is equal to
    //  the targetState after performing action runNextStep with bAgent

      tAgent.runNextStep();
      Assert.assertTrue(targetState.equals(tAgent.getState()));
  }


/**
*  Read an state from the current position of the file trough the
*  BufferedReader object
*
*  @param br BufferedReader object interface to the opened file of states
*  @param wDim dimension of the world
**/
  public TFState readTargetStateFromFile( BufferedReader br, int wDim )  throws
          IOException {
       TFState tfstate = new TFState(wDim);
       String row;
       String[] rowvalues;

       for (int i = wDim; i >= 1; i--) {
           row = br.readLine();
           rowvalues = row.split(" ");
           for (int j = 1; j <= wDim; j++) {
              tfstate.set(i,j,rowvalues[j-1]);
           }
        }
        return tfstate;
  }

/**
*  Load a sequence of states from a file, and return the list
*
*  @param wDim        dimension of the world
*  @param numStates   num of states to read from the file
*  @param statesFile file name with sequence of target states, that should
*                      be the resulting states after each movement in fileSteps
*
*  @return returns an ArrayList of TFState with the resulting list of states
**/
  ArrayList<TFState> loadListOfTargetStates( int wDim, int numStates, String statesFile ) {

     ArrayList<TFState> listOfStates = new ArrayList<TFState>(numStates);

     try {
         BufferedReader br = new BufferedReader(new FileReader(statesFile));
         String row;

         // steps = br.readLine();
         for (int s = 0; s < numStates ; s++ ) {
           listOfStates.add(readTargetStateFromFile(br,wDim));
            // Read a blank line between states
            row = br.readLine();
         }
         br.close();
     } catch (FileNotFoundException ex) {
         System.out.println("MSG.   => States file not found");
         exit(1);
     } catch (IOException ex) {
         Logger.getLogger(TreasureFinderTest.class.getName()).log(Level.SEVERE, null, ex);
         exit(2);
     }

     return listOfStates;
  }


  /**
  *   This function should run the sequence of steps stored in the file fileSteps,
  *   but only up to numSteps steps.
  *
  *   @param wDim the dimension of world
  *   @param tX x coordinate of Treasure position
  *   @param tY y coordinate of Treasure position
  *   @param numSteps num of steps to perform
  *   @param fileSteps file name with sequence of steps to perform
  *   @param fileStates file name with sequence of target states, that should
  *                      be the resulting states after each movement in fileSteps
  *   @param filePirates
  *
  **/
  public void testMakeSeqOfSteps( int wDim, int tX, int tY,
                                  int numSteps, String fileSteps, String fileStates,
                                  String filePirates  ) 
       throws   IOException,  ContradictionException, TimeoutException {
      // You should make TreasureFinder and TreasureWorldEnv objects to  test.
      // Then load sequence of target states, load sequence of steps into the bAgent
      // and then test the sequence calling testMakeSimpleStep once for each step.
     TreasureFinder TAgent = new TreasureFinder(wDim);
     // load information about the World into the EnvAgent
     TreasureWorldEnv EnvAgent = new TreasureWorldEnv(wDim,tX,tY,filePirates);
     // Load list of states
     ArrayList<TFState> seqOfStates = loadListOfTargetStates(wDim,numSteps,fileStates);

     // Set environment agent and load list of steps into the agent
     TAgent.loadListOfSteps(  numSteps, fileSteps ) ;
     TAgent.setEnvironment( EnvAgent );

     // Test here the sequence of steps and check the resulting states with the
     // ones in seqOfStates
      for (int i = 0; i < numSteps; i++) { testMakeSimpleStep(TAgent,seqOfStates.get(i)); }
  }

    /**
     * The propouse of this test is to check that the solver is
     * working correctly regardless of the rest of the program.
     * Also it was useful during the development to understand better the way
     * ISolver it works.
     *
     * @throws ContradictionException it must be included when adding clauses to a solver,
     *                           it prevents from inserting contradictory clauses in the formula.
     * @throws TimeoutException      needed for solver.isSatisfiable method, its thrown if
     *                               exceeds the timeout.
     **/
    @Test public void testSolver1()   throws ContradictionException, TimeoutException {
        ISolver solver = SolverFactory.newDefault();
        solver.newVar(2);
        solver.setTimeout(3600);

        VecInt clause1 = new VecInt();
        clause1.insertFirst(1);
        clause1.insertFirst(2);
        solver.addClause(clause1);
        Assert.assertTrue(solver.isSatisfiable());

        VecInt clause2 = new VecInt();
        clause2.insertFirst(-1);
        clause2.insertFirst(-2);
        solver.addClause(clause2);
        Assert.assertFalse(solver.isSatisfiable());
    }

    /**
     * The propouse of this test is to check that the solver is
     * working correctly regardless of the rest of the program.
     * Also it was useful during the development to understand better the way
     * ISolver it works.
     *
     * @throws ContradictionException it must be included when adding clauses to a solver,
     *                           it prevents from inserting contradictory clauses in the formula.
     * @throws TimeoutException      needed for solver.isSatisfiable method, its thrown if
     *                               exceeds the timeout.
     **/
    @Test public void testSolver2()   throws ContradictionException, TimeoutException {
        ISolver solver = SolverFactory.newDefault();
        solver.newVar(2);
        solver.setTimeout(3600);

        VecInt clause1 = new VecInt();
        clause1.insertFirst(-1);
        clause1.insertFirst(2);
        solver.addClause(clause1);
        Assert.assertTrue(solver.isSatisfiable());

        VecInt clause2 = new VecInt();
        clause2.insertFirst(1);
        clause2.insertFirst(-2);
        solver.addClause(clause2);
        Assert.assertFalse(solver.isSatisfiable());
    }

  /**
  * Tests the specific configuration of: "steps1.txt" , "states1.txt", "pirates1.txt"
   *                        4x4 world, Treasure at 3,3 and 5 steps.
   *
   * @throws IOException            Signals that an I/O exception of some sort has occurred.
   * @throws ContradictionException it must be included when adding clauses to a solver,
   *                           it prevents from inserting contradictory clauses in the formula.
   * @throws TimeoutException       needed for solver.isSatisfiable method, its thrown if
   *                                exceeds the timeout.
  **/
  @Test public void TWorldTest1()   throws
          IOException,  ContradictionException, TimeoutException {
   // Example test for 4x4 world , Treasure at 3,3 and 5 steps
    testMakeSeqOfSteps(  4, 3, 3, 5, "tests/steps1.txt", "tests/states1.txt", "tests/pirates1.txt"  );
  }

    /**
     * Tests the specific configuration of: "steps1.txt" , "states1.txt", "pirates1.txt"
     *                        4x4 world, Treasure at 3,3 and 5 steps.
     *
     * @throws IOException            Signals that an I/O exception of some sort has occurred.
     * @throws ContradictionException it must be included when adding clauses to a solver,
     *                           it prevents from inserting contradictory clauses in the formula.
     * @throws TimeoutException       needed for solver.isSatisfiable method, its thrown if
     *                                exceeds the timeout.
     **/
    @Test public void TWorldTest2()   throws
            IOException,  ContradictionException, TimeoutException {
        // Example test for 4x4 world , Treasure at 3,3 and 5 steps
        testMakeSeqOfSteps(  6, 4, 4, 5, "tests/steps2.txt", "tests/states2.txt", "tests/pirates2.txt"  );
    }

    /**
     * Tests the specific configuration of: "steps1.txt" , "states1.txt", "pirates1.txt"
     *                        4x4 world, Treasure at 3,3 and 5 steps.
     *
     * @throws IOException            Signals that an I/O exception of some sort has occurred.
     * @throws ContradictionException it must be included when adding clauses to a solver,
     *                           it prevents from inserting contradictory clauses in the formula.
     * @throws TimeoutException       needed for solver.isSatisfiable method, its thrown if
     *                                exceeds the timeout.
     **/
    @Test public void TWorldTest3()   throws
            IOException,  ContradictionException, TimeoutException {
        // Example test for 4x4 world , Treasure at 3,3 and 5 steps
        testMakeSeqOfSteps(  7, 5, 4, 7, "tests/steps3.txt", "tests/states3.txt", "tests/pirates3.txt"  );
    }

    /**
     * Tests the specific configuration of: "steps1.txt" , "states1.txt", "pirates1.txt"
     *                        4x4 world, Treasure at 3,3 and 5 steps.
     *
     * @throws IOException            Signals that an I/O exception of some sort has occurred.
     * @throws ContradictionException it must be included when adding clauses to a solver,
     *                           it prevents from inserting contradictory clauses in the formula.
     * @throws TimeoutException       needed for solver.isSatisfiable method, its thrown if
     *                                 exceeds the timeout.
     **/
    @Test public void TWorldTest4()   throws
            IOException,  ContradictionException, TimeoutException {
        // Example test for 4x4 world , Treasure at 3,3 and 5 steps
        testMakeSeqOfSteps(  8, 3, 7, 6, "tests/steps4.txt", "tests/states4.txt", "tests/pirates4.txt"  );
    }

}
