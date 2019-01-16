package cn.supcon.dao;

import cn.supcon.entity.SuccessKilled;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;


@RunWith(SpringJUnit4ClassRunner.class)
// 告诉Junit Spring配置文件
@ContextConfiguration({"classpath:spring/spring-dao.xml"})
public class SuccessKilledDaoTest {

    @Resource
    private SuccessKilledDao successKilledDao;


    @Test
    public void testInsertSuccessKilled() throws Exception {
        long id = 1000L;
        long userPhone = 15968161237L;
        int insertCount = successKilledDao.insertSuccessKilled(id, userPhone);
        System.out.println(insertCount);
    }

    @Test
    public void testQueryByIdWithSeckill() throws Exception {
        long id = 1000L;
        long userPhone = 15968161237L;
        SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(id, userPhone);
        System.out.println(successKilled);
        System.out.println(successKilled.getSeckill());
    }

}