

package apryraz.tworld;

import java.util.ArrayList;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static java.lang.System.exit;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sat4j.core.VecInt;

import org.sat4j.specs.*;
import org.sat4j.minisat.*;
import org.sat4j.reader.*;


/**
 * This agent performs a sequence of movements, and after each
 * movement it "senses" from the evironment the resulting position
 * and then the outcome from the smell sensor, to try to locate
 * the position of Treasure
 **/
public class TreasureFinder {


    /**
     * The list of steps to perform
     **/
    ArrayList<Position> listOfSteps;
    /**
     * index to the next movement to perform, and total number of movements
     **/
    int idNextStep, numMovements;
    /**
     * Array of clauses that represent conclusiones obtained in the last
     * call to the inference function, but rewritten using the "past" variables
     **/
    ArrayList<VecInt> futureToPast = null;
    /**
     * the current state of knowledge of the agent (what he knows about
     * every position of the world)
     **/
    TFState tfstate;
    /**
     * The object that represents the interface to the Treasure World
     **/
    TreasureWorldEnv EnvAgent;
    /**
     * SAT solver object that stores the logical boolean formula with the rules
     * and current knowledge about not possible locations for Treasure
     **/
    ISolver solver;
    /**
     * Agent position in the world and variable to record if there is a pirate
     * at that current position
     **/
    int agentX, agentY, pirateFound;
    /**
     * Dimension of the world and total size of the world (Dim^2)
     **/
    int WorldDim, WorldLinealDim;

    /**
     * This set of variables CAN be use to mark the beginning of different sets
     * of variables in your propositional formula (but you may have more sets of
     * variables in your solution).
     **/
    int TreasurePastOffset;
    int TreasureFutureOffset;
    int DetectorOffset;
    int actualLiteral;


    /**
     * The class constructor must create the initial Boolean formula with the
     * rules of the Treasure World, initialize the variables for indicating
     * that we do not have yet any movements to perform, make the initial state.
     *
     * @param WDim the dimension of the Treasure World
     **/
    public TreasureFinder(int WDim) {

        WorldDim = WDim;
        WorldLinealDim = WorldDim * WorldDim;

        try {
            solver = buildGamma();
        } catch (IOException | ContradictionException ex) {
            Logger.getLogger(TreasureFinder.class.getName()).log(Level.SEVERE, null, ex);
        }
        numMovements = 0;
        idNextStep = 0;
        System.out.println("STARTING TREASURE FINDER AGENT...");


        tfstate = new TFState(WorldDim);  // Initialize state (matrix) of knowledge with '?'
        tfstate.printState();
    }

    /**
     * Store a reference to the Environment Object that will be used by the
     * agent to interact with the Treasure World, by sending messages and getting
     * answers to them. This function must be called before trying to perform any
     * steps with the agent.
     *
     * @param environment the Environment object
     **/
    public void setEnvironment(TreasureWorldEnv environment) {
        EnvAgent = environment;
    }


    /**
     * Load a sequence of steps to be performed by the agent. This sequence will
     * be stored in the listOfSteps ArrayList of the agent.  Steps are represented
     * as objects of the class Position.
     *
     * @param numSteps  number of steps to read from the file
     * @param stepsFile the name of the text file with the line that contains
     *                  the sequence of steps: x1,y1 x2,y2 ...  xn,yn
     **/
    public void loadListOfSteps(int numSteps, String stepsFile) {
        String[] stepsList;
        String steps = ""; // Prepare a list of movements to try with the FINDER Agent
        try {
            BufferedReader br = new BufferedReader(new FileReader(stepsFile));
            System.out.println("STEPS FILE OPENED ...");
            steps = br.readLine();
            br.close();
        } catch (FileNotFoundException ex) {
            System.out.println("MSG.   => Steps file not found");
            exit(1);
        } catch (IOException ex) {
            Logger.getLogger(TreasureFinder.class.getName()).log(Level.SEVERE, null, ex);
            exit(2);
        }
        stepsList = steps.split(" ");
        listOfSteps = new ArrayList<Position>(numSteps);
        for (int i = 0; i < numSteps; i++) {
            String[] coords = stepsList[i].split(",");
            listOfSteps.add(new Position(Integer.parseInt(coords[0]), Integer.parseInt(coords[1])));
        }
        numMovements = listOfSteps.size(); // Initialization of numMovements
        idNextStep = 0;
    }

    /**
     * Returns the current state of the agent.
     *
     * @return the current state of the agent, as an object of class TFState
     **/
    public TFState getState() {
        return tfstate;
    }

    /**
     * Execute the next step in the sequence of steps of the agent, and then
     * use the agent sensor to get information from the environment. In the
     * original Treasure World, this would be to use the Smelll Sensor to get
     * a binary answer, and then to update the current state according to the
     * result of the logical inferences performed by the agent with its formula.
     **/
    public void runNextStep() throws
            IOException, ContradictionException, TimeoutException {
        pirateFound = 0;
        // Add the conclusions obtained in the previous step
        // but as clauses that use the "past" variables
        addLastFutureClausesToPastClauses();

        // Ask to move, and check whether it was successful
        // Also, record if a pirate was found at that position
        processMoveAnswer(moveToNext());


        // Next, use Detector sensor to discover new information
        processDetectorSensorAnswer(DetectsAt());
        // If a pirate was found at new agent position, ask question to
        // pirate and process Answer to discover new information
        if (pirateFound == 1) { /*COMO ENCUENTRA AL PIRATA????????????????????*/
            processPirateAnswer(IsTreasureUpOrDown());
        }

        // Perform logical consequence questions for all the positions
        // of the Treasure World
        performInferenceQuestions();
        tfstate.printState();      // Print the resulting knowledge matrix
    }


    /**
     * Ask the agent to move to the next position, by sending an appropriate
     * message to the environment object. The answer returned by the environment
     * will be returned to the caller of the function.
     *
     * @return the answer message from the environment, that will tell whether the
     * movement was successful or not.
     **/
    public AMessage moveToNext() {
        Position nextPosition;

        if (idNextStep < numMovements) {
            nextPosition = listOfSteps.get(idNextStep);
            idNextStep = idNextStep + 1;
            return moveTo(nextPosition.x, nextPosition.y);
        } else {
            System.out.println("NO MORE steps to perform at agent!");
            return (new AMessage("NOMESSAGE", "", "","")); //par3 = ""
        }
    }

    /**
     * Use agent "actuators" to move to (x,y)
     * We simulate this why telling to the World Agent (environment)
     * that we want to move, but we need the answer from it
     * to be sure that the movement was made with success
     *
     * @param x horizontal coordinate of the movement to perform
     * @param y vertical coordinate of the movement to perform
     * @return returns the answer obtained from the environment object to the
     * moveto message sent
     **/
    public AMessage moveTo(int x, int y) {
        // Tell the EnvironmentAgentID that we want  to move
        AMessage msg, ans;

        msg = new AMessage("moveto", (new Integer(x)).toString(), (new Integer(y)).toString(), "");
        ans = EnvAgent.acceptMessage(msg);
        System.out.println("FINDER => moving to : (" + x + "," + y + ")");

        return ans;
    }

    /**
     * Process the answer obtained from the environment when we asked
     * to perform a movement
     *
     * @param moveans the answer given by the environment to the last move message
     **/
    public void processMoveAnswer(AMessage moveans) {
        if (moveans.getComp(0).equals("movedto")) {
            agentX = Integer.parseInt(moveans.getComp(1));
            agentY = Integer.parseInt(moveans.getComp(2));
            pirateFound = Integer.parseInt(moveans.getComp(3));
            System.out.println("FINDER => moved to : (" + agentX + "," + agentY + ")" + " Pirate found : " + pirateFound);
        }
    }

    /**
     * Send to the environment object the question:
     * "Does the detector sense something around(agentX,agentY) ?"
     *
     * @return return the answer given by the environment
     **/
    public AMessage DetectsAt() {
        AMessage msg, ans;

        msg = new AMessage("detectsat", (new Integer(agentX)).toString(),
                (new Integer(agentY)).toString(), "");
        ans = EnvAgent.acceptMessage(msg);
        System.out.println("FINDER => detecting at : (" + agentX + "," + agentY + ")");
        return ans;
    }


    /**
     * Process the answer obtained for the query "Detects at (x,y)?"
     * by adding the appropriate evidence clause to the formula
     *
     * @param ans message obtained to the query "Detects at (x,y)?".
     *            It will a message with three fields: [0,1,2,3] x y
     **/
    public void processDetectorSensorAnswer(AMessage ans) throws
            IOException, ContradictionException, TimeoutException {

        int x = Integer.parseInt(ans.getComp(1));
        int y = Integer.parseInt(ans.getComp(2));
        String detects = ans.getComp(0);

        // Call your function/functions to add the evidence clauses
        // to Gamma to then be able to infer new NOT possible positions
        addDetectorEvidenceClauses(x,y,detects);

    }

    /** We need to add the clauses reggarding to the evidences we get from the metal detector.
     *
     * @param x coordinate for the possition x
     * @param y coordinate for the possition y
     * @param detects it correspounds to the range detected by the agent, will be between
     *                the ranges: 0,1,2,3.
     */
    private void addDetectorEvidenceClauses(int x, int y, String detects) throws ContradictionException{
        System.out.println("Detector returned: " + detects);
        System.out.println("Inserting detector evidence");
        switch (detects){
            case "1":
                for (int i = 0; i < WorldDim; i++) {
                    for (int j = 0; j < WorldDim; j++) {
                        if(x!=i && y!=j){
                            addClause(x,y,-1,TreasureFutureOffset);
                        }
                    }
                }
            case "2":
                for (int i = 0; i < WorldDim; i++) {
                    for (int j = 0; j < WorldDim; j++) {
                        if(Math.abs(i-x)>1 || Math.abs(j-y)>1){
                            addClause(x,y,-1,TreasureFutureOffset);
                        }
                    }
                }
            case "3":
                for (int i = 0; i < WorldDim; i++) {
                    for (int j = 0; j < WorldDim; j++) {
                        if(Math.abs(i-x)>2 || Math.abs(j-y)>2){
                            addClause(x,y,-1,TreasureFutureOffset);
                        }
                    }
                }
            case "0":
                for (int i = 0; i < WorldDim; i++) {
                    for (int j = 0; j < WorldDim; j++) {
                        if(Math.abs(i-x)>=3 || Math.abs(j-y)>=3){
                            addClause(x,y,-1,TreasureFutureOffset);
                        }
                    }
                }
        }
    }

    /**
     * It models the information using solver's vector and adds it to the solver.
     *
     * @param x coordinate of the possition x
     * @param y coordinate for the possition y
     * @param sign it indicates the sign of an specific literal, may be -1 or 1
     * @param offset it is the offset that correspounds to the subset of variables
     *               that contains that literal.
     * @throws ContradictionException it must be included when adding clauses to a solver,
     * it prevents from inserting contradictory clauses in the formula.
     */
    private void addClause(int x, int y, int sign, int offset) throws ContradictionException {
        int lc;
        VecInt evidence = new VecInt();

        if(sign == -1){
            lc = -(coordToLineal(x,y,offset));
        }else{
            lc = coordToLineal(x,y,offset);
        }
        evidence.insertFirst(lc);
        solver.addClause(evidence);

    }

    /**
     * Send to the pirate (using the environment object) the question:
     * "Is the treasure up or down of (agentX,agentY)  ?"
     *
     * @return return the answer given by the pirate
     **/
    public AMessage IsTreasureUpOrDown() {
        AMessage msg, ans;

        msg = new AMessage("treasureup", (new Integer(agentX)).toString(),
                (new Integer(agentY)).toString(), "");
        ans = EnvAgent.acceptMessage(msg);
        System.out.println("FINDER => checking treasure up of : (" + agentX + "," + agentY + ")");
        return ans;
    }

    /**
     * We need to add the clauses reggarding to the evidences we get from the pirate.
     *
     * @param ans message obtained from the query to the pirate.
     * @throws ContradictionException it must be included when adding clauses to a solver,
     * it prevents from inserting contradictory clauses in the formula.
     */
    public void processPirateAnswer(AMessage ans) throws ContradictionException{
        int y = Integer.parseInt(ans.getComp(2));
        String isup = ans.getComp(0);
        //DETECTOR OFFSET I PIRATE OFFSET?
        if(isup.equals("yes")){
            for (int i = 1; i <= WorldDim; i++) {
                for (int j = y; j > 0; j--) {
                    addClause(i,j,-1,TreasureFutureOffset);
                }
            }
        }else{
            for (int i = 1; i <= WorldDim; i++) {
                for (int j = y+1; j <= WorldDim; j++) {
                    addClause(i,j,-1,TreasureFutureOffset);
                }
            }
        }
    }


    /**
     * This function should add all the clauses stored in the list
     * futureToPast to the formula stored in solver.
     * Use the function addClause( VecInt ) to add each clause to the solver
     **/
    public void addLastFutureClausesToPastClauses() throws IOException,
            ContradictionException, TimeoutException {
        if(futureToPast != null){
            for(VecInt v: futureToPast){
                solver.addClause(v);
            }
        }

    }

    /**
     * This function should check, using the future variables related
     * to possible positions of Treasure, whether it is a logical consequence
     * that Treasure is NOT at certain positions. This should be checked for all the
     * positions of the Treasure World.
     * The logical consequences obtained, should be then stored in the futureToPast list
     * but using the variables corresponding to the "past" variables of the same positions
     * <p>
     * An efficient version of this function should try to not add to the futureToPast
     * conclusions that were already added in previous steps, although this will not produce
     * any bad functioning in the reasoning process with the formula.
     *
     * @throws TimeoutException needed for solver.isSatisfiable method, its thrown if
     *                          exceeds the timeout.
     **/
    public void performInferenceQuestions() throws IOException,
            ContradictionException, TimeoutException {
        futureToPast = new ArrayList<>();
        for (int i = 0; i < WorldDim; i++) {
            for (int j = 0; j < WorldDim; j++) {
                int index = coordToLineal(i,j,TreasureFutureOffset);
                int indexPast = coordToLineal(i,j,TreasurePastOffset);
                VecInt positiveVar = new VecInt();
                positiveVar.insertFirst(index);

                //It checks if Γ + positiveVar it is unsatisfiable
                //Then it adds the conclusion to the list but regarding to variables from the past
                if(!(solver.isSatisfiable(positiveVar))){
                    VecInt past = new VecInt();
                    past.insertFirst(-(indexPast));
                    futureToPast.add(past);
                    tfstate.set(i,j,"X");
                }
            }
        }
    }

    /**
     * This function builds the initial logical formula of the agent and stores it
     * into the solver object.
     *
     * @return returns the solver object where the formula has been stored
     **/
    public ISolver buildGamma() throws UnsupportedEncodingException,
            FileNotFoundException, IOException, ContradictionException {
        int totalNumVariables;

        // You must set this variable to the total number of boolean variables
        // in your formula Gamma
        // totalNumVariables =  ???????????????????
        solver = SolverFactory.newDefault();
        solver.setTimeout(3600);
        solver.newVar(totalNumVariables);
        // This variable is used to generate, in a particular sequential order,
        // the variable indentifiers of all the variables
        actualLiteral = 1;

        // call here functions to add the differen sets of clauses
        // of Gamma to the solver object
        pastState(); //Treasure state t-1, 1 clause [(1,1)-(n,n)]
        futureState(); //Treasure state t+1, 1 clause [(1,1)-(n,n)]
        pastTofutureState(); //Treasure state t-1 to Treasure state t+1

        detectorClauses(); //Implications from the metal sensor
        pirateClauses();

        notInInitialPos(); //Implicates that the treasure is not in the initial position

        return solver;
    }

    /**
     * We need to save the id for the first variable of detector set variables
     */
    private void pirateClauses() {
    }

    private void detectorClauses() {
    }

    /**
     *It add a clause to the solver that implies that the treasure must be
     * in a position considering past information.
     *
     * @throws ContradictionException it must be included when adding clauses to a solver,
     * it prevents from inserting contradictory clauses in the formula.
     *
     **/
    private void pastState() throws ContradictionException {
        // VecInt its the vector that use the solver for primitive integers.
        VecInt pastInf = new VecInt();
        TreasurePastOffset = actualLiteral;
        for (int i = 0; i < WorldLinealDim; i++) {
            pastInf.insertFirst(actualLiteral);
            actualLiteral+=1;
        }
        solver.addClause(pastInf);
    }

    /**
     *It add a clause to the solver that implies that the treasure must be
     * in a position considering future information.
     *
     * @throws ContradictionException it must be included when adding clauses to a solver,
     * it prevents from inserting contradictory clauses in the formula.
     *
     **/
    private void futureState() throws ContradictionException {

        VecInt futureInf = new VecInt();
        TreasurePastOffset = actualLiteral;
        for (int i = 0; i < WorldLinealDim; i++) {
            futureInf.insertFirst(actualLiteral);
            actualLiteral+=1;
        }
        solver.addClause(futureInf);
    }

    /**Adds the clauses which say that if we have concluded that the Treasure was not in an specific
     * location, we have to keep these conclusions and those will still be true in the future
     *
     * @throws ContradictionException it must be included when adding clauses to a solver,
     * it prevents from inserting contradictory clauses in the formula.
     */

    private void pastTofutureState() throws  ContradictionException{
        for (int i = 0; i < WorldLinealDim ; i++) {
            VecInt clause = new VecInt();
            clause.insertFirst(i+1);
            clause.insertFirst(-(TreasureFutureOffset+i));
            solver.addClause(clause);
        }
    }

    /**Adds the clauses which say that the Treasure can't be found at (1,1) position.
     * We need to add one clause for the future and one for the past.
     *
     * @throws ContradictionException it must be included when adding clauses to a solver,
     * it prevents from inserting contradictory clauses in the formula.
     */
    private void notInInitialPos() throws ContradictionException{
        VecInt clause = new VecInt();
        clause.insertFirst(-TreasurePastOffset);
        solver.addClause(clause);
        clause.clear();
        clause.insertFirst(-TreasureFutureOffset);
        solver.addClause(clause);
    }

    /**
     * Convert a coordinate pair (x,y) to the integer value  t_[x,y]
     * of variable that stores that information in the formula, using
     * offset as the initial index for that subset of position variables
     * (past and future position variables have different variables, so different
     * offset values)
     *
     * @param x      x coordinate of the position variable to encode
     * @param y      y coordinate of the position variable to encode
     * @param offset initial value for the subset of position variables
     *               (past or future subset)
     * @return the integer indentifer of the variable  b_[x,y] in the formula
     **/
    public int coordToLineal(int x, int y, int offset) {
        return ((x - 1) * WorldDim) + (y - 1) + offset;
    }

    /**
     * Perform the inverse computation to the previous function.
     * That is, from the identifier t_[x,y] to the coordinates  (x,y)
     * that it represents
     *
     * @param lineal identifier of the variable
     * @param offset offset associated with the subset of variables that
     *               lineal belongs to
     * @return array with x and y coordinates
     **/
    public int[] linealToCoord(int lineal, int offset) {
        lineal = lineal - offset + 1;
        int[] coords = new int[2];
        coords[1] = ((lineal - 1) % WorldDim) + 1;
        coords[0] = (lineal - 1) / WorldDim + 1;
        return coords;
    }


}