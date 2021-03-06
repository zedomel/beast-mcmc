/*
 * TangHuLuPlot.java
 *
 * Copyright (c) 2002-2015 Alexei Drummond, Andrew Rambaut and Marc Suchard
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package dr.app.gui.chart;

import dr.stats.CredibleSetAnalysis;
import dr.stats.FrequencyCounter;
import dr.stats.Variate;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Create a string of bubbles to visualise the joint probability
 * between two integer/categorical random variables.
 * The size of circle is proportional to the joint probability,
 * the blue coloured circle is in the credible set of
 * given a probability threshold default to 0.95,
 * the red is in the set not in the credible set.
 * The tile background is coloured if there is any circle,
 * in case the very small circle wouldn't be missed out.
 * The coloured background can also show the area of
 * the credible set and non-credible set.
 *
 * TangHuLu is a traditional Chinese snack of candied fruit
 * normally sugar-coated hawthorns on a stick.
 *
 * @author Walter Xie
 */
public class TangHuLuPlot extends ScatterPlot {

    private Paint circlePaint = new Color(124, 164, 221);
    private Paint incrediblePaint = new Color(232, 114, 103);
    private Paint tileShade = new Color(222, 229, 235);
    private Paint incredTileShade = new Color(235, 225, 230);

    // set min circle size to avoid the circle too small to see
    private final double MIN_CIRCLE_SIZE = 5;

    protected List<XY> uniqueXYList; // todo duplicate to xData yData, but it is used for key of FrequencyCounter
    protected FrequencyCounter<XY> xyFC;

    protected double credProb = 0.95;

    public TangHuLuPlot(List<Double> xData, List<Double> yData) {
        super(xData, yData);
    }

    public void setPrThreshold(double credProb){
        this.credProb = credProb;
    }

    /**
     * Set data
     */
    public void setData(List<Double> xData, List<Double> yData) {

        if (xData.size() != yData.size())
            throw new IllegalArgumentException("xData and yData must have the same size ! " +
                    xData.size() + " != " + yData.size());

        List<XY> xyList = new ArrayList<XY>();
        for (int i = 0; i < xData.size(); i++) {
            Double x = xData.get(i);
            Double y = yData.get(i);
//            if (x > 0 && y == 0)
//              System.out.println(i + "  :  " + x + "  " + y);
            xyList.add(new XY(x,y));
        }

        // find unique pairs
        xyFC = new FrequencyCounter<XY>(xyList, false);

        List<Double> xUnique = new ArrayList<Double>();
        List<Double> yUnique = new ArrayList<Double>();
        uniqueXYList = new ArrayList<XY>();
        for (XY xy : xyFC.uniqueValues()) {
            xUnique.add(xy.x);
            yUnique.add(xy.y);
            uniqueXYList.add(xy); // store counts for circle size
        }

        Variate.D xd = new Variate.D(xUnique);
        Variate.D yd = new Variate.D(yUnique);

        setData(xd, yd);
    }

    /**
     * Paint data series.
     * The maximum circle size is the smaller major tick space in x or y
     * multiplying with <code>xScale</code> or <code>yScale</code>.
     */
    protected void paintData(Graphics2D g2, Variate.N xData, Variate.N yData) {
        float x, y;

        // remove ?
        markBounds = new java.util.Vector<Rectangle2D>();

        // todo do we need selectedPoints here?
        Set<Integer> selectedPoints = getSelectedPoints();

        int n = xData.getCount();

        double maxDiameter = MIN_CIRCLE_SIZE * 2;
        if (n > 1) {
            double xGap = Math.abs(xAxis.getMajorTickSpacing() * xScale);
            double yGap = Math.abs(yAxis.getMajorTickSpacing() * yScale);
            // take the smaller gap to fit in circles
            double maxCS = Math.min(xGap, yGap);
            if (maxCS > maxDiameter)
                maxDiameter = maxCS;
        }

//        if (xyFC != null) {
        for (int i = 0; i < n; i++) {
            x = (float) transformX(((Number) xData.get(i)).doubleValue());
            y = (float) transformY(((Number) yData.get(i)).doubleValue());

            XY xy = uniqueXYList.get(i);
            // probability is proportional to area not diameter
            double diameter = maxDiameter * Math.sqrt(xyFC.getFreqScaledMaxTo1(xy));

            CredibleSetAnalysis credibleSetAnalysis = xyFC.getCredibleSetAnalysis(credProb);
            Set incredibleSet = credibleSetAnalysis.getIncredibleSet();

            // background tiles
            Paint currentPaint = tileShade;
            if (incredibleSet.contains(xy))
                currentPaint = incredTileShade;
            setMarkStyle(SQUARE_MARK, maxDiameter, new BasicStroke(1), currentPaint, currentPaint);
            drawMark(g2, x + (float) maxDiameter/2, y + (float) maxDiameter/2, null);

            // circles
            currentPaint = circlePaint;
            if (incredibleSet.contains(xy))
                currentPaint = incrediblePaint;

            if (selectedPoints.contains(i)) {
                setHilightedMarkStyle(new BasicStroke(1), currentPaint, currentPaint);
                drawMarkHilighted(g2, x, y);
            } else {
                setMarkStyle(CIRCLE_MARK, diameter, new BasicStroke(1),
                        currentPaint, currentPaint);

//                    if (colours != null && colours.size() == n)
//                        drawMark(g2, x, y, colours.get(i));
//                    else
                drawMark(g2, x, y, null);
            }
        }
//        }
    }
}


