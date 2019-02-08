$(function () {
  'use strict';
  getNotice();
  floatWindowRoll();
  DetectionSpeed();
});

// 公告
getNotice = function () {

    $.getJSON("/data/json/notice_list.json", function (result) {
        if(result.roll_notice!=undefined){
            if(result.roll_notice.length>0){
                var noticeContent = "";
                for (var i = 0; i < result.roll_notice.length; i++) {
                    noticeContent += result.roll_notice[i].noticeContent + "<div style='width: 100px;display: inline-block;'></div>";
                }
                var mar_2 = '<marquee id="ele-msgNews" onmouseover="this.stop()" onmouseleave="this.start()" behavior="scroll" scrollamount="7" direction="left" style="height: 43px;line-height:43px;width:100%;margin-left:-30px">' + noticeContent + '</marquee>';
                $("#noticeCon").html(mar_2);//.liMarquee({direction: 'left',scrollamount:20});
            }else {
                $("#noticeCon").html("暂无公告");
            }
        }else {
            $("#noticeCon").html("暂无公告");
        }
    }).fail(function () {
        $("#noticeCon").html("暂无公告");
    });
};


//规则
rlueWeb = function () {
  window.open('/views/html/rule.html', 'newwindow', 'height=720, width=1020, top=20, left=20, toolbar=no, menubar=no, scrollbars=no, resizable=no, location=no, status=no');
};

//体育
sportTab = function (_this, _type) {
  $(_this).parent().find("li").removeClass("active");
  $(_this).addClass("active");
  switch (_type) {
    case "hgty":
      $(".sport-main-text").text('InplayMatrix是运动博彩行业体育博彩平台的领先厂商和专业提供商。' +
        '我们的技术和数据驱动解决方案普遍适用于满足每个市场需求。InplayMatrix Sportsbook平台使体育博彩运营商有效; ' +
        '效率和盈利能力，帮助您为体育博彩界提供更好的娱乐和游戏体验。');
      break;
    case 'm8ty':
      $(".sport-main-text").text('m8体育是全亚洲最受欢迎的足球平台之一，数据丰富，玩法多样，提供英超，' +
        '德甲，西甲，意甲，法甲欧洲五大联赛，以及南美洲，亚洲各级联赛的竞猜，深受广大玩家喜欢.....');
      break;
    default:
      $(".sport-main-text").text('更多精彩 体育赛事，敬请期待！')
  }
};

// 优惠
activity = function (_this, _type) {
  var _titleHtml = $(_this).find(".youhuiDelite-title").html(),
    _dateHtml = $(_this).find(".youhuiDelite-date").html();
  if ($("#youhui" + _type).length > 0) {
    var _deliteHTml = $("#youhui" + _type).html();
  } else {
    layer.msg("暂未添加详细信息!");
    return false;
  }
  layer.open({
    type: 1,
    title: _titleHtml + "<span>" + _dateHtml + "</span>",
    skin: "LAY_layuipro",
    area: ['800px', '500px'],
    max: true,
    shade: 0.8,
    shadeClose: false,
    id: 'LAY_layuipro',
    resize: false,
    scrollbar: true,
    btnAlign: 'c',
    moveType: 1,
    content: _deliteHTml
  });
};

 //浮窗 滚动
 floatWindowRoll = function () {
     if($('#rightFloat').length>0) {
         var _top = (+$('#rightFloat').css('top').replace('px', ''))
         $(window).scroll(function () {
             $('#rightFloat').stop().animate({top: $(window).scrollTop() + _top}, 1000);
         })
     }
};

// 跳转额度转换
amountConversion = function () {
  var token = $.cookie("token", {path: "/"}),
    userType = $.cookie("userType", {path: "/"});
  if (token == null || token == "") {
    layer.open({
      skin: 'layui-layer-molved',
      closeBtn: 1,
      shadeClose: false,
      anim: 3,
      btn: ['确定'],
      icon: 6,
      content: '请登入后再进入额度转换!!'
    });
    return false;
  }
  if (userType != null) {
    if (userType.toLowerCase() === "test") {
      layer.open({
        skin: 'layui-layer-molved',
        closeBtn: 1,
        shadeClose: false,
        anim: 3,
        btn: ['确定'],
        icon: 6,
        content: '试玩账号不能进入,请注册成正式会员!'
      });
      return false;
    }
  }
  window.location.href = "/page/user-center/user-center.html?startPage=replacement";
};

// 关闭 浮动窗
closeFloat = function (_this) {
  $(_this).parent().hide();
};

// 检测网速()
DetectionSpeed = function () {
  $(".LineDetection-wrap ul").empty();
  var lineYM = [
      "http://dongfweb.zhushuqt.com",
      "http://dbweb.zhushuqt.com",
      "http://qlcweb.zhushuqt.com",
      "http://wcpweb.zhushuqt.com",
      "http://kfcp8888.com",
      "http://betweb.zhushuqt.com",
      "http://betweb.zhushuqt.com",
      "http://betweb.zhushuqt.com"
    ],
    lineSpeend = 0,
    shuffle = lineYM.shuffle();
  for (var x = 0; x < lineYM.length; x++) {
    lineSpeend = Math.ceil(Math.random() * 10 + 20) + lineSpeend;
    $("<li/>", {
      html: '<li><input type="text" value="' + lineSpeend + 'ms" readonly /><span></span><input class="weblink" type="text" value="' + shuffle[x] + '" readonly /><span onclick="webLink(this)"></span></li>'
    }).appendTo($(".LineDetection-wrap ul"));
  }
};

Array.prototype.shuffle = function () {
  var _this = this;
  for (var i = _this.length - 1; i >= 0; i--) {
    var randomIndex = Math.floor(Math.random() * (i + 1));
    var itemAtIndex = _this[randomIndex];

    _this[randomIndex] = _this[i];
    _this[i] = itemAtIndex;
  }
  return _this;
};

webLink = function (_this) {
  window.open($(_this).parent().find(".weblink").val());
};


//代理合作
getPager = function (_this, _ele) {
  $(".midrightnavs").find("a").removeClass("active");
  $(_this).addClass("active");
  $(".baseMessageCon > div").hide();
  $("#" + _ele).show();
};

agentTab = function (_this, _ele) {

  if (_ele === "AgentRES") {
    layer.msg("代理注册暂无开放，敬请期待!", {btn: "知道了"});
    return false;
  }
  if (_ele === "AgentENT") {
    layer.msg("代理登入暂无开放，敬请期待!", {btn: "知道了"});
    return false;
  }
  $(".agent-nav").find("li").removeClass("active");
  $(_this).addClass("active");
  $(".Agent").hide();
  $("#" + _ele).show();
};

//  试玩判断登入方式
// registerStyle = function () {
//     $.getJSON("/data/json/limit/registerLimit.json", function (response) {
//            if ( response.trailUserValidCode === 1) {
//                  openRegisterTrailUser();
//            } else {
//                   registerTest();
//            }
//      }).fail(function(){
//            openRegisterTrailUser();
//      })
// };

function SetHomePage(url){
    if(document.all) {
        document.body.style.behavior='url(#default#homepage)';
        document.body.setHomePage(url);
    }else{
        alert("您好,您的浏览器不支持自动设置页面为首页功能,请您手动在浏览器里设置该页面为首页!");
    }
}

function toggleColor( id , arr , s ){
    var self = this;
    self._i = 0;
    self._timer = null;

    self.run = function(){
        if(arr[self._i]){
            $(id).css('color', arr[self._i]);
        }
        self._i == 0 ? self._i++ : self._i = 0;
        self._timer = setTimeout(function(){
            self.run( id , arr , s);
        }, s);
    }
    self.run();
}