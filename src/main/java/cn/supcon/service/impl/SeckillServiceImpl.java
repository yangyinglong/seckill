package cn.supcon.service.impl;

import cn.supcon.dao.SeckillDao;
import cn.supcon.dao.SuccessKilledDao;
import cn.supcon.dto.Exposer;
import cn.supcon.dto.SeckillExecution;
import cn.supcon.entity.Seckill;
import cn.supcon.entity.SuccessKilled;
import cn.supcon.enums.SeckillStatEnum;
import cn.supcon.exception.RepeatKillException;
import cn.supcon.exception.SeckillCloseException;
import cn.supcon.exception.SeckillException;
import cn.supcon.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.List;

// 当不知道是什么组件的时候，使用 @Component 所有的组件，
@Service
public class SeckillServiceImpl implements SeckillService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SeckillDao seckillDao;

    @Autowired
    private SuccessKilledDao successKilledDao;


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

        Seckill seckill = seckillDao.queryById(seckillId);
        if (seckill == null) {
            return new Exposer(false, seckillId);
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
            // 减库存
            int updateConut = seckillDao.reduceNumber(seckillId, nowTime);
            if (updateConut <= 0) {
                // 没有更新到记录，秒杀结束
                throw new SeckillCloseException("seckill is close");
            } else {
                // 购买行为
                int insertCount = successKilledDao.insertSuccessKilled(seckillId, userPhone);
                if (insertCount <= 0) {
                    throw new RepeatKillException("seckill repeated");
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



    private String getMD5(long seckillId) {
        String base = seckillId + "/" + slat;
        String md5 = DigestUtils.md5DigestAsHex(base.getBytes());
        return md5;
    }
}
