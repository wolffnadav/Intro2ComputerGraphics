package edu.cg;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class SeamsCarver extends ImageProcessor {

    // MARK: Fields
    private int numOfSeams;
    private ResizeOperation resizeOp;
    private boolean[][] imageMask;
    private boolean[][] PictureMaskMatrix;          //The new image mask
    private boolean[][] showSeamMatrix;             //The paths of the each seam
    private int[][] greyIndexMatrix;                //Image GretScale as 2d array
    private long[][] costMatrix;                    //The cost matrix
    private int currentSeam;                        //current number of seam working on
    private int[][] indexMatrix;                    //matrix with indexes
    private int[][] indexMatrixIncrease;            //for increase picture operation
    private int[][] seamsPathMatrix;                //paths of all the seams removed
    private int[][] directionMatrix;                //tracking
    private ArrayList<ArrayList<Integer>> indexes;  //copy of indexMatrix as arrayList

    /**
     * Initalization and Run of the seamCarver
     *
     * @param logger       - the logget to write to
     * @param workingImage - the working image
     * @param outWidth     - the new width
     * @param rgbWeights   - the rgb colors
     * @param imageMask    - the image mask
     */
    public SeamsCarver(Logger logger, BufferedImage workingImage, int outWidth, RGBWeights rgbWeights,
                       boolean[][] imageMask) {
        super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, workingImage.getHeight());

        numOfSeams = Math.abs(outWidth - inWidth);
        this.imageMask = imageMask;
        if (inWidth < 2 | inHeight < 2)
            throw new RuntimeException("Can not apply seam carving: workingImage is too small");

        if (numOfSeams > inWidth / 2)
            throw new RuntimeException("Can not apply seam carving: too many seams...");

        // Setting resizeOp by with the appropriate method reference
        if (outWidth > inWidth)
            resizeOp = this::increaseImageWidth;
        else if (outWidth < inWidth)
            resizeOp = this::reduceImageWidth;
        else
            resizeOp = this::duplicateWorkingImage;

        this.greyIndexMatrix = new int[this.inHeight][this.inWidth];
        //initialize the seamsPath Matrix that holds all the paths of the removed seams
        this.seamsPathMatrix = new int[this.numOfSeams][this.inHeight];
        this.indexes = new ArrayList<>();
        this.costMatrix = new long[this.inHeight][this.inWidth];
        this.showSeamMatrix = new boolean[this.inHeight][this.inWidth];
        this.indexMatrix = new int[this.inHeight][this.inWidth];
        this.indexMatrixIncrease = new int[this.inHeight][this.inWidth];
        this.PictureMaskMatrix = new boolean[this.inHeight][this.inWidth];
        this.directionMatrix = new int[this.inHeight][this.inWidth];

        this.currentSeam = 0;
        //Get GreyScaleMatrix
        greyscaleMatrix();
        //Start seamsPathMatrix
        //Start indexMatrix and indexMatrixIncrease
        //Start PictureMaskMatrix
        initIndexes();
        this.logger.log("Init finished");
    }

    /**
     * Function that initialize the matrix index for reduction and for increase
     * for each column will have place the x value of it
     */
    private void initIndexes() {
        for (int y = 0; y < this.inHeight; y++) {
            for (int x = 0; x < this.inWidth; x++) {
                this.indexMatrix[y][x] = x;
                this.indexMatrixIncrease[y][x] = x;
                this.PictureMaskMatrix[y][x] = this.imageMask[y][x];
            }
        }
        this.logger.log(" Matrix Index  done!");
    }

    /**
     * Function that initialize the matrix Greyscale
     */
    private void greyscaleMatrix() {

        int r = rgbWeights.redWeight;
        int g = rgbWeights.greenWeight;
        int b = rgbWeights.blueWeight;
        int ammountOfRGB = rgbWeights.weightsAmount;

        forEach((y, x) -> {
            Color c = new Color(workingImage.getRGB(x, y));
            int red = r * c.getRed();
            int green = g * c.getGreen();
            int blue = b * c.getBlue();
            int greyColor = (red + green + blue) / ammountOfRGB;
            this.greyIndexMatrix[y][x] = greyColor;

        });
        logger.log(" greyScale matrix done!");
    }

    /**
     * Function that calculate the CostMatrix
     */
    private void calculateCostMatrix() {
        int currWidth = (this.inWidth - this.currentSeam);

        for (int y = 0; y < this.inHeight; y++) {
            for (int x = 0; x < currWidth; x++) {
                long up = 0L;
                long right = 0L;
                long left = 0L;
                if (y != 0) {
                    up = calculatePixelUp(y, x);
                    right = calculatePixelRight(y, x, (currWidth - 1));
                    left = calculatePixelLeft(y, x);
                }

                long minCost = Math.min(left, Math.min(up, right));
                this.directionMatrix[y][x] = up == minCost ? x : right == minCost ? (x + 1) : (x - 1);
                this.costMatrix[y][x] = getPixelEnergy(y, x) + minCost;
            }
        }
    }


    /**
     * help functions to calculte the cost matrix
     *
     * @param y - y index to calculate
     * @param x - x index to calculate
     * @return
     */
    private long calculatePixelUp(int y, int x) {
        return (y == 0) ? c_v(y, x) : this.costMatrix[y - 1][x] + c_v(y, x);
    }

    private long calculatePixelRight(int y, int x, int currWidth) {
        if (x == currWidth) return ((long) Integer.MAX_VALUE);
        return (this.costMatrix[y - 1][x + 1] + c_r(y, x));
    }

    private long calculatePixelLeft(int y, int x) {
        return (x == 0) ? ((long) Integer.MAX_VALUE) : this.costMatrix[y - 1][x - 1] + c_l(y, x);
    }

    private long c_l(int y, int x) {
        if (x == 0 || y == 0 || x == (this.inWidth - this.currentSeam - 1)) return 0;
        return Math.abs(this.greyIndexMatrix[y][x - 1] - this.greyIndexMatrix[y][x + 1])
                + Math.abs(this.greyIndexMatrix[y - 1][x] - this.greyIndexMatrix[y][x - 1]);
    }

    private long c_v(int y, int x) {
        if (x == 0 || y == 0 || x == (this.inWidth - this.currentSeam - 1)) return 0;
        return Math.abs(this.greyIndexMatrix[y][x - 1] - this.greyIndexMatrix[y][x + 1]);

    }

    private long c_r(int y, int x) {
        if (x == 0 || y == 0 || x == (this.inWidth - this.currentSeam - 1)) return 0;
        return Math.abs(this.greyIndexMatrix[y][x - 1] - this.greyIndexMatrix[y][x + 1])
                + Math.abs(this.greyIndexMatrix[y - 1][x] - this.greyIndexMatrix[y][x + 1]);
    }

    /**
     * Function that calculate the PixelEnergy
     *
     * @param y - y index
     * @param x - x index
     * @return the pixel energy at index (y,x)
     */
    private long getPixelEnergy(int y, int x) {
        return e_1(y, x) + e_2(y, x) + e_3(y, x);
    }

    private long e_1(int y, int x) {
        return x < ((this.inWidth - this.currentSeam) - 1) ? Math.abs(greyIndexMatrix[y][x + 1] - greyIndexMatrix[y][x])
                : Math.abs(greyIndexMatrix[y][x - 1] - greyIndexMatrix[y][x]);
    }

    private long e_2(int y, int x) {
        return y < (this.inHeight - 1) ? Math.abs(greyIndexMatrix[y + 1][x] - greyIndexMatrix[y][x])
                : Math.abs(greyIndexMatrix[y - 1][x] - greyIndexMatrix[y][x]);
    }

    private long e_3(int y, int x) {
        return this.PictureMaskMatrix[y][x] ? (long) Integer.MIN_VALUE : 0;
    }

    /**
     * method for remove seam by running over the cost matrix,
     * find the best seam path
     */
    private void removeSeam() {
        int[] SeamPath = new int[inHeight];
        int minArg = calculateMinIndexLastRow();

        for (int y = inHeight - 1; y >= 0; y--) {

            this.seamsPathMatrix[this.currentSeam][y] = this.indexMatrix[y][minArg];
            this.showSeamMatrix[y][this.indexMatrix[y][minArg]] = true;
            SeamPath[y] = minArg;

            minArg = this.directionMatrix[y][minArg];
        }
        ShiftMapping(SeamPath);
    }

    /**
     * Function the minimal value of the last row of the CostMatrix
     */
    private int calculateMinIndexLastRow() {
        int minimalIndex = 0;
        int currWidth = this.inWidth - this.currentSeam;
        int height = this.inHeight - 1;

        for (int x = 0; x < currWidth; x++) {
            if (this.costMatrix[height][x] < this.costMatrix[height][minimalIndex]) {
                minimalIndex = x;
            }
        }
        return minimalIndex;
    }

    /**
     * Function that shift the row at each seam index to the left
     */
    private void ShiftMapping(int[] seamPath) {
        for (int y = 0; y < inHeight; y++) {
            for (int x = seamPath[y] + 1; x < this.inWidth - this.currentSeam; x++) {

                this.PictureMaskMatrix[y][x - 1] = this.PictureMaskMatrix[y][x];
                this.indexMatrix[y][x - 1] = indexMatrix[y][x];
                this.greyIndexMatrix[y][x - 1] = greyIndexMatrix[y][x];
            }
        }
    }

    public BufferedImage resize() {
        return resizeOp.resize();
    }

    private BufferedImage reduceImageWidth() {
        logger.log("Images width reducing start...");
        //initialize an empty output sized image
        BufferedImage ans = newEmptyOutputSizedImage();

        while (this.currentSeam < this.numOfSeams) {
            calculateCostMatrix();
            removeSeam();
            this.currentSeam++;
        }

        setForEachOutputParameters();
        forEach((y, x) -> {
            int originalXIndex = this.indexMatrix[y][x];

            Color c = new Color(workingImage.getRGB(originalXIndex, y));
            ans.setRGB(x, y, c.getRGB());
        });

        logger.log("Images width reducing done!");
        return ans;
    }

    private BufferedImage increaseImageWidth() {
        logger.log("Images width increasing start...");

        //initialize an empty output sized image
        BufferedImage ans = newEmptyOutputSizedImage();

        //find and remove seams
        while (this.currentSeam < this.numOfSeams) {
            calculateCostMatrix();
            removeSeam();
            this.currentSeam++;
        }


        for (int y = 0; y < this.indexMatrixIncrease.length; y++) {
            indexes.add(new ArrayList<>());
            for (int x = 0; x < indexMatrixIncrease[0].length; x++) {
                indexes.get(y).add(x);
            }
        }

        //update the indexes for increase
        for (int y = 0; y < this.seamsPathMatrix.length; y++) {
            for (int x = 0; x < this.seamsPathMatrix[y].length; x++) {
                int XremovedIndex = this.seamsPathMatrix[y][x];
                for (int i = 0; i <= y; i++) {
                    if (XremovedIndex == indexes.get(x).get(XremovedIndex + i)) {
                        indexes.get(x).add(XremovedIndex + i + 1, XremovedIndex);
                        break;
                    }
                }
            }
        }

        //add the deleted pixels
        setForEachOutputParameters();
        forEach((y, x) -> {
            int originalXIndex = indexes.get(y).get(x);

            // get color of original index
            Color c = new Color(workingImage.getRGB(originalXIndex, y));

            ans.setRGB(x, y, c.getRGB());
        });
        logger.log("Images width increase is done!");

        return ans;
    }

    public BufferedImage showSeams(int seamColorRGB) {
        BufferedImage output = newEmptyInputSizedImage();

        if (inWidth > outWidth) {
            reduceImageWidth();
        } else {
            increaseImageWidth();
        }

        setForEachInputParameters();
        forEach((y, x) -> {
            if (this.showSeamMatrix[y][x]) {
                output.setRGB(x, y, seamColorRGB);
            } else {
                output.setRGB(x, y, workingImage.getRGB(x, y));
            }

        });

        return output;
    }

    public boolean[][] getMaskAfterSeamCarving() {
        if (outWidth == inWidth) return imageMask;
        boolean[][] newMaskMatrix = new boolean[outHeight][outWidth];
        setForEachOutputParameters();
        if (outWidth > inWidth) {
            forEach((y, x) -> {
                int originalXIndex = indexes.get(y).get(x);
                newMaskMatrix[y][x] = this.imageMask[y][originalXIndex];
            });
        } else {
            forEach((y, x) -> {
                int originalXIndex = this.indexMatrix[y][x];
                newMaskMatrix[y][x] = this.imageMask[y][originalXIndex];
            });
        }
        return newMaskMatrix;
    }

    // MARK: An inner interface for functional programming.
    @FunctionalInterface
    interface ResizeOperation {
        BufferedImage resize();
    }
}



