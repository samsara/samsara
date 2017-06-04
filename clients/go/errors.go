package client

// Configuration validation error exception.
type ConfigValidationError struct {
	Message string
}

// Event validation error exception.
type EventValidationError struct {
	Message string
}

// Returns error message.
func (e ConfigValidationError) Error() string {
	return e.Message
}

// Returns error message.
func (e EventValidationError) Error() string {
	return e.Message
}
