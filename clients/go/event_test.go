package client

import (
	"reflect"
	"regexp"
	"strconv"
	"testing"
)

func TestEvent_Enrich_PopulatesWithMissingAttributes(t *testing.T) {
	config := NewConfig()
	config.SourceId = "mobile"

	sets := []Event{
		{
			"eventName": "foo",
		},
		{
			"eventName": "foo",
			"timestamp": 123,
		},
		{
			"eventName": "foo",
			"sourceId":  "baz",
			"timestamp": 123,
		},
	}

	for i, event := range sets {
		event.enrich(config)

		if event["eventName"] == nil {
			t.Errorf("Set #%d: eventName should be present for event %v", i, event)
		}
		if event["sourceId"] == nil {
			t.Errorf("Set #%d: sourceId should be present for event %v", i, event)
		}
		if event["timestamp"] == nil {
			t.Errorf("Set #%d: timestamp should be present for event %v", i, event)
		}
	}
}

func TestEvent_Enrich_PreservesExistingAttributeValues(t *testing.T) {
	config := NewConfig()
	config.SourceId = "devops.bar"

	event := Event{
		"eventName": "foo",
		"sourceId":  "baz",
		"timestamp": 123,
	}
	event.enrich(config)

	if event["eventName"] != "foo" {
		t.Errorf("eventName should be preserved. Got %v", event["eventName"])
	}
	if event["sourceId"] != "baz" {
		t.Errorf("sourceId should be preserved. Got %v", event["sourceId"])
	}
	if event["timestamp"] != 123 {
		t.Errorf("timestamp should be preserved. Got %v", event["timestamp"])
	}
}

func TestEvent_Enrich_PreservesAdditionalExistingAttributeValues(t *testing.T) {
	config := NewConfig()
	config.SourceId = "devops.bar"

	event := Event{
		"eventName": "中文",
		"sourceId":  "baz",
		"timestamp": 123,
		"color":     "red",
		"price":     100.59,
	}
	event.enrich(config)

	if event["eventName"] != "中文" {
		t.Errorf("eventName should be preserved. Got %v", event["eventName"])
	}
	if event["sourceId"] != "baz" {
		t.Errorf("sourceId should be preserved. Got %v", event["sourceId"])
	}
	if event["timestamp"] != 123 {
		t.Errorf("timestamp should be preserved. Got %v", event["timestamp"])
	}
	if event["color"] != "red" {
		t.Errorf("color should be preserved. Got %v", event["color"])
	}
	if event["price"] != 100.59 {
		t.Errorf("price should be preserved. Got %v", event["price"])
	}
}

func TestEvent_Enrich_PopulatesWithCorrectTimestamp(t *testing.T) {
	config := NewConfig()
	config.SourceId = "devops.bar"

	event := Event{
		"eventName": "中文",
	}
	event.enrich(config)

	stringTimestamp := strconv.FormatInt(event["timestamp"].(int64), 10)
	if len(stringTimestamp) != 13 {
		t.Errorf("Event's timestamp should be correct one. Got %v", event["timestamp"])
	}
}

func TestEvent_Enrich_PopulatesWithCorrectSourceId(t *testing.T) {
	config := NewConfig()
	config.SourceId = "devops.bar"

	event := Event{
		"eventName": "John",
	}
	event.enrich(config)

	if event["sourceId"] != "devops.bar" {
		t.Errorf("Event's sourceId should be %q, got %q", "devops.bar", event["sourceId"])
	}
}

func TestEvent_Validate_DoesNotChangeGivenData(t *testing.T) {
	event := Event{
		"eventName": "John",
		"sourceId":  "foo",
		"timestamp": 1479988864057,
		"color":     "blue",
		"price":     123.890,
	}
	original := Event{}
	for k, v := range event {
		original[k] = v
	}

	event.validate()

	if !reflect.DeepEqual(original, event) {
		t.Errorf("Event should remain the same. Event: %+v", event)
	}
}

func TestEvent_Validate_InvalidData(t *testing.T) {
	sets := []struct {
		event      Event
		msgPattern string
	}{
		{
			Event{},
			"sourceId.*required",
		},
		{
			Event{"sourceId": "foo", "eventName": "Johnny"},
			"timestamp.*required",
		},
		{
			Event{"sourceId": "foo", "timestamp": int64(1479988864057)},
			"eventName.*required",
		},
		{
			Event{"sourceId": "foo", "timestamp": Timestamp()},
			"eventName.*required",
		},
		{
			Event{"sourceId": "", "eventName": "bar", "timestamp": int64(1479988864057)},
			"sourceId.*blank",
		},
		{
			Event{"sourceId": " ", "eventName": "bar", "timestamp": int64(1479988864057)},
			"sourceId.*blank",
		},
		{
			Event{"sourceId": "foo", "eventName": "", "timestamp": int64(1479988864057)},
			"eventName.*blank",
		},
		{
			Event{"sourceId": "foo", "eventName": "     ", "timestamp": int64(1479988864057)},
			"eventName.*blank",
		},
		{
			Event{"sourceId": 123, "eventName": "bar", "timestamp": int64(1479988864057)},
			"sourceId.*type",
		},
		{
			Event{"sourceId": [1]string{"hola"}, "eventName": "bar", "timestamp": int64(1479988864057)},
			"sourceId.*type",
		},
		{
			Event{"sourceId": map[string]string{"key": "hola"}, "eventName": "bar", "timestamp": Timestamp()},
			"sourceId.*type",
		},
		{
			Event{"sourceId": "foo", "eventName": [1]string{"bar"}, "timestamp": int64(1479988864057)},
			"eventName.*type",
		},
		{
			Event{"sourceId": "foo", "eventName": map[string]string{"key": "bar"}, "timestamp": int64(1479988864057)},
			"eventName.*type",
		},
		{
			Event{"sourceId": "foo", "eventName": "bar", "timestamp": "1234456"},
			"timestamp.*type",
		},
		{
			Event{"sourceId": "foo", "eventName": "bar", "timestamp": int64(-1)},
			"timestamp.*less",
		},
	}

	for i, v := range sets {
		err := v.event.validate()
		if err == nil {
			t.Errorf("Set #%d. Expected error validation for Event %+v", i, v.event)
		}
		if !regexp.MustCompile(v.msgPattern).MatchString(err.Error()) {
			t.Errorf("Set #%d. Expected error message contains %q, got %q", i, v.msgPattern, err.Error())
		}
	}
}

func TestEvent_Validate_CorrectData(t *testing.T) {
	sets := []Event{
		{
			"sourceId":  "foo",
			"eventName": "Johnny",
			"timestamp": int64(1479988864057),
		},
		{
			"sourceId":  "foo",
			"eventName": "Имя События",
			"timestamp": Timestamp(),
		},
		{
			"sourceId":  "sooome-verryyyyyyyy-loooooooong-striiiiiiiing-i-mean-verrrrrrrrrrry-loooooooooooooooooooog",
			"eventName": "sooome-verryyyyyyyy-loooooooong-striiiiiiiing-i-mean-verrrrrrrrrrry-loooooooooooooooooooog",
			"timestamp": Timestamp(),
		},
		{
			"sourceId":  "foo",
			"eventName": "Johnny",
			"timestamp": int64(0),
		},
		{
			"sourceId":  "foo",
			"eventName": "Johnny",
			"timestamp": int64(15),
		},
		{
			"sourceId":  "foo.bar",
			"eventName": "Johnny",
			"timestamp": int64(15),
		},
		{
			"sourceId":  "    foo     ",
			"eventName": "baz.test.test.test.test",
			"timestamp": int64(15),
		},
	}

	for i, event := range sets {
		err := event.validate()
		if err != nil {
			t.Errorf("Set #%d. Expected NO error validation for Event %+v. Got Error: %v", i, event, err)
		}
	}
}
