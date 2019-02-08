/**
 * Created by Zelei on 2017/4/22.
 */
(function ($) {
    var bankAddressLimit;
    $(function () {
        $.getJSON($.toFullPath('/api/user/info'), null, function (response) {
            if (!response || !response.userInfo || !response.userInfo.type
                || response.userInfo.type === 'TEST') {
                $.confirm({
                    title: '试玩帐号无法提现',
                    content: '马上注册成为会员？',
                    onConfirmed: function (modal, event) {
                        var target = !window.parent === false && window.parent !== window
                            ? window.parent : windown;
                        target.location.href = $.toFullPath(
                            '/views/html/register.html');
                    },
                    onDenied: function (modal, event) {
                        window.location.href = $.toFullPath(
                            '/page/user-center/account/overviews.html');
                    }
                });
            }

            var fullName = response.userInfo.fullName;
            if (!fullName === false) {
                $('#fullNameStatic').text(fullName).show();
                $('#fullName, #fullNameTips').hide();
                $('#fullName').val(fullName);
            } else {
                $('#fullNameStatic').text(fullName).hide();
                $('#fullName, #fullNameTips').show();
                $('#fullName').val(fullName);
            }

            if (!response.userBank === false) {
                renderDisplayForm(response.userBank);
            } else {
                renderInputForm();
            }
        }).error($.errorHandler);

        $.getJSON($.toFullPath('/data/json/limit/userWithdrawLimit.json'), null,
            function (response) {
                if (response) {
                    bankAddressLimit = response.bankAddressLimit || 0;
                    if (bankAddressLimit == 2){
                        $("#AddressInfo").hide();
                    } else if (bankAddressLimit == 1) {
                        $("#AddressInfo .m-r-xs").show();
                    } else if (bankAddressLimit == 0) {
                        $("#AddressInfo .m-r-xs").hide();
                    }
                }
            }).error($.errorHandler);
    });

    function renderInputForm() {
        $('#userBankForm').removeClass('hidden');
        $.getJSON($.toFullPath('/data/json/config.json'), null,
            function (response) {
                $.each(response.rech_bank, function (i, e) {
                    if (e.configKey !== '支付宝' && e.configKey !== '微信支付' && e.configKey
                        !== '财付通') {
                        $('#bankName').append(
                            '<option value="' + e.configKey + '">' + e.configKey
                            + '</option>');
                    }
                });
            }).error($.errorHandler);
        $('#city').citySelect({prov: '北京', city: '东城区', dist: ''});
        $('#userBankForm').validate({
            rules: {
                fullName: {
                    pattern: /([0-9\u4e00-\u9fa5]{2,15})/,
                    remote: {
                        url: $.toFullPath('/v/user/checkUnique2'),
                        type: 'get',
                        data: {
                            type: 1,
                            val: function () {
                                return $('#fullName').val();
                            }
                        }
                    }
                },
                cardNo: {
                    pattern: /^(\d{16,19})$/
                }
            },
            messages: {
                fullName: {
                    pattern: '姓名由2-15个汉字或数字组成',
                    remote: '姓名已存在'
                },
                cardNo: {
                    pattern: '卡号由16到19位数字组成'
                }
            },
            submitHandler: function (form) {
                if (bankAddressLimit==1 && $("#bankAddress").val().replace(/^\s+|\s+$/g,"").length==0) {
                    layer.msg("详细地址为必填项！", {icon: 5}, 1200);
                    return false;
                }
                $.loading();
                var data = $(form).serializeObject();
                data.bankAddress = data.bankAddress.replace(/^\s+|\s+$/g,"");
                $.post($.toFullPath('/api/user/modifyUserInfo'), data,
                    function (response) {
                        $.loaded();
                        $.info('保存成功！', null, function () {
                            $('#fullNameStatic').text(data.fullName).show();
                            $('#fullName, #fullNameTips').hide();
                            $('#fullName').val(data.fullName);
                            renderDisplayForm({
                                fullName: data.fullName,
                                bankName: data.bankName,
                                cardNo: data.cardNo,
                                subAddress: undefinedToEmpty(data.bankProvince)
                                + undefinedToEmpty(data.bankCity) + undefinedToEmpty(
                                    data.bankArea) + undefinedToEmpty(data.bankAddress)
                            });
                        });
                        $.loaded();
                    }, 'text').error($.errorHandler);
            },
            errorPlacement: $.errorPlacement
        });
    }

    function renderDisplayForm(userBank) {
        $('#tip').remove();
        $('#bankName').parent().html(
            '<p id="bankName" class="form-control-static">' + userBank.bankName
            + '</p>');
        $('#cardNo').parent().html(
            '<p id="cardNo" class="form-control-static">' + userBank.cardNo
            + '</p>');
        $('#city').children('div').remove();
        $('#city').append(
            '<div class="col-lg-10"><p id="subAddress" class="form-control-static">'
            + userBank.subAddress + '</p></div>')
        $('#bankAddress').parent().parent().remove();
        $('#userBankForm .fa-required').removeClass('fa-required');
        $('#buttons').remove();
        $('#userBankForm').removeClass('hidden');
    }

    function undefinedToEmpty(str) {
        return !str === false ? str : '';
    }
})(jQuery);