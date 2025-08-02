package com.stocks.service;

import com.stocks.dto.StockData;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository
public class StockDataManager {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void saveStockData(String stock, String date, String time, String oiInterpretation, String type, double limit) {
        StockData stockData = new StockData(stock, type);
        stockData.setDate(date);
        stockData.setTime(time);
        stockData.setOiInterpretation(oiInterpretation);
        stockData.setTrend(String.format("%.2f", limit));
        entityManager.persist(stockData);
    }

    @Transactional(readOnly = true)
    public StockData getRecord(String stock, String date, String time) {
        String hql = "FROM StockData WHERE stock = :stock AND date = :date AND time = :time";
        Session session = entityManager.unwrap(Session.class);
        return session.createQuery(hql, StockData.class)
                .setParameter("stock", stock)
                .setParameter("date", date)
                .setParameter("time", time)
                .uniqueResult();
    }

    @Transactional(readOnly = true)
    public List<StockData> getStocksByDate(String date) {
        String hql = "FROM StockData WHERE date = :date";
        Session session = entityManager.unwrap(Session.class);
        return session.createQuery(hql, StockData.class)
                .setParameter("date", date)
                .getResultList();
    }

    @Transactional(readOnly = true)
    public String getRecentTime(String stock, String date) {
        String hql = "SELECT MAX(time) FROM StockData WHERE stock = :stock AND date = :date";
        Session session = entityManager.unwrap(Session.class);
        return session.createQuery(hql, String.class)
                .setParameter("stock", stock)
                .setParameter("date", date)
                .uniqueResult();
    }

    @Transactional(readOnly = true)
    public StockData getRecentRecord(String stock, String date) {
        String hql = "FROM StockData WHERE stock = :stock AND date = :date AND time = (SELECT MAX(time) FROM StockData WHERE stock = :stock AND date = :date)";
        Session session = entityManager.unwrap(Session.class);
        return session.createQuery(hql, StockData.class)
                .setParameter("stock", stock)
                .setParameter("date", date)
                .uniqueResult();
    }

    public List<StockData> getAllRecord() {
        String hql = "FROM StockData";
        Session session = entityManager.unwrap(Session.class);
        return session.createQuery(hql, StockData.class).getResultList();
    }

    @Transactional
    public void saveStockData(String stock, String date, String time, String oiInterpretation, String type, Integer  entryPrice1, Integer entryPrice2,
                              Integer stopLoss,  Integer targetPrice1, Integer targetPrice2, Integer averagePrice) {

       if(getRecentTime(stock, date) != null) {
            // If the record already exists for the given stock, date, and time, do not save it again
            return;
        }
        StockData stockData = new StockData(stock, type);
        stockData.setDate(date);
        stockData.setTime(time);
        stockData.setOiInterpretation(oiInterpretation);
       // stockData.setTrend(String.format("%.2f", limit));
        stockData.setEntryPrice1(entryPrice1);
        stockData.setEntryPrice2(entryPrice2);
        stockData.setStopLoss(stopLoss);
        stockData.setTargetPrice1(targetPrice1);
        stockData.setTargetPrice2(targetPrice2);
        stockData.setAveragePrice(averagePrice);
        entityManager.persist(stockData);
    }
}

