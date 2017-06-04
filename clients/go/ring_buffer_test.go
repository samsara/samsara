package client

import (
	"reflect"
	"sync"
	"testing"
	"time"
)

func TestRingBuffer_New(t *testing.T) {
	sets := []int64{
		0,
		1,
		3,
		10,
		5,
		5000,
		15000,
		14000000,
	}
	for i, size := range sets {
		rb := NewRingBuffer(size)
		if rb.Size() != size {
			t.Errorf("SET #%d: Size should be: %d,\n Got: %d", i, size, rb.Size())
		}
		if rb.Count() != 0 {
			t.Errorf("SET #%d: Count should be 0,\n Got: %d", i, rb.Count())
		}
		if rb.IsEmpty() != true {
			t.Errorf("SET #%d: Ring buffer should be empty", i)
		}
	}
}

func TestRingBuffer_IsFull_ElementsCountEqualToCapacity(t *testing.T) {
	rb := NewRingBuffer(2)
	rb.Push(Event{"1": "a"})
	rb.Push(Event{"1": "b"})
	if rb.IsFull() != true {
		t.Error("Ring buffer should be full")
	}
}

func TestRingBuffer_IsFull_ElementsCountGreaterThanCapacity(t *testing.T) {
	rb := NewRingBuffer(2)
	rb.Push(Event{"1": "a"})
	rb.Push(Event{"1": "b"})
	rb.Push(Event{"1": "c"})
	if rb.IsFull() != true {
		t.Error("Ring buffer should be full")
	}
}

func TestRingBuffer_IsFull_ElementsCountLessThanCapacity(t *testing.T) {
	rb := NewRingBuffer(2)
	rb.Push(Event{"1": "a"})
	if rb.IsFull() != false {
		t.Error("Ring buffer should not be full")
	}
}

func TestRingBuffer_IsFull_EmptyBuffer(t *testing.T) {
	rb := NewRingBuffer(2)
	if rb.IsFull() != false {
		t.Error("Ring buffer should not be full")
	}
}

func TestRingBuffer_IsEmpty_ZeroElements(t *testing.T) {
	rb := NewRingBuffer(3)
	if rb.IsEmpty() != true {
		t.Error("Ring buffer should be empty")
	}
}

func TestRingBuffer_IsEmpty_FullBuffer(t *testing.T) {
	rb := NewRingBuffer(3)
	rb.Push(Event{"1": "a"})
	rb.Push(Event{"1": "b"})
	rb.Push(Event{"1": "c"})
	if rb.IsEmpty() != false {
		t.Error("Ring buffer should not be empty")
	}
}

func TestRingBuffer_IsEmpty_OneElement(t *testing.T) {
	rb := NewRingBuffer(3)
	rb.Push(Event{"1": "a"})
	if rb.IsEmpty() != false {
		t.Error("Ring buffer should not be empty")
	}
}

func TestRingBuffer_IsEmpty_SeveralElementsButNotFull(t *testing.T) {
	rb := NewRingBuffer(3)
	rb.Push(Event{"1": "a"})
	rb.Push(Event{"1": "b"})
	if rb.IsEmpty() != false {
		t.Error("Ring buffer should not be empty")
	}
}

func TestRingBuffer_Push_AddsElement(t *testing.T) {
	rb := NewRingBuffer(5)
	rb.Push(Event{"1": "a"})
	if rb.Count() != 1 {
		t.Errorf("Ring buffer should have only 1 element. Got %d elements", rb.Count())
	}
}

func TestRingBuffer_Push_DoesNothingIfHasInitialZeroCapacity(t *testing.T) {
	rb := NewRingBuffer(0)
	rb.Push(Event{"1": "a"})
	rb.Push(Event{"1": "b"})
	if rb.Count() != 0 {
		t.Errorf("Ring buffer should have no elements. Got %d elements", rb.Count())
	}
}

func TestRingBuffer_Push_AddsElementsNextToEachOther(t *testing.T) {
	rb := NewRingBuffer(5)

	data := []Event{
		{"1": "a"},
		{"1": "b"},
		{"1": "c"},
	}
	for _, event := range data {
		rb.Push(event)
	}

	result := rb.Flush()

	if !reflect.DeepEqual(result, data) {
		t.Errorf("Expected %v, Got %v", data, result)
	}
}

func TestRingBuffer_Push_CanTakeMoreElementsThanInitialCapacity(t *testing.T) {
	rb := NewRingBuffer(5)

	data := []Event{
		{"1": "a"},
		{"1": "b"},
		{"1": "c"},
		{"1": "d"},
		{"1": "e"},
		{"1": "f"},
	}
	for _, event := range data {
		rb.Push(event)
	}

	if rb.Count() != 5 {
		t.Errorf("Count of elements should be %d, Got %d", 5, rb.Count())
	}
}

func TestRingBuffer_Push_OverwritesElementsThatExceedInitialCapacity(t *testing.T) {
	rb := NewRingBuffer(5)

	data := []Event{
		{"1": "a"},
		{"1": "b"},
		{"1": "c"},
		{"1": "d"},
		{"1": "e"},
		{"1": "f"},
	}
	for _, event := range data {
		rb.Push(event)
	}

	expected := []Event{
		{"1": "b"},
		{"1": "c"},
		{"1": "d"},
		{"1": "e"},
		{"1": "f"},
	}
	result := rb.Flush()

	if !reflect.DeepEqual(expected, result) {
		t.Errorf("Expected %v, Got %v", expected, result)
	}
}

func TestRingBuffer_Flush_ReturnsOnlyRealElementsThatAreNotNil(t *testing.T) {
	for _, n := range [6]int{0, 1, 2, 3, 4, 5} {
		rb := NewRingBuffer(5)

		for i := 0; i < n; i++ {
			rb.Push(Event{"1": "a"})
		}
		result := rb.Flush()

		if len(result) != n {
			t.Errorf("Expected result size %d, Got %d", n, len(result))
		}
	}
}

func TestRingBuffer_Flush_ReturnsSnapshotInFIFO(t *testing.T) {
	sets := []struct {
		capacity int64
		data     []Event
		want     []Event
	}{
		{
			0,
			[]Event{},
			[]Event{},
		},
		{
			0,
			[]Event{{"1": "a"}},
			[]Event{},
		},
		{
			1,
			[]Event{},
			[]Event{},
		},
		{
			1,
			[]Event{{"1": "a"}},
			[]Event{{"1": "a"}},
		},
		{
			1,
			[]Event{{"1": "a"}, {"2": "b"}},
			[]Event{{"2": "b"}},
		},
		{
			1,
			[]Event{{"1": "a"}, {"2": "b"}, {"3": "c"}},
			[]Event{{"3": "c"}},
		},
		{
			2,
			[]Event{},
			[]Event{},
		},
		{
			2,
			[]Event{{"1": "a"}},
			[]Event{{"1": "a"}},
		},
		{
			2,
			[]Event{{"1": "a"}, {"2": "b"}},
			[]Event{{"1": "a"}, {"2": "b"}},
		},
		{
			2,
			[]Event{{"1": "a"}, {"2": "b"}, {"3": "c"}},
			[]Event{{"2": "b"}, {"3": "c"}},
		},
		{
			2,
			[]Event{{"1": "a"}, {"2": "b"}, {"3": "c"}, {"4": "d"}},
			[]Event{{"3": "c"}, {"4": "d"}},
		},
		{
			2,
			[]Event{{"1": "a"}, {"2": "b"}, {"3": "c"}, {"4": "d"}, {"5": "e"}},
			[]Event{{"4": "d"}, {"5": "e"}},
		},
		{
			3,
			[]Event{},
			[]Event{},
		},
		{
			3,
			[]Event{{"1": "a"}},
			[]Event{{"1": "a"}},
		},
		{
			3,
			[]Event{{"1": "a"}, {"2": "b"}},
			[]Event{{"1": "a"}, {"2": "b"}},
		},
		{
			3,
			[]Event{{"1": "a"}, {"2": "b"}, {"3": "c"}},
			[]Event{{"1": "a"}, {"2": "b"}, {"3": "c"}},
		},
		{
			3,
			[]Event{{"1": "a"}, {"2": "b"}, {"3": "c"}, {"4": "d"}},
			[]Event{{"2": "b"}, {"3": "c"}, {"4": "d"}},
		},
		{
			3,
			[]Event{{"1": "a"}, {"2": "b"}, {"3": "c"}, {"4": "d"}, {"5": "e"}},
			[]Event{{"3": "c"}, {"4": "d"}, {"5": "e"}},
		},
		{
			3,
			[]Event{{"1": "a"}, {"2": "b"}, {"3": "c"}, {"4": "d"}, {"5": "e"}, {"6": "f"}},
			[]Event{{"4": "d"}, {"5": "e"}, {"6": "f"}},
		},
		{
			3,
			[]Event{{"1": "a"}, {"2": "b"}, {"3": "c"}, {"4": "d"}, {"5": "e"}, {"6": "f"}, {"7": "g"}},
			[]Event{{"5": "e"}, {"6": "f"}, {"7": "g"}},
		},
		{
			3,
			[]Event{{"1": "a"}, {"2": "b"}, {"3": "c"}, {"4": "d"}, {"5": "e"}, {"6": "f"}, {"7": "g"}, {"8": "h"}},
			[]Event{{"6": "f"}, {"7": "g"}, {"8": "h"}},
		},
	}

	for i, v := range sets {
		rb := NewRingBuffer(v.capacity)

		for _, event := range v.data {
			rb.Push(event)
		}

		result := rb.Flush()

		if !reflect.DeepEqual(v.want, result) {
			t.Errorf("Set #%d. Expected %v, Got %v", i, v.want, result)
		}
	}
}

func TestRingBuffer_Flush_DeletesDataSnapshotIfNoConsumerFunction(t *testing.T) {
	rb := NewRingBuffer(5)
	rb.Push(Event{"1": "a"})
	rb.Push(Event{"1": "b"})

	rb.Flush()
	result := rb.Flush()

	if !reflect.DeepEqual([]Event{}, result) {
		t.Errorf("Expected %v, Got %v", []Event{}, result)
	}
}

func TestRingBuffer_Flush_PassesDataSnapshotConsumerFunction(t *testing.T) {
	rb := NewRingBuffer(5)

	input := []Event{
		{"1": "a"},
		{"1": "b"},
	}
	for _, event := range input {
		rb.Push(event)
	}

	consumer := func(data []Event) bool {
		if !reflect.DeepEqual(input, data) {
			t.Errorf("Expected %v, Got %v", input, data)
		}
		return true
	}

	rb.Flush(consumer)
}

func TestRingBuffer_Flush_DeletesDataSnapshotIfConsumerFunctionSucceeds(t *testing.T) {
	rb := NewRingBuffer(5)

	rb.Push(Event{"1": "a"})
	rb.Push(Event{"1": "b"})

	rb.Flush(func(data []Event) bool { return true })
	result := rb.Flush()

	if !reflect.DeepEqual([]Event{}, result) {
		t.Errorf("Expected %v, Got %v", []Event{}, result)
	}
}

func TestRingBuffer_Flush_PreservesDataSnapshotIfConsumerFunctionDidNotSucceed(t *testing.T) {
	rb := NewRingBuffer(5)

	input := []Event{
		{"1": "a"},
		{"1": "b"},
	}
	for _, event := range input {
		rb.Push(event)
	}

	rb.Flush(func(data []Event) bool { return false })
	result := rb.Flush()

	if !reflect.DeepEqual(input, result) {
		t.Errorf("Expected %v, Got %v", input, result)
	}
}

func TestRingBuffer_Flush_PreservesStorageCapacityAfterDataDeletion(t *testing.T) {
	rb := NewRingBuffer(15)

	rb.Push(Event{"1": "a"})
	rb.Push(Event{"1": "b"})
	rb.Push(Event{"1": "c"})

	rb.Flush()

	if len(rb.buffer) != 15 {
		t.Errorf("Expected %v, Got %v", 15, len(rb.buffer))
	}

	rb.Flush()

	if len(rb.buffer) != 15 {
		t.Errorf("Expected %v, Got %v", 15, len(rb.buffer))
	}
}

func TestRingBuffer_Push_And_Flush_OnZeroCapacity(t *testing.T) {
	rb := NewRingBuffer(0)
	rb.Push(Event{"1": "a"})
	rb.Push(Event{"1": "b"})

	var result []Event
	expected := []Event{}

	result = rb.Flush()
	if !reflect.DeepEqual(expected, result) {
		t.Errorf("Final flush. Expected %v, Got %v", expected, result)
	}

	result = rb.Flush()
	if !reflect.DeepEqual(expected, result) {
		t.Errorf("Final flush. Expected %v, Got %v", expected, result)
	}

	result = rb.Flush()
	if !reflect.DeepEqual(expected, result) {
		t.Errorf("Final flush. Expected %v, Got %v", expected, result)
	}
}

// -====== Concurrent access check ======-

// - when flush consumer function returns FALSE after processing -

func TestRingBuffer_Flush_CA_False_OneElementWasPushedToFullBufferAfterFlush(t *testing.T) {
	var wg sync.WaitGroup
	rb := NewRingBuffer(4)
	rb.Push(Event{"1": "a"})
	rb.Push(Event{"1": "b"})
	rb.Push(Event{"1": "c"})
	rb.Push(Event{"1": "d"})

	if rb.IsFull() != true {
		t.Error("Buffer should be full")
	}

	consumer := func(data []Event) bool {
		expected := []Event{{"1": "a"}, {"1": "b"}, {"1": "c"}, {"1": "d"}}

		if !reflect.DeepEqual(expected, data) {
			t.Errorf("Inside consumer function was expected data %v, Got %v", expected, data)
		}

		wg.Add(1)
		go func() {
			defer wg.Done()
			rb.Push(Event{"1": "f"})
		}()

		wg.Wait()
		return false
	}

	rb.Flush(consumer)

	result := rb.Flush()
	expected := []Event{{"1": "b"}, {"1": "c"}, {"1": "d"}, {"1": "f"}}
	if !reflect.DeepEqual(expected, result) {
		t.Errorf("Final flush. Expected %v, Got %v", expected, result)
	}
}

func TestRingBuffer_Flush_CA_False_SeveralElementsWerePushedToFullBufferAfterFlush(t *testing.T) {
	var wg sync.WaitGroup
	rb := NewRingBuffer(4)
	rb.Push(Event{"1": "a"})
	rb.Push(Event{"1": "b"})
	rb.Push(Event{"1": "c"})
	rb.Push(Event{"1": "d"})

	if rb.IsFull() != true {
		t.Error("Buffer should be full")
	}

	consumer := func(data []Event) bool {
		expected := []Event{{"1": "a"}, {"1": "b"}, {"1": "c"}, {"1": "d"}}

		if !reflect.DeepEqual(expected, data) {
			t.Errorf("Inside consumer function was expected data %v, Got %v", expected, data)
		}

		wg.Add(1)
		go func() {
			defer wg.Done()
			rb.Push(Event{"1": "f"})
			rb.Push(Event{"1": "e"})
			rb.Push(Event{"1": "g"})
		}()

		wg.Wait()
		return false
	}

	rb.Flush(consumer)

	result := rb.Flush()
	expected := []Event{{"1": "d"}, {"1": "f"}, {"1": "e"}, {"1": "g"}}
	if !reflect.DeepEqual(expected, result) {
		t.Errorf("Final flush. Expected %v, Got %v", expected, result)
	}
}

func TestRingBuffer_Flush_CA_False_EmptyBuffer(t *testing.T) {
	var wg sync.WaitGroup
	rb := NewRingBuffer(2)

	if rb.IsEmpty() != true {
		t.Error("Buffer should be empty")
	}

	consumer := func(data []Event) bool {
		expected := []Event{}

		if !reflect.DeepEqual(expected, data) {
			t.Errorf("Inside consumer function was expected data %v, Got %v", expected, data)
		}

		wg.Add(1)
		go func() {
			defer wg.Done()
			rb.Push(Event{"1": "f"})
			rb.Push(Event{"1": "e"})
			rb.Push(Event{"1": "g"})
		}()

		wg.Wait()
		return false
	}

	rb.Flush(consumer)

	result := rb.Flush()
	expected := []Event{{"1": "e"}, {"1": "g"}}
	if !reflect.DeepEqual(expected, result) {
		t.Errorf("Final flush. Expected %v, Got %v", expected, result)
	}
}

func TestRingBuffer_Flush_CA_False_OneElementWasPushedToFullBufferBy2ThreadsEachAfterFlush(t *testing.T) {
	var wg sync.WaitGroup
	rb := NewRingBuffer(4)
	rb.Push(Event{"1": "a"})
	rb.Push(Event{"1": "b"})
	rb.Push(Event{"1": "c"})
	rb.Push(Event{"1": "d"})

	if rb.IsFull() != true {
		t.Error("Buffer should be empty")
	}

	consumer := func(data []Event) bool {
		expected := []Event{{"1": "a"}, {"1": "b"}, {"1": "c"}, {"1": "d"}}

		if !reflect.DeepEqual(expected, data) {
			t.Errorf("Inside consumer function was expected data %v, Got %v", expected, data)
		}

		wg.Add(2)
		go func() {
			defer wg.Done()
			rb.Push(Event{"1": "f"})
		}()
		go func() {
			time.Sleep(time.Duration(50) * time.Microsecond)
			defer wg.Done()
			rb.Push(Event{"1": "e"})
		}()

		wg.Wait()
		return false
	}

	rb.Flush(consumer)

	result := rb.Flush()
	expected := []Event{{"1": "c"}, {"1": "d"}, {"1": "f"}, {"1": "e"}}
	if !reflect.DeepEqual(expected, result) {
		t.Errorf("Final flush. Expected %v, Got %v", expected, result)
	}
}

func TestRingBuffer_Flush_CA_False_OneElementWasPushedToNotFullBufferAfterFlush(t *testing.T) {
	var wg sync.WaitGroup
	rb := NewRingBuffer(4)
	rb.Push(Event{"1": "a"})
	rb.Push(Event{"1": "b"})
	rb.Push(Event{"1": "c"})

	if rb.IsFull() != false {
		t.Error("Buffer should not be full")
	}

	consumer := func(data []Event) bool {
		expected := []Event{{"1": "a"}, {"1": "b"}, {"1": "c"}}

		if !reflect.DeepEqual(expected, data) {
			t.Errorf("Inside consumer function was expected data %v, Got %v", expected, data)
		}

		wg.Add(1)
		go func() {
			defer wg.Done()
			rb.Push(Event{"1": "f"})
		}()

		wg.Wait()
		return false
	}

	rb.Flush(consumer)

	result := rb.Flush()
	expected := []Event{{"1": "a"}, {"1": "b"}, {"1": "c"}, {"1": "f"}}
	if !reflect.DeepEqual(expected, result) {
		t.Errorf("Final flush. Expected %v, Got %v", expected, result)
	}
}

func TestRingBuffer_Flush_CA_False_TwoElementsWerePushedToNotFullBufferAfterFlush(t *testing.T) {
	var wg sync.WaitGroup
	rb := NewRingBuffer(4)
	rb.Push(Event{"1": "a"})
	rb.Push(Event{"1": "b"})
	rb.Push(Event{"1": "c"})

	if rb.IsFull() != false {
		t.Error("Buffer should not be full")
	}

	consumer := func(data []Event) bool {
		expected := []Event{{"1": "a"}, {"1": "b"}, {"1": "c"}}

		if !reflect.DeepEqual(expected, data) {
			t.Errorf("Inside consumer function was expected data %v, Got %v", expected, data)
		}

		wg.Add(1)
		go func() {
			defer wg.Done()
			rb.Push(Event{"1": "f"})
			rb.Push(Event{"1": "e"})
		}()

		wg.Wait()
		return false
	}

	rb.Flush(consumer)

	result := rb.Flush()
	expected := []Event{{"1": "b"}, {"1": "c"}, {"1": "f"}, {"1": "e"}}
	if !reflect.DeepEqual(expected, result) {
		t.Errorf("Final flush. Expected %v, Got %v", expected, result)
	}
}

func TestRingBuffer_Flush_CA_False_ElementsWerePushedToNotFullBufferAfterFlushAndItIsStillNotFull(t *testing.T) {
	var wg sync.WaitGroup
	rb := NewRingBuffer(7)
	rb.Push(Event{"1": "a"})
	rb.Push(Event{"1": "b"})
	rb.Push(Event{"1": "c"})

	if rb.IsFull() != false {
		t.Error("Buffer should not be full")
	}

	consumer := func(data []Event) bool {
		expected := []Event{{"1": "a"}, {"1": "b"}, {"1": "c"}}

		if !reflect.DeepEqual(expected, data) {
			t.Errorf("Inside consumer function was expected data %v, Got %v", expected, data)
		}

		wg.Add(1)
		go func() {
			defer wg.Done()
			rb.Push(Event{"1": "f"})
			rb.Push(Event{"1": "e"})
		}()

		wg.Wait()
		return false
	}

	rb.Flush(consumer)

	result := rb.Flush()
	expected := []Event{{"1": "a"}, {"1": "b"}, {"1": "c"}, {"1": "f"}, {"1": "e"}}
	if !reflect.DeepEqual(expected, result) {
		t.Errorf("Final flush. Expected %v, Got %v", expected, result)
	}
}

func TestRingBuffer_Flush_CA_False_ElementsWereTotallyRewrittenAfterFlush(t *testing.T) {
	var wg sync.WaitGroup
	rb := NewRingBuffer(3)
	rb.Push(Event{"1": "a"})
	rb.Push(Event{"1": "b"})
	rb.Push(Event{"1": "c"})

	consumer := func(data []Event) bool {
		expected := []Event{{"1": "a"}, {"1": "b"}, {"1": "c"}}

		if !reflect.DeepEqual(expected, data) {
			t.Errorf("Inside consumer function was expected data %v, Got %v", expected, data)
		}

		wg.Add(1)
		go func() {
			defer wg.Done()
			rb.Push(Event{"1": "f"})
			rb.Push(Event{"1": "e"})
			rb.Push(Event{"1": "h"})
			rb.Push(Event{"1": "o"})
		}()

		wg.Wait()
		return false
	}

	rb.Flush(consumer)

	result := rb.Flush()
	expected := []Event{{"1": "e"}, {"1": "h"}, {"1": "o"}}
	if !reflect.DeepEqual(expected, result) {
		t.Errorf("Final flush. Expected %v, Got %v", expected, result)
	}
}

// - when flush consumer function returns FALSE after processing -

func TestRingBuffer_Flush_CA_True_OneElementSizedBufferWithPushesAfterFlush(t *testing.T) {
	var wg sync.WaitGroup
	rb := NewRingBuffer(1)
	rb.Push(Event{"1": "a"})
	rb.Push(Event{"1": "b"})

	if rb.IsFull() != true {
		t.Error("Buffer should be full")
	}

	consumer := func(data []Event) bool {
		expected := []Event{{"1": "b"}}

		if !reflect.DeepEqual(expected, data) {
			t.Errorf("Inside consumer function was expected data %v, Got %v", expected, data)
		}

		wg.Add(1)
		go func() {
			defer wg.Done()
			rb.Push(Event{"1": "f"})
			rb.Push(Event{"1": "u"})
		}()

		wg.Wait()
		return true
	}

	rb.Flush(consumer)

	result := rb.Flush()
	expected := []Event{{"1": "u"}}
	if !reflect.DeepEqual(expected, result) {
		t.Errorf("Final flush. Expected %v, Got %v", expected, result)
	}
}

func TestRingBuffer_Flush_CA_True_OneElementWasPushedToFullBufferAfterFlush(t *testing.T) {
	var wg sync.WaitGroup
	rb := NewRingBuffer(4)
	rb.Push(Event{"1": "a"})
	rb.Push(Event{"1": "b"})
	rb.Push(Event{"1": "c"})
	rb.Push(Event{"1": "d"})

	if rb.IsFull() != true {
		t.Error("Buffer should be full")
	}

	consumer := func(data []Event) bool {
		expected := []Event{{"1": "a"}, {"1": "b"}, {"1": "c"}, {"1": "d"}}

		if !reflect.DeepEqual(expected, data) {
			t.Errorf("Inside consumer function was expected data %v, Got %v", expected, data)
		}

		wg.Add(1)
		go func() {
			defer wg.Done()
			rb.Push(Event{"1": "f"})
		}()

		wg.Wait()
		return true
	}

	rb.Flush(consumer)

	result := rb.Flush()
	expected := []Event{{"1": "f"}}
	if !reflect.DeepEqual(expected, result) {
		t.Errorf("Final flush. Expected %v, Got %v", expected, result)
	}
}

func TestRingBuffer_Flush_CA_True_SeveralElementsWerePushedToFullBufferAfterFlush(t *testing.T) {
	var wg sync.WaitGroup
	rb := NewRingBuffer(4)
	rb.Push(Event{"1": "a"})
	rb.Push(Event{"1": "b"})
	rb.Push(Event{"1": "c"})
	rb.Push(Event{"1": "d"})

	if rb.IsFull() != true {
		t.Error("Buffer should be full")
	}

	consumer := func(data []Event) bool {
		expected := []Event{{"1": "a"}, {"1": "b"}, {"1": "c"}, {"1": "d"}}

		if !reflect.DeepEqual(expected, data) {
			t.Errorf("Inside consumer function was expected data %v, Got %v", expected, data)
		}

		wg.Add(1)
		go func() {
			defer wg.Done()
			rb.Push(Event{"1": "f"})
			rb.Push(Event{"1": "e"})
			rb.Push(Event{"1": "g"})
		}()

		wg.Wait()
		return true
	}

	rb.Flush(consumer)

	result := rb.Flush()
	expected := []Event{{"1": "f"}, {"1": "e"}, {"1": "g"}}
	if !reflect.DeepEqual(expected, result) {
		t.Errorf("Final flush. Expected %v, Got %v", expected, result)
	}
}

func TestRingBuffer_Flush_CA_True_EmptyBuffer(t *testing.T) {
	var wg sync.WaitGroup
	rb := NewRingBuffer(2)

	if rb.IsEmpty() != true {
		t.Error("Buffer should be empty")
	}

	consumer := func(data []Event) bool {
		expected := []Event{}

		if !reflect.DeepEqual(expected, data) {
			t.Errorf("Inside consumer function was expected data %v, Got %v", expected, data)
		}

		wg.Add(1)
		go func() {
			defer wg.Done()
			rb.Push(Event{"1": "f"})
			rb.Push(Event{"1": "e"})
			rb.Push(Event{"1": "g"})
		}()

		wg.Wait()
		return true
	}

	rb.Flush(consumer)

	result := rb.Flush()
	expected := []Event{{"1": "e"}, {"1": "g"}}
	if !reflect.DeepEqual(expected, result) {
		t.Errorf("Final flush. Expected %v, Got %v", expected, result)
	}
}

func TestRingBuffer_Flush_CA_True_SeveralElementsWerePushedToFullBufferBy2ThreadsAfterFlush(t *testing.T) {
	var wg sync.WaitGroup
	rb := NewRingBuffer(4)
	rb.Push(Event{"1": "a"})
	rb.Push(Event{"1": "b"})
	rb.Push(Event{"1": "c"})
	rb.Push(Event{"1": "d"})

	if rb.IsFull() != true {
		t.Error("Buffer should be full")
	}

	consumer := func(data []Event) bool {
		expected := []Event{{"1": "a"}, {"1": "b"}, {"1": "c"}, {"1": "d"}}

		if !reflect.DeepEqual(expected, data) {
			t.Errorf("Inside consumer function was expected data %v, Got %v", expected, data)
		}

		wg.Add(2)
		go func() {
			defer wg.Done()
			rb.Push(Event{"1": "f"})
		}()
		go func() {
			defer wg.Done()
			time.Sleep(time.Duration(50) * time.Microsecond)
			rb.Push(Event{"1": "e"})
			rb.Push(Event{"1": "h"})
		}()

		wg.Wait()
		return true
	}

	rb.Flush(consumer)

	result := rb.Flush()
	expected := []Event{{"1": "f"}, {"1": "e"}, {"1": "h"}}
	if !reflect.DeepEqual(expected, result) {
		t.Errorf("Final flush. Expected %v, Got %v", expected, result)
	}
}

func TestRingBuffer_Flush_CA_True_OneElementWasPushedToNotFullBufferAfterFlush(t *testing.T) {
	var wg sync.WaitGroup
	rb := NewRingBuffer(3)
	rb.Push(Event{"1": "a"})
	rb.Push(Event{"1": "b"})

	if rb.IsFull() != false {
		t.Error("Buffer should not be full")
	}

	consumer := func(data []Event) bool {
		expected := []Event{{"1": "a"}, {"1": "b"}}

		if !reflect.DeepEqual(expected, data) {
			t.Errorf("Inside consumer function was expected data %v, Got %v", expected, data)
		}

		wg.Add(1)
		go func() {
			defer wg.Done()
			rb.Push(Event{"1": "f"})
		}()

		wg.Wait()
		return true
	}

	rb.Flush(consumer)

	result := rb.Flush()
	expected := []Event{{"1": "f"}}
	if !reflect.DeepEqual(expected, result) {
		t.Errorf("Final flush. Expected %v, Got %v", expected, result)
	}
}

func TestRingBuffer_Flush_CA_True_TwoElementsWerePushedToNotFullBufferAfterFlush(t *testing.T) {
	var wg sync.WaitGroup
	rb := NewRingBuffer(4)
	rb.Push(Event{"1": "a"})
	rb.Push(Event{"1": "b"})
	rb.Push(Event{"1": "c"})

	if rb.IsFull() != false {
		t.Error("Buffer should not be full")
	}

	consumer := func(data []Event) bool {
		expected := []Event{{"1": "a"}, {"1": "b"}, {"1": "c"}}

		if !reflect.DeepEqual(expected, data) {
			t.Errorf("Inside consumer function was expected data %v, Got %v", expected, data)
		}

		wg.Add(1)
		go func() {
			defer wg.Done()
			rb.Push(Event{"1": "f"})
			rb.Push(Event{"1": "e"})
		}()

		wg.Wait()
		return true
	}

	rb.Flush(consumer)

	result := rb.Flush()
	expected := []Event{{"1": "f"}, {"1": "e"}}
	if !reflect.DeepEqual(expected, result) {
		t.Errorf("Final flush. Expected %v, Got %v", expected, result)
	}
}

func TestRingBuffer_Flush_CA_True_ElementsWerePushedToNotFullBufferAfterFlushAndItIsStillNotFull(t *testing.T) {
	var wg sync.WaitGroup
	rb := NewRingBuffer(7)
	rb.Push(Event{"1": "a"})
	rb.Push(Event{"1": "b"})
	rb.Push(Event{"1": "c"})

	if rb.IsFull() != false {
		t.Error("Buffer should not be full")
	}

	consumer := func(data []Event) bool {
		expected := []Event{{"1": "a"}, {"1": "b"}, {"1": "c"}}

		if !reflect.DeepEqual(expected, data) {
			t.Errorf("Inside consumer function was expected data %v, Got %v", expected, data)
		}

		wg.Add(1)
		go func() {
			defer wg.Done()
			rb.Push(Event{"1": "f"})
			rb.Push(Event{"1": "e"})
			rb.Push(Event{"1": "g"})
		}()

		wg.Wait()
		return true
	}

	rb.Flush(consumer)

	result := rb.Flush()
	expected := []Event{{"1": "f"}, {"1": "e"}, {"1": "g"}}
	if !reflect.DeepEqual(expected, result) {
		t.Errorf("Final flush. Expected %v, Got %v", expected, result)
	}
}

func TestRingBuffer_Flush_CA_True_ElementsWereTotallyRewrittenAfterFlush_OddCapacity(t *testing.T) {
	var wg sync.WaitGroup
	rb := NewRingBuffer(3)
	rb.Push(Event{"1": "a"})
	rb.Push(Event{"1": "b"})
	rb.Push(Event{"1": "c"})

	consumer := func(data []Event) bool {
		expected := []Event{{"1": "a"}, {"1": "b"}, {"1": "c"}}

		if !reflect.DeepEqual(expected, data) {
			t.Errorf("Inside consumer function was expected data %v, Got %v", expected, data)
		}

		wg.Add(1)
		go func() {
			defer wg.Done()
			rb.Push(Event{"1": "f"})
			rb.Push(Event{"1": "e"})
			rb.Push(Event{"1": "h"})
			rb.Push(Event{"1": "o"})
		}()

		wg.Wait()
		return true
	}

	rb.Flush(consumer)

	result := rb.Flush()
	expected := []Event{{"1": "e"}, {"1": "h"}, {"1": "o"}}
	if !reflect.DeepEqual(expected, result) {
		t.Errorf("Final flush. Expected %v, Got %v", expected, result)
	}
}

func TestRingBuffer_Flush_CA_True_ElementsWereTotallyRewrittenAfterFlush_EvenCapacity(t *testing.T) {
	var wg sync.WaitGroup
	rb := NewRingBuffer(2)
	rb.Push(Event{"1": "a"})
	rb.Push(Event{"1": "b"})

	consumer := func(data []Event) bool {
		expected := []Event{{"1": "a"}, {"1": "b"}}

		if !reflect.DeepEqual(expected, data) {
			t.Errorf("Inside consumer function was expected data %v, Got %v", expected, data)
		}

		wg.Add(1)
		go func() {
			defer wg.Done()
			rb.Push(Event{"1": "f"})
			rb.Push(Event{"1": "e"})
			rb.Push(Event{"1": "h"})
		}()

		wg.Wait()
		return true
	}

	rb.Flush(consumer)

	result := rb.Flush()
	expected := []Event{{"1": "e"}, {"1": "h"}}
	if !reflect.DeepEqual(expected, result) {
		t.Errorf("Final flush. Expected %v, Got %v", expected, result)
	}
}
