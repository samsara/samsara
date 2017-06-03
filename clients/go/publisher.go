package client

import (
	"bytes"
	"compress/gzip"
	"encoding/json"
	"net/http"
	"strconv"
	"strings"
	"time"
)

const PUBLISHED_TIMESTAMP_HEADER = "X-Samsara-publishedTimestamp"
const API_PATH = "/v1/events"

type IPublisher interface {
	Post(data []Event) bool
}

type Publisher struct {
	config Config
}

func (p *Publisher) Post(data []Event) bool {
	jsonData, err := json.Marshal(data)
	if err != nil {
		return false
	}

	var buffer *bytes.Buffer
	if p.config.compression == "gzip" {
		buffer = gzipWrap(jsonData)
	} else {
		buffer = noneWrap(jsonData)
	}

	req, _ := http.NewRequest("POST", strings.Trim(p.config.url, "/")+API_PATH, buffer)
	p.setHeaders(req)

	client := &http.Client{
		Timeout: time.Duration(p.config.send_timeout_ms) * time.Millisecond,
	}
	resp, err := client.Do(req)
	if err != nil {
		return false
	}

	if resp.StatusCode == 202 {
		return true
	} else {
		return false
	}
}

func (p *Publisher) setHeaders(req *http.Request) {
	var compression string
	if p.config.compression == "gzip" {
		compression = "gzip"
	} else {
		compression = "identity"
	}
	req.Header.Set("Accept", "application/json")
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Content-Encoding", compression)
	req.Header.Set(PUBLISHED_TIMESTAMP_HEADER, strconv.FormatInt(timestamp(), 10))
}

func gzipWrap(data []byte) *bytes.Buffer {
	var buffer bytes.Buffer
	gz := gzip.NewWriter(&buffer)
	defer gz.Close()
	gz.Write(data)
	return &buffer
}

func noneWrap(data []byte) *bytes.Buffer {
	return bytes.NewBuffer(data)
}
