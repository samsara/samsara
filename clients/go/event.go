package client

import (
	"fmt"
	"strings"
)

// Event for Samsara Ingestion API.
type Event map[string]interface{}

// Enriches missing event properties with ones from config.
func (e Event) enrich(config Config) {
	if e["sourceId"] == nil {
		e["sourceId"] = config.SourceId
	}
	if e["timestamp"] == nil {
		e["timestamp"] = Timestamp()
	}
}

// Validates event to conform Ingestion API requirements.
func (e Event) validate() error {
	mainMsg := "Field '%s' is required and must be of %s type"
	notBlankMsg := "Field '%s' can't be blank"

	sid, ok := e["sourceId"].(string)
	if !ok {
		return EventValidationError{fmt.Sprintf(mainMsg, "sourceId", "string")}
	} else if strings.Trim(sid, " ") == "" {
		return EventValidationError{fmt.Sprintf(notBlankMsg, "sourceId")}
	}

	ts, ok := e["timestamp"].(int64)
	if !ok {
		return EventValidationError{fmt.Sprintf(mainMsg, "timestamp", "int64")}
	} else if ts < 0 {
		return EventValidationError{"timestamp can't be less then 0"}
	}

	name, ok := e["eventName"].(string)
	if !ok {
		return EventValidationError{fmt.Sprintf(mainMsg, "eventName", "string")}
	} else if strings.Trim(name, " ") == "" {
		return EventValidationError{fmt.Sprintf(notBlankMsg, "eventName")}
	}

	return nil
}
