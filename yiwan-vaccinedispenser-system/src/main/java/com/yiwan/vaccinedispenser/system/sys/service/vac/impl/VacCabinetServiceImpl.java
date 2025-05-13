package com.yiwan.vaccinedispenser.system.sys.service.vac.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.yiwan.vaccinedispenser.system.domain.model.vac.VacCabinet;
import com.yiwan.vaccinedispenser.system.sys.dao.VacCabinetMapper;
import com.yiwan.vaccinedispenser.system.sys.service.vac.VacCabinetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;


/**
 * @author
 **/
@Service
@Slf4j
public class VacCabinetServiceImpl extends ServiceImpl<VacCabinetMapper, VacCabinet> implements VacCabinetService {

	@Resource(name = "redisTemplate")
	private ValueOperations<String, String> valueOperations;
	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@Autowired
	private VacCabinetMapper vacCabinetMapper;


	@Override
	public VacCabinet getOneVacCabinet(String name, int status) {
        return vacCabinetMapper.selectOne(new LambdaQueryWrapper<VacCabinet>()
				.eq(VacCabinet::getName, name)
				.eq(VacCabinet::getStatus, status)
				.eq(VacCabinet::getDeleted,0)
		);
	}

	@Override
	public void updateVacCabinet(Integer type, String ip) {
		VacCabinet vacCabinet = new VacCabinet();
		vacCabinet.setIp(ip);


		vacCabinetMapper.update(vacCabinet,new UpdateWrapper<VacCabinet>()
				.eq("type",type)
				.eq("deleted",0));

	}


}








