package client

import (
	"time"
)

type Client struct {
	config    Config
	publisher IPublisher
	queue     *RingBuffer
}

func NewClient(config Config) (*Client, error) {
	if err := config.Validate(); err != nil {
		return nil, err
	}

	client := &Client{
		config:    config,
		publisher: &Publisher{config},
		queue:     NewRingBuffer(config.max_buffer_size),
	}

	if config.start_publishing_thread {
		go client.publishing()
	}

	return client, nil
}

func (c *Client) Publish_events(events []Event) (bool, error) {
	for _, event := range events {
		event.enrich(c.config)
		if err := event.validate(); err != nil {
			return false, err
		}
	}

	return c.publisher.Post(events), nil
}

func (c *Client) Record_event(event Event) error {
	event.enrich(c.config)
	if err := event.validate(); err != nil {
		return err
	}
	c.queue.Push(event)
	return nil
}

func (c *Client) publishing() {
	for {
		if c.queue.Count() >= c.config.min_buffer_size {
			c.queue.Flush(c.publisher.Post)
		}
		time.Sleep(time.Duration(c.config.publish_interval_ms) * time.Millisecond)
	}
}
