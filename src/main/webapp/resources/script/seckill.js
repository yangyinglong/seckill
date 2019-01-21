//存放主要交互逻辑js代码
//javascript 模块化
var seckill = {
	// 封装秒杀相关ajax的URL
	URL : {
		now: function() {
			return '/seckill/time/now';
		},
		exposer: function(seckillId) {
			return '/seckill/' + seckillId + '/exposer';
		},
		execution: function(seckillId, md5) {
			return '/seckill/' + seckillId + '/' + md5 + '/execution';
		}
	},

	handleSeckill : function(seckillId, node) {
		// 获取秒杀地址，控制显示逻辑，执行秒杀
		console.log("seckillId:" + seckillId);
		node.hide().html('<button class="btn btn-primary btn-lg" id="killBtn">开始秒杀</button>');
		console.log("url="+seckill.URL.exposer(seckillId));
		$.post(seckill.URL.exposer(seckillId), {}, function(result){
			// 在回调函数中，执行交互流程
			if (result && result['success']) {
				var exposer = result['date'];
				if (exposer['exposed']) {
					// 开启秒杀
					// 获取秒杀地址
					var md5 = exposer['md5'];
					var killUrl = seckill.URL.execution(seckillId, md5);
					// 用 one 只绑定一次点击事件
					$('#killBtn').one('click', function(){
						// 执行秒杀请求
						// 1. 先禁用按钮
						$(this).addClass('disabled')
						// 2. 发送秒杀请求
						$.post(killUrl, {}, function(result){
							if (result && result['success']) {
								var killResult = result['date'];
								var state = killResult['state'];
								var stateInfo = killResult['stateInfo'];
								// 显示秒杀结果
								node.html('<span class="label label-success">' + stateInfo + '</span>')
							}

						});
					});
					node.show();
				} else {
					// 未开启秒杀
					var now = exposer['now'];
					var start = exposer['start'];
					var end = exposer['end'];
					// 重新计算计时逻辑
					seckill.countdown(seckillId, now, start, end);
				}
			} else {
				console.log('result:' + result);
			}
		});
	},

	// 验证手机号码
	validatePhone : function(phone) {
		if (phone && phone.length == 11 && !isNaN(phone)) {
			return true;
		} else {
			return false;
		}
	},

	countdown:function(seckillId, nowTime, startTime, endTime){
		console.log("countdown");
		console.log("nowTime="+nowTime);
		console.log("startTime="+startTime);
		console.log("endTime="+endTime);
		var seckillBox = $('#seckill-box');
		if (nowTime > endTime) {
			// 秒杀结束
			seckillBox.html('秒杀结束！');
		} else if(nowTime < startTime) {
			// 秒杀还未开始，计时事件绑定
			var killTime = new Date(startTime + 1000);
			seckillBox.countdown(killTime, function(event){
				// 控制时间格式
				var format = event.strftime('秒杀倒计时： %D天 %H时 %M分 %S秒');
				seckillBox.html(format);
			}).on('finish.countdown', function(){
				// 获取秒杀地址，控制实现逻辑，执行秒杀
				seckill.handleSeckill(seckillId, seckillBox);
			});
			seckillBox.html('秒杀未开始！')
		} else {
			// 秒杀开始
			seckill.handleSeckill(seckillId, seckillBox);
		}
	},

	// 详情页秒杀逻辑
	detail:{
		// 详情页初始化
		init:function(params) {
			// 收集验证和登录，计时交互
			// 规划交互流程
			// 在 cookie 中查找手机号
			var killPhone = $.cookie('killPhone');
			// 验证收集号
			if (!seckill.validatePhone(killPhone)) {
				// 绑定phone
				// 控制输出
				var killPhoneModel = $('#killPhoneModel')
				killPhoneModel.modal({
					show:true, // 显示弹出层
					backdrop:'static',  // 禁止位置关闭
					keyboard: false	// 关闭键盘事件
				});
				$('#killPhoneBtn').click(function(){
					var inputPhone = $('#killPhoneKey').val();
					if (seckill.validatePhone(inputPhone)) {
						// 电话写入cookie
						$.cookie('killPhone', inputPhone, {expires: 7, paht: '/seckill'})
						// 刷新页面
						window.location.reload();
					} else {
						$('#killPhoneMessage').hide('').html('<label class="label label-danger">手机号错误！</label>').show(300);
					}
				});
			}
			// 已经登录
			// 计时交互
			var startTime = params['startTime'];
			var endTime = params['endTime'];
			var seckillId = params['seckillId']
			$.get(seckill.URL.now(), {}, function(result){
				if (result && result['success']) {
					var nowTime = result['date'];
					seckill.countdown(seckillId, nowTime, startTime, endTime);
				} else {
					console.log('result:'+result)
				}
			});
		}
	}
}