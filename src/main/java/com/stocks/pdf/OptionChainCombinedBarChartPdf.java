package com.stocks.pdf;

import com.stocks.dto.OptionChainData;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
public class OptionChainCombinedBarChartPdf {

    public void createPdfWithCombinedBarChart(TreeMap<Integer, List<OptionChainData>> groupedData, String filePath) throws Exception {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (Map.Entry<Integer, List<OptionChainData>> entry : groupedData.entrySet()) {
            int strike = entry.getKey();
            int ceOiChange = entry.getValue().stream()
                    .filter(d -> "CE".equals(d.getStOptionsType()))
                    .mapToInt(OptionChainData::getOIChange)
                    .sum();
            int peOiChange = entry.getValue().stream()
                    .filter(d -> "PE".equals(d.getStOptionsType()))
                    .mapToInt(OptionChainData::getOIChange)
                    .sum();
            dataset.addValue(ceOiChange, "CE", String.valueOf(strike));
            dataset.addValue(peOiChange, "PE", String.valueOf(strike));
        }

        if (dataset.getRowCount() == 0 || dataset.getColumnCount() == 0) {
            throw new IllegalArgumentException("No data available to plot the chart.");
        }

        JFreeChart barChart = ChartFactory.createBarChart(
                "CE & PE OI Change by Strike Price",
                "Strike Price",
                "OI Change",
                dataset,
                PlotOrientation.VERTICAL,      // Orientation
                true,                          // Show legend
                true,                          // Tooltips
                false
        );

        CategoryPlot plot = (CategoryPlot) barChart.getPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setItemMargin(0.0);

        // Set CE (series 0) to green, PE (series 1) to red
        renderer.setSeriesPaint(0, Color.GREEN);
        renderer.setSeriesPaint(1, Color.RED);

        renderer.setDefaultToolTipGenerator(new StandardCategoryToolTipGenerator() {
            @Override
            public String generateToolTip(CategoryDataset dataset, int row, int column) {
                String s = super.generateToolTip(dataset, row, column);
                int b = s.indexOf('(', 1) + 1;
                int e = s.indexOf(')');
                return s.substring(b, e);
            }
        });

        // 3. Save the chart as a PNG file
        try {
            ChartUtils.saveChartAsPNG(new File(filePath), barChart, 800, 600);
            System.out.println("Bar chart saved successfully as quarterly_sales_bar_chart.png");
        } catch (IOException e) {
            System.err.println("Error saving chart: " + e.getMessage());
        }


    }
}