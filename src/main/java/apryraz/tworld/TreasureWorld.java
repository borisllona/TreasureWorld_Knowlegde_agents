package apryraz.tworld;


import java.io.IOException;

import org.sat4j.specs.*;
import org.sat4j.minisat.*;
import org.sat4j.reader.*;


/**
 * The class for the main program of the Treasure World
 **/
public class TreasureWorld {


    /**
     * This function should execute the sequence of steps stored in the file fileSteps,
     * but only up to numSteps steps. Each step must be executed with function
     * runNextStep() of the TreasureFinder agent.
     *
     * @param wDim        the dimension of world
     * @param tX          x coordinate of treasure position
     * @param tY          y coordinate of treasure position
     * @param numSteps    num of steps to perform
     * @param fileSteps   file name with sequence of steps to perform
     * @param filePirates file name with sequence of steps to perform
     **/
    public static void runStepsSequence(int wDim, int tX, int tY,
                                        int numSteps, String fileSteps, String filePirates) throws
            IOException, ContradictionException, TimeoutException {
        // Make instances of TreasureFinder agent and environment object classes
        TreasureFinder TAgent = new TreasureFinder(wDim);
        TreasureWorldEnv EnvAgent = new TreasureWorldEnv(wDim, tX, tY, filePirates);

        // Set environment object, and load list of pirate positions
        TAgent.setEnvironment(EnvAgent);

        // load list of steps into the Finder Agent
        TAgent.loadListOfSteps(numSteps, fileSteps);

        // Execute sequence of steps with the Agent
        for (int step = 0; step < numSteps; step++) {
            TAgent.runNextStep();
        }
    }

    /**
     * This function should load five arguments from the command line:
     * arg[0] = dimension of the word
     * arg[1] = x coordinate of treasure position
     * arg[2] = y coordinate of treasure position
     * arg[3] = num of steps to perform
     * arg[4] = file name with sequence of steps to perform
     * arg[5] = file name with list of pirate positions
     **/
    public static void main(String[] args) throws
            IOException, ContradictionException, TimeoutException {
        if (args.length < 5) {
            System.out.println("You must specify all arguments needed");
        } else {
            int wDim = Integer.parseInt(args[0]);
            int tX = Integer.parseInt(args[1]);
            int tY = Integer.parseInt(args[2]);
            int numSteps = Integer.parseInt(args[3]);
            String fileSteps = args[4];
            String filePirates = args[5];
            runStepsSequence(wDim,tX,tY,numSteps,fileSteps,filePirates);
        }
    }
}
