var lotteryBlock = true; // 是否不封盘中true是,false否
var gameConstants = {BET_SEPARATOR : ",", DAN_TUO_SEPARATOR : "|"};

/**
 * 开奖结果
 */
var lotteryManager = (function () {
    var opts = {
        turnNumText:"#turnNumText", // 当前期号DIV
        preTurnNum:"#preTurnNum", // 上期期号DIV
        logo:"#lottery_logo", // 页面LOGO
        gameName : "#game_name", // 页面游戏名称
        flipClockDiv : "#RestTime", // 时间控件DIV
        openResultDiv : "#openResult", // 开奖结果显示DIV
        startCallback : undefined, // 开盘事件
        stopCallback : undefined, // 封盘事件
        openCallback : undefined, // 开奖事件
        gameId :"#gameId"
    };

    var init = function(opt){
        opts = $.extend(opts,opt || {});
        gameLogo();

        //ffc
        if(gameId == 42){
            timeInterval = 2000;
        }
        getLottery();
        window.setInterval(function(){
            getLottery();
        }, timeInterval);
    };
    var up = function(opt,tmb){
        opts = $.extend(opts,opt || {});
        if(isStop && opts.stopCallback){
            opts.stopCallback();
        }
        if(isStart && opts.startCallback){
            opts.startCallback();
        }

        if (tmb) {
            $(".rebateclass").css('display','none')
        } else {
            $(".rebateclass").css('display','block')
        }

    };

    // 获取六合彩弹窗地址
    var getLotteryUrl =function () {
        $.ajax({
            type: "GET",
            url: "/views/lhc_alert_title.html",
            dataType: "text",
            success: function (data) {
                if(data!==null && data!==''&& data!=='/views/image/'){
                    showAlert(data);
                }
            },
            error: function (data) {
                console.log('error');
            }
        });
    };

   // 六合彩弹窗
    var showAlert = function (picUrl) {
        if ($.cookie("lhcAlertStatus", {path: "/"}) != 'ok') {
            $.cookie("lhcAlertStatus", 'ok', {path: "/"});
            layer.open({
                skin: 'lhc-alert-warp',
                type: 1,
                shade: false,
                title: false,
                area: ['600px', '270px'],
                anim: 3,
                scrollbar: false,
                content: '<img style="width:100%;height:100%;cursor: pointer;"  onclick="javascript:window.location.href=\'/data/game/lhc300/index.html\'"  src="' + picUrl + '" />'
            });
            // var layuiHide=window.setTimeout(function () {
            //     $(".layui-layer").animate({left:"2000px"},100,function () {
            //         layer.closeAll();
            //         clearTimeout(layuiHide);
            //     });
            // }, 5000);

        }
    };

    var isStart = false;
    var isStop = false;
    var closeTime = 0;
    var gameCode = '';
    var game = {};
    var gameId=$(opts.gameId).val();
    var timeInterval = 5000; //5s
    //gameId = 26
    // 时钟处理
    if(gameId==70){
        $(".drawNumber .c-resttime").css("width","200px");
        $(".openinfo").css("width","328px");
        $(".drawNumber").css("width","345px");

        //六合彩弹窗
        getLotteryUrl();
    }else {
        $(".drawNumber .c-resttime").css("width","145px");
        $(".openinfo").css("width","285px;");
        $(".drawNumber").css("width","300px");
    }
    var clock = new FlipClock($(opts.flipClockDiv),{
        clockFace: gameId == 70 ? 'DailyCounter' : 'HourCounter',
        countdown: true,
        callbacks:{
            start:function () {
            	xdialog.hide();
                if(opts.startCallback){
                    opts.startCallback();
                }
                isStart = true;
                isStop = false;
                // lotteryBlock = true;
                lotteryBlock = false;
            },
            stop:function(){
            	var content = $("#turnNum").val() + "已截止<br/>投注时请注意期号";
            	xdialog.show({content:content});
                lotteryBlock = true;
                isStart = false;
                isStop = true;
                if(opts.stopCallback){
                    opts.stopCallback();
                }
            }
        }
    });

    var getLottery = function(){
        $.ajax({
            type : "get",
            url : "/v/lottery/openInfo?gameId=" + gameId,
            success : function(data) {
                closeTime = data.cur.closeTime;
                var time = Math.floor((closeTime - data.serverTime)/1000);
                if(time < 0){
                    time = 0;
                }
                if(time > 0){
                    clock.setTime(time);
                }
                if(time <= 0 && !isStop){ // 停止
                    clock.stop();
                }
                if(time > 0 && !isStart){ // 启动
                    clock.start();
                }
                $("#turnNum").val(data.cur.turnNum);
                $(opts.turnNumText).html(data.cur.turnNum);
                var preTurnNum = $(opts.preTurnNum).html();
                $("#showLotteryStatus").html(data.cur.status == 1 ? '开奖中':'开奖结果')
                if($('#showLotteryStatus').html()=='开奖结果'){
                    gameManager.addLotteryResultListener($('#showLotteryStatus'))
                }else{
                    $('#showLotteryStatus').attr('href','javascript:void(0)')
                }
                if(data.pre.turnNum != preTurnNum){ // 只有下期开奖结果出来,出现动画效果
                    result(data.pre);
                    if(opts.openCallback){
                        opts.openCallback();
                    }
                }
            }
        });
    };

    var gameLogo = function(){ // 更新页面logo
        $.ajax({
            type : "get",
            url : "/data/json/games.json",
            success : function(data) {
                for(var i=0;i<data.length;i++){
                    if(data[i].id==gameId){
                        gameCode = data[i].code;
                        game = data[i];
                        $(opts.logo).attr('src','/page/game/img/'+data[i].code+'.png');
                        $(opts.gameName).html(data[i].name);
                        return;
                    }
                }
            }
        });

    };
    var lhc_sx = null;
    var isContain = function (ary, elm) {
        for (var i = 0; i < ary.length; i++) {
            if (ary[i] == elm) {
                return true
            }
        }
        return false
    }

    var result = function(pre){ //开奖结果模板
        var resultData = {};
        resultData['preTurnNum'] = pre.turnNum;
        if(pre.openNum){
            resultData['openNums'] = pre.openNum.split(",");
        }
        var temp = ""; // 模板名称
        if (game.type === 'lhc'){
            if (!lhc_sx) {
                $.ajax({
                    url: '/data/json/lhc_sx.json',
                    type: 'get',
                    dataType: 'json',
                    async: false,
                    success: function (response) {
                        lhc_sx = response;
                    }
                })
            }
            var sx = ['鼠', '牛', '虎', '兔', '龙', '蛇', '马', '羊', '猴', '鸡', '狗', '猪']
            var resultSX = []
            for (var i = 0; i <resultData['openNums'].length; i++) {
                for (var j = 0; j < lhc_sx.length; j++) {
                    if (isContain(lhc_sx[j], resultData['openNums'][i])) {
                        resultSX[i] = sx[j]
                        break
                    }
                }
            }
            resultData['sxs'] = resultSX;
            temp = 'lhc_Template';
        }else if( resultData['openNums'].length >= 10){
            temp = 'PK10_Template';
        }else{
            temp = 'sscTemplate';
        }
        html=template(temp, resultData);
        $(opts.openResultDiv).html(html);
        //判断北京pk10
        if(gameId==22){
            $(".pk10VideoLottery").css("display","inline-block");
        }else{
            $(".pk10VideoLottery").remove();
        }
        //初始化ogg
        oggManager.init();
        oggManager.play();
        rotateYDIV();
    };
    return {init : init, up:up};
})();



