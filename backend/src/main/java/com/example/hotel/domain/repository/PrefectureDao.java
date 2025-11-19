package com.example.hotel.domain.repository;

import com.example.hotel.domain.model.Prefecture;
import org.seasar.doma.Dao;
import org.seasar.doma.Select;
import org.seasar.doma.boot.ConfigAutowireable;
import java.util.List;

@Dao
@ConfigAutowireable
public interface PrefectureDao {
  @Select
  List<Prefecture> selectAll();
}
