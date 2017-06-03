package client

type ConfigValidationError struct {
	Message string
}

type EventValidationError struct {
	Message string
}

func (e ConfigValidationError) Error() string {
	return e.Message
}

func (e EventValidationError) Error() string {
	return e.Message
}
