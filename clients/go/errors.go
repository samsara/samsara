package client

// ConfigValidationError is configuration validation error exception.
type ConfigValidationError struct {
	Message string
}

// EventValidationError event validation error exception.
type EventValidationError struct {
	Message string
}

// Error returns error message.
func (e ConfigValidationError) Error() string {
	return e.Message
}

// Error returns error message.
func (e EventValidationError) Error() string {
	return e.Message
}
