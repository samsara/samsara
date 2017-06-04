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

// PUBLISHED_TIMESTAMP_HEADER is Samsara specific HTTP Header.
const PUBLISHED_TIMESTAMP_HEADER = "X-Samsara-publishedTimestamp"

// API_PATH is Samsara Ingestion API endpoint.
const API_PATH = "/v1/events"

// IPublisher interface for publishing data.
// Used mainly for test purpopses.
type IPublisher interface {
	Post(data []Event) bool
}

// Publisher is a physical connector that Publishes messages to Samsara Ingestion API.
type Publisher struct {
	config Config
}

// Post sends message to Ingestion API.
func (p *Publisher) Post(data []Event) bool {
	jsonData, err := json.Marshal(data)
	if err != nil {
		return false
	}

	var buffer *bytes.Buffer
	if p.config.Compression == "gzip" {
		buffer = gzipWrap(jsonData)
	} else {
		buffer = noneWrap(jsonData)
	}

	req, _ := http.NewRequest("POST", strings.Trim(p.config.Url, "/")+API_PATH, buffer)
	p.setHeaders(req)

	client := &http.Client{
		Timeout: time.Duration(p.config.SendTimeout) * time.Millisecond,
	}
	resp, err := client.Do(req)
	if err != nil {
		return false
	}

	if resp.StatusCode == 202 {
		return true
	}
	return false
}

// Helper method to generate HTTP request headers for Ingestion API.
func (p *Publisher) setHeaders(req *http.Request) {
	var compression string
	if p.config.Compression == "gzip" {
		compression = "gzip"
	} else {
		compression = "identity"
	}
	req.Header.Set("Accept", "application/json")
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Content-Encoding", compression)
	req.Header.Set(PUBLISHED_TIMESTAMP_HEADER, strconv.FormatInt(Timestamp(), 10))
}

// Gzip wrapper for data.
func gzipWrap(data []byte) *bytes.Buffer {
	var buffer bytes.Buffer
	gz := gzip.NewWriter(&buffer)
	defer gz.Close()
	gz.Write(data)
	return &buffer
}

// None wrapper for data.
func noneWrap(data []byte) *bytes.Buffer {
	return bytes.NewBuffer(data)
}
