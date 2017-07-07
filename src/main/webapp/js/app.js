$(document).ready(function ($) {

    var account = {};
    var cardBlock = $(".card-footer");
    var currencyTabs = $("#currencyTabs");
    var currencyGraphs = $("#currencyGraphs");

    updateCandles();
    function updateCandles() {
        $.ajax({url: "/api/candles"}).done(function (data) {
            var showTb = null;
            for (var key in data) {
                if (!data.hasOwnProperty(key)) continue;
                currencyTabs.append('<a class="tb" href="#" style="padding: 10px;">' + key + '</a>');
                createGraph(key, data[key]);
                createIchimoku(key, data[key]);
                if (!showTb) {
                    showTb = key;
                    $("a").filter(function (index) {
                        return $(this).html() == key;
                    }).addClass("green");
                }
            }
            $(".chart-wrapper").hide();
            $(".tb").click(function () {
                $(".chart-wrapper").hide();
                var key = $(this).html();
                $("#cw_" + key).show();
                $("#ichimoku_" + key).show();
                $(".tb").removeClass("green");
                $(this).addClass("green");
            });

            $("#cw_" + showTb).show();
            $("#ichimoku_" + showTb).show();
        });
    }

    updateAccountInfo();
    function updateAccountInfo() {
        $.ajax({url: "/api/account"}).done(function (data) {
            account = data;
            $('#accountId').html('<img height="50" src="img/maneki-neko.gif" alt="Maneki Neko"> ID: ' + account.id);
            $('#accountBalance').html(account.balance + " " + account.currency);
            $('#ordersCount').html(account.pendingOrderCount);
            $('#positionsCount').html(account.openPositionCount);
            $('#tradesCount').html(account.openTradeCount);
            $('#lastTransactionID').html(account.lastTransactionID);

            $('#marginRate').html(account.marginRate);
            $('#resettablePL').html(account.resettablePL);
            $('#financing').html(account.financing);
            $('#currency').html(account.currency);

            var accountInfo = $("#account-info");
            accountInfo.html("");

            accountInfo.append("<h4>Orders</h4>");
            accountInfo.append("<table id=\"ordersTable\" class=\"table table-striped\" style=\"background-color: white;\">");
            $("#ordersTable").append($("<thead>").append("<tr>").html(
                "<th>id</th> <th>instrument</th> <th>type</th> <th>price</th>" +
                "<th>units</th> <th>stop loss</th> <th>take profit</th> <th>create time</th>" +
                "<th>cancelled time</th> <th>time in force</th> <th>gtd time</th> <th>state</th>"
            ));
            new Map(Object.entries(account.orders)).forEach(orders);

            accountInfo.append("<h4>Trades</h4>");
            accountInfo.append("<table id=\"tradesTable\" class=\"table table-striped\" style=\"background-color: yellow;\">");
            $("#tradesTable").append($("<thead>").append("<tr>").html(
                "<th>id</th> <th>instrument</th> <th>price</th> <th>initial units</th>" +
                "<th>current units</th> <th>open time</th> <th>financing</th> <th>realizedPL</th>" +
                "<th>unrealizedPL</th> <th>state</th>"
            ));
            new Map(Object.entries(account.trades)).forEach(trades);

            accountInfo.append("<h4>Positions</h4>");
            accountInfo.append("<table id=\"positionsTable\" class=\"table table-striped\" style=\"background-color: #63c2de;\">");
            $("#positionsTable").append($("<thead>").append("<tr>").html(
                "<th>instrument</th> <th>pl</th> <th>resettablePL</th> <th>unrealizedPL</th>"
            ));
            new Map(Object.entries(account.positions)).forEach(positions);

            function orders(value, key, map) {
                if (value instanceof Object) {
                    var tr = $('<tr>');
                    tr.append($("<td>").text(value.id));
                    tr.append($("<td>").text(value.instrument));
                    tr.append($("<td>").text(value.orderType));
                    tr.append($("<td>").text(value.price));
                    tr.append($("<td>").text(value.units));
                    tr.append($("<td>").text(value.stopLoss));
                    tr.append($("<td>").text(value.takeProfit));
                    tr.append($("<td>").text(value.createTime));
                    tr.append($("<td>").text(value.cancelledTime));
                    tr.append($("<td>").text(value.timeInForce));
                    tr.append($("<td>").text(value.gtdTime));
                    tr.append($("<td>").text(value.state));
                    $("#ordersTable").append(tr);
                }
            }

            function trades(value, key, map) {
                if (value instanceof Object) {
                    var tr = $('<tr>');
                    tr.append($("<td>").text(value.id));
                    tr.append($("<td>").text(value.instrument));
                    tr.append($("<td>").text(value.price));
                    tr.append($("<td>").text(value.initialUnits));
                    tr.append($("<td>").text(value.currentUnits));
                    tr.append($("<td>").text(value.openTime));
                    tr.append($("<td>").text(value.financing));
                    tr.append($("<td>").text(value.realizedPL));
                    tr.append($("<td>").text(value.unrealizedPL));
                    tr.append($("<td>").text(value.state));
                    $("#tradesTable").append(tr);
                }
            }

            function positions(value, key, map) {
                if (value instanceof Object) {
                    var tr = $('<tr>');
                    tr.append($("<td>").text(value.instrument));
                    tr.append($("<td>").text(value.pl));
                    tr.append($("<td>").text(value.resettablePL));
                    tr.append($("<td>").text(value.unrealizedPL));
                    $("#positionsTable").append(tr);
                }
            }
        });
    }

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

    function createIchimoku(key, value) {
        var margin = {top: 20, right: 20, bottom: 30, left: 50},
            width = cardBlock.width() - margin.left - margin.right,
            height = 400 - margin.top - margin.bottom;

        var x = techan.scale.financetime()
            .range([0, width]);

        var y = d3.scaleLinear()
            .range([height, 0]);

        var candlestick = techan.plot.candlestick()
            .xScale(x)
            .yScale(y);

        var ichimoku = techan.plot.ichimoku()
            .xScale(x)
            .yScale(y);

        var xAxis = d3.axisBottom(x);

        var yAxis = d3.axisLeft(y)
            .tickFormat(d3.format(",.3s"));

        currencyGraphs.append('<div id="ichimoku_' + key + '" class="chart-wrapper" style="height:400px;margin-top:40px;"></div>');

        var svg = d3.select("#ichimoku_" + key)
            .append("svg")
            .attr("width", width + margin.left + margin.right)
            .attr("height", height + margin.top + margin.bottom)
            .append("g")
            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

        svg.append("clipPath")
            .attr("id", "clip")
            .append("rect")
            .attr("x", 0)
            .attr("y", y(1))
            .attr("width", width)
            .attr("height", y(0) - y(1));

        var accessor = candlestick.accessor();
        value = value.map(function (d) {
            return {
                date: new Date(d.dateTime),
                volume: +d.volume,
                open: +d.openMid,
                high: +d.highMid,
                low: +d.lowMid,
                close: +d.closeMid
            };
        }).sort(function (a, b) {
            return d3.ascending(accessor.d(a), accessor.d(b));
        });

        svg.append("g")
            .attr("class", "ichimoku")
            .attr("clip-path", "url(#clip)");

        svg.append("g")
            .attr("class", "candlestick")
            .attr("clip-path", "url(#clip)");

        svg.append("g")
            .attr("class", "x axis")
            .attr("transform", "translate(0," + height + ")");

        svg.append("g")
            .attr("class", "y axis")
            .append("text")
            .attr("transform", "rotate(-90)")
            .attr("y", 6)
            .attr("dy", ".71em")
            .style("text-anchor", "end")
            .text("Ichimoku");

        var ichimokuIndicator = techan.indicator.ichimoku();
        var indicatorPreRoll = ichimokuIndicator.kijunSen() + ichimokuIndicator.senkouSpanB();
        var ichimokuData = ichimokuIndicator(value);

        x.domain(value.map(ichimokuIndicator.accessor().d));
        y.domain(techan.scale.plot.ichimoku(ichimokuData.slice(indicatorPreRoll - ichimokuIndicator.kijunSen())).domain());
        x.zoomable().clamp(false).domain([indicatorPreRoll, value.length + ichimokuIndicator.kijunSen()]);
        svg.selectAll("g.candlestick").datum(value).call(candlestick);
        svg.selectAll("g.ichimoku").datum(ichimokuData).call(ichimoku);
        svg.selectAll("g.x.axis").call(xAxis);
        svg.selectAll("g.y.axis").call(yAxis);
    }

    function createGraph(key, value) {
        var labels = [];
        var datasets = [];

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

        currencyGraphs.append('<div id="cw_' + key + '" class="chart-wrapper" style="height:400px;margin-top:40px;">' +
            '<canvas id="graph_' + key + '" class="chart" height="400"></canvas>' +
            '</div>');

        var graph = document.getElementById("graph_" + key).getContext("2d");

        new Chart(graph, {
            type: 'line',
            data: {
                labels: labels,
                datasets: datasets
            }
        });
    }

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

    setInterval(function () {
        updateAccountInfo()
    }, 60000);

    setInterval(function () {
        updateCandles()
    }, 86400000);
});
