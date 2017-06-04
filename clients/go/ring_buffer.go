package client

import (
	"sync"
	"sync/atomic"
)

// RingBuffer is a thread-safe ring-buffer data queue tailored for Samsara Client.
type RingBuffer struct {
	size   int64
	low    int64
	high   int64
	buffer []Event
	sync.Mutex
}

// NewRingBuffer creates new ring buffer with given capacity.
func NewRingBuffer(capacity int64) *RingBuffer {
	return &RingBuffer{
		size:   capacity,
		low:    -1,
		high:   -1,
		buffer: make([]Event, capacity),
	}
}

// Size gets overall capacity of a buffer.
func (r *RingBuffer) Size() int64 {
	return atomic.LoadInt64(&r.size)
}

// Count gets current number of items in buffer.
func (r *RingBuffer) Count() int64 {
	return atomic.LoadInt64(&r.high) - atomic.LoadInt64(&r.low)
}

// IsEmpty answers whether buffer is empty.
func (r *RingBuffer) IsEmpty() bool {
	return r.Count() == 0
}

// IsFull answers whether buffer is full.
func (r *RingBuffer) IsFull() bool {
	return r.Count() == r.Size()
}

// Push puts element into buffer.
func (r *RingBuffer) Push(event Event) {
	r.Lock()
	defer r.Unlock()

	if r.Size() != 0 {
		r.high++
		if r.Count() > r.Size() {
			r.low++
		}
		r.buffer[r.calculatePosition(r.high)] = event
	}
}

// Flush extracts all existing elements out of buffer and return them in FIFO order.
// Accepts optional function that processes data and returns success of the processing.
// Elements are deleted based on the result of consumer function and deleted always if no consumer provided.
func (r *RingBuffer) Flush(consumerFn ...func([]Event) bool) []Event {
	data, atMark := r.takeSnapshot()
	success := true
	if len(consumerFn) > 0 {
		success = consumerFn[0](data)
	}
	if success {
		r.deleteData(atMark)
	}
	return data
}

// Helper-method for calculating position in a circle.
func (r *RingBuffer) calculatePosition(pointer int64) int64 {
	return pointer % r.Size()
}

// Make a snapshot of the current buffer at a given moment in FIFO order.
func (r *RingBuffer) takeSnapshot() ([]Event, int64) {
	r.Lock()
	defer r.Unlock()

	high := r.high
	low := r.low

	result := make([]Event, 0)
	for i := low; i < high; i++ {
		result = append(result, r.buffer[r.calculatePosition(i+1)])
	}

	return result, high
}

// Removes chunk of elements that present in a snapshot out of buffer.
// Detects if the last consumed element has been overridden by new pushes.
func (r *RingBuffer) deleteData(mark int64) {
	r.Lock()
	defer r.Unlock()

	r.low = max(r.low, mark)
	r.high = max(r.high, mark)
}

// Helper. Get max of 2 int64 elements.
func max(a, b int64) int64 {
	if a > b {
		return a
	}
	return b
}
