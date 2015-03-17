-- One event post with wrk2 (https://github.com/giltene/wrk2)
-- HTTP method, body, and adding a header

wrk.method = "POST"
wrk.headers["Content-Type"] = "application/json"
wrk.body   = "[ { \"timestamp\": 1424444803463, \"sourceId\": \"3aw4sedrtcyvgbuhjkn\",  \"eventName\": \"user.clicked\", \"page\": \"orders\", \"item\": \"sku-1234\" }]"
