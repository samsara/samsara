package client

import (
	"time"
)

type Config struct {
	// Samsara ingestion api endpoint "http://samsara-ingestion.local/"
	url string

	// Identifier of the source of these events.
	// OPTIONAL used only for record-event
	sourceId string

	// Start the publishing thread?
	start_publishing_thread bool

	// How often should the events being sent to Samsara
	// in milliseconds.
	// default = 30s
	publish_interval_ms uint32

	// Max size of the buffer.
	// When buffer is full older events are dropped.
	max_buffer_size int64

	// Minimum number of events that must be in the buffer
	// before attempting to publish them.
	min_buffer_size int64

	// Network timeout for send operations
	// in milliseconds.
	// default 30s
	send_timeout_ms uint32

	// Should the payload be compressed?
	// allowed values :gzip, :none
	compression string

	// NOT CURRENTLY SUPPORTED
	// Add Samsara client statistics events
	// this helps you to understand whether the
	// buffer size and publish-intervals are
	// adequately configured.
	// send_client_stats bool
}

func NewConfig() Config {
	config := Config{}
	config.url = ""
	config.sourceId = ""
	config.start_publishing_thread = true
	config.publish_interval_ms = 30000
	config.max_buffer_size = 10000
	config.min_buffer_size = 100
	config.send_timeout_ms = 30000
	config.compression = "gzip"
	//config.send_client_stats = true
	return config
}

func (c *Config) Validate() error {
	switch {
	case len(c.url) == 0:
		return ConfigValidationError{"URL for Ingestion API should be specified."}
	case c.compression != "gzip" && c.compression != "none":
		return ConfigValidationError{"Incorrect compression option."}
	case c.publish_interval_ms <= 0:
		return ConfigValidationError{"Invalid interval time for Samsara client."}
	case c.max_buffer_size < c.min_buffer_size:
		return ConfigValidationError{"max_buffer_size can not be less than min_buffer_size."}
	default:
		return nil
	}
}

func timestamp() int64 {
	return time.Now().UnixNano() / 1000000
}
