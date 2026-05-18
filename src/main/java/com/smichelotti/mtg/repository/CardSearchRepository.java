package com.smichelotti.mtg.repository;

import com.smichelotti.mtg.entity.CardSearchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardSearchRepository
        extends JpaRepository<CardSearchEntity, Long> {
}
