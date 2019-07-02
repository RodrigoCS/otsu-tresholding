import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class Otsu {
  public static void main(String[] args) throws IOException {
    JFrame frame = new JFrame("Otsu");
    JPanel panel = new JPanel(new BorderLayout());

    // Read image file and save its size
    BufferedImage sourceImage = ImageIO.read(Otsu.class.getResource("lenacolor.png"));
    int width = sourceImage.getWidth();
    int height = sourceImage.getHeight();

    frame.setSize(width, height);
    frame.add(panel);
    frame.setVisible(true);

    // Convert bufferedImage to matrix
    int[][] matrix = imageToMatrix(sourceImage, width, height);

    // Actual Otsu process
    matrix = getOtsuMatrix(matrix, width, height);

    // Convert Otsu matrix back to BufferedImage
    BufferedImage image = getImageFromMatrix(matrix, width, height);

    JLabel imageLabel = new JLabel(new ImageIcon(image));
    panel.add(imageLabel);
    frame.validate();
  }

  /**
   * 
   * @param matrix The matrix with GrayValues
   * @param width
   * @param height
   * @return Matrix converted to binary with 0's and 255 values
   */
  private static int[][] getOtsuMatrix(int[][] matrix, int width, int height) {
    int[] values = IntStream.range(0, 256).toArray();
    int[] counts = new int[values.length];
    ArrayList<Double> tresholdCache = new ArrayList<Double>();
    ArrayList<Double> varianceCache = new ArrayList<Double>();

    for (int i = 0; i < matrix.length; i++) {
      for (int j = 0; j < matrix[i].length; j++) {
        int pos = matrix[i][j];

        counts[pos] += 1;
      }
    }

    for (int i = 1; i < 256; i++) {
      int treshold = values[i];
      Range rangeA = new Range(1, treshold);
      Range rangeB = new Range(treshold + 1, 256);

      OtsuValues A = getOtsuValues(counts, values, rangeA);
      OtsuValues B = getOtsuValues(counts, values, rangeB);

      Double withinClassVariance = (A.weight * A.variance) + (B.weight * B.variance);

      varianceCache.add(withinClassVariance);
      tresholdCache.add((double)treshold);
      
    }

    double selectedTreshold = getSelectedTreshold(varianceCache, tresholdCache);

    for (int i = 0; i < matrix.length; i++) {
      for (int j = 0; j < matrix[i].length; j++) {
        int value = matrix[i][j];
        if (value > selectedTreshold) {
          matrix[i][j] = 255;
        } else {
          matrix[i][j] = 0;
        }
      }
    }

    return matrix;
  }

  /**
   * 
   * @param variance
   * @param treshold
   * @return The treshold that corresponds to the minimum variance value
   */
  private static double getSelectedTreshold(ArrayList<Double> variance, ArrayList<Double> treshold) {
    double min = variance.get(0);
    int minIndex2 = 0;
    for (int i = 0; i < variance.size(); i++) {
      if (variance.get(i) < min) {
        min = variance.get(i);
        minIndex2 = i;
      }
    }
    int minIndex = variance.indexOf(Collections.min(variance));
    return treshold.get(minIndex);
  }

  /**
   * 
   * @param counts
   * @param values
   * @param range
   * @return Returns an OtsuValues Object cotaining the calculated
   * weight, mean and variance.
   */
  private static OtsuValues getOtsuValues(int[] counts, int[] values, Range range) {
    int[] section = Arrays.copyOfRange(counts, range.start, range.limit);
    int[] sectionValues = Arrays.copyOfRange(values, range.start, range.limit);
    double weight = IntStream.of(section).sum();

    double mean = dot(doubleArray(section), doubleArray(sectionValues)) / weight;
    double[] preVariance = sub(doubleArray(sectionValues), mean);
    preVariance = multi(preVariance, preVariance);
    double variance = dot(preVariance, doubleArray(section)) / weight;
    if (Double.isNaN(variance)) {
      variance = 0.0;
    }
    return new OtsuValues(weight, mean, variance);
  }


  /**
   * 
   * @param image
   * @param width
   * @param height
   * @return A Matrix with the gray values of the image
   */
  private static int[][] imageToMatrix(BufferedImage image, int width, int height) {
    int[][] result = new int[height][width];
    for (int row = 0; row < height; row++) {
      for (int col = 0; col < width; col++) {
        int rgb = image.getRGB(col, row);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = (rgb & 0xFF);
        int gray = (r + g + b) / 3;
        result[row][col] = gray;
      }
    }

    return result;
  }

  /**
   * 
   * @param matrix
   * @param width
   * @param height
   * @return A buffered image from a matrix
   */
  private static BufferedImage getImageFromMatrix(int[][] matrix, int width, int height) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        int pixel = matrix[j][i];
        int color = new Color(0, 0, pixel).getRGB();
        System.out.print(" " + pixel + " ");
        image.setRGB(i, j, color);
      }
    }
    return image;
  }

  /**
   * 
   * @param a
   * @param b
   * @return Dot product from a.b
   */
  public static double dot(double[] a, double[] b) {
    double sum = 0;
    for (int i = 0; i < a.length - 1; i++) {
      sum += a[i] * b[i];
    }
    return sum;
  }

  /**
   * 
   * @param a
   * @param b
   * @return axb
   */
  public static double[] multi(double[] a, double[] b) {
    double[] result = new double[a.length];
    for (int i = 0; i < a.length; i++) {
      result[i] = a[i] * b[i];
    }
    return result;
  }

  /**
   * 
   * @param array
   * @param value
   * @return
   */
  public static double[] sub(double[] array, double value) {
    double[] newArray = new double [array.length];
    for (int i = 0; i < array.length; i++) {
      newArray[i] = array[i] - value;
    }
    return newArray;
  }

  /**
   * 
   * @param source
   * @return int array converted to double values
   */
  public static double[] doubleArray(int[] source) {
    double[] dest = new double[source.length];
    for(int i=0; i<source.length; i++) {
        dest[i] = source[i];
    }
    return dest;
  }

}