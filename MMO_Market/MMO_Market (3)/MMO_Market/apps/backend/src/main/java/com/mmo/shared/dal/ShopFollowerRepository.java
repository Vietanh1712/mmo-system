package com.mmo.shared.dal;

import com.mmo.shared.model.ShopFollower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShopFollowerRepository extends JpaRepository<ShopFollower, Long> {

    @Query("SELECT COUNT(sf) FROM ShopFollower sf WHERE sf.seller.id = :sellerId AND sf.isDelete = false")
    long countBySellerIdAndIsDeleteFalse(@Param("sellerId") Long sellerId);

    @Query("SELECT sf FROM ShopFollower sf WHERE sf.follower.id = :followerId AND sf.seller.id = :sellerId AND sf.isDelete = false")
    Optional<ShopFollower> findByFollowerIdAndSellerIdAndIsDeleteFalse(@Param("followerId") Long followerId, @Param("sellerId") Long sellerId);

    @Query("SELECT sf FROM ShopFollower sf WHERE sf.follower.id = :followerId AND sf.seller.id = :sellerId")
    Optional<ShopFollower> findByFollowerIdAndSellerId(@Param("followerId") Long followerId, @Param("sellerId") Long sellerId);
}
