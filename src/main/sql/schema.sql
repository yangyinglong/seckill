-- 数据库初始化脚本

-- 创建数据库
CREATE DATABASE seckill;
-- 使用数据库
use seckill;
-- 创建秒杀库存表
CREATE TABLE seckill (
`seckill_id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品库存id',
`name` varchar(120) NOT NULL COMMENT '商品名称',
`number` int NOT NULL COMMENT '库存数量',
`start_time` timestamp NOT NULL COMMENT '秒杀开启时间',
`end_time` timestamp NOT NULL COMMENT '秒杀结束时间',
`create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
PRIMARY KEY (seckill_id),
key idx_start_time(start_time),
key idx_end_time(end_time),
key idx_create_time(create_time)
)ENGINE=InnoDB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8 COMMENT='秒杀库存表';

--ERROR 1067 (42000): Invalid default value for 'end_time'
--mysql> set @@explicit_defaults_for_timestamp = 1;                               Query OK, 0 rows affected (0.00 sec)


-- 初始化数据
insert into
    seckill (name, number, start_time, end_time)
values
    ('1000元秒杀iphone6', 100, '2018-12-31 00:00:00', '2018-12-31 23:59:59'),
    ('500元秒杀ipd2', 200, '2019-1-31 00:00:00', '2019-1-31 23:59:59'),
    ('300元秒杀小米4', 300, '2019-1-1 00:00:00', '2019-12-31 23:59:59'),
    ('200元秒杀红米note', 400, '2018-12-31 00:00:00', '2018-12-31 23:59:59');

-- 秒啊成功明细表
-- 用户登录认证相关的信息
CREATE TABLE success_killed (
`seckill_id` bigint NOT NULL AUTO_INCREMENT COMMENT '秒杀商品id',
`user_phone` bigint NOT NULL COMMENT '用户手机号码',
`state` tinyint NOT NULL DEFAULT -1 COMMENT '状态标志： -1：无效 0：成功 1：已付款',
`create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
PRIMARY KEY (seckill_id, user_phone),
key ids_create_time(create_time)
)ENGINE=InnoDB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8 COMMENT='秒杀明细表';

-- 链接数据库控制台
mysql -u root -p
-- 查看建表语句
show create table seckill\G;
