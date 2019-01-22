package cn.supcon.service.impl;

import cn.supcon.dao.SeckillDao;
import cn.supcon.dao.SuccessKilledDao;
import cn.supcon.dao.chche.RedisDao;
import cn.supcon.dto.Exposer;
import cn.supcon.dto.SeckillExecution;
import cn.supcon.entity.Seckill;
import cn.supcon.entity.SuccessKilled;
import cn.supcon.enums.SeckillStatEnum;
import cn.supcon.exception.RepeatKillException;
import cn.supcon.exception.SeckillCloseException;
import cn.supcon.exception.SeckillException;
import cn.supcon.service.SeckillService;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 当不知道是什么组件的时候，使用 @Component 所有的组件，
@Service
public class SeckillServiceImpl implements SeckillService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SeckillDao seckillDao;

    @Autowired
    private SuccessKilledDao successKilledDao;

    @Autowired
    private RedisDao redisDao;


    // md5 盐值支付串，用户混淆md5
    private final String slat = "sdfkgdadf21412421^&*";


    @Override
    public List<Seckill> getSeckillList() {
        return seckillDao.queryAll(0, 4);
    }

    @Override
    public Seckill getById(long seckillId) {
        return seckillDao.queryById(seckillId);
    }

    @Override
    public Exposer exportSeckillUrl(long seckillId) {

        // 1. 访问redis
        Seckill seckill = redisDao.getSeckill(seckillId);
        if (seckill == null) {
            // 2. 访问数据库
            seckill = seckillDao.queryById(seckillId);
            if (seckill == null) {
                return new Exposer(false, seckillId);
            } else {
                redisDao.putSeckill(seckill);
            }
        }
        Date startTime = seckill.getStartTime();
        Date endTime = seckill.getEndTime();
        Date nowTime = new Date();
        if (nowTime.getTime() < startTime.getTime() || nowTime.getTime() > endTime.getTime()) {
            return new Exposer(false, seckillId, nowTime.getTime(), startTime.getTime(), endTime.getTime());
        }

        // 转化特定字符串的过程，不可逆
        String md5 = getMD5(seckillId);
        return new Exposer(true, md5, seckillId);
    }

    @Override
    @Transactional
    /**
     * 使用注解控制事务方法的优点：
     * 1. 开发团队达成一致约定，明确标注事务方法的编程风格
     * 2. 保证事务方法的执行时间尽可能的短，不要穿插其他的网络操作，或者剥离到事务方法外部
     * 3. 不是所有的方法都需要事务，如只有一条修改操作或者只读操作
     */
    public SeckillExecution executeSeckill(long seckillId, long userPhone, String md5) throws SeckillException, RepeatKillException, SeckillCloseException {
        if (md5 == null || !md5.equals(getMD5(seckillId))) {
            throw new SeckillException("seckill data rewrite");
        }
        // 执行秒杀逻辑
        Date nowTime = new Date();
        try {
            // 先减库存，然后插入购买明细，这样会有两次网络延迟
//            // 减库存
//            int updateConut = seckillDao.reduceNumber(seckillId, nowTime);
//            if (updateConut <= 0) {
//                // 没有更新到记录，秒杀结束
//                throw new SeckillCloseException("seckill is close");
//            } else {
//                // 购买行为
//                int insertCount = successKilledDao.insertSuccessKilled(seckillId, userPhone);
//                if (insertCount <= 0) {
//                    throw new RepeatKillException("seckill repeated");
//                } else {
//                    // 秒杀成功
//                    SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(seckillId, userPhone);
//                    return new SeckillExecution(seckillId, SeckillStatEnum.SUCCESS, successKilled);
//                }
//
//            }

            // 以下方式是先增加购买明细，然后减库存，这样只有一次网络延迟

            // 记录购买明细
            int insertCount = successKilledDao.insertSuccessKilled(seckillId, userPhone);
            // 唯一的seckillId, userPhone
            if (insertCount <= 0) {
                throw new RepeatKillException("seckill repeated");
            } else {
                // 减库存，热点商品竞争
                int updateCount = seckillDao.reduceNumber(seckillId, nowTime);
                if (updateCount <= 0) {
                    throw new SeckillCloseException("seckill is close");
                } else {
                    // 秒杀成功
                    SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(seckillId, userPhone);
                    return new SeckillExecution(seckillId, SeckillStatEnum.SUCCESS, successKilled);
                }
            }
        } catch (SeckillCloseException | RepeatKillException e1) {
            throw e1;
        } catch (Exception e3) {
            // 捕获其他的异常
            logger.error(e3.getMessage(), e3);
            throw new SeckillException("seckill inner error " + e3.getMessage());
        }
    }

    @Override
    public SeckillExecution executeSeckillProcedure(long seckillId, long userPhone, String md5) {
        if (md5 == null || !md5.equals(getMD5(seckillId))) {
            return new SeckillExecution(seckillId, SeckillStatEnum.DATA_REWRITE);
        }
        Date killTime = new Date();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("seckillId", seckillId);
        map.put("phone", userPhone);
        map.put("killTime", killTime);
        map.put("resutl", null);
        try {
            seckillDao.killByProcedure(map);
            int result = MapUtils.getInteger(map, "result", -2);
            if (result == 1) {
                SuccessKilled sk = successKilledDao.queryByIdWithSeckill(seckillId, userPhone);
                return new SeckillExecution(seckillId, SeckillStatEnum.SUCCESS, sk);
            } else {
                return new SeckillExecution(seckillId, SeckillStatEnum.stateOf(result));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new SeckillExecution(seckillId, SeckillStatEnum.INNER_ERROR);
        }
    }


    private String getMD5(long seckillId) {
        String base = seckillId + "/" + slat;
        String md5 = DigestUtils.md5DigestAsHex(base.getBytes());
        return md5;
    }
}
