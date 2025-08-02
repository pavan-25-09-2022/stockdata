package com.stocks.pdf;

import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;
import com.stocks.dto.OptionChainData;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.imageio.ImageIO;

@Component
public class OptionChainCombinedPieChartPdf {

    public void createPdfWithCombinedPieChart(TreeMap<Integer, List<OptionChainData>> groupedData, String pdfPath) throws Exception {
        DefaultPieDataset dataset = new DefaultPieDataset();

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
            dataset.setValue("CE-" + strike, ceOiChange);
            dataset.setValue("PE-" + strike, peOiChange);
        }

        JFreeChart chart = ChartFactory.createPieChart(
            "CE & PE OI Change by Strike Price",
            dataset,
            true, true, false
        );

        BufferedImage chartImage = chart.createBufferedImage(800, 600);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(chartImage, "png", baos);
        Image chartImg = Image.getInstance(baos.toByteArray());

        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(pdfPath));
        document.open();
        document.add(chartImg);
        document.close();
    }
}