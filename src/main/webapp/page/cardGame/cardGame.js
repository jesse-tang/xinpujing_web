
$(function () {
   "use strict";

    var gameType = getQueryString('gameType');
    if (gameType === null) {
        gameType = 'jb'
    } else {
        $(".gameTab").removeClass('CardTabStyle')
        $("#" + gameType + "Btn").addClass("CardTabStyle")
    }

   var data = {
       liveCode: gameType,
       page:1,
       rows: 100,
       isFlash: 0

   };

    getAllCardGameList(data);

});


/***
 *  获取棋牌 列表
 */

function getAllCardGameList(data) {
    $(".jbao-list-wrap").append("<img style=\"width: 160px;position: absolute;top: 200px;left: 422px\" src=\"/page/cardGame/img/load.gif\" />");
    // $.getJSON('/api/live/qst', data).then(response => {
    //     $(".jbao-list-wrap").empty();
    //     response.data.forEach((item) => {
    //         $("<div/>", {
    //             title: item.chineseName,
    //             class: "jbao-list-row",
    //             "onclick": "entereleGame(this)",
    //             "liveCode": item.liveCode,
    //             "gameType": item.gameType,
    //             "gameKind": item.gameKind,
    //             html: "<div class='jbao-list-icno'><img src='/data/" + data.liveCode + "Picture/" + item.imageName + "' /></div><div class='jbao-list-name'>" + item.chineseName + "</div>"
    //         }).appendTo($(".jbao-list-wrap"));
    //     })
    // })
    $.getJSON('/api/live/qst', data, function (response) {
        $(".jbao-list-wrap").empty();
        for (var i = 0; i < response.data.length; i++) {
            $("<div/>", {
                title: response.data[i].chineseName,
                class: "jbao-list-row",
                "onclick": "entereleGame(this)",
                "liveCode": response.data[i].liveCode,
                "gameType": response.data[i].gameType,
                "gameKind": response.data[i].gameKind,
                html: "<div class='jbao-list-icno'><img onerror=\"this.src='/page/eleGame/img/404.png'\" src='/data/" + data.liveCode + "Picture/" + response.data[i].imageName + "' /></div><div class='jbao-list-name'>" + response.data[i].chineseName + "</div>"
            }).appendTo($(".jbao-list-wrap"));
        }
    })
}

/***
 **   获取url 参数 判断游戏种类
 */

function getQueryString(gameType) {
    var reg = new RegExp("(^|&)" + gameType + "=([^&]*)(&|$)", "i");
    var r = window.location.search.substr(1).match(reg);
    if (r != null) {
        return unescape(r[2]);
    }
    return null;
}

/***
 **   切换游戏
 */

function gameTab(that, gameType) {
    var data = {
        liveCode: gameType,
        page:1,
        rows: 100,
        isFlash: 0
    };
    getAllCardGameList(data)
    $(".gameTab").removeClass('CardTabStyle');
    $(that).addClass("CardTabStyle")
}
