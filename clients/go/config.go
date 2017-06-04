package client

import (
	"time"
)

// Config is Samsara SDK default configuration.
type Config struct {
	// Samsara ingestion api endpoint "http://samsara-ingestion.local/"
	Url string

	// Identifier of the source of these events.
	// OPTIONAL used only for record-event
	SourceId string

	// Start the publishing thread?
	// default = true
	StartPublishingThread bool

	// How often should the events being sent to Samsara
	// in milliseconds.
	// default = 30s
	PublishInterval uint32

	// Max size of the buffer.
	// When buffer is full older events are dropped.
	MaxBufferSize int64

	// Minimum number of events that must be in the buffer
	// before attempting to publish them.
	MinBufferSize int64

	// Network timeout for send operations
	// in milliseconds.
	// default 30s
	SendTimeout uint32

	// Should the payload be compressed?
	// allowed values :gzip, :none
	Compression string

	// NOT CURRENTLY SUPPORTED
	// Add Samsara client statistics events
	// this helps you to understand whether the
	// buffer size and publish-intervals are
	// adequately configured.
	// SendClientStats bool
}

// NewConfig creates a new Config with all default values.
// Later you may want to replace them with specific values.
func NewConfig() Config {
	config := Config{}
	config.Url = ""
	config.SourceId = ""
	config.StartPublishingThread = true
	config.PublishInterval = 30000
	config.MaxBufferSize = 10000
	config.MinBufferSize = 100
	config.SendTimeout = 30000
	config.Compression = "gzip"
	//config.SendClientStats = true
	return config
}

// Validate validates given configuration values.
func (c *Config) Validate() error {
	switch {
	case len(c.Url) == 0:
		return ConfigValidationError{"URL for Ingestion API should be specified."}
	case c.Compression != "gzip" && c.Compression != "none":
		return ConfigValidationError{"Incorrect compression option."}
	case c.PublishInterval <= 0:
		return ConfigValidationError{"Invalid interval time for Samsara client."}
	case c.MaxBufferSize < c.MinBufferSize:
		return ConfigValidationError{"maxBufferSize can not be less than minBufferSize."}
	default:
		return nil
	}
}

// Timestamp generates current timestamp.
func Timestamp() int64 {
	return time.Now().UnixNano() / 1000000
}
