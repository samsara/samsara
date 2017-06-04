package client

import (
	"bytes"
	"compress/gzip"
	"encoding/json"
	"io/ioutil"
	"math"
	"net/http"
	"net/http/httptest"
	"reflect"
	"strconv"
	"testing"
	"time"
)

func TestPublisher_ApiPath(t *testing.T) {
	mockServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.RequestURI != "/v1/events" {
			t.Errorf("Incorrect api path. Got %q", r.RequestURI)
		}
	}))
	defer mockServer.Close()

	config := NewConfig()
	config.Url = mockServer.URL

	publisher := Publisher{config}
	publisher.Post([]Event{{"sourceId": "foo", "eventName": "baz", "timestamp": int64(1479988864057)}})
}

func TestPublisher_Post_HeadersCheck(t *testing.T) {
	mockServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Header.Get("Accept") != "application/json" {
			t.Errorf("Request should contain header 'Accept' => 'application/json'. Got %v", r.Header)
		}
		if r.Header.Get("Content-Type") != "application/json" {
			t.Errorf("Request should contain header 'Content-Type' => 'application/json'. Got %v", r.Header)
		}
		if r.Header.Get("Content-Encoding") != "identity" {
			t.Errorf("Request should contain header 'Content-Encoding' => 'identity'. Got %v", r.Header)
		}
		stamp, err := strconv.ParseInt(r.Header.Get("X-Samsara-Publishedtimestamp"), 10, 64)
		if err != nil || stamp > Timestamp() {
			t.Errorf("Request should contain header 'X-Samsara-Publishedtimestamp' with timestamp . Got %v", r.Header)
		}
	}))
	defer mockServer.Close()

	config := NewConfig()
	config.Url = mockServer.URL
	config.Compression = "none"

	publisher := Publisher{config}
	publisher.Post([]Event{{"sourceId": "foo", "eventName": "baz", "timestamp": int64(1479988864057)}})
}

func TestPublisher_Post_DataTransmissionCheck(t *testing.T) {
	canonicGzip := func(input []Event) []byte {
		var buf bytes.Buffer
		data, _ := json.Marshal(input)
		compressor := gzip.NewWriter(&buf)
		compressor.Write(data)
		compressor.Close()
		return buf.Bytes()
	}
	canonicJSON := func(input []Event) []byte {
		data, _ := json.Marshal(input)
		return data
	}

	sets := []struct {
		compression string
		data        []Event
		want        interface{}
	}{
		{
			compression: "none",
			data:        []Event{},
			want:        canonicJSON([]Event{}),
		},
		{
			compression: "none",
			data:        []Event{{"sourceId": "foo", "eventName": "baz", "timestamp": int64(1479988864057)}},
			want: canonicJSON([]Event{
				{"sourceId": "foo", "eventName": "baz", "timestamp": int64(1479988864057)},
			}),
		},
		{
			compression: "none",
			data:        []Event{{"sourceId": "событие", "你": "baz", "timestamp": Timestamp()}},
			want:        canonicJSON([]Event{{"sourceId": "событие", "你": "baz", "timestamp": Timestamp()}}),
		},
		{
			compression: "gzip",
			data:        []Event{},
			want:        canonicGzip([]Event{}),
		},
		{
			compression: "gzip",
			data:        []Event{{"sourceId": "foo", "eventName": "baz", "timestamp": int64(123456)}},
			want:        canonicGzip([]Event{{"sourceId": "foo", "eventName": "baz", "timestamp": int64(123456)}}),
		},
		{
			compression: "gzip",
			data:        []Event{{"sourceId": "событие", "你": "baz", "timestamp": Timestamp()}},
			want:        canonicGzip([]Event{{"sourceId": "событие", "你": "baz", "timestamp": Timestamp()}}),
		},
	}

	for i, set := range sets {
		mockServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			payload, _ := ioutil.ReadAll(r.Body)
			if !reflect.DeepEqual(payload, set.want) {
				t.Errorf("Set #%d. Incorrect payload sent.\nWant: %+v\nGot: %+v", i, set.want, payload)
			}
		}))
		defer mockServer.Close()

		config := NewConfig()
		config.Url = mockServer.URL
		config.Compression = set.compression

		publisher := Publisher{config}
		publisher.Post(set.data)
	}
}

func TestPublisher_Post_MalformedData(t *testing.T) {
	publisher := Publisher{NewConfig()}
	success := publisher.Post([]Event{{"sourceId": "foo", "timestamp": math.NaN()}})
	if success != false {
		t.Error("Post method should return false if data marshalling failed")
	}
}

func TestPublisher_Post_SendTimeout(t *testing.T) {
	sets := []struct {
		responseTime uint32
		timeout      uint32
		want         bool
	}{
		{responseTime: 6, timeout: 5, want: false},
		{responseTime: 2, timeout: 5, want: true},
	}

	for i, set := range sets {
		mockServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			time.Sleep(time.Duration(set.responseTime) * time.Millisecond)
			w.WriteHeader(http.StatusAccepted)
		}))
		defer mockServer.Close()

		config := NewConfig()
		config.Url = mockServer.URL
		config.SendTimeout = uint32(set.timeout)

		publisher := Publisher{config}
		success := publisher.Post([]Event{{"sourceId": "foo"}})
		if success != set.want {
			t.Errorf("Set #%d. Post method should return %t", i, set.want)
		}
	}
}

func TestPublisher_Post_ResponseStatus(t *testing.T) {
	sets := []struct {
		response int
		want     bool
	}{
		{101, false},
		{102, false},

		{200, false},
		{201, false},
		{202, true},
		{203, false},
		{204, false},
		{205, false},
		{206, false},
		{207, false},
		{208, false},
		{226, false},

		{300, false},
		{301, false},
		{302, false},
		{303, false},
		{304, false},
		{305, false},
		{307, false},
		{308, false},

		{400, false},
		{401, false},
		{402, false},
		{403, false},
		{404, false},
		{405, false},
		{407, false},
		{408, false},
		{409, false},
		{410, false},
		{411, false},
		{412, false},
		{413, false},
		{414, false},
		{411, false},
		{415, false},
		{416, false},
		{417, false},
		{418, false},
		{421, false},
		{422, false},
		{423, false},
		{424, false},
		{426, false},
		{428, false},
		{429, false},
		{431, false},
		{444, false},
		{451, false},
		{499, false},

		{500, false},
		{501, false},
		{502, false},
		{503, false},
		{504, false},
		{505, false},
		{506, false},
		{507, false},
		{508, false},
		{511, false},
		{599, false},
	}

	for i, set := range sets {
		mockServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			w.WriteHeader(set.response)
		}))
		defer mockServer.Close()

		config := NewConfig()
		config.Url = mockServer.URL

		publisher := Publisher{config}
		success := publisher.Post([]Event{{"sourceId": "foo"}})
		if success != set.want {
			t.Errorf("Set #%d. Post method should return %t", i, set.want)
		}
	}
}
