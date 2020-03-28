package apryraz.tworld;

public class TFState {
    /**
     *
     **/

    int wDim;
    String[][] matrix;

    public TFState(int dim) {
        wDim = dim;
        matrix = new String[wDim][wDim];
        initializeState();
    }

    public void initializeState() {
        for (int i = 0; i < wDim; i++) {
            for (int j = 0; j < wDim; j++) {
                matrix[i][j] = "?";
            }
        }
    }

    public void set(int i, int j, String val) {

        matrix[i - 1][j - 1] = val;
    }

    public boolean equals(Object obj) {
        TFState tfstate2 = (TFState) obj;
        boolean status = true;

        for (int i = 0; i < wDim; i++) {
            for (int j = 0; j < wDim; j++) {
                if (!matrix[i][j].equals(tfstate2.matrix[i][j]))
                    status = false;
            }
        }

        return status;
    }

    public void printState() {
        System.out.println("FINDER => Printing Treasure world matrix");
        for (int i = wDim - 1; i > -1; i--) {
            System.out.print("\t#\t");
            for (int j = 0; j < wDim; j++) {
                System.out.print(matrix[i][j] + " ");
            }
            System.out.println("\t#");
        }
    }

}
