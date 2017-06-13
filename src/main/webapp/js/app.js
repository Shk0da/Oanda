$(document).ready(function ($) {

    var account = {};
    var heartbeat = document.getElementById("heartbeat").getContext('2d');

    $.ajax({url: "/api/account"}).done(function (data) {
        account = data;
        $('#accountId').html("ID: " + account.id);
        $('#accountBalance').html(account.balance + " " + account.currency);
        $('#ordersCount').html(account.orders.length);
        $('#positionsCount').html(account.positions.length);
        $('#tradesCount').html(account.trades.length);
        $('#lastTransactionID').html(account.lastTransactionID);
    });

    $.ajax({url: "/api/candles"}).done(function (data) {

        var backgroundColor = [
            'rgba(255, 99, 132, 0.2)',
            'rgba(54, 162, 235, 0.2)',
            'rgba(255, 206, 86, 0.2)',
            'rgba(75, 192, 192, 0.2)',
            'rgba(153, 102, 255, 0.2)',
            'rgba(255, 159, 64, 0.2)'
        ];

        var borderColor = [
            'rgba(255,99,132,1)',
            'rgba(54, 162, 235, 1)',
            'rgba(255, 206, 86, 1)',
            'rgba(75, 192, 192, 1)',
            'rgba(153, 102, 255, 1)',
            'rgba(255, 159, 64, 1)'
        ];

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

            datasets.push(
                {
                    label: label,
                    data: values,
                    backgroundColor: backgroundColor,
                    borderColor: borderColor,
                    borderWidth: 1
                }
            );
        }

        var myChart = new Chart(heartbeat, {
            type: 'line',
            data: {
                labels: labels,
                datasets: datasets
            },
            options: {
                scales: {
                    yAxes: [{
                        ticks: {
                            beginAtZero: true
                        }
                    }]
                }
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
