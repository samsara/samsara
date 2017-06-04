package client

import (
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"reflect"
	"testing"
	"time"
)

// ==== mocks ====
type PublisherMock struct {
	fakePost func([]Event) bool
}

func (p *PublisherMock) Post(events []Event) bool {
	return p.fakePost(events)
}

// ==== end mocks ====

func TestNewClient_DoesNotCreateClientWithBadConfigGiven(t *testing.T) {
	sets := []Config{
		{},
		NewConfig(),
	}

	for i, config := range sets {
		client, err := NewClient(config)

		if err == nil {
			t.Errorf("Set %d: Client init with incorrect config should return error. Got %+v", i, err)
		}
		if client != nil {
			t.Errorf("Set %d: Client shoud not have been created. Got %+v", i, client)
		}
	}
}

func TestNewClient_CreatesClientWithCorrectConfigGiven(t *testing.T) {
	config := NewConfig()
	config.Url = "http://test.com"

	client, err := NewClient(config)

	if err != nil {
		t.Errorf("Client init with correct config should not return error. Got %+v", err)
	}
	if client == nil {
		t.Error("Client should have been created")
	}
}

func TestNewClient_CreatesClientWithFieldsProperlySet(t *testing.T) {
	config := NewConfig()
	config.Url = "http://test.com"
	config.MaxBufferSize = 555

	client, _ := NewClient(config)

	if !reflect.DeepEqual(config, client.config) {
		t.Errorf("Client's config field. Expected %v, Got %v", config, client.config)
	}
	if client.queue.Size() != 555 {
		t.Errorf("Client's queue should be of size %v, Got %v", 555, client.queue.Size())
	}
}

func TestNewClient_ShouldStartPublishingThreadDependingOnConfiguration(t *testing.T) {
	sets := []struct {
		startPublishing bool
		shouldBeSend    bool
	}{
		{
			true,
			true,
		},
		{
			false,
			false,
		},
	}

	for i, set := range sets {
		var payload interface{}

		mockServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			payload, _ = ioutil.ReadAll(r.Body)
		}))
		defer mockServer.Close()

		config := NewConfig()
		config.Url = mockServer.URL
		config.MinBufferSize = 0
		config.PublishInterval = 1
		config.StartPublishingThread = set.startPublishing

		client, err := NewClient(config)

		if client == nil {
			t.Errorf("Client should have been created, but got error: %+v", err)
		}

		time.Sleep(1 * time.Second)

		if set.shouldBeSend {
			if payload == nil {
				t.Errorf("Set: %d. Data should have been sent. Got: %+v", i, payload)
			}
		} else {
			if payload != nil {
				t.Errorf("Set: %d. Data should not have been sent. Got: %+v", i, payload)
			}
		}
	}
}

func TestClient_PublishEvents_ShouldPostEventsToIngestionAPI(t *testing.T) {
	mockServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		payload, err := ioutil.ReadAll(r.Body)
		if payload == nil || err != nil {
			t.Errorf("Data should have been sent. Got error: %+v", err)
		}
	}))
	defer mockServer.Close()

	config := NewConfig()
	config.Url = mockServer.URL
	client, _ := NewClient(config)

	data := []Event{
		{
			"eventName": "foo",
			"sourceId":  "baz",
			"timestamp": 123,
		},
		{
			"eventName": "событие",
			"sourceId":  "loooooooooooooooooooooooooooooooooooooooooooong string",
		},
	}
	client.PublishEvents(data)
}

func TestClient_PublishEvents_ReturnsFalseInCasePostFailed(t *testing.T) {
	config := NewConfig()
	config.Url = "http://test.com"
	client, _ := NewClient(config)
	client.publisher = &PublisherMock{fakePost: func(events []Event) bool { return false }}

	result, _ := client.PublishEvents([]Event{{"eventName": "foo", "sourceId": "baz"}})
	if result != false {
		t.Errorf("PublishEvents should have returned false since post failed. Got: %+v", result)
	}
}

func TestClient_PublishEvents_ReturnsTrueInCasePostSucceeded(t *testing.T) {
	config := NewConfig()
	config.Url = "http://test.com"
	client, _ := NewClient(config)
	client.publisher = &PublisherMock{fakePost: func(events []Event) bool { return true }}

	result, _ := client.PublishEvents([]Event{{"eventName": "foo", "sourceId": "baz"}})
	if result != true {
		t.Errorf("PublishEvents should have returned true since server responded with 200. Got: %+v", result)
	}
}

func TestClient_PublishEvents_DoesNotPostToIngestionIfOneOfTheEventsIsInvalid(t *testing.T) {
	config := NewConfig()
	config.Url = "http://test.com"

	data := []Event{
		{
			"eventName": "foo",
		},
		{
			"eventName": "foo",
			"timestamp": "i-am-a-wrong-tiiiiime-staaamp",
		},
	}
	wasPosted := false

	client, _ := NewClient(config)
	client.publisher = &PublisherMock{
		fakePost: func(events []Event) bool {
			wasPosted = true
			return true
		},
	}

	_, err := client.PublishEvents(data)
	if err == nil {
		t.Error("Error should be raised.")
	}
	if wasPosted != false {
		t.Error("Data should not have been posted to ingestion raised.")
	}
}

func TestClient_PublishEvents_EnrichesEachEventIfNeeded(t *testing.T) {
	config := NewConfig()
	config.Url = "http://test.com"
	config.SourceId = "secret"

	data := []Event{
		{
			"eventName": "foo",
		},
		{
			"eventName": "foo",
			"timestamp": int64(123),
		},
		{
			"eventName": "foo",
			"sourceId":  "not-a-secret",
		},
		{
			"eventName": "foo",
			"sourceId":  "baz",
			"timestamp": Timestamp(),
		},
	}

	client, _ := NewClient(config)
	client.publisher = &PublisherMock{
		fakePost: func(events []Event) bool {
			if len(events) != len(data) {
				t.Errorf("We should post %d events, but posted %d", len(data), len(events))
			}
			if events[0]["sourceId"] != "secret" {
				t.Errorf("Event was not enriched, Got %+v", events[0])
			}
			if events[1]["timestamp"] != int64(123) {
				t.Errorf("Event's timestamp was rewritten! Got %+v", events[1])
			}
			if events[2]["sourceId"] != "not-a-secret" {
				t.Errorf("Event's sourceId was rewritten! Got %+v", events[2])
			}
			if !reflect.DeepEqual(events[3], data[3]) {
				t.Errorf("Event should not been touched and changed! Got %+v", events[3])
			}
			return true
		},
	}

	_, err := client.PublishEvents(data)
	if err != nil {
		t.Errorf("Error raised: %+v", err)
	}
}

func TestClient_RecordEvent_AddsEventToQueue(t *testing.T) {
	config := NewConfig()
	config.Url = "http://test.com"
	config.StartPublishingThread = false
	config.MaxBufferSize = 1
	config.MinBufferSize = 1
	client, _ := NewClient(config)

	event := Event{
		"eventName": "foo",
		"sourceId":  "baz",
		"timestamp": Timestamp(),
	}

	want := []Event{event}

	client.RecordEvent(event)
	out := client.queue.Flush()
	if !reflect.DeepEqual(want, out) {
		t.Errorf("Given event should be in a queue. Got %+v", out)
	}
}

func TestClient_RecordEvent_EnrichesEventCorrectly(t *testing.T) {
	config := NewConfig()
	config.Url = "http://test.com"
	config.StartPublishingThread = false
	config.MaxBufferSize = 1
	config.MinBufferSize = 1
	config.SourceId = "secret"
	client, _ := NewClient(config)

	event := Event{
		"eventName": "foo-bar-baz",
		"timestamp": Timestamp(),
	}

	client.RecordEvent(event)
	out := client.queue.Flush()
	if out[0]["sourceId"] != "secret" {
		t.Errorf("Given event should have been enriched. Got %+v", out[0])
	}
	if out[0]["eventName"] != "foo-bar-baz" {
		t.Errorf("Existing fields shouldn't have been changed. Got %+v", out[0])
	}
}

func TestClient_RecordEvent_ValidatesEventAndDoesNotPutItToQueue(t *testing.T) {
	config := NewConfig()
	config.Url = "http://test.com"
	config.StartPublishingThread = false
	config.MaxBufferSize = 1
	config.MinBufferSize = 1
	config.SourceId = "secret"
	client, _ := NewClient(config)

	event := Event{
		"eventName": "foo-bar-baz",
		"timestamp": "incorrect-stamp",
	}

	err := client.RecordEvent(event)

	if err == nil {
		t.Error("Error should be raised.")
	}

	out := client.queue.Flush()
	if len(out) != 0 {
		t.Errorf("Queue should be empty. Got: %+v", out)
	}
}

func TestClient_Posting_PostsOnlyWhenQueueThresholdIsReached(t *testing.T) {
	sets := []struct {
		threshold int64
		wantPush  bool
	}{
		{
			1,
			true,
		},
		{
			2,
			true,
		},
		{
			3,
			false,
		},
	}

	for i, set := range sets {
		config := NewConfig()
		config.Url = "https://test.com"
		config.MaxBufferSize = 5
		config.MinBufferSize = set.threshold
		config.PublishInterval = 1
		config.StartPublishingThread = false

		events := []Event{
			{
				"eventName": "1",
				"sourceId":  "baz",
				"timestamp": Timestamp(),
			},
			{
				"eventName": "2",
				"sourceId":  "baz",
				"timestamp": Timestamp(),
			},
		}

		werePushed := false

		client, _ := NewClient(config)
		client.publisher = &PublisherMock{
			fakePost: func(eventsIn []Event) bool {
				werePushed = true
				return true
			},
		}

		client.RecordEvent(events[0])
		client.RecordEvent(events[1])
		go client.publishing()
		time.Sleep(1 * time.Second)

		if werePushed != set.wantPush {
			t.Errorf("Set: %d. Post to ingestion done?. Want: %t, Got: %t", i, set.wantPush, werePushed)
		}
	}
}
