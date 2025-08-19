package com.stocks.service;

import com.stocks.dto.FutureAnalysis;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository
public class FutureAnalysisManager {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<FutureAnalysis> getRecordsBySymbolAndDate(String symbol, String date) {
        String hql = "FROM FutureAnalysis WHERE symbol = :symbol AND date = :date";
        Session session = entityManager.unwrap(Session.class);
        return session.createQuery(hql, FutureAnalysis.class)
                .setParameter("symbol", symbol)
                .setParameter("date", date)
                .list();
    }

    @Transactional(readOnly = true)
    public List<FutureAnalysis> getAllRecords() {
        String hql = "FROM FutureAnalysis";
        Session session = entityManager.unwrap(Session.class);
        return session.createQuery(hql, FutureAnalysis.class).list();
    }

    @Transactional
    public void saveFutureAnalysis(FutureAnalysis futureAnalysis) {
        entityManager.persist(futureAnalysis);
    }

    @Transactional(readOnly = true)
    public FutureAnalysis getRecordBySymbolDateAndTime(String symbol, String date, String duration) {
        String hql = "FROM FutureAnalysis WHERE symbol = :symbol AND date = :date AND duration = :duration";
        Session session = entityManager.unwrap(Session.class);
        return session.createQuery(hql, FutureAnalysis.class)
                .setParameter("symbol", symbol)
                .setParameter("date", date)
                .setParameter("duration", duration)
                .uniqueResult();
    }

    @Transactional(readOnly = true)
    public List<FutureAnalysis> getRecordsBySymbol(String symbol) {
        String hql = "FROM FutureAnalysis WHERE symbol = :symbol";
        Session session = entityManager.unwrap(Session.class);
        return session.createQuery(hql, FutureAnalysis.class)
                .setParameter("symbol", symbol)
                .list();
    }

    @Transactional(readOnly = true)
    public List<FutureAnalysis> getRecordsByDate(String date) {
        String hql = "FROM FutureAnalysis WHERE date = :date";
        Session session = entityManager.unwrap(Session.class);
        return session.createQuery(hql, FutureAnalysis.class)
                .setParameter("date", date)
                .list();
    }

    @Transactional(readOnly = true)
    public List<FutureAnalysis> getRecordsByDateRange(String fromDate, String toDate) {
        String hql = "FROM FutureAnalysis WHERE date BETWEEN :fromDate AND :toDate";
        Session session = entityManager.unwrap(Session.class);
        return session.createQuery(hql, FutureAnalysis.class)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .list();
    }

}
