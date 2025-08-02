package com.stocks.pdf;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import java.io.File;
import java.io.IOException;

public class BarChartSample {

    public static void main(String[] args) {
        try{

            /* Step - 1: Define the data for the bar chart  */
            DefaultCategoryDataset my_bar_chart_dataset = new DefaultCategoryDataset();
            my_bar_chart_dataset.addValue(34, "Q1", "Rome");
            my_bar_chart_dataset.addValue(45, "Q1", "Cairo");
            my_bar_chart_dataset.addValue(22, "Q2", "Rome");
            my_bar_chart_dataset.addValue(12, "Q2", "Cairo");
            my_bar_chart_dataset.addValue(56, "Q3", "Rome");
            my_bar_chart_dataset.addValue(98, "Q3", "Cairo");
            my_bar_chart_dataset.addValue(2, "Q4", "Rome");
            my_bar_chart_dataset.addValue(15, "Q4", "Cairo");

            /* Step -2:Define the JFreeChart object to create bar chart */
            JFreeChart BarChartObject = ChartFactory.createBarChart("CountryVsSales - Bar Chart", "Country", "Sales", my_bar_chart_dataset, PlotOrientation.VERTICAL, true, true, false);

            CategoryPlot plot = (CategoryPlot) BarChartObject.getPlot();
            BarRenderer renderer = (BarRenderer) plot.getRenderer();
            renderer.setItemMargin(0.0);
            /* Step -3: Write the output as PNG file with bar chart information */
            int width = 640; /* Width of the image */
            int height = 480; /* Height of the image */
            File BarChart = new File("C:\\Users\\nithin.venkaiahgari\\Documents\\Oipulse\\output_chart.png");
            ChartUtils.saveChartAsPNG(BarChart, BarChartObject, width, height);
            System.out.println("Bar chart saved successfully as quarterly_sales_bar_chart.png");
        }catch(Exception i) {
            System.out.println(i);
        }
    }

    public static void main1() {
        // 1. Create a dataset
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(75, "Sales", "Q1");
        dataset.addValue(90, "Sales", "Q2");
        dataset.addValue(60, "Sales", "Q3");
        dataset.addValue(100, "Sales", "Q4");

        // 2. Create the bar chart
        JFreeChart chart = ChartFactory.createBarChart(
                "Quarterly Sales Performance", // Chart title
                "Quarter",                     // X-axis label
                "Sales (Units)",               // Y-axis label
                dataset,                       // Dataset
                PlotOrientation.VERTICAL,      // Orientation
                true,                          // Show legend
                true,                          // Tooltips
                false                          // URLs
        );

        // 3. Save the chart as a PNG file
        try {
            ChartUtils.saveChartAsPNG(new File("C:\\Users\\nithin.venkaiahgari\\Documents\\Oipulse\\bar_bar1.png"), chart, 800, 600);
            System.out.println("Bar chart saved successfully as quarterly_sales_bar_chart.png");
        } catch (IOException e) {
            System.err.println("Error saving chart: " + e.getMessage());
        }
    }
}