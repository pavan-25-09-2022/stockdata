package com.stocks.service;

import com.stocks.dto.StockProfitLossResult;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository
public class StockResultManager {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void saveStockProfitLoss(StockProfitLossResult result) {
        entityManager.persist(result);
    }

    @Transactional(readOnly = true)
    public List<StockProfitLossResult> getResultsByDate(String date) {
        String jpql = "SELECT r FROM StockProfitLossResult r WHERE r.date = :date";
        return entityManager.createQuery(jpql, StockProfitLossResult.class)
                .setParameter("date", date)
                .getResultList();
    }

    @Transactional
    public void deleteResultsByDate(String date) {
        String jpql = "DELETE FROM StockProfitLossResult r WHERE r.date = :date";
        entityManager.createQuery(jpql)
                .setParameter("date", date)
                .executeUpdate();
    }
}