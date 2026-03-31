package com.maoyouquan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maoyouquan.entity.Cat;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CatMapper extends BaseMapper<Cat> {}
