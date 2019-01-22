-- 秒杀执行存储过程
-- console ; 转化成 $$ 将换行标志；换成$$
DELIMITER $$
-- 定义存储过程
-- 参数： in 表示输入参数；out 表示输出参数
-- row_count() 是一个函数，返回上一条修改类型的sql的影响函数
-- row_count : 0 : 未修改数据； >0： 表示修改的行数； <0: sql错误/未执行修改sql
CREATE PROCEDURE `seckill`.`execute_seckill`
    (in v_seckill_id bigint,
     in v_phone bigint,
     in v_kill_time timestamp,
     out r_result int)
    BEGIN
        DECLARE insert_count int DEFAULT 0;
        START TRANSACTION;
        insert ignore into success_killed
            (seckill_id, user_phone, create_time)
            values (v_seckill_id, v_phone, v_kill_time);
        select row_count() into insert_count;
        IF (insert_count = 0) THEN
            ROLLBACK;
            set r_result = -1;
        ELSEIF (insert_count < 0) THEN
            ROLLBACK;
            set r_result = -2;
        ELSE
            update seckill
            set number = number -1
            where seckill_id = v_seckill_id
                and end_time > v_kill_time
                and start_time < v_kill_time
                and number > 0;
            select row_count() into insert_count;
            IF (insert_count = 0) THEN
                ROLLBACK;
                set r_result = 0;
            ELSEIF (insert_count < 0) THEN
                ROLLBACK;
                set r_result = -2;
            ELSE
                COMMIT;
                set r_result = 1;
            END IF;
        END IF;
    END;
$$
-- 存储过程定义结束
-- 把换行标志重新改成;
DELIMITER ;
-- 定义r_result
set @r_result=-3;
-- 执行存储过程
call execute_seckill(1002, 13588856787, now(), @r_result);
-- 获取结果
select @r_result;

-- 存储过程
-- 存储过程优化：事务行级锁持有时间
-- 不要过度依赖存储过程，银行应用较多，其他地方应用较少
-- 简单的逻辑可以应用存储过程
-- QPS：一个秒杀单 6000/qps