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
    public void saveStockData(String stock, String date, String time, String oiInterpretation) {
        StockData stockData = new StockData();
        stockData.setStock(stock);
        stockData.setDate(date);
        stockData.setTime(time);
        stockData.setOiInterpretation(oiInterpretation);
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
    public StockData getRecordByDate(String stock, String date) {
        String hql = "FROM StockData WHERE stock = :stock AND date = :date";
        Session session = entityManager.unwrap(Session.class);
        return session.createQuery(hql, StockData.class)
                .setParameter("stock", stock)
                .setParameter("date", date)
                .uniqueResult();
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

    public List<StockData> getAllRecord() {
        String hql = "FROM StockData";
        Session session = entityManager.unwrap(Session.class);
        return session.createQuery(hql, StockData.class).getResultList();
    }
}