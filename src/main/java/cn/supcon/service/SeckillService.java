package cn.supcon.service;


import cn.supcon.dto.Exposer;
import cn.supcon.dto.SeckillExecution;
import cn.supcon.entity.Seckill;
import cn.supcon.exception.RepeatKillException;
import cn.supcon.exception.SeckillCloseException;
import cn.supcon.exception.SeckillException;

import java.util.List;

/**
 * 业务接口，站在“使用者”的角度上设计接口
 * 三个方面：方法定义粒度，参数，返回类型和异常
 */
public interface SeckillService {


    List<Seckill> getSeckillList();

    Seckill getById(long seckillId);


    /**
     * 秒杀开启时输出秒杀接口地址，
     * 否则输出系统时间和秒杀时间
     * @param seckillId
     */
    Exposer exportSeckillUrl(long seckillId);

    /**
     * 执行秒杀操作
     * @param seckillId
     * @param userPhone
     * @param md5
     */
    SeckillExecution executeSeckill(long seckillId, long userPhone, String md5) throws SeckillException, RepeatKillException, SeckillCloseException;


    /**
     * 执行秒杀操作 通过 存储过程
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     * @throws SeckillException
     * @throws RepeatKillException
     * @throws SeckillCloseException
     */
    SeckillExecution executeSeckillProcedure(long seckillId, long userPhone, String md5);

}
