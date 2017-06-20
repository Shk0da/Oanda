$(document).ready(function ($) {

    var account = {};
    var heartbeat = document.getElementById("heartbeat").getContext('2d');

    $.ajax({url: "/api/account"}).done(function (data) {
        account = data;
        $('#accountId').html("ID: " + account.id);
        $('#accountBalance').html(account.balance + " " + account.currency);
        $('#ordersCount').html(account.pendingOrderCount);
        $('#positionsCount').html(account.openPositionCount);
        $('#tradesCount').html(account.openTradeCount);
        $('#lastTransactionID').html(account.lastTransactionID);

        var accountInfo = $("#account-info");
        new Map(Object.entries(data)).forEach(informer);
        function informer(value, key, map) {
            if (value instanceof Object) {
                accountInfo.append("<p>" + key + ": <pre>" + JSON.stringify(value, null, 4) + "</pre></p>");
            } else {
                accountInfo.append("<p>" + key + ": " + value + "</p>");
            }
        }
    });

    $.ajax({url: "/api/candles"}).done(function (data) {
        var labels = [];
        var datasets = [];

        for (var key in data) {
            if (!data.hasOwnProperty(key)) continue;

            var value = data[key];
            var label = key;
            var values = [];
            value.forEach(function (item, i, arr) {
                labels[i] = timeConverter(item.dateTime);
                values[i] = item.closeMid;
            });

            var min = Math.min.apply(Math, values);
            var max = Math.max.apply(Math, values);

            datasets.push(
                {
                    label: label + "(min: " + min + ", max: " + max + ")",
                    data: values,
                    backgroundColor: ['rgba(' + getRandomInt(1, 255) + ',99,' + getRandomInt(1, 255) + ',0.1)'],
                    borderColor: ['rgba(' + getRandomInt(1, 255) + ',99,' + getRandomInt(1, 255) + ',1)'],
                    borderWidth: 1
                }
            );
        }

        new Chart(heartbeat, {
            type: 'line',
            data: {
                labels: labels,
                datasets: datasets
            }
        });
    });

    $("#resetWork").click(function () {
        $.ajax({url: "/api/reset"}).done(function (data) {
            alert(data);
        });
    });

    $("#startWork").click(function () {
        $.ajax({url: "/api/startwork"}).done(function (data) {
            alert(data);
        });
    });

});

function timeConverter(UNIX_timestamp) {
    var a = new Date(UNIX_timestamp);
    var months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    var year = a.getFullYear();
    var month = months[a.getMonth()];
    var date = a.getDate();
    var hour = a.getHours();
    var min = a.getMinutes();
    var sec = a.getSeconds();

    return date + ' ' + month + ' ' + year + ' ' + hour + ':' + min + ':' + sec;
}

function getRandomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}
