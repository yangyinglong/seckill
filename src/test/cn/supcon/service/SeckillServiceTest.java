package cn.supcon.service;


import cn.supcon.dto.Exposer;
import cn.supcon.dto.SeckillExecution;
import cn.supcon.entity.Seckill;
import cn.supcon.exception.RepeatKillException;
import cn.supcon.exception.SeckillCloseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
// 告诉Junit Spring配置文件
@ContextConfiguration({"classpath:spring/spring-*.xml"})
public class SeckillServiceTest {


    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SeckillService seckillService;

    @Test
    public void testGetSeckillList() throws Exception{
        List<Seckill> seckills = seckillService.getSeckillList();
        logger.info("list={}", seckills);
    }

    @Test
    public void testGetById() throws Exception {
        long id = 1000L;
        Seckill seckill = seckillService.getById(id);
        logger.info("seckill={}", seckill);
    }

    @Test
    public void testExportSeckillUrl() throws Exception {
        long id = 1002L;
        Exposer exposer = seckillService.exportSeckillUrl(id);
        logger.info("exposer={}", exposer);
        /**
         * exposer=Exposer{
         *  exposed=true,
         *  md5='c2a6648be7e7cd776bf16212a2f8d81c',
         *  seckillId=1002,
         *  now=0,
         *  start=0,
         *  end=0}
         */
    }

    @Test
    public void testExecuteSeckill() throws Exception {
        long id = 1002L;
        long phone = 13678788978L;
        String md5 = "c2a6648be7e7cd776bf16212a2f8d81c";
        SeckillExecution execution = seckillService.executeSeckill(id, phone, md5);
        logger.info("result={}", execution);
    }

    @Test
    public void testSeckillLogic() throws Exception {
        long id = 1002;
        Exposer exposer = seckillService.exportSeckillUrl(id);
        if (exposer.isExposed()) {
            logger.info("exposer={}", exposer);
            long phone = 17867854567L;
            String md5 = exposer.getMd5();
            try {
                SeckillExecution execution = seckillService.executeSeckill(id, phone, md5);
                logger.info("result={}", execution);
            }catch (RepeatKillException e1) {
                logger.error(e1.getMessage());
            } catch (SeckillCloseException e2) {
                logger.error(e2.getMessage());
            }
        } else {
            logger.warn("exposer={}", exposer);
        }
    }

    @Test
    public void executeSeckillProcedure() {
        long secillId = 1002;
        long phoen = 12122228888L;
        Exposer exposer = seckillService.exportSeckillUrl(secillId);
        if (exposer.isExposed()) {
            String md5 = exposer.getMd5();
            SeckillExecution seckillExecution = seckillService.executeSeckillProcedure(secillId, phoen, md5);
            logger.info(seckillExecution.getStateInfo());
        }

    }


}