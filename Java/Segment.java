	@RequestMapping(value = "/exitNow", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public String exitNow(String orderid) {
		return gameCtrlService.exitNow(orderid);
	}
	
	private void updateRedisPay(TbOrder order) {
		Map<String, String> redisMap = jedisClientDao.hgetAll(order.getSongid());
		String isPayVal = order.getId();
		String timeLongVal = order.getTimelong();
		String oldOrderid = redisMap.get(RedisCnst.REDIS_isPay_KEY);
		long redisRemainTimeLong = 0;
		if (StringUtils.isNotBlank(oldOrderid)) {
			redisRemainTimeLong = jedisClientDao.ttl(oldOrderid);
			if (redisRemainTimeLong > 0) {
				isPayVal += ("," + oldOrderid);
				timeLongVal += (redisRemainTimeLong / 60);
			}
		}
		redisMap.put(RedisCnst.REDIS_isPay_KEY, isPayVal);
		redisMap.put(RedisCnst.REDIS_timeLong_KEY, timeLongVal);
		/*String flag = jedisClientDao.hmset(order.getSongid(), redisMap);
		int expireExpressedInSeconds = (Integer.parseInt(order.getTimelong()) - 1) * 60; */
		int expireExpressedInSeconds = ((Integer.parseInt(order.getTimelong())) * 60) + (int) redisRemainTimeLong;
		jedisClientDao.setExByExpireValueCompareToZero(expireExpressedInSeconds, redisMap, order);
	}
	
	
	updateWcPayCallbackHandler
	
		public void setExByExpireValueCompareToZero(int expireExpressedInSeconds, Map<String, String> redisMap, TbOrder order) {
		LOG.debug(order.getId() + ":" + expireExpressedInSeconds);
		if (expireExpressedInSeconds > 0) {
			setex(order.getId(), expireExpressedInSeconds, order.getSongid());
		} else {// 订单已过期
			redisMap.put(RedisCnst.REDIS_isPay_KEY, "");
			redisMap.put(RedisCnst.REDIS_timeLong_KEY, "");
			String res = hmset(order.getSongid(), redisMap);
			if (LOG.isInfoEnabled()) {
				LOG.info(" hmset = {} res = {}", JSON.toJSONString(redisMap), res);
			}
		}
	}
	
	
//================================
package com.wbhl.service.impl;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wbhl.dao.JedisClientDAO;
import com.wbhl.service.GameCtrlService;
import com.wbhl.util.ResultUtil;

@Service
public class GameCtrlServiceImpl implements GameCtrlService {

	private static Logger LOG = LoggerFactory.getLogger(GameCtrlServiceImpl.class);

	@Autowired
	private JedisClientDAO jedisDao;

	@Override
	public String exitNow(String orderid) {
		if (StringUtils.isBlank(orderid)) {
			return ResultUtil.errResult("orderid_Is_Null");
		}
		if (jedisDao.EXISTS(orderid)) {
			LOG.info("Exit_Game_Active [{}]", orderid);
			return ResultUtil.buildOKResult(jedisDao.del(orderid));
		}
		LOG.error("{} Not_Found", orderid);
		return ResultUtil.errResult("找不到订单 " + orderid);
	}
}
