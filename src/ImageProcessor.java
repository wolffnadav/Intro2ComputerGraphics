package edu.cg;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class ImageProcessor extends FunctioalForEachLoops {
    // MARK: fields
    public final Logger logger;
    public final BufferedImage workingImage;
    public final RGBWeights rgbWeights;
    public final int inWidth;
    public final int inHeight;
    public final int workingImageType;
    public final int outWidth;
    public final int outHeight;
    public int[][] greyIndexArray;

    // MARK: constructors
    public ImageProcessor(Logger logger, BufferedImage workingImage, RGBWeights rgbWeights, int outWidth,
                          int outHeight) {
        super(); // initializing for each loops...

        this.logger = logger;
        this.workingImage = workingImage;
        this.rgbWeights = rgbWeights;
        inWidth = workingImage.getWidth();
        inHeight = workingImage.getHeight();
        workingImageType = workingImage.getType();
        this.outWidth = outWidth;
        this.outHeight = outHeight;
        setForEachInputParameters();
        this.greyIndexArray = new int[inHeight][inWidth];
    }

    public ImageProcessor(Logger logger, BufferedImage workingImage, RGBWeights rgbWeights) {
        this(logger, workingImage, rgbWeights, workingImage.getWidth(), workingImage.getHeight());
    }

    // Changes the picture's hue - example
    public BufferedImage changeHue() {
        logger.log("Prepareing for hue changing...");

        int r = rgbWeights.redWeight;
        int g = rgbWeights.greenWeight;
        int b = rgbWeights.blueWeight;
        int max = rgbWeights.maxWeight;

        BufferedImage ans = newEmptyInputSizedImage();

        forEach((y, x) -> {
            Color c = new Color(workingImage.getRGB(x, y));
            int red = r * c.getRed() / max;
            int green = g * c.getGreen() / max;
            int blue = b * c.getBlue() / max;
            Color color = new Color(red, green, blue);
            ans.setRGB(x, y, color.getRGB());
        });

        logger.log("Changing hue done!");

        return ans;
    }

    // Sets the ForEach parameters with the input dimensions
    public final void setForEachInputParameters() {
        setForEachParameters(inWidth, inHeight);
    }

    // Sets the ForEach parameters with the output dimensions
    public final void setForEachOutputParameters() {
        setForEachParameters(outWidth, outHeight);
    }

    // A helper method that creates an empty image with the specified input dimensions.
    public final BufferedImage newEmptyInputSizedImage() {
        return newEmptyImage(inWidth, inHeight);
    }

    // A helper method that creates an empty image with the specified output dimensions.
    public final BufferedImage newEmptyOutputSizedImage() {
        return newEmptyImage(outWidth, outHeight);
    }

    // A helper method that creates an empty image with the specified dimensions.
    public final BufferedImage newEmptyImage(int width, int height) {
        return new BufferedImage(width, height, workingImageType);
    }

    // A helper method that deep copies the current working image.
    public final BufferedImage duplicateWorkingImage() {
        BufferedImage output = newEmptyInputSizedImage();

        forEach((y, x) -> output.setRGB(x, y, workingImage.getRGB(x, y)));

        return output;
    }

    public BufferedImage greyscale() {
        logger.log("Prepareing for greyScale changing...");

        int r = rgbWeights.redWeight;
        int g = rgbWeights.greenWeight;
        int b = rgbWeights.blueWeight;
        int ammountOfRGB = rgbWeights.weightsAmount;

        BufferedImage ans = newEmptyInputSizedImage();

        forEach((y, x) -> {
            Color c = new Color(workingImage.getRGB(x, y));
            int red = r * c.getRed();
            int green = g * c.getGreen();
            int blue = b * c.getBlue();
            int greyColor = (red + green + blue) / ammountOfRGB;
            this.greyIndexArray[y][x] = greyColor;

            Color newGreyColor = new Color(greyColor, greyColor, greyColor);
            ans.setRGB(x, y, newGreyColor.getRGB());

        });

        logger.log("Changing greyScale done!");

        return ans;
    }

    public BufferedImage nearestNeighbor() {
        logger.log("Prepareing for nearestNeighbor changing...");

        BufferedImage ans = newEmptyOutputSizedImage();
        setForEachOutputParameters();
        forEach((y, x) -> {
            int outX = (x * inWidth) / outWidth;
            int outY = (y * inHeight) / outHeight;

            Color newColor = new Color(workingImage.getRGB(outX, outY));
            ans.setRGB(x, y, newColor.getRGB());
        });

        logger.log("Changing nearestNeighbor done!");

        return ans;
    }

}
