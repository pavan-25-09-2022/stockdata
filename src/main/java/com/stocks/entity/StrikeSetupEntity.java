package com.stocks.entity;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "strick_setup")
@Data
public class StrikeSetupEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne
        @JoinColumn(name = "trade_setup_id", nullable = false)
        private TradeSetupEntity tradeSetup;

        private double curPrice;
        private int ceOi; // Open Interest for Call Option
        private double ceOiChg;
        private String ceOiInt;  // Open Interest in integer format
        private int ceVolume;
        private double ceIv;
        private double ceIvChg; // Change in Implied Volatility
        private double ceLtpChg; // Change in last traded price
        private double strikePrice;
        private int peOi; // Open Interest for Put Option
        private double peOiChg;
        private String peOiInt; // Open Interest in integer format
        private int peVolume;
        private double peIv;
        private double peIvChg; // Change in Implied Volatility
        private double peLtpChg;

        @Override
        public String toString() {
                return "StrikeSetupEntity{" +
                        "id=" + id +
                        ", strikePrice=" + strikePrice +
                        // only reference tradeSetup's id
                        ", tradeSetupId=" + (tradeSetup != null ? tradeSetup.getId() : null) +
                        '}';
        }
    }
