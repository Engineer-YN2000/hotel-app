package com.example.hotel.domain.repository;

import com.example.hotel.domain.model.Reserver;
import org.seasar.doma.Dao;
import org.seasar.doma.Insert;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;
import org.seasar.doma.jdbc.Result;

@Dao
@ConfigAutowireable
public interface ReserverDao {
  @Insert
  Result<Reserver> insert(Reserver reserver);

  @Update(sqlFile = true)
  Result<Reserver> update(Reserver reserver);
}
